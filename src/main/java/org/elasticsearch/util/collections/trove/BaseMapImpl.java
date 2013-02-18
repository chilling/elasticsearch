package org.elasticsearch.util.collections.trove;

import gnu.trove.map.hash.THashMap;

import java.util.Map;

public class BaseMapImpl<K, V> extends THashMap<K, V> implements Map<K, V> {

    public BaseMapImpl() {
        super();
    }

    public BaseMapImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public BaseMapImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public BaseMapImpl(Map<? extends K, ? extends V> map) {
        super(map);
    }

    public BaseMapImpl(THashMap<? extends K, ? extends V> map) {
        super(map);
    }

    
}
