package org.elasticsearch.util.collections.trove;

import java.util.Collection;

import org.elasticsearch.util.ESCollections.IntSet;

import gnu.trove.TIntCollection;
import gnu.trove.set.hash.TIntHashSet;

public class IntSetImpl extends TIntHashSet implements IntSet {

    public IntSetImpl() {
        super();
    }

    public IntSetImpl(Collection<? extends Integer> collection) {
        super(collection);
    }

    public IntSetImpl(int initial_capacity, float load_factor, int no_entry_value) {
        super(initial_capacity, load_factor, no_entry_value);
    }

    public IntSetImpl(int initialCapacity, float load_factor) {
        super(initialCapacity, load_factor);
    }

    public IntSetImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public IntSetImpl(int[] array) {
        super(array);
    }

    public IntSetImpl(TIntCollection collection) {
        super(collection);
    }

    
    
}
