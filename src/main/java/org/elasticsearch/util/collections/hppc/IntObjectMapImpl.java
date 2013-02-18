package org.elasticsearch.util.collections.hppc;

import org.elasticsearch.util.ESCollections.IntObjectMap;

import com.carrotsearch.hppc.IntObjectOpenHashMap;

public class IntObjectMapImpl<E> extends IntObjectOpenHashMap<E> implements IntObjectMap<E> {

    @Override
    public E[] values(E[] target) {
        System.arraycopy(super.values, 0, target, 0, size());
        return target;
    }
    
}
