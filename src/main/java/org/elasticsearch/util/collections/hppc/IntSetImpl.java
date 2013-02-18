package org.elasticsearch.util.collections.hppc;

import org.elasticsearch.util.ESCollections.IntSet;

import com.carrotsearch.hppc.IntContainer;
import com.carrotsearch.hppc.IntOpenHashSet;

public class IntSetImpl extends IntOpenHashSet implements IntSet {

    public IntSetImpl() {
        super();
    }

    public IntSetImpl(int[] init) {
        super(init.length);
        this.add(init);
    }

    public IntSetImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public IntSetImpl(int initialCapacity) {
        super(initialCapacity);
    }

    public IntSetImpl(IntContainer container) {
        super(container);
    }

    @Override
    public int[] toArray(int[] dest) {
        int index = 0;
        for (int i = 0; i < keys.length; i++) {
            if(allocated[i]) {
                dest[index++] = keys[i];
            }
        }
        return dest;
    }

    
}
