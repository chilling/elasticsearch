package org.elasticsearch.util.collections.trove;

import java.util.Collection;

import org.elasticsearch.util.ESCollections.CharSet;

import gnu.trove.TCharCollection;
import gnu.trove.set.hash.TCharHashSet;

public class CharSetImpl extends TCharHashSet implements CharSet {

    public CharSetImpl() {
        super();
    }

    public CharSetImpl(char[] array) {
        super(array);
    }

    public CharSetImpl(Collection<? extends Character> collection) {
        super(collection);
    }

    public CharSetImpl(int initial_capacity, float load_factor, char no_entry_value) {
        super(initial_capacity, load_factor, no_entry_value);
    }

    public CharSetImpl(int initialCapacity, float load_factor) {
        super(initialCapacity, load_factor);
    }

    public CharSetImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public CharSetImpl(TCharCollection collection) {
        super(collection);
    }

}
