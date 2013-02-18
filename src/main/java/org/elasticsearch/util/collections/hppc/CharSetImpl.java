package org.elasticsearch.util.collections.hppc;

import org.elasticsearch.util.ESCollections.CharSet;

import com.carrotsearch.hppc.CharOpenHashSet;


public class CharSetImpl extends CharOpenHashSet implements CharSet {

    public CharSetImpl() {
        super();
    }

    public CharSetImpl(char[] chars) {
        super();
        add(chars);
    }

}
