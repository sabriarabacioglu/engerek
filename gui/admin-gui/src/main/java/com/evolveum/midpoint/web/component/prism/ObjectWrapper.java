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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ReportTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

import java.io.Serializable;
import java.util.*;

/**
 * @author lazyman
 */
public class ObjectWrapper implements Serializable {

    public static final String F_DISPLAY_NAME = "displayName";
    public static final String F_SELECTED = "selected";

	private static final Trace LOGGER = TraceManager.getTrace(ObjectWrapper.class);

    private static final String DOT_CLASS = ObjectWrapper.class.getName() + ".";
    private static final String CREATE_CONTAINERS = DOT_CLASS + "createContainers";

	private PrismObject object;
	private ObjectDelta oldDelta;
	private ContainerStatus status;
	private HeaderStatus headerStatus;
	private String displayName;
	private String description;
	private List<ContainerWrapper> containers;

	private boolean showEmpty;
	private boolean minimalized;
	private boolean selectable;
	private boolean selected;

    private boolean showAssignments = false;
    private boolean showInheritedObjectAttributes = true;       // whether to show name and description properties and metadata container
    private boolean readonly = false;

    private static final List<QName> INHERITED_OBJECT_SUBCONTAINERS = Arrays.asList(ObjectType.F_METADATA,
            ObjectType.F_EXTENSION);

    private OperationResult result;
    private boolean protectedAccount;
    
    private List<PrismProperty> associations;

    private OperationResult fetchResult;
    private Definition editedDefinition;

    public ObjectWrapper(String displayName, String description, PrismObject object, Definition editedDefinition, ContainerStatus status) {
		Validate.notNull(object, "Object must not be null.");
		Validate.notNull(status, "Container status must not be null.");

		this.displayName = displayName;
		this.description = description;
		this.object = object;
		this.status = status;
		this.editedDefinition = editedDefinition;

        createContainers();
	}

    public List<PrismProperty> getAssociations() {
		return associations;
	}
    
    public void setAssociations(List<PrismProperty> associations) {
		this.associations = associations;
	}
    
    public OperationResult getFetchResult() {
        return fetchResult;
    }

    public void setFetchResult(OperationResult fetchResult) {
        this.fetchResult = fetchResult;
    }

    public OperationResult getResult() {
        return result;
    }

    public void clearResult() {
        result = null;
    }

	public HeaderStatus getHeaderStatus() {
		if (headerStatus == null) {
			headerStatus = HeaderStatus.NORMAL;
		}
		return headerStatus;
	}

	public ObjectDelta getOldDelta() {
		return oldDelta;
	}

	public void setOldDelta(ObjectDelta oldDelta) {
		this.oldDelta = oldDelta;
	}

	public void setHeaderStatus(HeaderStatus headerStatus) {
		this.headerStatus = headerStatus;
	}

	public PrismObject getObject() {
		return object;
	}

	public String getDisplayName() {
		if (displayName == null) {
			return WebMiscUtil.getName(object);
		}
		return displayName;
	}

	public ContainerStatus getStatus() {
		return status;
	}

	public String getDescription() {
		return description;
	}

	public boolean isMinimalized() {
		return minimalized;
	}

	public void setMinimalized(boolean minimalized) {
		this.minimalized = minimalized;
	}

	public boolean isShowEmpty() {
		return showEmpty;
	}

	public void setShowEmpty(boolean showEmpty) {
		this.showEmpty = showEmpty;
	}

	public boolean isSelectable() {
		return selectable;
	}

	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public List<ContainerWrapper> getContainers() {
		if (containers == null) {
			containers = createContainers();
		}
		return containers;
	}

	public ContainerWrapper findContainerWrapper(ItemPath path) {
		for (ContainerWrapper wrapper : getContainers()) {
            if (path != null) {
                if (path.equals(wrapper.getPath())) {
                    return wrapper;
                }
            } else {
                if (wrapper.getPath() == null) {
                    return wrapper;
                }
            }
		}

		return null;
	}

	private List<ContainerWrapper> createCustomContainerWrapper(PrismObject object, QName name) {
		PrismContainer container = object.findContainer(name);
		ContainerStatus status = container == null ? ContainerStatus.ADDING : ContainerStatus.MODIFYING;
		List<ContainerWrapper> list = new ArrayList<ContainerWrapper>();
		if (container == null) {
			PrismContainerDefinition definition = getDefinition().findContainerDefinition(name);
//			PrismContainerDefinition definition = object.getDefinition().findContainerDefinition(name);
			container = definition.instantiate();
		}

        ContainerWrapper wrapper = new ContainerWrapper(this, container, status, new ItemPath(name));
        addSubresult(wrapper.getResult());
		list.add(wrapper);
		list.addAll(createContainerWrapper(container, new ItemPath(name)));

		return list;
	}

    private void addSubresult(OperationResult subResult) {
        if (result == null || subResult == null) {
            return;
        }

        result.addSubresult(subResult);
    }

//    private PrismObjectDefinition determineObjectDefinition(){
//    	if (editedDefinition != null){
//    		return editedDefinition;
//    	}
//    	
//    	return object.getDefinition();
//    }
    
	private List<ContainerWrapper> createContainers() {
        result = new OperationResult(CREATE_CONTAINERS);

		List<ContainerWrapper> containers = new ArrayList<ContainerWrapper>();

		try {
			Class clazz = object.getCompileTimeClass();
			if (ShadowType.class.isAssignableFrom(clazz)) {
				PrismContainer attributes = object.findContainer(ShadowType.F_ATTRIBUTES);
				ContainerStatus status = attributes != null ? getStatus() : ContainerStatus.ADDING;
				if (attributes == null) {
					PrismContainerDefinition definition = object.getDefinition().findContainerDefinition(
							ShadowType.F_ATTRIBUTES);
//					if (editedDefinition != null){
//						definition = editedDefinition.findContainerDefinition(
//								ShadowType.F_ATTRIBUTES);
//					} else {
//						definition = object.getDefinition().findContainerDefinition(
//								ShadowType.F_ATTRIBUTES);
//					}	
//					
					attributes = definition.instantiate();
				}

				ContainerWrapper container = new ContainerWrapper(this, attributes, status, new ItemPath(
						ShadowType.F_ATTRIBUTES));
                addSubresult(container.getResult());

				container.setMain(true);
				containers.add(container);

				if (hasResourceCapability(((ShadowType) object.asObjectable()).getResource(), ActivationCapabilityType.class)){
					containers.addAll(createCustomContainerWrapper(object, ShadowType.F_ACTIVATION));
				}
				if (ShadowType.class.isAssignableFrom(clazz) &&
						hasResourceCapability(((ShadowType) object.asObjectable()).getResource(), CredentialsCapabilityType.class)) {
					containers.addAll(createCustomContainerWrapper(object, ShadowType.F_CREDENTIALS));
				}
				
				PrismContainer<ShadowAssociationType> associationContainer = object.findContainer(ShadowType.F_ASSOCIATION);
				if (associationContainer != null){
					containers.addAll(createCustomContainerWrapper(object, ShadowType.F_ASSOCIATION));
				}
            } else if (ResourceType.class.isAssignableFrom(clazz)) {
                containers =  createResourceContainers();
            } else if (ReportType.class.isAssignableFrom(clazz)) {
                containers = createReportContainers();
			} else {
				ContainerWrapper container = new ContainerWrapper(this, object, getStatus(), null);
                addSubresult(container.getResult());
				containers.add(container);

				containers.addAll(createContainerWrapper(object, null));
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Error occurred during container wrapping", ex);
			result.recordFatalError("Error occurred during container wrapping, reason: " + ex.getMessage(), ex);
		}

		Collections.sort(containers, new ItemWrapperComparator());
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

		return containers;
	}

    private List<ContainerWrapper> createReportContainers() throws SchemaException {
        List<ContainerWrapper> containers = new ArrayList<ContainerWrapper>();

        PrismContainer container = object.findContainer(ReportType.F_CONFIGURATION);
        ContainerStatus status = container != null ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;

        if (container == null) {
            PrismSchema schema = ReportTypeUtil.parseReportConfigurationSchema((PrismObject<ReportType>) object,
                    object.getPrismContext());
            PrismContainerDefinition definition = ReportTypeUtil.findReportConfigurationDefinition(schema);
            if (definition == null) {
                return containers;
            }
            container = definition.instantiate();
        }

        ContainerWrapper wrapper = new ContainerWrapper(this, container, status, new ItemPath(ReportType.F_CONFIGURATION));
        addSubresult(wrapper.getResult());

        containers.add(wrapper);

        return containers;
    }

    private List<ContainerWrapper> createResourceContainers() throws SchemaException {
        List<ContainerWrapper> containers = new ArrayList<ContainerWrapper>();
        PrismObject<ConnectorType> connector = loadConnector();

        containers.add(createResourceContainerWrapper(SchemaConstants.ICF_CONFIGURATION_PROPERTIES, connector));
        containers.add(createResourceContainerWrapper(SchemaConstants.ICF_CONNECTOR_POOL_CONFIGURATION, connector));
        containers.add(createResourceContainerWrapper(SchemaConstants.ICF_TIMEOUTS, connector));

        return containers;
    }

    private PrismObject<ConnectorType> loadConnector() {
        PrismReference connectorRef = object.findReference(ResourceType.F_CONNECTOR_REF);
        return connectorRef.getValue().getObject();
        //todo reimplement
    }

    private ContainerWrapper createResourceContainerWrapper(QName name, PrismObject<ConnectorType> connector)
        throws SchemaException {

        PrismContainer container = object.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        if (container != null && container.size() == 1 &&  container.getValue() != null) {
            PrismContainerValue value = container.getValue();
            container = value.findContainer(name);
        }

        ContainerStatus status = container != null ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
        if (container == null) {
            ConnectorType connectorType = connector.asObjectable();
            PrismSchema schema = ConnectorTypeUtil.parseConnectorSchema(connectorType, connector.getPrismContext());
            PrismContainerDefinition definition = ConnectorTypeUtil.findConfigurationContainerDefintion(connectorType, schema);

            definition = definition.findContainerDefinition(new ItemPath(name));
            container =  definition.instantiate();
        }

        ContainerWrapper wrapper = new ContainerWrapper(this, container, status,
                new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION, name));
        addSubresult(wrapper.getResult());

        return wrapper;
    }

	private List<ContainerWrapper> createContainerWrapper(PrismContainer parent, ItemPath path) {

		PrismContainerDefinition definition = parent.getDefinition();
		List<ContainerWrapper> wrappers = new ArrayList<ContainerWrapper>();

		List<ItemPathSegment> segments = new ArrayList<ItemPathSegment>();
		if (path != null) {
			segments.addAll(path.getSegments());
		}
		ItemPath parentPath = new ItemPath(segments);
		for (ItemDefinition def : (Collection<ItemDefinition>) definition.getDefinitions()) {
			if (!(def instanceof PrismContainerDefinition)) {
				continue;
			}
            if (ObjectSpecificationType.COMPLEX_TYPE.equals(def.getTypeName())) {
                continue;       // TEMPORARY FIX
            }
            if (TriggerType.COMPLEX_TYPE.equals(def.getTypeName())) {
                continue;       // TEMPORARY FIX TODO: remove after getEditSchema (authorization) will be fixed.
            }
            LOGGER.trace("ObjectWrapper.createContainerWrapper processing definition: {}", def);

			PrismContainerDefinition containerDef = (PrismContainerDefinition) def;
			if (!showAssignments && AssignmentType.COMPLEX_TYPE.equals(containerDef.getTypeName())) {
				continue;
			}
            if (!showInheritedObjectAttributes) {
                boolean result = INHERITED_OBJECT_SUBCONTAINERS.contains(containerDef.getName());
                LOGGER.info("checking " + containerDef.getName() + ", result = " + result);
                if (result) {
                    continue;
                }
            }

			ItemPath newPath = createPropertyPath(parentPath, containerDef.getName());
			PrismContainer prismContainer = object.findContainer(def.getName());
            ContainerWrapper container;
			if (prismContainer != null) {
                container = new ContainerWrapper(this, prismContainer, ContainerStatus.MODIFYING, newPath);
			} else {
				prismContainer = containerDef.instantiate();
				container = new ContainerWrapper(this, prismContainer, ContainerStatus.ADDING, newPath);
			}
            addSubresult(container.getResult());
            wrappers.add(container);

            if (!AssignmentType.COMPLEX_TYPE.equals(containerDef.getTypeName())) {      // do not show internals of Assignments (e.g. activation)
			    wrappers.addAll(createContainerWrapper(prismContainer, newPath));
            }
		}

		return wrappers;
	}

	private ItemPath createPropertyPath(ItemPath path, QName element) {
		List<ItemPathSegment> segments = new ArrayList<ItemPathSegment>();
		segments.addAll(path.getSegments());
		segments.add(new NameItemPathSegment(element));

		return new ItemPath(segments);
	}

	public void normalize() throws SchemaException {
		ObjectDelta delta = getObjectDelta();
		if (ChangeType.ADD.equals(delta.getChangeType())) {
			object = delta.getObjectToAdd();
		} else {
			delta.applyTo(object);
		}
	}

	public ObjectDelta getObjectDelta() throws SchemaException {
		if (ContainerStatus.ADDING.equals(getStatus())) {
			return createAddingObjectDelta();
		}

		ObjectDelta delta = new ObjectDelta(object.getCompileTimeClass(), ChangeType.MODIFY, object.getPrismContext());
		delta.setOid(object.getOid());

		List<ContainerWrapper> containers = getContainers();
		// sort containers by path size
		Collections.sort(containers, new PathSizeComparator());

		for (ContainerWrapper containerWrapper : getContainers()) {
			if (!containerWrapper.hasChanged()) {
				continue;
			}

			for (PropertyWrapper propertyWrapper : (List<PropertyWrapper>) containerWrapper.getProperties()) {
				if (!propertyWrapper.hasChanged()) {
					continue;
				}

				PrismPropertyDefinition propertyDef = propertyWrapper.getItem().getDefinition();

				ItemPath path = containerWrapper.getPath() != null ? containerWrapper.getPath()
						: new ItemPath();
				PropertyDelta pDelta = new PropertyDelta(path, propertyDef.getName(), propertyDef);
				for (ValueWrapper valueWrapper : propertyWrapper.getValues()) {
                    valueWrapper.normalize();
					ValueStatus valueStatus = valueWrapper.getStatus();
					if (!valueWrapper.hasValueChanged()
							&& (ValueStatus.NOT_CHANGED.equals(valueStatus) || ValueStatus.ADDED.equals(valueStatus))) {
						continue;
					}

					//TODO: need to check if the resource has defined capabilities
                    //todo this is bad hack because now we have not tri-state checkbox
					if (SchemaConstants.PATH_ACTIVATION.equals(path)) {

						if (object.asObjectable() instanceof ShadowType
                                && (((ShadowType) object.asObjectable()).getActivation() == null
                                || ((ShadowType) object.asObjectable()).getActivation().getAdministrativeStatus() == null)) {

							if (!hasResourceCapability(((ShadowType) object.asObjectable()).getResource(), ActivationCapabilityType.class)){
								continue;
							}
						}
					}

                    PrismPropertyValue newValCloned = clone(valueWrapper.getValue());
                    PrismPropertyValue oldValCloned = clone(valueWrapper.getOldValue());
                    switch (valueWrapper.getStatus()) {
                        case ADDED:
                            if (newValCloned != null) {
                                if (SchemaConstants.PATH_PASSWORD.equals(path)) {
                                    // password change will always look like add,
                                    // therefore we push replace
                                    pDelta.setValuesToReplace(Arrays.asList(newValCloned));
                                } else {
                                    pDelta.addValueToAdd(newValCloned);
                                }
                            }
                            break;
                        case DELETED:
                            if (newValCloned != null) {
                                pDelta.addValueToDelete(newValCloned);
                            }
                            break;
                        case NOT_CHANGED:
                            // this is modify...
                            if (propertyDef.isSingleValue()) {
                                if (newValCloned != null && newValCloned.getValue() != null) {
                                    pDelta.setValuesToReplace(Arrays.asList(newValCloned));
                                } else {
                                    if (oldValCloned != null) {
                                        pDelta.addValueToDelete(oldValCloned);
                                    }
                                }
                            } else {
                                if (newValCloned != null && newValCloned.getValue() != null) {
                                    pDelta.addValueToAdd(newValCloned);
                                }
                                if (oldValCloned != null) {
                                    pDelta.addValueToDelete(oldValCloned);
                                }
                            }
                            break;
                    }
                }
                if (!pDelta.isEmpty()) {
					delta.addModification(pDelta);
				}
			}
		}

        //returning container to previous order
        Collections.sort(containers, new ItemWrapperComparator());

		return delta;
	}

    private PrismPropertyValue clone(PrismPropertyValue value) {
        if (value == null) {
            return null;
        }
        PrismPropertyValue cloned = value.clone();
        cloned.setOriginType(OriginType.USER_ACTION);
        if (value.getValue() instanceof ProtectedStringType) {
            cloned.setValue(((ProtectedStringType)value.getValue()).clone());
        }
        if (value.getValue() instanceof PolyString) {
            PolyString poly = (PolyString) value.getValue();
            if (StringUtils.isEmpty(poly.getOrig())) {
                return null;
            }
            cloned.setValue(new PolyString(poly.getOrig(), poly.getNorm()));
        }

        return cloned;
    }

	private boolean hasResourceCapability(ResourceType resource, Class<? extends CapabilityType> capabilityClass){
		if (resource == null){
			return false;
		}
		return ResourceTypeUtil.hasEffectiveCapability(resource, capabilityClass);
	}

	private ObjectDelta createAddingObjectDelta() throws SchemaException {
		PrismObject object = this.object.clone();

		List<ContainerWrapper> containers = getContainers();
		// sort containers by path size
		Collections.sort(containers, new PathSizeComparator());

		for (ContainerWrapper containerWrapper : getContainers()) {
			if (!containerWrapper.hasChanged()) {
				continue;
			}

			PrismContainer container = containerWrapper.getItem();
			ItemPath path = containerWrapper.getPath();
			if (containerWrapper.getPath() != null) {
				container = container.clone();
				if (path.size() > 1) {
					ItemPath parentPath = path.allExceptLast();
					PrismContainer parent = object.findOrCreateContainer(parentPath);
					parent.add(container);
				} else {
					PrismContainer existing = object.findContainer(container.getElementName());
					if (existing == null) {
						object.add(container);
					} else {
						continue;
					}
				}
			} else {
				container = object;
			}

			for (PropertyWrapper propertyWrapper : (List<PropertyWrapper>) containerWrapper.getProperties()) {
				if (!propertyWrapper.hasChanged()) {
					continue;
				}

				PrismProperty property = propertyWrapper.getItem().clone();
				if (container.findProperty(property.getElementName()) != null) {
					continue;
				}
				for (ValueWrapper valueWrapper : propertyWrapper.getValues()) {
                    valueWrapper.normalize();
					if (!valueWrapper.hasValueChanged() || ValueStatus.DELETED.equals(valueWrapper.getStatus())) {
						continue;
					}

					if (property.hasRealValue(valueWrapper.getValue())) {
						continue;
					}

                    PrismPropertyValue cloned = clone(valueWrapper.getValue());
                    if (cloned != null) {
                        property.addValue(cloned);
                    }
				}

                if (!property.isEmpty()) {
                    container.add(property);
                }
			}
		}

		// cleanup empty containers
		cleanupEmptyContainers(object);

		ObjectDelta delta = ObjectDelta.createAddDelta(object);

        //returning container to previous order
        Collections.sort(containers, new ItemWrapperComparator());
        
        if (InternalsConfig.consistencyChecks) {
        	delta.checkConsistence(true, true, true);
        }

		return delta;
	}

	private void cleanupEmptyContainers(PrismContainer container) {
		List<PrismContainerValue> values = container.getValues();

		List<PrismContainerValue> valuesToBeRemoved = new ArrayList<PrismContainerValue>();
		for (PrismContainerValue value : values) {
			List<? extends Item> items = value.getItems();
			if (items != null) {
				Iterator<? extends Item> iterator = items.iterator();
				while (iterator.hasNext()) {
					Item item = iterator.next();

					if (item instanceof PrismContainer) {
						cleanupEmptyContainers((PrismContainer) item);

						if (item.isEmpty()) {
							iterator.remove();
						}
					}
				}
			}

			if (items == null || value.isEmpty()) {
				valuesToBeRemoved.add(value);
			}
		}

		container.removeAll(valuesToBeRemoved);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(ContainerWrapper.getDisplayNameFromItem(object));
		builder.append(", ");
		builder.append(status);
		builder.append("\n");
		for (ContainerWrapper wrapper : getContainers()) {
			builder.append("\t");
			builder.append(wrapper.toString());
			builder.append("\n");
		}
		return builder.toString();
	}

    public boolean isProtectedAccount() {
        if (object == null || !(ShadowType.class.isAssignableFrom(object.getCompileTimeClass()))) {
            return false;
        }

        PrismProperty<Boolean> protectedObject = object.findProperty(ShadowType.F_PROTECTED_OBJECT);
        if (protectedObject == null) {
            return false;
        }

        return protectedObject.getRealValue() != null ? protectedObject.getRealValue() : false;
    }

    private static class PathSizeComparator implements Comparator<ContainerWrapper> {

		@Override
		public int compare(ContainerWrapper c1, ContainerWrapper c2) {
			int size1 = c1.getPath() != null ? c1.getPath().size() : 0;
			int size2 = c2.getPath() != null ? c2.getPath().size() : 0;

			return size1 - size2;
		}
	}

    public boolean isShowAssignments() {
        return showAssignments;
    }

    public void setShowAssignments(boolean showAssignments) {
        this.showAssignments = showAssignments;
    }

    public boolean isReadonly() {
        if (isProtectedAccount()) {
            return true;
        }
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public boolean isShowInheritedObjectAttributes() {
        return showInheritedObjectAttributes;
    }

    public void setShowInheritedObjectAttributes(boolean showInheritedObjectAttributes) {
        this.showInheritedObjectAttributes = showInheritedObjectAttributes;
    }
    
    public PrismContainerDefinition getDefinition() {
    	if (editedDefinition instanceof PrismContainerDefinition){
    		return (PrismContainerDefinition) editedDefinition;
    	} else if (editedDefinition instanceof RefinedObjectClassDefinition){
    		return ((RefinedObjectClassDefinition) editedDefinition).toResourceAttributeContainerDefinition();
    	}
		return object.getDefinition();
	}
}
