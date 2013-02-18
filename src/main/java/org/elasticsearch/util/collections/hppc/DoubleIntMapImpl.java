package org.elasticsearch.util.collections.hppc;


import org.elasticsearch.util.ESCollections.DoubleIntMap;

import com.carrotsearch.hppc.DoubleIntOpenHashMap;

public class DoubleIntMapImpl extends DoubleIntOpenHashMap implements DoubleIntMap {

    @Override
    public int adjustOrPutValue(double key, int adjust_amount, int put_amount) {
        return putOrAdd(key, adjust_amount, put_amount);
    }
    
}
