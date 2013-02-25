/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.util.collections.hppc.HPPCollections;

public abstract class ESCollections {

    public static class Constants {
//        public static Implementation IMPLEMENTATION = new TroveCollections();
        public static Implementation IMPLEMENTATION = new HPPCollections();
        
        public static final int DEFAULT_CAPACITY = 16;
        public static final float DEFAULT_LOAD_FACTOR = 0.75f;
    }
    
    public static abstract class Implementation {
        
        public abstract <E> Set<E> newSet();
        public abstract <E> Set<E> newSet(int capacity);
        public abstract <E> Set<E> newSet(Collection<E> init);

        public abstract CharSet newCharSet();
        public abstract CharSet newCharSet(char[] chars);

        public abstract IntSet newIntSet();
        public abstract IntSet newIntSet(int[] chars);

        public abstract <K, V> Map<K, V> newMap();
        public abstract <K, V> Map<K, V> newMap(int initialCapacity, float loadFactor);
        
        public abstract <E> ObjectIntMap<E> newObjectIntMap();
        public abstract <E> ObjectIntMap<E> newObjectIntMap(int initialCpacity, float loadFactor, int noEntryValue);
        public abstract <E> ObjectFloatMap<E> newObjectFloatMap();
        public abstract <E> ObjectFloatMap<E> newObjectFloatMap(int initialCpacity, float loadFactor, float noEntryValue);
        public abstract <E> ObjectLongMap<E> newObjectLongMap();
        
        public abstract ByteIntMap newByteIntMap();
        public abstract ShortIntMap newShortIntMap();
        public abstract IntIntMap newIntIntMap();
        public abstract FloatIntMap newFloatIntMap();
        public abstract LongIntMap newLongIntMap();
        public abstract DoubleIntMap newDoubleIntMap();

        public abstract LongLongMap newLongLongMap();
        
        public abstract <E> IntObjectMap<E> newIntObjectMap();
        public abstract <E> FloatObjectMap<E> newFloatObjectMap();
        public abstract <E> LongObjectMap<E> newLongObjectMap();
        public abstract <E> DoubleObjectMap<E> newDoubleObjectMap();

        public abstract ByteList newByteList();
        public abstract ShortList newShortList();
        public abstract IntList newIntList();
        public abstract IntList newIntList(int capacity);
        public abstract FloatList newFloatList();
        public abstract DoubleList newDoubleList();
        public abstract LongList newLongList();
    }
    
//    public static <E> Set<E> newSet() {
//        return Constants.IMPLEMENTATION.newSet();
//    }
//
//    public static <E> Set<E> newSet(int capacity) {
//        return Constants.IMPLEMENTATION.newSet(capacity);
//    }
//    
//    public static <E> Set<E> newSet(Collection<E> init) {
//        return Constants.IMPLEMENTATION.newSet(init);
//    }
//
//    public static <K, V> Map<K, V> newMap() {
//        return Constants.IMPLEMENTATION.newMap();
//    }
//    
//    public static <K, V> Map<K, V> newMap(int initialCapacity, float loadFactor) {
//        return Constants.IMPLEMENTATION.newMap(initialCapacity, loadFactor);
//    }
//    
//    public static <E> ObjectIntMap<E> newObjectIntMap() {
//        return Constants.IMPLEMENTATION.newObjectIntMap();
//    }
//
//    public static <E> ObjectIntMap<E> newObjectIntMap(int initialCpacity, float loadFactor, int noEntryValue) {
//        return Constants.IMPLEMENTATION.newObjectIntMap(initialCpacity, loadFactor, noEntryValue);
//    }
//    
//    public static <E> ObjectFloatMap<E> newObjectFloatMap() {
//        return Constants.IMPLEMENTATION.newObjectFloatMap();
//    }
//    
//    public static <E> ObjectFloatMap<E> newObjectFloatMap(int initialCpacity, float loadFactor, float noEntryValue) {
//        return Constants.IMPLEMENTATION.newObjectFloatMap(initialCpacity, loadFactor, noEntryValue);
//    }
//    
//    public static <E> ObjectLongMap<E> newObjectLongMap() {
//        return Constants.IMPLEMENTATION.newObjectLongMap();
//    }
//    
//    public static IntSet newIntSet() {
//        return Constants.IMPLEMENTATION.newIntSet();
//    }
//    
//    public static IntSet newIntSet(int[] init) {
//        return Constants.IMPLEMENTATION.newIntSet(init);
//    }
//    
//    public static IntIntMap newIntIntMap() {
//        return Constants.IMPLEMENTATION.newIntIntMap();
//    }
//
//    public static <E> IntObjectMap<E> newIntObjectMap() {
//        return Constants.IMPLEMENTATION.newIntObjectMap();
//    }
//
//    public static FloatIntMap newFloatIntMap() {
//        return Constants.IMPLEMENTATION.newFloatIntMap();
//    }
//
//    public static <E> FloatObjectMap<E> newFloatObjectMap() {
//        return Constants.IMPLEMENTATION.newFloatObjectMap();
//    }
//
//    public static LongIntMap newLongIntMap() {
//        return Constants.IMPLEMENTATION.newLongIntMap();
//    }
//    
//    public static <E> LongObjectMap<E> newLongObjectMap() {
//        return Constants.IMPLEMENTATION.newLongObjectMap();
//    }
//    
//    public static DoubleIntMap newDoubleIntMap() {
//        return Constants.IMPLEMENTATION.newDoubleIntMap();
//    }
//    
//    public static <E> DoubleObjectMap<E> newDoubleObjectMap() {
//        return Constants.IMPLEMENTATION.newDoubleObjectMap();
//    }
//
//    public static LongLongMap newLongLongMap() {
//        return Constants.IMPLEMENTATION.newLongLongMap();
//    }
//    
//    public static ByteIntMap newByteIntMap() {
//        return Constants.IMPLEMENTATION.newByteIntMap();
//    }
//    
//    public static CharSet newCharSet() {
//        return Constants.IMPLEMENTATION.newCharSet();
//    }
//
//    public static CharSet newCharSet(char[] chars) {
//        return Constants.IMPLEMENTATION.newCharSet(chars);
//    }
//    
//    public static ByteList newByteList() {
//        return Constants.IMPLEMENTATION.newByteList();
//    }
//    
//    public static ShortList newShortList() {
//        return Constants.IMPLEMENTATION.newShortList();
//    }
//    
//    public static IntList newIntList() {
//        return Constants.IMPLEMENTATION.newIntList();
//    }
//
//    public static IntList newIntList(int capacity) {
//        return Constants.IMPLEMENTATION.newIntList(capacity);
//    }
//    
//    public static FloatList newFloatList() {
//        return Constants.IMPLEMENTATION.newFloatList();
//    }
//    
//    public static DoubleList newDoubleList() {
//        return Constants.IMPLEMENTATION.newDoubleList();
//    }
//
//    public static LongList newLongList() {
//        return Constants.IMPLEMENTATION.newLongList();
//    }
//
//    public static ShortIntMap newShortIntMap() {
//        return Constants.IMPLEMENTATION.newShortIntMap();
//    }
//    
    private static interface PrimCollection {
        public int size();
        public void clear();
    }

    public static interface ByteList extends PrimCollection {
        public byte get(int index);
        public void addX(byte data);
        public byte[] toArray(byte[] data);
    }
    
    public static interface ShortList extends PrimCollection {
        public short get(int index);
        public void addX(short data);
        public short[] toArray(short[] data);
    }
    
   public static interface FloatList extends PrimCollection  {
       public float get(int index);
       public void addX(float data);
       public int size();
       public float[] toArray(float[] data);
   }
   
    public static interface IntList extends PrimCollection  {
        public int get(int index);
        public void addX(int data);
        public int size();
        public int[] toArray(int[] data);
//        public int[] unsafeArray();
    }
    
    public static interface LongList extends PrimCollection  {
        public long get(int index);
        public void addX(long data);
        public long[] toArray(long[] target);
    }

    public static interface DoubleList extends PrimCollection  {
        public double get(int index);
        public void addX(double data);
        public int size();
        public double[] toArray(double[] data);
    }

    public static interface ByteIntMap extends PrimCollection {
        public int put(byte key, int value);
        public int get(byte key);
    }
    
    public static interface ShortIntMap extends PrimCollection {
        public int put(short key, int value);
        public int get(short key);
    }
    
    public static interface IntByteMap extends PrimCollection {
        public byte put(int key, byte value);
        public byte get(int key);
    }
    
    public static interface IntLongMap extends PrimCollection {
        public long put(int key, long value);
        public long get(int key);
    }
    
    public static interface IntIntMap extends PrimCollection  {
        public int put(int key, int value);
        public int get(int key);
    }
    
    public static interface ObjectFloatMap<E> extends PrimCollection  {
        public float put(E key, float value);
        public float adjustOrPutValue(E key, float a, float b);
        public boolean adjustValue(E key, float amount);
        public float getX(Object key);
        public ObjectFloatIterator<E> floatIterator();
    } 
    
    public static interface ObjectIntMap<E> extends PrimCollection {
        public Iterable<E> keySet();
        public int put(E key, int value);
        public int adjustOrPutValue(E key, int a, int b);
        public boolean increment(E key);
        public int getX(Object key);
        public boolean contains(E key);
        public void trimToSize();
        public int freeSlots();
        public int slots();
        public E key(E key);
    } 
    
    public static interface ObjectLongMap<E> extends PrimCollection {
        public long put(E key, long value);
        public long getX(Object key);
        public boolean containsKeyX(E value);
        public ObjectLongIterator<E> longIterator();
    } 
    
    public static interface IntObjectMap<E> extends PrimCollection  {
        public E put(int key, E value);
        public E get(int key);
        public E[] values(E[] target);
    } 
    
    public static interface LongIntMap extends PrimCollection  {
        public int put(long key, int value);
        public int get(long key);
    } 
    
    public static interface LongLongMap extends PrimCollection  {
        public long put(long key, long value);
        public long get(long key);
    } 
    
    public static interface FloatIntMap extends PrimCollection  {
        public int put(float key, int value);
        public int get(float key);
    }
    
    public static interface FloatObjectMap<E> extends PrimCollection  {
        public E put(float key, E value);
        public E get(float key);
    }

    public static interface LongObjectMap<E> extends PrimCollection {
        public E put(long key, E value);
        public E get(long key);
    }

    public static interface DoubleIntMap extends PrimCollection  {
        public int put(double key, int value);
        public int get(double key);
        public int adjustOrPutValue(double key, int adjust_amount, int put_amount);
    }

    public static interface DoubleObjectMap<E> extends PrimCollection {
        public E put(double key, E value);
        public E get(double key);
    }

    public static interface CharSet extends PrimCollection {
        public boolean contains(char c);
    }

    public static interface IntSet extends PrimCollection {
        public boolean contains(int c);
        public boolean remove(int key);
        public int[] toArray();
        public int[] toArray(int[] dest);
    }

    public static interface DoubleSet extends PrimCollection {
        public boolean contains(double d);
    }
   
    public static interface ObjectFloatIterator<E> {
        public E key();
        public float value();
        public boolean hasNext();
        public void advance();
    }

    public static interface ObjectLongIterator<E> {
        public E key();
        public long value();
        public boolean hasNext();
        public void advance();
        public void remove();
    }
}
