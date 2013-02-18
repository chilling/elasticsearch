package org.elasticsearch.util.collections.trove;

import org.elasticsearch.util.ESCollections.ObjectFloatIterator;
import org.elasticsearch.util.ESCollections.ObjectFloatMap;

import gnu.trove.iterator.TObjectFloatIterator;
import gnu.trove.map.TObjectFloatMap;
import gnu.trove.map.hash.TObjectFloatHashMap;

public class ObjectFloatMapImpl<E> extends TObjectFloatHashMap<E> implements ObjectFloatMap<E> {

    public ObjectFloatMapImpl() {
        super();
    }

    public ObjectFloatMapImpl(int initialCapacity, float loadFactor, float noEntryValue) {
        super(initialCapacity, loadFactor, noEntryValue);
    }

    public ObjectFloatMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ObjectFloatMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public ObjectFloatMapImpl(TObjectFloatMap<? extends E> map) {
        super(map);
    }

    @Override
    public float getX(Object key) {
        return get(key);
    }
    
    @Override
    public ObjectFloatIterator<E> floatIterator() {
        return new ObjectFloatIteratorImpl();
    }
    
    class ObjectFloatIteratorImpl implements ObjectFloatIterator<E> {

        final TObjectFloatIterator<E> iterator = ObjectFloatMapImpl.this.iterator();
        
        @Override
        public E key() {
            return iterator.key();
        }

        @Override
        public float value() {
            return iterator.value();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public void advance() {
            iterator.advance();
        }
        
    }
    
}
