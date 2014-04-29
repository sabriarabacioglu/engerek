/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.evolveum.midpoint.prism.path.IdItemPathSegment;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Holds internal (parsed) form of midPoint-style XPath-like expressions.
 * It is able to retrieve/export these expressions from/to various forms (text, text in XML document,
 * XPathSegment list, prism path specification).
 * 
 * Assumes relative XPath, but somehow can also work with absolute XPaths.
 * 
 * @author semancik
 * @author mederly
 */
public class XPathHolder {

	private static final Trace LOGGER = TraceManager.getTrace(XPathHolder.class);
	public static final String DEFAULT_PREFIX = "c";
	private boolean absolute;
	private List<XPathSegment> segments;
	Map<String, String> explicitNamespaceDeclarations;

    // Part 1: Import from external representations.

	/**
	 * Sets "current node" Xpath.
	 */
	public XPathHolder() {
		absolute = false;
		segments = new ArrayList<XPathSegment>();
	}

	// This should not really be used. There should always be a namespace
	public XPathHolder(String xpath) {
		parse(xpath, null, null);
	}

	public XPathHolder(String xpath, Map<String, String> namespaceMap) {
		parse(xpath, null, namespaceMap);
	}

	public XPathHolder(Element domElement) {

		String xpath = ".";
		if (null != domElement) {
			xpath = domElement.getTextContent();
		}

		parse(xpath, domElement, null);
	}

	public XPathHolder(String xpath, Node domNode) {

		parse(xpath, domNode, null);
	}

    /**
     * Parses XPath-like expression (midPoint flavour), with regards to domNode from where the namespace declarations
     * (embedded in XML using xmlns attributes) are taken.
     *
     * @param xpath text representation of the XPath-like expression
     * @param domNode context (DOM node from which the expression was taken)
     * @param namespaceMap externally specified namespaces
     */
	private void parse(String xpath, Node domNode, Map<String, String> namespaceMap) {

		segments = new ArrayList<XPathSegment>();
		absolute = false;

		if (".".equals(xpath)) {
			return;
		}

		// Check for explicit namespace declarations.
		TrivialXPathParser parser = TrivialXPathParser.parse(xpath);
		explicitNamespaceDeclarations = parser.getNamespaceMap();

		// Continue parsing with Xpath without the "preamble"
		xpath = parser.getPureXPathString();

        // todo: fixme what if there's '/' within ID value we are looking for?
		String[] segArray = xpath.split("/");
		for (int i = 0; i < segArray.length; i++) {
			if (segArray[i] == null || segArray[i].isEmpty()) {
				if (i == 0) {
					absolute = true;
					// ignore the first empty segment of absolute path
					continue;
				} else {
					throw new IllegalArgumentException("XPath " + xpath + " has an empty segment (number " + i
							+ ")");
				}
			}

			String segmentStr = segArray[i];
            XPathSegment idValueFilterSegment;

            // is ID value filter attached to this segment?
            int idValuePosition = segmentStr.indexOf('[');
            if (idValuePosition >= 0) {
                if (!segmentStr.endsWith("]")) {
                    throw new IllegalArgumentException("XPath " + xpath + " has a ID segment not ending with ']': '" + segmentStr + "'");
                }
                String value = segmentStr.substring(idValuePosition+1, segmentStr.length()-1);
                segmentStr = segmentStr.substring(0, idValuePosition);
                idValueFilterSegment = new XPathSegment(value);
            } else {
                idValueFilterSegment = null;
            }

            // processing the rest (i.e. the first part) of the segment

            boolean variable = false;
            if (segmentStr.startsWith("$")) {
                // We have variable here
                variable = true;
                segmentStr = segmentStr.substring(1);
            }

            String[] qnameArray = segmentStr.split(":");
            if (qnameArray.length > 2) {
                throw new IllegalArgumentException("Unsupported format: more than one colon in XPath segment: "
                        + segArray[i]);
            }
            QName qname;
            if (qnameArray.length == 1 || qnameArray[1] == null || qnameArray[1].isEmpty()) {
                // default namespace <= empty prefix
                String namespace = findNamespace(null, domNode, namespaceMap);
                qname = new QName(namespace, qnameArray[0]);
            } else {
                String namespace = findNamespace(qnameArray[0], domNode, namespaceMap);
                if (namespace == null) {
                	LOGGER.warn("Undeclared namespace prefix '" + qnameArray[0]+"' in '"+xpath+"'. Default matching will be used to find namespace.");
//                	throw new IllegalArgumentException("Undeclared namespace prefix '"+qnameArray[0]+"'");
                }
                qname = new QName(namespace, qnameArray[1], qnameArray[0]);
            }
            if (StringUtils.isEmpty(qname.getNamespaceURI())) {
                LOGGER.debug("WARNING: Namespace was not defined for {} in xpath\n{}", new Object[] {
                        segmentStr, xpath });
            }

            segments.add(new XPathSegment(qname, variable));
            if (idValueFilterSegment != null) {
                segments.add(idValueFilterSegment);
            }
		}
	}

	private String findNamespace(String prefix, Node domNode, Map<String, String> namespaceMap) {

		String ns = null;

		if (explicitNamespaceDeclarations != null) {
			if (prefix == null) {
				ns = explicitNamespaceDeclarations.get("");
			} else {
				ns = explicitNamespaceDeclarations.get(prefix);
			}
			if (ns != null) {
				return ns;
			}
		}

		if (namespaceMap != null) {
			if (prefix == null) {
				ns = namespaceMap.get("");
			} else {
				ns = namespaceMap.get(prefix);
			}
			if (ns != null) {
				return ns;
			}
		}

		if (domNode != null) {
			if (prefix == null || prefix.isEmpty()) {
				ns = domNode.lookupNamespaceURI(null);
			} else {
				ns = domNode.lookupNamespaceURI(prefix);
			}
			if (ns != null) {
				return ns;
			}
		}

		return ns;
	}

    public XPathHolder(List<XPathSegment> segments) {
        this(segments, false);
    }

    public XPathHolder(List<XPathSegment> segments, boolean absolute) {
        this.segments = new ArrayList<XPathSegment>();
        for (XPathSegment segment : segments) {
            if (segment.getQName() != null && StringUtils.isEmpty(segment.getQName().getPrefix())) {
                QName qname = segment.getQName();
                this.segments.add(new XPathSegment(new QName(qname.getNamespaceURI(), qname.getLocalPart())));
            } else {
                this.segments.add(segment);
            }
        }

        // this.segments = segments;
        this.absolute = absolute;
    }

    public XPathHolder(QName... segmentQNames) {
        this.segments = new ArrayList<XPathSegment>();
        for (QName segmentQName : segmentQNames) {
            XPathSegment segment = new XPathSegment(segmentQName);
            this.segments.add(segment);
        }

        this.absolute = false;
    }

    public XPathHolder(ItemPath propertyPath) {
        this.segments = new ArrayList<XPathSegment>();
        for (ItemPathSegment segment: propertyPath.getSegments()) {
            XPathSegment xsegment = null;
            if (segment instanceof NameItemPathSegment) {
            	boolean variable = ((NameItemPathSegment) segment).isVariable();
                xsegment = new XPathSegment(((NameItemPathSegment)segment).getName(), variable);
            } else if (segment instanceof IdItemPathSegment) {
                xsegment = new XPathSegment(idToString(((IdItemPathSegment) segment).getId()));
            }
            this.segments.add(xsegment);
        }
        this.explicitNamespaceDeclarations = propertyPath.getNamespaceMap();

        this.absolute = false;
    }

    // Part 2: Export to external representations.

	public String getXPath() {
		StringBuilder sb = new StringBuilder();

//		addPureXpath(sb);
		sb.append(getXPathWithDeclarations());
		return sb.toString();
	}

    public String getXPathWithoutDeclarations() {
        StringBuilder sb = new StringBuilder();
		addPureXpath(sb);
        return sb.toString();
    }


    public String getXPathWithDeclarations() {
//		StringBuilder sb = new StringBuilder();
//
//		addExplicitNsDeclarations(sb);
//		addPureXpath(sb);

		return getXPathWithDeclarations(false);
//		return sb.toString();
	}
	
	public String getXPathWithDeclarations(boolean forceExplicitDeclaration) {
		StringBuilder sb = new StringBuilder();

		addExplicitNsDeclarations(sb, forceExplicitDeclaration);
		addPureXpath(sb);

		return sb.toString();
	}

	private void addPureXpath(StringBuilder sb) {
		if (!absolute && segments.isEmpty()) {
			// Empty segment list gives a "local node" XPath
			sb.append(".");
			return;
		}

		if (absolute) {
			sb.append("/");
		}

        boolean first = true;

		for (XPathSegment seg : segments) {

            if (seg.isIdValueFilter()) {

                sb.append("[");
                sb.append(seg.getValue());
                sb.append("]");

            } else {

                if (!first) {
                    sb.append("/");
                } else {
                    first = false;
                }

                if (seg.isVariable()) {
                    sb.append("$");
                }
                QName qname = seg.getQName();
                if (!StringUtils.isEmpty(qname.getPrefix())) {
                    sb.append(qname.getPrefix() + ":" + qname.getLocalPart());
                } else {
                    if (StringUtils.isNotEmpty(qname.getNamespaceURI())) {
                        String prefix = GlobalDynamicNamespacePrefixMapper.getPreferredPrefix(qname.getNamespaceURI());
                        seg.setQNamePrefix(prefix);     // hack - we modify the path segment here (only the form, not the meaning), but nevertheless it's ugly
                        sb.append(seg.getQName().getPrefix() + ":" + seg.getQName().getLocalPart());
                    } else {
                        // no namespace, no prefix
                        sb.append(qname.getLocalPart());
                    }
                }
            }
		}
	}

	public Map<String, String> getNamespaceMap() {

		Map<String, String> namespaceMap = new HashMap<String, String>();
		Iterator<XPathSegment> iter = segments.iterator();
		while (iter.hasNext()) {
			XPathSegment seg = iter.next();
			QName qname = seg.getQName();
            if (qname != null) {
                if (qname.getPrefix() != null && !qname.getPrefix().isEmpty()) {
                    namespaceMap.put(qname.getPrefix(), qname.getNamespaceURI());
                } else {
                    // Default namespace
                    // HACK. See addPureXpath method
                    namespaceMap.put(DEFAULT_PREFIX, qname.getNamespaceURI());
                }
            }
		}

		return namespaceMap;
	}

	public Element toElement(String elementNamespace, String localElementName) {
		// TODO: is this efficient?
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder loader = factory.newDocumentBuilder();
			return toElement(elementNamespace, localElementName, loader.newDocument());
		} catch (ParserConfigurationException ex) {
			throw new AssertionError("Error on creating XML document " + ex.getMessage());
		}
	}

	public Element toElement(QName elementQName, Document document) {
		return toElement(elementQName.getNamespaceURI(), elementQName.getLocalPart(), document);
	}

	public Element toElement(String elementNamespace, String localElementName, Document document) {
		Element element = document.createElementNS(elementNamespace, localElementName);
		if (!StringUtils.isBlank(elementNamespace)) {
			String prefix = GlobalDynamicNamespacePrefixMapper.getPreferredPrefix(elementNamespace);
			if (!StringUtils.isBlank(prefix)) {
				try {
					element.setPrefix(prefix);
				} catch (DOMException e) {
					throw new SystemException("Error setting XML prefix '"+prefix+"' to element {"+elementNamespace+"}"+localElementName+": "+e.getMessage(), e);
				}
			}
		}
		element.setTextContent(getXPathWithDeclarations());
		Map<String, String> namespaceMap = getNamespaceMap();
		if (namespaceMap != null) {
			for (Entry<String, String> entry : namespaceMap.entrySet()) {
				DOMUtil.setNamespaceDeclaration(element, entry.getKey(), entry.getValue());
			}
		}
		return element;
	}

	public List<XPathSegment> toSegments() {
		// FIXME !!!
		return Collections.unmodifiableList(segments);
	}

    public ItemPath toItemPath() {
        List<XPathSegment> xsegments = toSegments();
        List<ItemPathSegment> segments = new ArrayList<ItemPathSegment>(xsegments.size());
        for (XPathSegment segment : xsegments) {
            if (segment.isIdValueFilter()) {
                segments.add(new IdItemPathSegment(idToLong(segment.getValue())));
            } else {
                QName qName = segment.getQName();
                boolean variable = segment.isVariable();
                segments.add(new NameItemPathSegment(qName, variable));
            }
        }
        ItemPath path = new ItemPath(segments);
        path.setNamespaceMap(explicitNamespaceDeclarations);
        return path;
    }

    // Part 3: Various

	/**
	 * Returns new XPath with a specified element prepended to the path. Useful
	 * for "transposing" relative paths to a absolute root.
	 * 
	 * @param parentPath
	 * @return
	 */
	public XPathHolder transposedPath(QName parentPath) {
		XPathSegment segment = new XPathSegment(parentPath);
		List<XPathSegment> segments = new ArrayList<XPathSegment>();
		segments.add(segment);
		return transposedPath(segments);
	}

	/**
	 * Returns new XPath with a specified element prepended to the path. Useful
	 * for "transposing" relative paths to a absolute root.
	 * 
	 * @param parentPath
	 * @return
	 */
	public XPathHolder transposedPath(List<XPathSegment> parentPath) {
		List<XPathSegment> allSegments = new ArrayList<XPathSegment>();
		allSegments.addAll(parentPath);
		allSegments.addAll(toSegments());
		return new XPathHolder(allSegments);
	}
	
	

	private void addExplicitNsDeclarations(StringBuilder sb, boolean forceExplicitDeclaration) {
		boolean emptyExplicit = false;
		if (explicitNamespaceDeclarations == null || explicitNamespaceDeclarations.isEmpty()) {
//			if (!forceExplicitDeclaration){
			return;
//			}
//			throw new IllegalStateException("Expecting explicit namespace declaration.");
		}

//		if (!emptyExplicit){
			for (String prefix : explicitNamespaceDeclarations.keySet()) {
				sb.append("declare ");
				if (prefix.equals("")) {
					sb.append("default namespace '");
					sb.append(explicitNamespaceDeclarations.get(prefix));
					sb.append("'; ");
				} else {
					sb.append("namespace ");
					sb.append(prefix);
					sb.append("='");
					sb.append(explicitNamespaceDeclarations.get(prefix));
					sb.append("'; ");
				}
			}
//		} else{
//			for (XPathSegment segment : this.toSegments()){
//				sb.append("declare ");
//				QName s = segment.getQName();
//				if (s.getPrefix()
//			}
//		}
	}

	public boolean isEmpty() {
		return segments.isEmpty();
	}

    @Override
    public String toString() {
        // TODO: more verbose toString later
        return getXPath();
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (absolute ? 1231 : 1237);
		result = prime * result + ((segments == null) ? 0 : segments.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		
		// Special case
		if (obj instanceof QName) {
			if (segments.size() != 1) {
				return false;
			}
			XPathSegment segment = segments.get(0);
			return segment.getQName().equals((QName)obj);
		}
		
		if (getClass() != obj.getClass())
			return false;
		XPathHolder other = (XPathHolder) obj;
		if (absolute != other.absolute)
			return false;
		if (segments == null) {
			if (other.segments != null)
				return false;
		} else if (!segments.equals(other.segments))
			return false;
		return true;
	}

	/**
	 * Returns true if this path is below a specified path.
	 */
	public boolean isBelow(XPathHolder path) {
		if (this.segments.size() < 1){
			return false;
		}
		for(int i = 0; i < path.segments.size(); i++) {
			if (i > this.segments.size()) {
				// We have run beyond all of local segments, therefore
				// this path cannot be below specified path
				return false;
			}
			if (!this.segments.get(i).equals(path.segments.get(i))) {
				// Segments don't match. We are not below.
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns a list of segments that are the "tail" after specified path.
	 * The path in the parameter is assumed to be a "superpath" to this path, e.i.
	 * this path is below specified path. This method returns all the segments 
	 * of this path that are below the specified path.
	 * Returns null if the assumption is false.
	 */
	public List<XPathSegment> getTail(XPathHolder path) {
		int i = 0;
		while(i < path.segments.size()) {
			if (i > this.segments.size()) {
				// We have run beyond all of local segments, therefore
				// this path cannot be below specified path
				return null;
			}
			if (!this.segments.get(i).equals(path.segments.get(i))) {
				// Segments don't match. We are not below.
				return null;
			}
			i++;
		}
		return segments.subList(i, this.segments.size());
	}

	public static boolean isDefault(Element pathElement) {
		if (pathElement == null) {
			return true;
		}
		XPathHolder xpath = new XPathHolder(pathElement);
		if (xpath.isEmpty()) {
			return true;
		}
		return false;
	}

	
	private Long idToLong(String stringVal) {
		if (stringVal == null) {
			return null;
		}
		return Long.valueOf(stringVal);
	}
	
	private String idToString(Long longVal) {
		if (longVal == null) {
			return null;
		}
		return longVal.toString();
	}

}
