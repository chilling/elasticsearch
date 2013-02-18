package org.elasticsearch.util.collections.trove;

import gnu.trove.list.array.TLongArrayList;

import org.elasticsearch.util.ESCollections.LongList;

public class LongListImpl extends TLongArrayList implements LongList {

    @Override
    public void addX(long data) {
        add(data);
    }
    
}
