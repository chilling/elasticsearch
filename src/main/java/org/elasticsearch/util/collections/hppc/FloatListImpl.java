package org.elasticsearch.util.collections.hppc;

import org.elasticsearch.util.ESCollections.FloatList;
import com.carrotsearch.hppc.FloatArrayList;

public class FloatListImpl extends FloatArrayList implements FloatList {

    @Override
    public void addX(float e1) {
        super.add(e1);
    }
    
    @Override
    public float[] toArray(float[] data) {
        System.arraycopy(toArray(), 0, data, 0, size());
        return data;
    }

}
