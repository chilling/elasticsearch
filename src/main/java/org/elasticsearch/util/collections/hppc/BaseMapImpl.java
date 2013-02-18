package org.elasticsearch.util.collections.hppc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.carrotsearch.hppc.ObjectObjectOpenHashMap;
import com.carrotsearch.hppc.cursors.ObjectObjectCursor;

public class BaseMapImpl<K, V> implements Map<K, V> {

    final ObjectObjectOpenHashMap<K, V> wrapped;
    final HashEntrySet entries = new HashEntrySet();
    final KeySet keyset = new KeySet();
    final ValueCollection values = new ValueCollection(); 
    
    public BaseMapImpl() {
        super();
        this.wrapped = new ObjectObjectOpenHashMap<K, V>();
    }

    public BaseMapImpl(int initialCapacity, float loadFactor) {
        super();
        this.wrapped = new ObjectObjectOpenHashMap<K, V>(initialCapacity, loadFactor);
    }

    public BaseMapImpl(Map<K, V> container) {
        super();
        this.wrapped = new ObjectObjectOpenHashMap<K, V>();
        for (Map.Entry<K, V> entry : container.entrySet()) {
            this.wrapped.put(entry.getKey(), entry.getValue());
        }
    }
    
    @Override
    public void clear() {
        wrapped.clear();
    }
    
    @Override
    public boolean containsKey(Object key) {
        return wrapped.containsKey((K)key);
    }
    
    @Override
    public boolean containsValue(Object value) {
        return wrapped.values().contains((V)value);
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return entries;
    }
    
    @Override
    public V get(Object key) {
        return wrapped.get((K)key);
    }
    
    @Override
    public boolean isEmpty() {
        return wrapped.isEmpty();
    }
    
    @Override
    public V put(K key, V value) {
        return wrapped.put(key, value);
    }
    
    @Override
    public V remove(Object key) {
        return wrapped.remove((K)key);
    }
    
    @Override
    public int size() {
        return wrapped.size();
    }
    
    @Override
    public Set<K> keySet() {
        return keyset;
    }
    
    @Override
    public Collection<V> values() {
        return values;
    }
    
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            wrapped.put(entry.getKey(), entry.getValue());
        }
    }
    
    class HashEntrySet implements Set<Map.Entry<K, V>> {

        @Override
        public boolean add(java.util.Map.Entry<K, V> entry) {
            V old = BaseMapImpl.this.wrapped.put(entry.getKey(), entry.getValue());
            return old != entry.getValue();
        }

        @Override
        public boolean addAll(Collection<? extends java.util.Map.Entry<K, V>> entries) {
            boolean changed = false;
            for(Map.Entry<K, V> entry : entries) {
                changed |= add(entry);
            }
            return changed;
        }

        @Override
        public void clear() {
            BaseMapImpl.this.clear();
        }

        @Override
        public boolean contains(Object object) {
            if (object instanceof Map.Entry) {
                Map.Entry<K, V> entry = (Map.Entry<K, V>) object;
                K key = entry.getKey();
                if(!BaseMapImpl.this.wrapped.containsKey(key))
                    return false;

                V value = entry.getValue();
                V inset = BaseMapImpl.this.wrapped.lget();
                return inset != null && inset.equals(value);
            }
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> entries) {
            for (Object object : entries) {
                if(!contains(object)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isEmpty() {
            return BaseMapImpl.this.wrapped.isEmpty();
        }

        @Override
        public boolean remove(Object object) {
            if (object instanceof Map.Entry) {
                Map.Entry<K, V> entry = (Map.Entry<K, V>) object;
                K key = entry.getKey();
                if(BaseMapImpl.this.wrapped.containsKey(key)) {
                    V inset = BaseMapImpl.this.wrapped.lget();
                    if(inset != null && inset.equals(entry.getValue())) {
                        BaseMapImpl.this.wrapped.remove(key);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> entries) {
            boolean all = true;
            for(Object object : entries) {
                all = remove(object) && all;
            }
            return all;
        }

        @Override
        public boolean retainAll(Collection<?> entries) {
            throw new RuntimeException("Not Yet Implemented");
            //TODO: Implement method retainAll
        }

        @Override
        public int size() {
            return BaseMapImpl.this.wrapped.size();
        }

        @Override
        public Object[] toArray() {
            return toArray(new Map.Entry<?, ?>[size()]);
        }

        @Override
        public <T> T[] toArray(T[] dest) {
            Object[] data = dest;
            final K[] keys = BaseMapImpl.this.wrapped.keys;
            final boolean[] states = BaseMapImpl.this.wrapped.allocated;
            
            int index = 0;
            for (int i = 0; i < states.length; i++) {
                if(states[i]) {
                    data[index++] = new SetEnty(keys[i]);
                }
            }
            
            return dest;
        }
        
        @Override
        public Iterator<java.util.Map.Entry<K, V>> iterator() {
            return new HashMapIterator();
        }
        
        class SetEnty implements Map.Entry<K, V> {

            final K key;

            public SetEnty(K key) {
                super();
                this.key = key;
            }

            @Override
            public K getKey() {
                return key;
            }

            @Override
            public V getValue() {
                return BaseMapImpl.this.wrapped.get(key);
            }

            @Override
            public V setValue(V value) {
                return BaseMapImpl.this.wrapped.put(key, value);
            }
            
        }
        
        class HashMapIterator implements Iterator<Map.Entry<K, V>> {

            final Iterator<ObjectObjectCursor<K, V>> iterator;
            final IteratorEntry entry;
            ObjectObjectCursor<K, V> next;
            
            public HashMapIterator() {
                this.iterator = BaseMapImpl.this.wrapped.iterator();
                this.entry = new IteratorEntry();
            }
            
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public java.util.Map.Entry<K, V> next() {
                 next = iterator.next();
                return entry;
            }

            @Override
            public void remove() {
                iterator.remove();
            }
            
            class IteratorEntry implements Entry<K, V> {
                @Override
                public K getKey() {
                    return next.key;
                }
                
                @Override
                public V getValue() {
                   return next.value;
                }
                
                @Override
                public V setValue(V value) {
                    return BaseMapImpl.this.wrapped.values[next.index] = value;
                }
            }
            
        }
    } 

    class KeySet implements Set<K> {

        @Override
        public boolean add(K e) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends K> c) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void clear() {
            BaseMapImpl.this.clear();
        }

        @Override
        public boolean contains(Object o) {
            return BaseMapImpl.this.wrapped.containsKey((K)o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isEmpty() {
            return wrapped.isEmpty();
        }

        @Override
        public Iterator<K> iterator() {
            return new CollectionWrapper.IteratorWrapper<K>(wrapped.keys().iterator());
        }

        @Override
        public boolean remove(Object o) {
            return wrapped.remove((K)o) != null;
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
            return false;
        }

        @Override
        public int size() {
            return wrapped.size();
        }

        @Override
        public Object[] toArray() {
            return toArray(new Object[wrapped.assigned]);
        }

        @Override
        public <T> T[] toArray(T[] a) {
            Object[] data = a;
            final K[] keys = BaseMapImpl.this.wrapped.keys;
            final boolean[] states = BaseMapImpl.this.wrapped.allocated;
            
            int index = 0;
            for (int i = 0; i < states.length; i++) {
                if(states[i]) {
                    data[index++] = keys[i];
                }
            }
            
            return a;
        }
        
    }
    
    class ValueCollection implements Collection<V> {
        
        @Override
        public int size() {
            return wrapped.size();
        }
        
        @Override
        public void clear() {
            wrapped.clear();
        }
        
        @Override
        public boolean isEmpty() {
            return wrapped.isEmpty();
        }
        
        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }
        
        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object object : c) {
                if(!contains(object)) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public Object[] toArray() {
            return toArray(new Object[wrapped.assigned]);
        }
        
        @Override
        public <T> T[] toArray(T[] a) {
            Object[] data = a;
            final V[] values = BaseMapImpl.this.wrapped.values;
            final boolean[] states = BaseMapImpl.this.wrapped.allocated;
            
            int index = 0;
            for (int i = 0; i < states.length; i++) {
                if(states[i]) {
                    data[index++] = values[i];
                }
            }
            
            return a;
        }
        
        @Override
        public Iterator<V> iterator() {
            return new CollectionWrapper.IteratorWrapper<V>(wrapped.values().iterator());
        }

        @Override
        public boolean add(V e) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends V> c) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean remove(Object o) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            // TODO Auto-generated method stub
            return false;
        }
    }
}
