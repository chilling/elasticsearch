package org.elasticsearch.util.collections.trove;

import gnu.trove.TIntCollection;
import gnu.trove.list.array.TIntArrayList;

import org.elasticsearch.util.ESCollections.IntList;

public class IntListImpl extends TIntArrayList implements IntList {

    public IntListImpl() {
        super();
    }

    public IntListImpl(int capacity, int no_entry_value) {
        super(capacity, no_entry_value);
    }

    public IntListImpl(int capacity) {
        super(capacity);
    }

    public IntListImpl(int[] values, int no_entry_value, boolean wrap) {
        super(values, no_entry_value, wrap);
    }

    public IntListImpl(int[] values) {
        super(values);
    }

    public IntListImpl(TIntCollection collection) {
        super(collection);
    }
    
    @Override
    public void addX(int data) {
        add(data);
    }


}
