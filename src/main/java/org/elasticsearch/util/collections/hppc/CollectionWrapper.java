package org.elasticsearch.util.collections.hppc;

import java.util.Iterator;

import com.carrotsearch.hppc.ObjectContainer;
import com.carrotsearch.hppc.cursors.ObjectCursor;

public class CollectionWrapper<E> implements Iterable<E> {

    final ObjectContainer<E> container;
    
    public CollectionWrapper(ObjectContainer<E> container) {
        super();
        this.container = container;
    }

    @Override
    public Iterator<E> iterator() {
        return new IteratorWrapper<E>(container.iterator());
    }
    
    protected static class IteratorWrapper<F> implements Iterator<F> {

        final Iterator<ObjectCursor<F>> iterator; 
        
        protected IteratorWrapper(Iterator<ObjectCursor<F>> iterator) {
            super();
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public F next() {
            return iterator.next().value;
        }

        @Override
        public void remove() {
            iterator.remove();
        }
        
    }
    
}
