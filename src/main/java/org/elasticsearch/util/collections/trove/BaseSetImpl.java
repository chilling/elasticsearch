package org.elasticsearch.util.collections.trove;

import gnu.trove.set.hash.THashSet;

import java.util.Collection;
import java.util.Set;

public class BaseSetImpl<E> extends THashSet<E> implements Set<E> {

    public BaseSetImpl() {
        super();
    }

    public BaseSetImpl(Collection<? extends E> collection) {
        super(collection);
    }

    public BaseSetImpl(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public BaseSetImpl(int initialCapacity) {
        super(initialCapacity);
    }

}
