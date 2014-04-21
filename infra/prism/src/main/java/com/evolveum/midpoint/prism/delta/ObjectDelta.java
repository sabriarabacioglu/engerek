/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PartiallyResolvedValue;
import com.evolveum.midpoint.prism.PathVisitable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPath.CompareResult;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import java.io.Serializable;
import java.util.*;

/**
 * Relative difference (delta) of the object.
 * <p/>
 * This class describes how the object changes. It can describe either object addition, modification of deletion.
 * <p/>
 * Addition described complete new (absolute) state of the object.
 * <p/>
 * Modification contains a set property deltas that describe relative changes to individual properties
 * <p/>
 * Deletion does not contain anything. It only marks object for deletion.
 * <p/>
 * The OID is mandatory for modification and deletion.
 *
 * @author Radovan Semancik
 * @see PropertyDelta
 */
public class ObjectDelta<T extends Objectable> implements DebugDumpable, Visitable, PathVisitable, Serializable {

    private static final long serialVersionUID = -528560467958335366L;

    private ChangeType changeType;

    /**
     * OID of the object that this delta applies to.
     */
    private String oid;

    /**
     * New object to add. Valid only if changeType==ADD
     */
    private PrismObject<T> objectToAdd;

    /**
     * Set of relative property deltas. Valid only if changeType==MODIFY
     */
    private Collection<? extends ItemDelta<?>> modifications;

    /**
     * Class of the object that we describe.
     */
    private Class<T> objectTypeClass;
    
    transient private PrismContext prismContext;

    public ObjectDelta(Class<T> objectTypeClass, ChangeType changeType, PrismContext prismContext) {
    	Validate.notNull(objectTypeClass,"No objectTypeClass");
    	Validate.notNull(changeType,"No changeType");
    	
        this.changeType = changeType;
        this.objectTypeClass = objectTypeClass;
        this.prismContext = prismContext;
        objectToAdd = null;
        modifications = createEmptyModifications();
    }
    
    @Override
    public void accept(Visitor visitor){
    	visitor.visit(this);
    	if (isAdd()) {
    		objectToAdd.accept(visitor);
    	} else if (isModify()) {
	    	for (ItemDelta<?> delta : getModifications()){
	    		delta.accept(visitor);
	    	}
    	}
    	// Nothing to visit for delete
    }

    @Override
	public void accept(Visitor visitor, ItemPath path, boolean recursive) {
    	if (path == null || path.isEmpty()) {
			if (recursive) {
				accept(visitor);
			} else {
				visitor.visit(this);
			}
		} else {
			ItemDelta.accept(getModifications(), visitor, path, recursive);
		}
	}

	public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }
    
	public static boolean isAdd(ObjectDelta<?> objectDelta) {
		if (objectDelta == null) {
			return false;
		}
		return objectDelta.isAdd();
	}

	public boolean isAdd() {
		return changeType == ChangeType.ADD;
	}

	public static boolean isDelete(ObjectDelta<?> objectDelta) {
		if (objectDelta == null) {
			return false;
		}
		return objectDelta.isDelete();
	}

	public boolean isDelete() {
		return changeType == ChangeType.DELETE;
	}

	public static boolean isModify(ObjectDelta<?> objectDelta) {
		if (objectDelta == null) {
			return false;
		}
		return objectDelta.isModify();
	}

	public boolean isModify() {
		return changeType == ChangeType.MODIFY;
	}

	public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
        if (objectToAdd != null) {
        	objectToAdd.setOid(oid);
        }
    }

    public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public PrismObject<T> getObjectToAdd() {
        return objectToAdd;
    }

    public void setObjectToAdd(PrismObject<T> objectToAdd) {
    	if (getChangeType() != ChangeType.ADD) {
    		throw new IllegalStateException("Cannot set object to "+getChangeType()+" delta");
    	}
        this.objectToAdd = objectToAdd;
        if (objectToAdd != null) {
            this.objectTypeClass = objectToAdd.getCompileTimeClass();
        }
    }

    public Collection<? extends ItemDelta> getModifications() {
        return modifications;
    }

    public void addModification(ItemDelta itemDelta) {
    	if (getChangeType() != ChangeType.MODIFY) {
    		throw new IllegalStateException("Cannot add modifications to "+getChangeType()+" delta");
    	}
    	ItemPath itemPath = itemDelta.getPath();
    	ItemDelta<?> existingModification = findModification(itemPath, itemDelta.getClass());
    	if (existingModification != null) {
    		existingModification.merge(itemDelta);
    	} else {
    		((Collection)modifications).add(itemDelta);
    	}
    }
    
	public boolean containsModification(ItemDelta itemDelta) {
		for (ItemDelta modification: modifications) {
			if (modification.contains(itemDelta)) {
				return true;
			}
		}
		return false;
	}

	public void addModifications(Collection<? extends ItemDelta> itemDeltas) {
    	for (ItemDelta<?> modDelta: itemDeltas) {
    		addModification(modDelta);
    	}
    }
    
    public void addModifications(ItemDelta<?>... itemDeltas) {
    	for (ItemDelta<?> modDelta: itemDeltas) {
    		addModification(modDelta);
    	}
    }
    
    public <V extends PrismValue> ItemDelta<V> findItemDelta(ItemPath propertyPath) {
    	return findItemDelta(propertyPath, ItemDelta.class, Item.class);
    }
    
    private <D extends ItemDelta, I extends Item> D findItemDelta(ItemPath propertyPath, Class<D> deltaType, Class<I> itemType) {
        if (changeType == ChangeType.ADD) {
            I item = objectToAdd.findItem(propertyPath, itemType);
            if (item == null) {
                return null;
            }
            D itemDelta = createEmptyDelta(propertyPath, item.getDefinition(), deltaType, item.getClass());
            itemDelta.addValuesToAdd(item.getClonedValues());
            return itemDelta;
        } else if (changeType == ChangeType.MODIFY) {
            return findModification(propertyPath, deltaType);
        } else {
            return null;
        }
    }
    
    public <V extends PrismValue> PartiallyResolvedDelta<V> findPartial(ItemPath propertyPath) {
        if (changeType == ChangeType.ADD) {
            PartiallyResolvedValue<V> partialValue = objectToAdd.findPartial(propertyPath);
            if (partialValue == null || partialValue.getItem() == null) {
                return null;
            }
            Item<V> item = partialValue.getItem();
            ItemDelta<V> itemDelta = item.createDelta();
            itemDelta.addValuesToAdd(item.getClonedValues());
            return new PartiallyResolvedDelta<V>(itemDelta, partialValue.getResidualPath());
        } else if (changeType == ChangeType.MODIFY) {
        	for (ItemDelta<?> modification: modifications) {
        		CompareResult compareComplex = modification.getPath().compareComplex(propertyPath);
        		if (compareComplex == CompareResult.EQUIVALENT) {
        			return new PartiallyResolvedDelta<V>((ItemDelta<V>)modification, null);
        		} else if (compareComplex == CompareResult.SUPERPATH) {
        			return new PartiallyResolvedDelta<V>((ItemDelta<V>)modification, null);
        		} else if (compareComplex == CompareResult.SUBPATH) {
        			return new PartiallyResolvedDelta<V>((ItemDelta<V>)modification,
        					propertyPath.remainder(modification.getPath()));
        		}
        	}
            return null;
        } else {
            return null;
        }
    }

    public boolean hasItemDelta(ItemPath propertyPath) {
        if (changeType == ChangeType.ADD) {
            Item item = objectToAdd.findItem(propertyPath, Item.class);
            return item != null;
        } else if (changeType == ChangeType.MODIFY) {
            ItemDelta modification = findModification(propertyPath, ItemDelta.class);
            return modification != null;
        } else {
            return false;
        }
    }
    
    public boolean hasCompleteDefinition() {
    	if (isAdd()) {
    		return getObjectToAdd().hasCompleteDefinition();
    	} else if (isModify()) {
    		for (ItemDelta modification: getModifications()) {
    			if (!modification.hasCompleteDefinition()) {
    				return false;
    			}
//    			return true;
    		}
    		return true;
    	} else if (isDelete()) {
    		return true;
    	}
    	throw new IllegalStateException("Strange things happen");
    }
    
    private <D extends ItemDelta, I extends Item> D createEmptyDelta(ItemPath propertyPath, ItemDefinition itemDef,
    		Class<D> deltaType, Class<I> itemType) {
    
    	if (PrismProperty.class.isAssignableFrom(itemType)) {
    		return (D) new PropertyDelta(propertyPath, (PrismPropertyDefinition)itemDef);
    	} else if (PrismContainer.class.isAssignableFrom(itemType)) {
    		return (D) new ContainerDelta(propertyPath, (PrismContainerDefinition)itemDef);
    	} else if (PrismReference.class.isAssignableFrom(itemType)) {
    		return (D) new ReferenceDelta(propertyPath, (PrismReferenceDefinition)itemDef);
    	} else {
    		throw new IllegalArgumentException("Unknown item type "+itemType);
    	}
    }

    public Class<T> getObjectTypeClass() {
        return objectTypeClass;
    }

    public void setObjectTypeClass(Class<T> objectTypeClass) {
        this.objectTypeClass = objectTypeClass;
    }

    /**
     * Top-level path is assumed.
     */
    public <X> PropertyDelta<X> findPropertyDelta(QName propertyName) {
        return findPropertyDelta(new ItemPath(propertyName));
    }

    public <X> PropertyDelta<X> findPropertyDelta(ItemPath parentPath, QName propertyName) {
        return findPropertyDelta(new ItemPath(parentPath, propertyName));
    }
    
    @SuppressWarnings("unchecked")
	public <X> PropertyDelta<X> findPropertyDelta(ItemPath propertyPath) {
    	return findItemDelta(propertyPath, PropertyDelta.class, PrismProperty.class);
    }
    
    @SuppressWarnings("unchecked")
	public <X extends Containerable> ContainerDelta<X> findContainerDelta(ItemPath propertyPath) {
    	return findItemDelta(propertyPath, ContainerDelta.class, PrismContainer.class);
    }

    public <X extends Containerable> ContainerDelta<X> findContainerDelta(QName name) {
    	return findContainerDelta(new ItemPath(name));
    }

    private <D extends ItemDelta> D findModification(ItemPath propertyPath, Class<D> deltaType) {
    	return ItemDelta.findItemDelta(modifications, propertyPath, deltaType);
    }
    
    private  <D extends ItemDelta> D findModification(QName itemName, Class<D> deltaType) {
    	return findModification(new ItemPath(itemName), deltaType);
    }
    
    public ReferenceDelta findReferenceModification(QName itemName) {
    	return findModification(itemName, ReferenceDelta.class);
    }

    public ReferenceDelta findReferenceModification(ItemPath itemPath) {
        return findModification(itemPath, ReferenceDelta.class);
    }

    /**
	 * Returns all item deltas at or below a specified path.
	 */
	public Collection<? extends ItemDelta<?>> findItemDeltasSubPath(ItemPath itemPath) {
		return ItemDelta.findItemDeltasSubPath(modifications, itemPath);
	}

    
    private <D extends ItemDelta> void removeModification(ItemPath propertyPath, Class<D> deltaType) {
    	ItemDelta.removeItemDelta(modifications, propertyPath, deltaType);
    }
    
    private <D extends ItemDelta> void removeModification(QName itemName, Class<D> deltaType) {
    	removeModification(new ItemPath(itemName), deltaType);
    }
    
    public void removeReferenceModification(QName itemName) {
    	removeModification(itemName, ReferenceDelta.class);
    }

    public boolean isEmpty() {
    	if (getChangeType() == ChangeType.DELETE) {
    		// Delete delta is never empty
    		return false;
    	}
    	if (getChangeType() == ChangeType.ADD) {
    		return objectToAdd == null || objectToAdd.isEmpty();
    	}
        if (modifications == null || modifications.isEmpty()) {
        	return true;
        }
        for (ItemDelta<?> mod: modifications) {
        	if (!mod.isEmpty()) {
        		return false;
        	}
        }
        return true;
    }
    
    public void normalize() {
    	if (objectToAdd != null) {
    		objectToAdd.normalize();
    	}
    	if (modifications != null) {
    		Iterator<? extends ItemDelta> iterator = modifications.iterator();
    		while (iterator.hasNext()) {
    			ItemDelta<?> modification = iterator.next();
    			modification.normalize();
    			if (modification.isEmpty()) {
    				iterator.remove();
    			}
    		}
    	}
    }
    
    public void applyDefinition(PrismObjectDefinition<T> definition) throws SchemaException {
    	if (objectToAdd != null) {
    		objectToAdd.applyDefinition(definition);
    	}
    	ItemDelta.applyDefinition(getModifications(), definition);
    }

    /**
     * Deep clone.
     */
    public ObjectDelta<T> clone() {
        ObjectDelta<T> clone = new ObjectDelta<T>(this.objectTypeClass, this.changeType, this.prismContext);
        clone.oid = this.oid;
        clone.modifications = createEmptyModifications();
        for (ItemDelta<?> thisModification: this.modifications) {
        	((Collection)clone.modifications).add(thisModification.clone());
        }
        if (this.objectToAdd == null) {
            clone.objectToAdd = null;
        } else {
            clone.objectToAdd = this.objectToAdd.clone();
        }
        return clone;
    }

    /**
     * Merge provided delta into this delta.
     * This delta is assumed to be chronologically earlier, delta in the parameter is assumed to come chronologicaly later.
     */
    public void merge(ObjectDelta<T> deltaToMerge) throws SchemaException {
        if (changeType == ChangeType.ADD) {
            if (deltaToMerge.changeType == ChangeType.ADD) {
                // Maybe we can, be we do not want. This is usually an error anyway.
                throw new IllegalArgumentException("Cannot merge two ADD deltas: " + this + ", " + deltaToMerge);
            } else if (deltaToMerge.changeType == ChangeType.MODIFY) {
                if (objectToAdd == null) {
                    throw new IllegalStateException("objectToAdd is null");
                }
                deltaToMerge.applyTo(objectToAdd);
            } else if (deltaToMerge.changeType == ChangeType.DELETE) {
                this.changeType = ChangeType.DELETE;
            }
        } else if (changeType == ChangeType.MODIFY) {
            if (deltaToMerge.changeType == ChangeType.ADD) {
                throw new IllegalArgumentException("Cannot merge 'add' delta to a 'modify' object delta");
            } else if (deltaToMerge.changeType == ChangeType.MODIFY) {
            	mergeModifications(deltaToMerge.modifications);
            } else if (deltaToMerge.changeType == ChangeType.DELETE) {
                this.changeType = ChangeType.DELETE;
            }
        } else { // DELETE
            if (deltaToMerge.changeType == ChangeType.ADD) {
                this.changeType = ChangeType.ADD;
                // TODO: clone?
                this.objectToAdd = deltaToMerge.objectToAdd;
            } else if (deltaToMerge.changeType == ChangeType.MODIFY) {
                // Just ignore the modification of a deleted object
            } else if (deltaToMerge.changeType == ChangeType.DELETE) {
                // Nothing to do
            }
        }
    }
    
    /**
     * Returns a delta that is a "sum" of all the deltas in the collection.
     * The deltas as processed as an ORDERED sequence. Therefore it correctly processes item overwrites and so on.
     * It also means that if there is an ADD delta it has to be first. 
     */
    public static <T extends Objectable> ObjectDelta<T> summarize(ObjectDelta<T>... deltas) throws SchemaException {
    	return summarize(Arrays.asList(deltas));
    }
    
    /**
     * Returns a delta that is a "sum" of all the deltas in the collection.
     * The deltas as processed as an ORDERED sequence. Therefore it correctly processes item overwrites and so on.
     * It also means that if there is an ADD delta it has to be first. 
     */
    public static <T extends Objectable> ObjectDelta<T> summarize(List<ObjectDelta<T>> deltas) throws SchemaException {
    	if (deltas == null || deltas.isEmpty()) {
    		return null;
    	}
    	Iterator<ObjectDelta<T>> iterator = deltas.iterator();
    	ObjectDelta<T> sumDelta = iterator.next().clone();
    	while (iterator.hasNext()) {
    		ObjectDelta<T> nextDelta = iterator.next();
    		sumDelta.merge(nextDelta);
    	}
    	return sumDelta;
    }

    /**
     * Union of several object deltas. The deltas are merged to create a single delta
     * that contains changes from all the deltas.
     * 
     * Union works on UNORDERED deltas.
     */
    public static <T extends Objectable> ObjectDelta<T> union(ObjectDelta<T>... deltas) throws SchemaException {
        List<ObjectDelta<T>> modifyDeltas = new ArrayList<ObjectDelta<T>>(deltas.length);
        ObjectDelta<T> addDelta = null;
        ObjectDelta<T> deleteDelta = null;
        for (ObjectDelta<T> delta : deltas) {
            if (delta == null) {
                continue;
            }
            if (delta.changeType == ChangeType.MODIFY) {
                modifyDeltas.add(delta);
            } else if (delta.changeType == ChangeType.ADD) {
                if (addDelta != null) {
                    // Maybe we can, be we do not want. This is usually an error anyway.
                    throw new IllegalArgumentException("Cannot merge two add deltas: " + addDelta + ", " + delta);
                }
                addDelta = delta;
            } else if (delta.changeType == ChangeType.DELETE) {
                deleteDelta = delta;
            }

        }

        if (deleteDelta != null && addDelta == null) {
            // Merging DELETE with anything except ADD is still a DELETE
            return deleteDelta.clone();
        }

        if (deleteDelta != null && addDelta != null) {
            throw new IllegalArgumentException("Cannot merge add and delete deltas: " + addDelta + ", " + deleteDelta);
        }

        if (addDelta != null) {
            return mergeToDelta(addDelta, modifyDeltas);
        } else {
            if (modifyDeltas.size() == 0) {
                return null;
            }
            if (modifyDeltas.size() == 1) {
                return modifyDeltas.get(0);
            }
            return mergeToDelta(modifyDeltas.get(0), modifyDeltas.subList(1, modifyDeltas.size()));
        }
    }

    private static <T extends Objectable> ObjectDelta<T> mergeToDelta(ObjectDelta<T> firstDelta,
            List<ObjectDelta<T>> modifyDeltas) throws SchemaException {
        if (modifyDeltas.size() == 0) {
            return firstDelta;
        }
        ObjectDelta<T> delta = firstDelta.clone();
        for (ObjectDelta<T> modifyDelta : modifyDeltas) {
            if (modifyDelta == null) {
                continue;
            }
            if (modifyDelta.changeType != ChangeType.MODIFY) {
                throw new IllegalArgumentException("Can only merge MODIFY changes, got " + modifyDelta.changeType);
            }
            delta.mergeModifications(modifyDelta.modifications);
        }
        return delta;
    }
    
    public void mergeModifications(Collection<? extends ItemDelta> modificationsToMerge) throws SchemaException {
        for (ItemDelta<?> propDelta : modificationsToMerge) {
        	mergeModification(propDelta);
        }
    }
    
    public void mergeModification(ItemDelta<?> modificationToMerge) throws SchemaException {
        if (changeType == ChangeType.ADD) {
        	modificationToMerge.applyTo(objectToAdd);
        } else if (changeType == ChangeType.MODIFY) {
        	ItemDelta myDelta = findModification(modificationToMerge.getPath(), ItemDelta.class);
            if (myDelta == null) {
                addModification(modificationToMerge.clone());
            } else {
                myDelta.merge(modificationToMerge);
            }
        } // else it is DELETE. There's nothing to do. Merging anything to delete is still delete
    }


    /**
     * Applies this object delta to specified object, returns updated object.
     * It modifies the provided object.
     */
    public void applyTo(PrismObject<T> targetObject) throws SchemaException {
    	if (isEmpty()) {
    		// nothing to do
    		return;
    	}
        if (changeType != ChangeType.MODIFY) {
            throw new IllegalStateException("Can apply only MODIFY delta to object, got " + changeType + " delta");
        }
        for (ItemDelta itemDelta : modifications) {
            itemDelta.applyTo(targetObject);
        }
    }

    /**
     * Applies this object delta to specified object, returns updated object.
     * It leaves the original object unchanged.
     *
     * @param objectOld object before change
     * @return object with applied changes or null if the object should not exit (was deleted)
     */
    public PrismObject<T> computeChangedObject(PrismObject<T> objectOld) throws SchemaException {
        if (objectOld == null) {
            if (getChangeType() == ChangeType.ADD) {
                objectOld = getObjectToAdd();
                return objectOld.clone();
            } else {
                //throw new IllegalStateException("Cannot apply "+getChangeType()+" delta to a null old object");
                // This seems to be quite OK
                return null;
            }
        }
        if (getChangeType() == ChangeType.DELETE) {
            return null;
        }
        // MODIFY change
        PrismObject<T> objectNew = objectOld.clone();
        for (ItemDelta modification : modifications) {
            modification.applyTo(objectNew);
        }
        return objectNew;
    }

    /**
     * Incorporates the property delta into the existing property deltas
     * (regardless of the change type).
     */
    public void swallow(ItemDelta<?> newItemDelta) throws SchemaException {
        if (changeType == ChangeType.MODIFY) {
            // TODO: check for conflict
            addModification(newItemDelta);
        } else if (changeType == ChangeType.ADD) {
        	Item item = objectToAdd.findOrCreateItem(newItemDelta.getPath(), newItemDelta.getItemClass());
            newItemDelta.applyTo(item);
        }
        // nothing to do for DELETE
    }

    private Collection<? extends ItemDelta<?>> createEmptyModifications() {
    	// Lists are easier to debug
        return new ArrayList<ItemDelta<?>>();
    }
    
    public <X> PropertyDelta<X> createPropertyModification(QName name, PrismPropertyDefinition propertyDefinition) {
    	PropertyDelta<X> propertyDelta = new PropertyDelta<X>(name, propertyDefinition);
    	addModification(propertyDelta);
    	return propertyDelta;
    }
    
    public <X> PropertyDelta<X> createPropertyModification(ItemPath path) {
	    PrismObjectDefinition<T> objDef = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(getObjectTypeClass());
		PrismPropertyDefinition propDef = objDef.findPropertyDefinition(path);
		return createPropertyModification(path, propDef);
    }
	    
    public <C> PropertyDelta<C> createPropertyModification(ItemPath path, PrismPropertyDefinition propertyDefinition) {
    	PropertyDelta<C> propertyDelta = new PropertyDelta<C>(path, propertyDefinition);
    	// No point in adding the modification to this delta. It will get merged anyway and it may disappear
    	// it is not reliable and therefore it is better not to add it now.
    	return propertyDelta;
    }
    
    public ReferenceDelta createReferenceModification(QName name, PrismReferenceDefinition referenceDefinition) {
    	ReferenceDelta referenceDelta = new ReferenceDelta(name, referenceDefinition);
    	addModification(referenceDelta);
    	return referenceDelta;
    }
    
    public <C extends Containerable> ContainerDelta<C> createContainerModification(ItemPath path) {
	    PrismObjectDefinition<T> objDef = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(getObjectTypeClass());
		PrismContainerDefinition<C> propDef = objDef.findContainerDefinition(path);
		return createContainerModification(path, propDef);
    }
    
    public <C extends Containerable> ContainerDelta<C> createContainerModification(ItemPath path, PrismContainerDefinition<C> containerDefinition) {
    	ContainerDelta<C> containerDelta = new ContainerDelta<C>(path, containerDefinition);
    	addModification(containerDelta);
    	return containerDelta;
    }
    
    /**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method. 
     */
    public static <O extends Objectable, X> ObjectDelta<O> createModificationReplaceProperty(Class<O> type, String oid, QName propertyName,
    		PrismContext prismContext, X... propertyValues) {
    	ItemPath propertyPath = new ItemPath(propertyName);
    	return createModificationReplaceProperty(type, oid, propertyPath, prismContext, propertyValues);
    }
    
    /**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method. 
     */
    public static <O extends Objectable, X> ObjectDelta<O> createModificationReplaceProperty(Class<O> type, String oid, 
    		ItemPath propertyPath, PrismContext prismContext, X... propertyValues) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setOid(oid);
    	fillInModificationReplaceProperty(objectDelta, propertyPath, propertyValues);
    	return objectDelta;
    }
    
    public static <O extends Objectable, X> ObjectDelta<O> createModificationAddProperty(Class<O> type, String oid, 
    		QName propertyName, PrismContext prismContext, X... propertyValues) {
    	return createModificationAddProperty(type, oid, new ItemPath(propertyName), prismContext, propertyValues);
    }
    
    public static <O extends Objectable, X> ObjectDelta<O> createModificationAddProperty(Class<O> type, String oid, 
    		ItemPath propertyPath, PrismContext prismContext, X... propertyValues) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setOid(oid);
    	fillInModificationAddProperty(objectDelta, propertyPath, propertyValues);
    	return objectDelta;
    }
    
    public static <O extends Objectable, X> ObjectDelta<O> createModificationDeleteProperty(Class<O> type, String oid, 
    		QName propertyName, PrismContext prismContext, X... propertyValues) {
    	return createModificationDeleteProperty(type, oid, new ItemPath(propertyName), prismContext, propertyValues);
    }
    
    public static <O extends Objectable, X> ObjectDelta<O> createModificationDeleteProperty(Class<O> type, String oid, 
    		ItemPath propertyPath, PrismContext prismContext, X... propertyValues) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setOid(oid);
    	fillInModificationDeleteProperty(objectDelta, propertyPath, propertyValues);
    	return objectDelta;
    }
    
    public <X> void addModificationReplaceProperty(QName propertyQName, X... propertyValues) {
    	addModificationReplaceProperty(new ItemPath(propertyQName), propertyValues);
    }
    
    public <X> void addModificationReplaceProperty(ItemPath propertyPath, X... propertyValues) {
    	fillInModificationReplaceProperty(this, propertyPath, propertyValues);
    }
    
    public <X> void addModificationAddProperty(QName propertyQName, X... propertyValues) {
    	addModificationAddProperty(new ItemPath(propertyQName), propertyValues);
    }
    
    public <X> void addModificationAddProperty(ItemPath propertyPath, X... propertyValues) {
    	fillInModificationAddProperty(this, propertyPath, propertyValues);
    }
    
    public <X> void addModificationDeleteProperty(QName propertyQName, X... propertyValues) {
    	addModificationDeleteProperty(new ItemPath(propertyQName), propertyValues);
    }
     
    public <X> void addModificationDeleteProperty(ItemPath propertyPath, X... propertyValues) {
    	fillInModificationDeleteProperty(this, propertyPath, propertyValues);
    }
    
    public <C extends Containerable> void addModificationAddContainer(QName propertyQName, C... containerables) throws SchemaException {
    	addModificationAddContainer(new ItemPath(propertyQName), containerables);
    }
    
    public <C extends Containerable> void addModificationAddContainer(ItemPath propertyPath, C... containerables) throws SchemaException {
    	fillInModificationAddContainer(this, propertyPath, prismContext, containerables);
    }
    
    public <C extends Containerable> void addModificationAddContainer(QName propertyQName, PrismContainerValue<C>... containerValues) {
    	addModificationAddContainer(new ItemPath(propertyQName), containerValues);
    }
    
    public <C extends Containerable> void addModificationAddContainer(ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
    	fillInModificationAddContainer(this, propertyPath, containerValues);
    }
    
    public <C extends Containerable> void addModificationDeleteContainer(QName propertyQName, C... containerables) throws SchemaException {
    	addModificationDeleteContainer(new ItemPath(propertyQName), containerables);
    }
    
    public <C extends Containerable> void addModificationDeleteContainer(ItemPath propertyPath, C... containerables) throws SchemaException {
    	fillInModificationDeleteContainer(this, propertyPath, prismContext, containerables);
    }
    
    public <C extends Containerable> void addModificationDeleteContainer(QName propertyQName, PrismContainerValue<C>... containerValues) {
    	addModificationDeleteContainer(new ItemPath(propertyQName), containerValues);
    }
    
    public <C extends Containerable> void addModificationDeleteContainer(ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
    	fillInModificationDeleteContainer(this, propertyPath, containerValues);
    }
    
    public <C extends Containerable> void addModificationReplaceContainer(QName propertyQName, PrismContainerValue<C>... containerValues) {
    	addModificationReplaceContainer(new ItemPath(propertyQName), containerValues);
    }
    
    public <C extends Containerable> void addModificationReplaceContainer(ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
    	fillInModificationReplaceContainer(this, propertyPath, containerValues);
    }
    
    public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationReplaceContainer(Class<O> type, String oid, 
    		ItemPath propertyPath, PrismContext prismContext, C... containerValues) throws SchemaException {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setOid(oid);
    	fillInModificationReplaceContainer(objectDelta, propertyPath, containerValues);
    	return objectDelta;
    }
    
    protected static <O extends Objectable, X> void fillInModificationReplaceProperty(ObjectDelta<O> objectDelta,
    		ItemPath propertyPath, X... propertyValues) {
    	PropertyDelta<X> propertyDelta = objectDelta.createPropertyModification(propertyPath);
    	if (propertyValues != null) {
	    	Collection<PrismPropertyValue<X>> valuesToReplace = toPrismPropertyValues(objectDelta.getPrismContext(), propertyValues);
	    	propertyDelta.setValuesToReplace(valuesToReplace);
	    	objectDelta.addModification(propertyDelta);
    	}
    }

    protected static <O extends Objectable, X> void fillInModificationAddProperty(ObjectDelta<O> objectDelta,
    		ItemPath propertyPath, X... propertyValues) {
    	PropertyDelta<X> propertyDelta = objectDelta.createPropertyModification(propertyPath);
    	if (propertyValues != null) {
	    	Collection<PrismPropertyValue<X>> valuesToAdd = toPrismPropertyValues(objectDelta.getPrismContext(), propertyValues);
	    	propertyDelta.addValuesToAdd(valuesToAdd);
	    	objectDelta.addModification(propertyDelta);
    	}
    }
    
    protected static <O extends Objectable, X> void fillInModificationDeleteProperty(ObjectDelta<O> objectDelta,
    		ItemPath propertyPath, X... propertyValues) {
    	PropertyDelta<X> propertyDelta = objectDelta.createPropertyModification(propertyPath);
    	if (propertyValues != null) {
	    	Collection<PrismPropertyValue<X>> valuesToDelete = toPrismPropertyValues(objectDelta.getPrismContext(), propertyValues);
	    	propertyDelta.addValuesToDelete(valuesToDelete);
	    	objectDelta.addModification(propertyDelta);
    	}
    }
    
    public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationAddContainer(Class<O> type, String oid, 
    		QName propertyName, PrismContext prismContext, PrismContainerValue<C>... containerValues) {
    	return createModificationAddContainer(type, oid, new ItemPath(propertyName), prismContext, containerValues);
    }
    
    public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationAddContainer(Class<O> type, String oid, 
    		ItemPath propertyPath, PrismContext prismContext, PrismContainerValue<C>... containerValues) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setOid(oid);
    	fillInModificationAddContainer(objectDelta, propertyPath, containerValues);
    	return objectDelta;
    }
    
    public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationAddContainer(Class<O> type, String oid, 
    		ItemPath propertyPath, PrismContext prismContext, C... containerValues) throws SchemaException {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setOid(oid);
    	PrismContainerValue<C>[] containerPValues = new PrismContainerValue[containerValues.length];
    	for (int i=0; i<containerValues.length; i++) {
    		C containerable = containerValues[i];
    		prismContext.adopt(containerable, type, propertyPath);
    		containerPValues[i] = containerable.asPrismContainerValue();
    	}
		fillInModificationAddContainer(objectDelta, propertyPath, containerPValues);
    	return objectDelta;
    }
    
    public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationDeleteContainer(Class<O> type, 
    		String oid, QName containerName, PrismContext prismContext, PrismContainerValue<C>... containerValues) {
    	return createModificationDeleteContainer(type, oid, new ItemPath(containerName), prismContext, containerValues);
    }
    
    public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationDeleteContainer(Class<O> type, String oid, ItemPath containerPath,
    		PrismContext prismContext, PrismContainerValue<C>... containerValues) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setOid(oid);
    	fillInModificationDeleteContainer(objectDelta, containerPath, containerValues);
    	return objectDelta;
    }
    
    public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationDeleteContainer(Class<O> type, String oid, 
    		QName containerName, PrismContext prismContext, C... containerValues) throws SchemaException {
    	return createModificationDeleteContainer(type, oid, new ItemPath(containerName), prismContext, containerValues);
    }
    
    public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationDeleteContainer(Class<O> type, String oid, 
    		ItemPath propertyPath, PrismContext prismContext, C... containerValues) throws SchemaException {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setOid(oid);
    	PrismContainerValue<C>[] containerPValues = new PrismContainerValue[containerValues.length];
    	for (int i=0; i<containerValues.length; i++) {
    		C containerable = containerValues[i];
    		prismContext.adopt(containerable, type, propertyPath);
    		containerPValues[i] = containerable.asPrismContainerValue();
    	}
		fillInModificationDeleteContainer(objectDelta, propertyPath, containerPValues);
    	return objectDelta;
    }
    
    protected static <O extends Objectable, C extends Containerable> void fillInModificationDeleteContainer(ObjectDelta<O> objectDelta,
    		ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
    	ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
    	if (containerValues != null) {
	    	containerDelta.addValuesToDelete(containerValues);
    	}
    }
    
    public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationReplaceContainer(Class<O> type, 
    		String oid, QName containerName, PrismContext prismContext, PrismContainerValue<C>... containerValues) {
    	return createModificationReplaceContainer(type, oid, new ItemPath(containerName), prismContext, containerValues);
    }
    
    public static <O extends Objectable, C extends Containerable> ObjectDelta<O> createModificationReplaceContainer(Class<O> type, String oid, ItemPath containerPath,
    		PrismContext prismContext, PrismContainerValue<C>... containerValues) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setOid(oid);
    	fillInModificationReplaceContainer(objectDelta, containerPath, containerValues);
    	return objectDelta;
    }
    
    protected static <O extends Objectable, C extends Containerable> void fillInModificationAddContainer(ObjectDelta<O> objectDelta,
    		ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
    	ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
    	if (containerValues != null) {
	    	containerDelta.addValuesToAdd(containerValues);
    	}
    }
    
    protected static <O extends Objectable, C extends Containerable> void fillInModificationAddContainer(ObjectDelta<O> objectDelta,
    		ItemPath propertyPath, PrismContext prismContext, C... containerables) throws SchemaException {
    	ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
    	if (containerables != null) {
    		for (C containerable: containerables) {
    			prismContext.adopt(containerable, objectDelta.getObjectTypeClass(), propertyPath);
    			PrismContainerValue<C> prismContainerValue = containerable.asPrismContainerValue();
    			containerDelta.addValueToAdd(prismContainerValue);
    		}
    	}
    }
    
    protected static <O extends Objectable, C extends Containerable> void fillInModificationDeleteContainer(ObjectDelta<O> objectDelta,
    		ItemPath propertyPath, PrismContext prismContext, C... containerables) throws SchemaException {
    	ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
    	if (containerables != null) {
    		for (C containerable: containerables) {
    			prismContext.adopt(containerable, objectDelta.getObjectTypeClass(), propertyPath);
    			PrismContainerValue<C> prismContainerValue = containerable.asPrismContainerValue();
    			containerDelta.addValueToDelete(prismContainerValue);
    		}
    	}
    }
    
    protected static <O extends Objectable, C extends Containerable> void fillInModificationReplaceContainer(ObjectDelta<O> objectDelta,
    		ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
    	ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
    	if (containerValues != null) {
	    	containerDelta.setValuesToReplace(containerValues);
    	}
    }
    
    protected static <O extends Objectable, C extends Containerable> void fillInModificationReplaceContainer(ObjectDelta<O> objectDelta,
    		ItemPath propertyPath, C... containerValues) throws SchemaException {
    	if (containerValues != null) {
    		ContainerDelta<C> containerDelta = objectDelta.createContainerModification(propertyPath);
	    	Collection<PrismContainerValue<C>> valuesToReplace = toPrismContainerValues(objectDelta.getObjectTypeClass(), propertyPath, objectDelta.getPrismContext(), containerValues);
	    	containerDelta.setValuesToReplace(valuesToReplace);
	    	objectDelta.addModification(containerDelta);
    	}
    }
        
    protected static <X> Collection<PrismPropertyValue<X>> toPrismPropertyValues(PrismContext prismContext, X... propertyValues) {
    	Collection<PrismPropertyValue<X>> pvalues = new ArrayList<PrismPropertyValue<X>>(propertyValues.length);
    	for (X val: propertyValues) {
    		PrismUtil.recomputeRealValue(val, prismContext);
    		PrismPropertyValue<X> pval = new PrismPropertyValue<X>(val);
    		pvalues.add(pval);
    	}
    	return pvalues;
    }
    
    protected static <O extends Objectable, C extends Containerable> Collection<PrismContainerValue<C>> toPrismContainerValues(Class<O> type, ItemPath path, PrismContext prismContext, C... containerValues) throws SchemaException {
    	Collection<PrismContainerValue<C>> pvalues = new ArrayList<PrismContainerValue<C>>(containerValues.length);
    	for (C val: containerValues) {
    		prismContext.adopt(val, type, path);
    		PrismUtil.recomputeRealValue(val, prismContext);
    		PrismContainerValue<C> pval = val.asPrismContainerValue();
    		pvalues.add(pval);
    	}
    	return pvalues;
    }
    
    /**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method. 
     */
    public static <O extends Objectable> ObjectDelta<O> createModificationAddReference(Class<O> type, String oid, QName propertyName,
    		PrismContext prismContext, PrismObject<?>... referenceObjects) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setOid(oid);
    	PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    	PrismReferenceDefinition refDef = objDef.findReferenceDefinition(propertyName);
    	ReferenceDelta referenceDelta = objectDelta.createReferenceModification(propertyName, refDef);
    	Collection<PrismReferenceValue> valuesToReplace = new ArrayList<PrismReferenceValue>(referenceObjects.length);
    	for (PrismObject<?> refObject: referenceObjects) {
    		PrismReferenceValue refVal = new PrismReferenceValue();
    		refVal.setObject(refObject);
    		valuesToReplace.add(refVal);
    	}
    	referenceDelta.setValuesToReplace(valuesToReplace);
    	return objectDelta;
    }

    public static <O extends Objectable> ObjectDelta<O> createModificationAddReference(Class<O> type, String oid, QName propertyName,
    		PrismContext prismContext, String... targetOids) {
    	PrismReferenceValue[] referenceValues = new PrismReferenceValue[targetOids.length];
    	for(int i=0; i < targetOids.length; i++) {
    		referenceValues[i] = new PrismReferenceValue(targetOids[i]);
    	}
    	return createModificationAddReference(type, oid, propertyName, prismContext, referenceValues);
    }
    
    /**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method. 
     */
    public static <O extends Objectable> ObjectDelta<O> createModificationAddReference(Class<O> type, String oid, QName propertyName,
    		PrismContext prismContext, PrismReferenceValue... referenceValues) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setOid(oid);
    	PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    	PrismReferenceDefinition refDef = objDef.findReferenceDefinition(propertyName);
    	ReferenceDelta referenceDelta = objectDelta.createReferenceModification(propertyName, refDef);
    	Collection<PrismReferenceValue> valuesToAdd = new ArrayList<PrismReferenceValue>(referenceValues.length);
    	for (PrismReferenceValue refVal: referenceValues) {
    		valuesToAdd.add(refVal);
    	}
    	referenceDelta.addValuesToAdd(valuesToAdd);
    	return objectDelta;
    }
    
    
    public static <O extends Objectable> ObjectDelta<O> createModificationDeleteReference(Class<O> type, String oid, QName propertyName,
    		PrismContext prismContext, String... targetOids) {
    	PrismReferenceValue[] referenceValues = new PrismReferenceValue[targetOids.length];
    	for(int i=0; i < targetOids.length; i++) {
    		referenceValues[i] = new PrismReferenceValue(targetOids[i]);
    	}
    	return createModificationDeleteReference(type, oid, propertyName, prismContext, referenceValues);
    }
    
    /**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method. 
     */
    public static <O extends Objectable> ObjectDelta<O> createModificationDeleteReference(Class<O> type, String oid, QName propertyName,
    		PrismContext prismContext, PrismReferenceValue... referenceValues) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setOid(oid);
    	PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    	PrismReferenceDefinition refDef = objDef.findReferenceDefinition(propertyName);
    	ReferenceDelta referenceDelta = objectDelta.createReferenceModification(propertyName, refDef);
    	Collection<PrismReferenceValue> valuesToDelete = new ArrayList<PrismReferenceValue>(referenceValues.length);
    	for (PrismReferenceValue refVal: referenceValues) {
    		valuesToDelete.add(refVal);
    	}
    	referenceDelta.addValuesToDelete(valuesToDelete);
    	return objectDelta;
    }
    
    public static <T extends Objectable> ObjectDelta<T> createModifyDelta(String oid, ItemDelta modification,
    		Class<T> objectTypeClass, PrismContext prismContext) {
    	Collection modifications = new ArrayList<ItemDelta>(1);
    	modifications.add(modification);
    	return createModifyDelta(oid, modifications, objectTypeClass, prismContext);
    }
    
    public static <T extends Objectable> ObjectDelta<T> createModifyDelta(String oid, Collection<? extends ItemDelta> modifications,
    		Class<T> objectTypeClass, PrismContext prismContext) {
    	ObjectDelta<T> objectDelta = new ObjectDelta<T>(objectTypeClass, ChangeType.MODIFY, prismContext);
    	objectDelta.addModifications(modifications);
    	objectDelta.setOid(oid);
    	return objectDelta;
    }
    
    public static <O extends Objectable> ObjectDelta<O> createEmptyAddDelta(Class<O> type, String oid, PrismContext prismContext) {
    	ObjectDelta<O> objectDelta = createEmptyDelta(type, oid, prismContext, ChangeType.ADD);
    	PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    	PrismObject<O> objectToAdd = objDef.instantiate();
    	objectDelta.setObjectToAdd(objectToAdd);
    	return objectDelta;
    }

    public static <O extends Objectable> ObjectDelta<O> createEmptyModifyDelta(Class<O> type, String oid, PrismContext prismContext) {
    	return createEmptyDelta(type, oid, prismContext, ChangeType.MODIFY);
    }

    public static <O extends Objectable> ObjectDelta<O> createEmptyDeleteDelta(Class<O> type, String oid, PrismContext prismContext) {
    	return createEmptyDelta(type, oid, prismContext, ChangeType.DELETE);
    }

    public static <O extends Objectable> ObjectDelta<O> createEmptyDelta(Class<O> type, String oid, PrismContext prismContext, 
    		ChangeType changeType) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, changeType, prismContext);
    	objectDelta.setOid(oid);
    	return objectDelta;
    }
    
    public static <O extends Objectable> ObjectDelta<O> createAddDelta(PrismObject<O> objectToAdd) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(objectToAdd.getCompileTimeClass(), ChangeType.ADD, objectToAdd.getPrismContext());
    	objectDelta.setOid(objectToAdd.getOid());
    	objectDelta.setObjectToAdd(objectToAdd);
    	return objectDelta;
    }
    
    public static <O extends Objectable> ObjectDelta<O> createDeleteDelta(Class<O> type, String oid, PrismContext prismContext) {
    	ObjectDelta<O> objectDelta = new ObjectDelta<O>(type, ChangeType.DELETE, prismContext);
    	objectDelta.setOid(oid);
    	return objectDelta;
    }
        
    public void checkConsistence() {
    	checkConsistence(true, false, false);
    }
    
    public void checkConsistence(boolean requireOid, boolean requireDefinition, boolean prohibitRaw) {
    	if (prismContext == null) {
    		throw new IllegalStateException("No prism context in "+this);
    	}
    	if (getChangeType() == ChangeType.ADD) {
			if (getModifications() != null && !getModifications().isEmpty()) {
				throw new IllegalStateException("Modifications present in ADD delta "+this);
			}
			if (getObjectToAdd() != null) {
				getObjectToAdd().checkConsistence(requireDefinition, prohibitRaw);
			} else {
				throw new IllegalStateException("User primary delta is ADD, but there is not object to add in "+this);
			}
		} else if (getChangeType() == ChangeType.MODIFY) {
			checkIdentifierConsistence(requireOid);
			if (getObjectToAdd() != null) {
				throw new IllegalStateException("Object to add present in MODIFY delta "+this);
			}
			if (getModifications() == null) {
				throw new IllegalStateException("Null modification in MODIFY delta "+this);
			}
			ItemDelta.checkConsistence(getModifications(), requireDefinition, prohibitRaw);
		} else if (getChangeType() == ChangeType.DELETE) {
	    	if (requireOid && getOid() == null) {
	    		throw new IllegalStateException("Null oid in delta "+this);
	    	}
			if (getObjectToAdd() != null) {
				throw new IllegalStateException("Object to add present in DELETE delta "+this);
			}
			if (getModifications() != null && !getModifications().isEmpty()) {
				throw new IllegalStateException("Modifications present in DELETE delta "+this);
			}			
		} else {
			throw new IllegalStateException("Unknown change type "+getChangeType()+" in delta "+this);
		}
    }
    
	protected void checkIdentifierConsistence(boolean requireOid) {
		if (requireOid && getOid() == null) {
    		throw new IllegalStateException("Null oid in delta "+this);
    	}
	}

	public static void checkConsistence(Collection<? extends ObjectDelta<?>> deltas) {
		for (ObjectDelta<?> delta: deltas) {
			delta.checkConsistence();
		}
	}
    
    public void assertDefinitions() throws SchemaException {
    	assertDefinitions("");
    }
    
    public void assertDefinitions(String sourceDescription) throws SchemaException {
    	assertDefinitions(false, sourceDescription);
    }
    
    public void assertDefinitions(boolean tolerateRawElements) throws SchemaException {
    	assertDefinitions(tolerateRawElements, "");
    }
    
    /**
     * Assert that all the items has appropriate definition.
     */
    public void assertDefinitions(boolean tolerateRawElements, String sourceDescription) throws SchemaException {
    	if (changeType == ChangeType.ADD) {
    		objectToAdd.assertDefinitions("add delta in "+sourceDescription);
    	}
    	if (changeType == ChangeType.MODIFY) {
    		for (ItemDelta<?> mod: modifications) {
    			mod.assertDefinitions(tolerateRawElements, "modify delta for "+getOid()+" in "+sourceDescription);
    		}
    	}
    }
    
    public void revive(PrismContext prismContext) throws SchemaException {
    	if (objectToAdd != null) {
    		objectToAdd.revive(prismContext);
    	}
    	if (modifications != null) {
    		for (ItemDelta<?> modification: modifications) {
    			modification.revive(prismContext);
    		}
    	}
        // todo is this correct? [pm]
        if (this.prismContext == null) {
            this.prismContext = prismContext;
        }
	}
    
    public void applyDefinition(PrismObjectDefinition<T> objectDefinition, boolean force) throws SchemaException {
    	if (objectToAdd != null) {
    		objectToAdd.applyDefinition(objectDefinition, force);
    	}
    	if (modifications != null) {
    		for (ItemDelta<?> modification: modifications) {
    			ItemPath path = modification.getPath();
    			ItemDefinition itemDefinition = objectDefinition.findItemDefinition(path);
    			modification.applyDefinition(itemDefinition, force);
    		}
    	}
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((changeType == null) ? 0 : changeType.hashCode());
		result = prime * result
				+ ((objectToAdd == null) ? 0 : objectToAdd.hashCode());
		result = prime * result
				+ ((objectTypeClass == null) ? 0 : objectTypeClass.hashCode());
		result = prime * result + ((oid == null) ? 0 : oid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectDelta<?> other = (ObjectDelta<?>) obj;
		if (changeType != other.changeType)
			return false;
		if (modifications == null) {
			if (other.modifications != null)
				return false;
		} else if (!MiscUtil.unorderedCollectionEquals(this.modifications,other.modifications))
			return false;
		if (objectToAdd == null) {
			if (other.objectToAdd != null)
				return false;
		} else if (!objectToAdd.equals(other.objectToAdd))
			return false;
		if (objectTypeClass == null) {
			if (other.objectTypeClass != null)
				return false;
		} else if (!objectTypeClass.equals(other.objectTypeClass))
			return false;
		if (oid == null) {
			if (other.oid != null)
				return false;
		} else if (!oid.equals(other.oid))
			return false;
		return true;
	}

	@Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(debugName());
        sb.append("(").append(debugIdentifiers());
        sb.append(",").append(changeType).append(": ");
        if (changeType == ChangeType.ADD) {
            if (objectToAdd == null) {
                sb.append("null");
            } else {
                sb.append(objectToAdd.toString());
            }
        } else if (changeType == ChangeType.MODIFY) {
            Iterator<? extends ItemDelta> i = modifications.iterator();
            while (i.hasNext()) {
                sb.append(i.next().toString());
                if (i.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        // Nothing to print for delete
        sb.append(")");
        return sb.toString();
    }
    
    protected String debugName() {
    	return "ObjectDelta";
    }
    
    protected String debugIdentifiers() {
    	return toDebugType()+":" + getOid();
    }
    
    /**
	 * Returns short string identification of object type. It should be in a form
	 * suitable for log messages. There is no requirement for the type name to be unique,
	 * but it rather has to be compact. E.g. short element names are preferred to long
	 * QNames or URIs.
	 * @return
	 */
	public String toDebugType() {
		if (objectTypeClass == null) {
			return "(unknown)";
		}
		return objectTypeClass.getSimpleName();
	}

	@Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(debugName());
        sb.append("<").append(objectTypeClass.getSimpleName()).append(">(");
        sb.append(debugIdentifiers()).append(",").append(changeType).append("):\n");
        if (objectToAdd == null) {
        	if (changeType == ChangeType.ADD) {
	        	DebugUtil.indentDebugDump(sb, indent + 1);
	            sb.append("null");
        	}
        } else {
            sb.append(objectToAdd.debugDump(indent + 1));
        }
        if (modifications != null) {
            Iterator<? extends ItemDelta> i = modifications.iterator();
            while (i.hasNext()) {
                sb.append(i.next().debugDump(indent + 1));
                if (i.hasNext()) {
                    sb.append("\n");
                }
            }
        }
        // Nothing to print for delete
        return sb.toString();
    }

}
