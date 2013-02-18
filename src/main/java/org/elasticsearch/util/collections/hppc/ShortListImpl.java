package org.elasticsearch.util.collections.hppc;

import org.elasticsearch.util.ESCollections.ShortList;

import com.carrotsearch.hppc.ShortArrayList;

public class ShortListImpl extends ShortArrayList implements ShortList {

    @Override
    public void addX(short data) {
        add(data);
    }
    
    @Override
    public short[] toArray(short[] data) {
        System.arraycopy(toArray(), 0, data, 0, size());
        return data;
    }
    
}
