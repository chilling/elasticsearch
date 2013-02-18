package org.elasticsearch.util.collections.hppc;

import org.elasticsearch.util.ESCollections.IntList;

import com.carrotsearch.hppc.IntArrayList;

public class IntListImpl extends IntArrayList implements IntList {

    public IntListImpl() {
        super();
    }

    public IntListImpl(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public void addX(int data) {
        add(data);
    }

    @Override
    public int[] toArray(int[] data) {
        System.arraycopy(toArray(), 0, data, 0, size());
        return data;
    }
    
}
