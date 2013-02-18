package org.elasticsearch.util.collections.trove;

import org.elasticsearch.common.RamUsage;
import org.elasticsearch.common.bytes.HashedBytesArray;
import org.elasticsearch.util.ESCollections.ObjectIntMap;

import gnu.trove.impl.hash.TObjectHash;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class ObjectIntMapImpl<E> extends TObjectIntHashMap<E> implements ObjectIntMap<E> {
    
    public ObjectIntMapImpl() {
        super();
    }

    public ObjectIntMapImpl(int initialCapacity, float loadFactor, int noEntryValue) {
        super(initialCapacity, loadFactor, noEntryValue);
    }

    public ObjectIntMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ObjectIntMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public ObjectIntMapImpl(TObjectIntMap<? extends E> map) {
        super(map);
    }

    public E key(E key) {
        int index = index(key);
        return index < 0 ? null : (E) _set[index];
    }
    
    @Override
    public int slots() {
        return _set.length;
    }
    
    @Override
    public int freeSlots() {
        int free = 0;
        for (Object o : _set) {
            if (o == TObjectHash.FREE || o == TObjectHash.REMOVED) {
                free++;
            }
        }
        return free;
    }
    
    @Override
    public int getX(Object key) {
        return get(key);
    }

}
