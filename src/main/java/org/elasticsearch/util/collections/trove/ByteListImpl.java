package org.elasticsearch.util.collections.trove;

import gnu.trove.list.array.TByteArrayList;

import org.elasticsearch.util.ESCollections.ByteList;

public class ByteListImpl extends TByteArrayList implements ByteList {
    
    @Override
    public void addX(byte data) {
        add(data);
    }

}
