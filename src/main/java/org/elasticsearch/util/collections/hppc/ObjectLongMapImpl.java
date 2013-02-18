package org.elasticsearch.util.collections.hppc;

import java.util.Iterator;

import org.elasticsearch.util.ESCollections.ObjectLongIterator;
import org.elasticsearch.util.ESCollections.ObjectLongMap;

import com.carrotsearch.hppc.ObjectLongOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectLongCursor;

public class ObjectLongMapImpl<E> extends ObjectLongOpenHashMap<E> implements ObjectLongMap<E> {

    @Override
    public long getX(Object key) {
        return get((E)key);
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

        final Iterator<ObjectLongCursor<E>> iterator = ObjectLongMapImpl.this.iterator();
        ObjectLongCursor<E> cursor;
        
        @Override
        public E key() {
            return cursor.key;
        }
        @Override
        public long value() {
            return cursor.value;
        }
        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }
        @Override
        public void advance() {
            cursor = iterator.next();
        }
        @Override
        public void remove() {
            iterator.remove();
        }
        
    }

}
