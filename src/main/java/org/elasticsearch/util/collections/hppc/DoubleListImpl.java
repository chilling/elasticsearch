package org.elasticsearch.util.collections.hppc;

import org.elasticsearch.util.ESCollections.DoubleList;

import com.carrotsearch.hppc.DoubleArrayList;

public class DoubleListImpl extends DoubleArrayList implements DoubleList {
    
    @Override
    public void addX(double e1) {
        super.add(e1);
    }
    
    @Override
    public double[] toArray(double[] data) {
        System.arraycopy(toArray(), 0, data, 0, size());
        return data;
    }

}
