
package com.evolveum.prism.xml.ns._public.query_2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Map.Entry;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Revivable;
import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.parser.PrismBeanConverter;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.parser.XNodeSerializer;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xjc.PrismForJAXBUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.xml.DomAwareEqualsStrategy;
import com.evolveum.midpoint.util.xml.DomAwareHashCodeStrategy;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.lang.HashCode;
import org.jvnet.jaxb2_commons.lang.HashCodeStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;
import org.w3c.dom.Element;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchFilterType", propOrder = {
	"description",
    "filterClause",
})
public class SearchFilterType implements Serializable, Cloneable, Equals, HashCode, DebugDumpable, Revivable
{
    private final static long serialVersionUID = 201303040000L;
    
    protected String description;
    @XmlAnyElement
    protected Element filterClause;
    @XmlTransient
    protected MapXNode filterClauseXNode;           // single-subnode map node (key = filter element qname, value = contents)
    
    public final static QName COMPLEX_TYPE = new QName(PrismConstants.NS_QUERY, "SearchFilterType");
	public static final QName F_DESCRIPTION = new QName(PrismConstants.NS_QUERY, "description");

    /**
     * Creates a new {@code QueryType} instance.
     * 
     */
    public SearchFilterType() {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
    }

    /**
     * Creates a new {@code QueryType} instance by deeply copying a given {@code QueryType} instance.
     * 
     * 
     * @param o
     *     The instance to copy.
     * @throws NullPointerException
     *     if {@code o} is {@code null}.
     */
    public SearchFilterType(final SearchFilterType o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
        if (o == null) {
            throw new NullPointerException("Cannot create a copy of 'SearchFilterType' from 'null'.");
        }
        // CWildcardTypeInfo: org.w3c.dom.Element
        this.filterClause = o.filterClause == null ? null : (Element) o.filterClause.cloneNode(true);
        this.filterClauseXNode = (MapXNode) o.filterClauseXNode.clone();
    }

    public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
     * Gets the value of the filter property. JAXB method. Only for JAXB compatibility. Do not use directly.
     * 
     * @return
     *     possible object is
     *     {@link Element }
     *     
     */
    public Element getFilterClause() {
        if (filterClauseXNode != null) {
        	try {
	        	DomParser domParser = PrismUtil.getDomParser(null);
				return domParser.serializeSingleElementMapToElement(filterClauseXNode);         // TODO: beware, there can be nodes with unparsed values!
			} catch (SchemaException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
        } else {
        	return filterClause;
        }
    }

    public boolean containsFilterClause() {
        return filterClause != null || (filterClauseXNode != null && !filterClauseXNode.isEmpty());
    }


    /**
     * Sets the value of the filter property. JAXB method. Only for JAXB compatibility. Do not use directly.
     * 
     * @param element
     *     allowed object is
     *     {@link Element }
     *     
     */
    public void setFilterClause(Element element) {
    	// This method CANNOT parse the element to filter yet. The element may not be complete
    	// at this stage. We must do the on-demand parsing instead
  		this.filterClause = element;
        this.filterClauseXNode = null;
    }
    
	public void setFilterClauseXNode(MapXNode filterClauseXNode) {
		this.filterClauseXNode = filterClauseXNode;
		filterClause = null;
	}

    public MapXNode getFilterClauseXNode(PrismContext prismContext) throws SchemaException {
        if (this.filterClause == null && this.filterClauseXNode == null) {
            return null;
        } else if (this.filterClause == null) {
            return (MapXNode) this.filterClauseXNode.clone();
        } else {
            DomParser domParser;
            if (prismContext != null) {
                domParser = prismContext.getParserDom();
            } else {
                domParser = PrismUtil.getDomParser(null);
            }
            return domParser.parseElementAsMap(filterClause);
        }
    }

    public static SearchFilterType createFromXNode(XNode xnode) throws SchemaException {
        SearchFilterType filter = new SearchFilterType();
        filter.parseFromXNode(xnode);
        return filter;
    }

    public void parseFromXNode(XNode xnode) throws SchemaException {
    	this.filterClause = null;
    	if (xnode == null || xnode.isEmpty()) {
    		this.filterClauseXNode = null;
    		this.description = null;
    	} else {
    		if (!(xnode instanceof MapXNode)) {
    			throw new SchemaException("Cannot parse filter from "+xnode);
    		}
    		MapXNode xmap = (MapXNode)xnode;
    		XNode xdesc = xmap.get(SearchFilterType.F_DESCRIPTION);
    		if (xdesc != null) {
    			if (xdesc instanceof PrimitiveXNode<?>) {
    				String desc = ((PrimitiveXNode<String>)xdesc).getParsedValue(DOMUtil.XSD_STRING);
    				setDescription(desc);
    			} else {
                    throw new SchemaException("Description must have a primitive value");
                }
            }
            MapXNode xfilter = new MapXNode();
            for (Entry<QName,XNode> entry: xmap.entrySet()) {
                if (!QNameUtil.match(entry.getKey(), SearchFilterType.F_DESCRIPTION) && !QNameUtil.match(entry.getKey(), new QName("condition"))) {
                    xfilter.put(entry.getKey(), entry.getValue());
                }
            }
            if (xfilter.size() > 1) {
                throw new SchemaException("Filter clause has more than one item: " + xfilter);
            }
    		this.filterClauseXNode = xfilter;
    	}
    }

    // beware, prismContext may be null
    public MapXNode serializeToXNode(PrismContext prismContext) throws SchemaException {
        MapXNode xmap = getFilterClauseXNode(prismContext);
    	if (description == null) {
    		return xmap;
    	} else {
    		if (xmap == null) {
    			xmap = new MapXNode();
    		}
    		xmap.put(SearchFilterType.F_DESCRIPTION, new PrimitiveXNode<>(description));
    	}
    	return xmap;
    }

    @Override
	public void revive(PrismContext prismContext) throws SchemaException {
    	if (filterClause != null) {
			DomParser domParser = prismContext.getParserDom();
			filterClauseXNode = domParser.parseElementAsMap(filterClause);
    		filterClause = null;
    	}
	}
    
    /**
     * Generates a String representation of the contents of this type.
     * This is an extension method, produced by the 'ts' xjc plugin
     * 
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public int hashCode(ObjectLocator locator, HashCodeStrategy strategy) {
        int currentHashCode = 1;
        {
            Element theFilter;
            theFilter = this.filterClause;
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "filter", theFilter), currentHashCode, theFilter);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof SearchFilterType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        final SearchFilterType that = ((SearchFilterType) object);
        {
            Element lhsFilter;
            lhsFilter = this.filterClause;
            Element rhsFilter;
            rhsFilter = that.filterClause;
            if (!strategy.equals(LocatorUtils.property(thisLocator, "filter", lhsFilter), LocatorUtils.property(thatLocator, "filter", rhsFilter), lhsFilter, rhsFilter)) {
                return false;
            }
            
        }
        
        if (filterClauseXNode == null) {
			if (that.filterClauseXNode != null)
				return false;
		} else if (!filterClauseXNode.equals(that.filterClauseXNode))
			return false;
      
        return true;
    }

    public boolean equals(Object object) {
        final EqualsStrategy strategy = DomAwareEqualsStrategy.INSTANCE;
        return equals(null, null, object, strategy);
    }

    /**
     * Creates and returns a deep copy of a given object.
     * 
     * @param o
     *     The instance to copy or {@code null}.
     * @return
     *     A deep copy of {@code o} or {@code null} if {@code o} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static Object copyOf(final Object o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        try {
            if (o!= null) {
                if (o.getClass().isPrimitive()) {
                    return o;
                }
                if (o.getClass().isArray()) {
                    return copyOfArray(o);
                }
                // Immutable types.
                if (o instanceof Boolean) {
                    return o;
                }
                if (o instanceof Byte) {
                    return o;
                }
                if (o instanceof Character) {
                    return o;
                }
                if (o instanceof Double) {
                    return o;
                }
                if (o instanceof Enum) {
                    return o;
                }
                if (o instanceof Float) {
                    return o;
                }
                if (o instanceof Integer) {
                    return o;
                }
                if (o instanceof Long) {
                    return o;
                }
                if (o instanceof Short) {
                    return o;
                }
                if (o instanceof String) {
                    return o;
                }
                if (o instanceof BigDecimal) {
                    return o;
                }
                if (o instanceof BigInteger) {
                    return o;
                }
                if (o instanceof UUID) {
                    return o;
                }
                if (o instanceof QName) {
                    return o;
                }
                if (o instanceof Duration) {
                    return o;
                }
                if (o instanceof Currency) {
                    return o;
                }
                // String based types.
                if (o instanceof File) {
                    return new File(o.toString());
                }
                if (o instanceof URI) {
                    return new URI(o.toString());
                }
                if (o instanceof URL) {
                    return new URL(o.toString());
                }
                if (o instanceof MimeType) {
                    return new MimeType(o.toString());
                }
                // Cloneable types.
                if (o instanceof XMLGregorianCalendar) {
                    return ((XMLGregorianCalendar) o).clone();
                }
                if (o instanceof Date) {
                    return ((Date) o).clone();
                }
                if (o instanceof Calendar) {
                    return ((Calendar) o).clone();
                }
                if (o instanceof TimeZone) {
                    return ((TimeZone) o).clone();
                }
                if (o instanceof Locale) {
                    return ((Locale) o).clone();
                }
                if (o instanceof Element) {
                    return ((Element)((Element) o).cloneNode(true));
                }
                if (o instanceof JAXBElement) {
                    return copyOf(((JAXBElement) o));
                }
                try {
                    return o.getClass().getMethod("clone", ((Class[]) null)).invoke(o, ((Object[]) null));
                } catch (NoSuchMethodException e) {
                    if (o instanceof Serializable) {
                        return copyOf(((Serializable) o));
                    }
                    // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                    throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
                } catch (IllegalAccessException e) {
                    // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                    throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
                } catch (InvocationTargetException e) {
                    // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                    throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
                } catch (SecurityException e) {
                    // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                    throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
                } catch (IllegalArgumentException e) {
                    // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                    throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
                } catch (ExceptionInInitializerError e) {
                    // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                    throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
                }
            }
            return null;
        } catch (URISyntaxException e) {
            throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
        } catch (MalformedURLException e) {
            throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
        } catch (MimeTypeParseException e) {
            throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
        }
    }

    /**
     * Creates and returns a deep copy of a given array.
     * 
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static Object copyOfArray(final Object array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            if (array.getClass() == boolean[].class) {
                return copyOf(((boolean[]) array));
            }
            if (array.getClass() == byte[].class) {
                return copyOf(((byte[]) array));
            }
            if (array.getClass() == char[].class) {
                return copyOf(((char[]) array));
            }
            if (array.getClass() == double[].class) {
                return copyOf(((double[]) array));
            }
            if (array.getClass() == float[].class) {
                return copyOf(((float[]) array));
            }
            if (array.getClass() == int[].class) {
                return copyOf(((int[]) array));
            }
            if (array.getClass() == long[].class) {
                return copyOf(((long[]) array));
            }
            if (array.getClass() == short[].class) {
                return copyOf(((short[]) array));
            }
            final int len = Array.getLength(array);
            final Object copy = Array.newInstance(array.getClass().getComponentType(), len);
            for (int i = (len- 1); (i >= 0); i--) {
                Array.set(copy, i, copyOf(Array.get(array, i)));
            }
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     * 
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static boolean[] copyOf(final boolean[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final boolean[] copy = ((boolean[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     * 
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static byte[] copyOf(final byte[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final byte[] copy = ((byte[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     * 
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static char[] copyOf(final char[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final char[] copy = ((char[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     * 
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static double[] copyOf(final double[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final double[] copy = ((double[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     * 
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static float[] copyOf(final float[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final float[] copy = ((float[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     * 
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static int[] copyOf(final int[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final int[] copy = ((int[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     * 
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static long[] copyOf(final long[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final long[] copy = ((long[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     * 
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static short[] copyOf(final short[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final short[] copy = ((short[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given {@code JAXBElement} instance.
     * 
     * @param element
     *     The instance to copy or {@code null}.
     * @return
     *     A deep copy of {@code element} or {@code null} if {@code element} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static JAXBElement copyOf(final JAXBElement element) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (element!= null) {
            final JAXBElement copy = new JAXBElement(element.getName(), element.getDeclaredType(), element.getScope(), element.getValue());
            copy.setNil(element.isNil());
            copy.setValue(copyOf(copy.getValue()));
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given {@code Serializable}.
     * 
     * @param serializable
     *     The instance to copy or {@code null}.
     * @return
     *     A deep copy of {@code serializable} or {@code null} if {@code serializable} is {@code null}.
     */
    private static Serializable copyOf(final Serializable serializable) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (serializable!= null) {
            try {
                final ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
                final ObjectOutputStream out = new ObjectOutputStream(byteArrayOutput);
                out.writeObject(serializable);
                out.close();
                final ByteArrayInputStream byteArrayInput = new ByteArrayInputStream(byteArrayOutput.toByteArray());
                final ObjectInputStream in = new ObjectInputStream(byteArrayInput);
                final Serializable copy = ((Serializable) in.readObject());
                in.close();
                return copy;
            } catch (SecurityException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            } catch (ClassNotFoundException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            } catch (InvalidClassException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            } catch (NotSerializableException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            } catch (StreamCorruptedException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            } catch (OptionalDataException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            } catch (IOException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            }
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of this object.
     * 
     * 
     * @return
     *     A deep copy of this object.
     */
    @Override
    public SearchFilterType clone() {
        try {
            {
                // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
                final SearchFilterType clone = ((SearchFilterType) super.clone());
                // CWildcardTypeInfo: org.w3c.dom.Element
                clone.filterClause = ((this.filterClause == null)?null:((this.filterClause == null)?null:((Element) this.filterClause.cloneNode(true))));
                return clone;
            }
        } catch (CloneNotSupportedException e) {
            // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
            throw new AssertionError(e);
        }
    }

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("SearchFilterType");
		if (description != null) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "description", description, indent + 1);
		}
		if (filterClause != null) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "filterClause", filterClause.toString(), indent + 1);
		}
		if (filterClauseXNode != null) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "filterClauseXNode", (DebugDumpable) filterClauseXNode, indent + 1);
		}
		return sb.toString();
	}

}
