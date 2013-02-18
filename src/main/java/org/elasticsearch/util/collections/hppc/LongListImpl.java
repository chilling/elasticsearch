package org.elasticsearch.util.collections.hppc;

import org.elasticsearch.util.ESCollections.LongList;

import com.carrotsearch.hppc.LongArrayList;

public class LongListImpl extends LongArrayList implements LongList {

    @Override
    public void addX(long data) {
        add(data);
    }
    
    @Override
    public long[] toArray(long[] target) {
        System.arraycopy(toArray(), 0, target, 0, size());
        return target;
    }
    
}
