package org.elasticsearch.util.collections.hppc;

import org.elasticsearch.util.ESCollections.ObjectIntMap;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class ObjectIntMapImpl<E> extends ObjectIntOpenHashMap<E> implements ObjectIntMap<E> {
    
    public ObjectIntMapImpl() {
        super();
    }

    public ObjectIntMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ObjectIntMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public E key(E key) {
//        int index = index(key);
//        return index < 0 ? null : (E) _set[index];
        return key;
    }

    @Override
    public Iterable<E> keySet() {
        return null;
    }

    @Override
    public int adjustOrPutValue(E key, int a, int b) {
        return putOrAdd(key, a, b);
    }

    @Override
    public boolean increment(E key) {
        return putOrAdd(key, 1, 1)>1;
    }

    @Override
    public int getX(Object key) {
        return get((E)key);
    }

    @Override
    public boolean contains(E key) {
        return containsKey(key);
    }

    @Override
    public int slots() {
        return allocated.length;
    }
    
    @Override
    public int freeSlots() {
        return allocated.length - assigned;
    }
    
    @Override
    public void trimToSize() {
        // TODO Auto-generated method stub
    }
    
}
