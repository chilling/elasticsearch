package org.elasticsearch.util.collections.trove;

import gnu.trove.list.array.TDoubleArrayList;

import org.elasticsearch.util.ESCollections.DoubleList;

public class DoubleListImpl extends TDoubleArrayList implements DoubleList {

    @Override
    public void addX(double data) {
        add(data);
    }

}
