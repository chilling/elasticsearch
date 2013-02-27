package org.elasticsearch.common.hppc;

import com.carrotsearch.hppc.ObjectFloatAssociativeContainer;
import com.carrotsearch.hppc.ObjectFloatOpenHashMap;

public class DefaultObjectFloatOpenHashMap<E> extends ObjectFloatOpenHashMap<E> {

    private final float defaultvalue;

    public DefaultObjectFloatOpenHashMap(float defaultvalue) {
        super();
        this.defaultvalue = defaultvalue;
    }

    public DefaultObjectFloatOpenHashMap(int initialCapacity, float loadFactor, float defaultvalue) {
        super(initialCapacity, loadFactor);
        this.defaultvalue = defaultvalue;
    }

    public DefaultObjectFloatOpenHashMap(int initialCapacity, float defaultvalue) {
        super(initialCapacity);
        this.defaultvalue = defaultvalue;
    }

    public DefaultObjectFloatOpenHashMap(ObjectFloatAssociativeContainer<E> container, float defaultvalue) {
        super(container);
        this.defaultvalue = defaultvalue;
    }

    @Override
    public float get(E key) {
        if(!containsKey(key)) {
            return defaultvalue;
        } else {
            return lget();
        }
    }
    
}
