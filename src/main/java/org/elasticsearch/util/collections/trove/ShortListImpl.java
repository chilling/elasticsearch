package org.elasticsearch.util.collections.trove;

import gnu.trove.list.array.TShortArrayList;

import org.elasticsearch.util.ESCollections.ShortList;

public class ShortListImpl extends TShortArrayList implements ShortList {

    @Override
    public void addX(short data) {
        add(data);
    }

}
