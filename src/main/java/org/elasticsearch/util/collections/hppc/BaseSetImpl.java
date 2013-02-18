package org.elasticsearch.util.collections.hppc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.carrotsearch.hppc.ObjectOpenHashSet;

public class BaseSetImpl<E> implements Set<E> {

    final ObjectOpenHashSet<E> wrapped;
    
    public BaseSetImpl() {
        super();
        this.wrapped = new ObjectOpenHashSet<E>();
    }
    
    public BaseSetImpl(int initialCapacity, float loadFactor) {
        super();
        this.wrapped = new ObjectOpenHashSet<E>(initialCapacity, loadFactor);
    }

    public BaseSetImpl(int initialCapacity) {
        super();
        this.wrapped = new ObjectOpenHashSet<E>(initialCapacity);
    }

    public BaseSetImpl(Collection<? extends E> init) {
        super();
        this.wrapped = new ObjectOpenHashSet<E>(init.size());
        for (E e : init) {
            wrapped.add(e);
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new CollectionWrapper.IteratorWrapper<E>(wrapped.iterator());
    }
    
    @Override
    public Object[] toArray() {
        return toArray(new Object[wrapped.assigned]);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        Object[] data = a;
        
        final E[] values = wrapped.keys;
        final boolean[] states = wrapped.allocated;
        
        int index = 0;
        for (int i = 0; i < states.length; i++) {
            if(states[i]) {
                data[index++] = values[i];
            }
        }

        return a;
    }
    
    @Override
    public boolean add(E e) {
        return wrapped.add(e);
    }
    
    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for(E e : c) {
            modified = wrapped.add(e) || modified;
        }
        return modified;
    }
    
    @Override
    public void clear() {
        wrapped.clear();
    }
    
    @Override
    public boolean contains(Object o) {
        return wrapped.contains((E)o);
    }
    
    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object object : c) {
            if(!wrapped.contains((E)object)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public boolean isEmpty() {
        return wrapped.isEmpty();
    }
    
    @Override
    public boolean remove(Object o) {
        return wrapped.remove((E)o);
    }
    
    @Override
    public int size() {
        return wrapped.assigned;
    }
    
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object object : c) {
            modified = remove(object) || modified;
        }
        return modified;
    }
    
    @Override
    public boolean retainAll(Collection<?> c) {
        // TODO Auto-generated method stub
        throw new RuntimeException("not implemented yet");
    }
}
