package org.elasticsearch.util.collections.trove;

import java.util.Collection;

import org.elasticsearch.util.ESCollections;
import org.elasticsearch.util.ESCollections.ByteIntMap;
import org.elasticsearch.util.ESCollections.ByteList;
import org.elasticsearch.util.ESCollections.DoubleIntMap;
import org.elasticsearch.util.ESCollections.DoubleList;
import org.elasticsearch.util.ESCollections.DoubleObjectMap;
import org.elasticsearch.util.ESCollections.FloatIntMap;
import org.elasticsearch.util.ESCollections.FloatList;
import org.elasticsearch.util.ESCollections.FloatObjectMap;
import org.elasticsearch.util.ESCollections.IntIntMap;
import org.elasticsearch.util.ESCollections.IntList;
import org.elasticsearch.util.ESCollections.IntObjectMap;
import org.elasticsearch.util.ESCollections.LongIntMap;
import org.elasticsearch.util.ESCollections.LongList;
import org.elasticsearch.util.ESCollections.LongLongMap;
import org.elasticsearch.util.ESCollections.LongObjectMap;
import org.elasticsearch.util.ESCollections.ObjectFloatMap;
import org.elasticsearch.util.ESCollections.ObjectLongMap;
import org.elasticsearch.util.ESCollections.ShortIntMap;
import org.elasticsearch.util.ESCollections.ShortList;

import com.carrotsearch.hppc.IntSet;

public class TroveCollections extends ESCollections.Implementation {

    @Override
    public <E> BaseSetImpl<E> newSet() {
        return new BaseSetImpl<E>();
    }

    @Override
    public <E> BaseSetImpl<E> newSet(int capacity) {
        return new BaseSetImpl<E>(capacity);
    }

    @Override
    public <E> BaseSetImpl<E> newSet(Collection<E> init) {
        return new BaseSetImpl<E>(init);
    }

    @Override
    public CharSetImpl newCharSet() {
        return new CharSetImpl();
    }

    @Override
    public CharSetImpl newCharSet(char[] chars) {
        return new CharSetImpl(chars);
    }

    @Override
    public <K, V> BaseMapImpl<K, V> newMap() {
        return new BaseMapImpl<K, V>();
    }

    @Override
    public <K, V> BaseMapImpl<K, V> newMap(int initialCapacity, float loadFactor) {
        return new BaseMapImpl<K, V>(initialCapacity, loadFactor);
    }

    @Override
    public <E> ObjectIntMapImpl<E> newObjectIntMap() {
        return new ObjectIntMapImpl<E>();
    }

    @Override
    public <E> ObjectIntMapImpl<E> newObjectIntMap(int initialCapacity, float loadFactor, int noEntryValue) {
        return new ObjectIntMapImpl<E>(initialCapacity, loadFactor, noEntryValue);
    }

    @Override
    public <E> ObjectFloatMap<E> newObjectFloatMap() {
        return new ObjectFloatMapImpl<E>();
    }

    @Override
    public <E> ObjectFloatMap<E> newObjectFloatMap(int initialCapacity, float loadFactor, float noEntryValue) {
        return new ObjectFloatMapImpl<E>(initialCapacity, loadFactor, noEntryValue);
    }

    @Override
    public <E> ObjectLongMap<E> newObjectLongMap() {
        return new ObjectLongMapImpl<E>();
    }

    @Override
    public ByteIntMap newByteIntMap() {
        return new ByteIntMapImpl();
    }

    @Override
    public ShortIntMap newShortIntMap() {
        return new ShortIntMapImpl();
    }

    @Override
    public IntIntMap newIntIntMap() {
        return new IntIntMapImpl();
    }

    public IntSetImpl newIntSet() {
        return new IntSetImpl();
    }
    
    public IntSetImpl newIntSet(int[] ints) {
        return new IntSetImpl(ints);
    }
    
    @Override
    public FloatIntMap newFloatIntMap() {
        return new FloatIntMapImpl();
    }

    @Override
    public LongIntMap newLongIntMap() {
        return new LongIntMapImpl();
    }

    @Override
    public DoubleIntMap newDoubleIntMap() {
        return new DoubleIntMapImpl();
    }

    @Override
    public LongLongMap newLongLongMap() {
        return new LongLongMapImpl();
    }

    @Override
    public <E> IntObjectMap<E> newIntObjectMap() {
        return new IntObjectMapImpl<E>();
    }

    @Override
    public <E> FloatObjectMap<E> newFloatObjectMap() {
        return new FloatObjectMapImpl<E>();
    }

    @Override
    public <E> LongObjectMap<E> newLongObjectMap() {
        return new LongObjectMapImpl<E>();
    }

    @Override
    public <E> DoubleObjectMap<E> newDoubleObjectMap() {
        return new DoubleObjectMapImpl<E>();
    }

    @Override
    public ByteList newByteList() {
        return new ByteListImpl();
    }

    @Override
    public ShortList newShortList() {
        return new ShortListImpl();
    }

    @Override
    public IntList newIntList() {
        return new IntListImpl();
    }

    @Override
    public IntList newIntList(int capacity) {
        return new IntListImpl(capacity);
    }

    @Override
    public FloatList newFloatList() {
        return new FloatListImpl();
    }

    @Override
    public DoubleList newDoubleList() {
        return new DoubleListImpl();
    }

    @Override
    public LongList newLongList() {
        return new LongListImpl();
    }


}
