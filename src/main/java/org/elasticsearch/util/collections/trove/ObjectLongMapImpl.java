package org.elasticsearch.util.collections.trove;

import org.elasticsearch.util.ESCollections.ObjectLongIterator;
import org.elasticsearch.util.ESCollections.ObjectLongMap;

import gnu.trove.iterator.TObjectLongIterator;
import gnu.trove.map.hash.TObjectLongHashMap;

public class ObjectLongMapImpl<E> extends TObjectLongHashMap<E> implements ObjectLongMap<E> {

    @Override
    public long getX(Object key) {
        return get(key);
    }

    @Override
    public boolean containsKeyX(E value) {
        return containsKey(value);
    }

    @Override
    public ObjectLongIterator<E> longIterator() {
        return new ObjectLongIteratorImpl();
    }
    
    class ObjectLongIteratorImpl implements ObjectLongIterator<E> {

        TObjectLongIterator<E> iterator = ObjectLongMapImpl.this.iterator();
        
        @Override
        public E key() {
            return iterator.key();
        }

        @Override
        public long value() {
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
        
        @Override
        public void remove() {
            iterator.remove();
        }
    }
    
}
