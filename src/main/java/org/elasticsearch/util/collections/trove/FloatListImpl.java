package org.elasticsearch.util.collections.trove;

import gnu.trove.list.array.TFloatArrayList;

import org.elasticsearch.util.ESCollections.FloatList;

public class FloatListImpl extends TFloatArrayList implements FloatList {

    @Override
    public void addX(float data) {
        add(data);
    }

}
