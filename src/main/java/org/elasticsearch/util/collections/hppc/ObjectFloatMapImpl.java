package org.elasticsearch.util.collections.hppc;

import java.util.Iterator;

import org.elasticsearch.util.ESCollections.ObjectFloatIterator;
import org.elasticsearch.util.ESCollections.ObjectFloatMap;

import com.carrotsearch.hppc.ObjectFloatOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectFloatCursor;

public class ObjectFloatMapImpl<E> extends ObjectFloatOpenHashMap<E> implements ObjectFloatMap<E> {

    public ObjectFloatMapImpl() {
        super();
    }

    public ObjectFloatMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ObjectFloatMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public float adjustOrPutValue(E key, float a, float b) {
        return putOrAdd(key, a, b);
    }

    @Override
    public boolean adjustValue(E key, float amount) {
        return adjustValue(key, amount);
    }

    @Override
    public float getX(Object key) {
        return get((E)key);
    }

    @Override
    public ObjectFloatIterator<E> floatIterator() {
        return new ObjectFloatIteratorImpl(super.iterator());
    }
    
    class ObjectFloatIteratorImpl implements ObjectFloatIterator<E> {
        final Iterator<ObjectFloatCursor<E>> iterator;
        ObjectFloatCursor<E> current;
        
        ObjectFloatIteratorImpl(Iterator<ObjectFloatCursor<E>> iterator) {
            this.iterator = iterator;
        }
        
        @Override
        public E key() {
            return current.key;
        }

        @Override
        public float value() {
            return current.value;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public void advance() {
            iterator.next();
        }
    }
   
}
