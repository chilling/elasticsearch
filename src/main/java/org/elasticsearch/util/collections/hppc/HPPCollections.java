package org.elasticsearch.util.collections.hppc;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.util.ESCollections;
import org.elasticsearch.util.ESCollections.*;

public class HPPCollections extends ESCollections.Implementation {

    @Override
    public <E> Set<E> newSet() {
        return new BaseSetImpl<E>();
    }

    @Override
    public <E> Set<E> newSet(int capacity) {
        return new BaseSetImpl<E>(capacity);
    }

    @Override
    public <E> Set<E> newSet(Collection<E> init) {
        return new BaseSetImpl<E>(init);
    }

    @Override
    public CharSet newCharSet() {
        return new CharSetImpl();
    }

    @Override
    public CharSet newCharSet(char[] chars) {
        return new CharSetImpl(chars);
    }

    @Override
    public <K, V> Map<K, V> newMap() {
        return new BaseMapImpl<K, V>();
    }

    @Override
    public <K, V> Map<K, V> newMap(int initialCapacity, float loadFactor) {
        return new BaseMapImpl<K, V>(initialCapacity, loadFactor);
    }

    @Override
    public IntSetImpl newIntSet() {
        return new IntSetImpl();
    }
    
    @Override
    public IntSetImpl newIntSet(int[] init) {
        return new IntSetImpl(init);
    }
    
    @Override
    public <E> ObjectIntMap<E> newObjectIntMap() {
        return new ObjectIntMapImpl<E>();
    }

    @Override
    public <E> ObjectIntMap<E> newObjectIntMap(int initialCapacity, float loadFactor, int noEntryValue) {
        return new ObjectIntMapImpl<E>(initialCapacity, loadFactor);
    }

    @Override
    public <E> ObjectFloatMap<E> newObjectFloatMap() {
        return new ObjectFloatMapImpl<E>();
    }

    @Override
    public <E> ObjectFloatMap<E> newObjectFloatMap(int initialCapacity, float loadFactor, float noEntryValue) {
        return new ObjectFloatMapImpl<E>(initialCapacity, loadFactor);
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
