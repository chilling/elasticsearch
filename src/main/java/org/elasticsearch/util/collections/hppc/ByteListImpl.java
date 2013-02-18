package org.elasticsearch.util.collections.hppc;

import org.elasticsearch.util.ESCollections.ByteList;

import com.carrotsearch.hppc.ByteArrayList;

public class ByteListImpl extends ByteArrayList implements ByteList {

    @Override
    public byte[] toArray(byte[] data) {
        System.arraycopy(super.toArray(),0,data,0,size());
        return data;
    }
    
    @Override
    public void addX(byte e1) {
        super.add(e1);
    }

}
