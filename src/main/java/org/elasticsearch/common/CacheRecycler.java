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

package org.elasticsearch.common;

import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.util.collections.hppc.BaseMapImpl;
import org.elasticsearch.util.collections.hppc.BaseSetImpl;

import com.carrotsearch.hppc.*;

import com.carrotsearch.hppc.ObjectOpenHashSet;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class CacheRecycler {

    public static void clear() {
        hashMap.clear();
        hashSet.clear();
        DoubleObjectMap.clear();
        LongObjectMap.clear();
        LongLongMap.clear();
        IntIntMap.clear();
        FloatIntMap.clear();
        DoubleIntMap.clear();
        ShortIntMap.clear();
        LongIntMap.clear();
        ObjectIntMap.clear();
        IntObjectMap.clear();
        ObjectFloatMap.clear();
        objectArray.clear();
        intArray.clear();
    }

    static class SoftWrapper<T> {
        private SoftReference<T> ref;

        public SoftWrapper() {
        }

        public void set(T ref) {
            this.ref = new SoftReference<T>(ref);
        }

        public T get() {
            return ref == null ? null : ref.get();
        }

        public void clear() {
            ref = null;
        }
    }

    // ----- ExtTHashMap -----

    private final static SoftWrapper<Queue<Map<?, ?>>> hashMap = new SoftWrapper<Queue<Map<?, ?>>>();

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> popHashMap() {
        Queue<Map<?, ?>> ref = hashMap.get();
        if (ref == null) {
            return new HashMap<K, V>();
        }
        Map map = ref.poll();
        if (map == null) {
            return new HashMap<K, V>();
        }
        return map;
    }

    public static void pushHashMap(Map<?, ?> map) {
        Queue<Map<?, ?>> ref = hashMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            hashMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }

    // ----- THashSet -----

    private final static SoftWrapper<Queue<Set<?>>> hashSet = new SoftWrapper<Queue<Set<?>>>();

    @SuppressWarnings("unchecked")
    public static <T> Set<T> popHashSet() {
        Queue<Set<?>> ref = hashSet.get();
        if (ref == null) {
            return new BaseSetImpl<T>();
        }
        Set set = ref.poll();
        if (set == null) {
            return new BaseSetImpl<T>();
        }
        return set;
    }

    public static void pushHashSet(Set<?> map) {
        Queue<Set<?>> ref = hashSet.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            hashSet.set(ref);
        }
        map.clear();
        ref.add(map);
    }

    // ------ ExtTDoubleObjectMap -----

    private final static SoftWrapper<Queue<DoubleObjectMap<?>>> DoubleObjectMap = new SoftWrapper<Queue<DoubleObjectMap<?>>>();

    @SuppressWarnings("unchecked")
    public static <T> DoubleObjectMap<T> popDoubleObjectMap() {
        Queue<DoubleObjectMap<?>> ref = DoubleObjectMap.get();
        if (ref == null) {
            return new DoubleObjectOpenHashMap<T>();
        }
        DoubleObjectMap map = ref.poll();
        if (map == null) {
            return new DoubleObjectOpenHashMap<T>();
        }
        return map;
    }

    public static void pushDoubleObjectMap(DoubleObjectMap<?> map) {
        Queue<DoubleObjectMap<?>> ref = DoubleObjectMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            DoubleObjectMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }

    // ----- ExtTLongObjectMap ----

    private final static SoftWrapper<Queue<LongObjectMap<?>>> LongObjectMap = new SoftWrapper<Queue<LongObjectMap<?>>>();

    @SuppressWarnings("unchecked")
    public static <T> LongObjectMap<T> popLongObjectMap() {
        Queue<LongObjectMap<?>> ref = LongObjectMap.get();
        if (ref == null) {
            return new LongObjectOpenHashMap<T>();
        }
        LongObjectMap map = ref.poll();
        if (map == null) {
            return new LongObjectOpenHashMap<T>();
        }
        return map;
    }

    public static void pushLongObjectMap(LongObjectMap<?> map) {
        Queue<LongObjectMap<?>> ref = LongObjectMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            LongObjectMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }

    // ----- TLongLongMap ----

    private final static SoftWrapper<Queue<LongLongMap>> LongLongMap = new SoftWrapper<Queue<LongLongMap>>();

    public static LongLongMap popLongLongMap() {
        Queue<LongLongMap> ref = LongLongMap.get();
        if (ref == null) {
            return new LongLongOpenHashMap();
        }
        LongLongMap map = ref.poll();
        if (map == null) {
            return new LongLongOpenHashMap();
        }
        return map;
    }

    public static void pushLongLongMap(LongLongMap map) {
        Queue<LongLongMap> ref = LongLongMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            LongLongMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }

    // ----- TIntIntMap ----

    private final static SoftWrapper<Queue<IntIntMap>> IntIntMap = new SoftWrapper<Queue<IntIntMap>>();


    public static IntIntMap popIntIntMap() {
        Queue<IntIntMap> ref = IntIntMap.get();
        if (ref == null) {
            return new IntIntOpenHashMap();
        }
        IntIntMap map = ref.poll();
        if (map == null) {
            return new IntIntOpenHashMap();
        }
        return map;
    }

    public static void pushIntIntMap(IntIntMap map) {
        Queue<IntIntMap> ref = IntIntMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            IntIntMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }


    // ----- TFloatIntMap ---

    private final static SoftWrapper<Queue<FloatIntMap>> FloatIntMap = new SoftWrapper<Queue<FloatIntMap>>();


    public static FloatIntMap popFloatIntMap() {
        Queue<FloatIntMap> ref = FloatIntMap.get();
        if (ref == null) {
            return new FloatIntOpenHashMap();
        }
        FloatIntMap map = ref.poll();
        if (map == null) {
            return new FloatIntOpenHashMap();
        }
        return map;
    }

    public static void pushFloatIntMap(FloatIntMap map) {
        Queue<FloatIntMap> ref = FloatIntMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            FloatIntMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }


    // ----- TDoubleIntMap ---

    private final static SoftWrapper<Queue<DoubleIntMap>> DoubleIntMap = new SoftWrapper<Queue<DoubleIntMap>>();


    public static DoubleIntMap popDoubleIntMap() {
        Queue<DoubleIntMap> ref = DoubleIntMap.get();
        if (ref == null) {
            return new DoubleIntOpenHashMap();
        }
        DoubleIntMap map = ref.poll();
        if (map == null) {
            return new DoubleIntOpenHashMap();
        }
        return map;
    }

    public static void pushDoubleIntMap(DoubleIntMap map) {
        Queue<DoubleIntMap> ref = DoubleIntMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            DoubleIntMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }


    // ----- TByteIntMap ---

    private final static SoftWrapper<Queue<ByteIntMap>> ByteIntMap = new SoftWrapper<Queue<ByteIntMap>>();


    public static ByteIntMap popByteIntMap() {
        Queue<ByteIntMap> ref = ByteIntMap.get();
        if (ref == null) {
            return ESCollections.newByteIntMap();
        }
        ByteIntMap map = ref.poll();
        if (map == null) {
            return ESCollections.newByteIntMap();
        }
        return map;
    }

    public static void pushByteIntMap(ByteIntMap map) {
        Queue<ByteIntMap> ref = ByteIntMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            ByteIntMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }

    // ----- TShortIntMap ---

    private final static SoftWrapper<Queue<ShortIntMap>> ShortIntMap = new SoftWrapper<Queue<ShortIntMap>>();


    public static ShortIntMap popShortIntMap() {
        Queue<ShortIntMap> ref = ShortIntMap.get();
        if (ref == null) {
            return ESCollections.newShortIntMap();
        }
        ShortIntMap map = ref.poll();
        if (map == null) {
            return ESCollections.newShortIntMap();
        }
        return map;
    }

    public static void pushShortIntMap(ShortIntMap map) {
        Queue<ShortIntMap> ref = ShortIntMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            ShortIntMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }


    // ----- TLongIntMap ----

    private final static SoftWrapper<Queue<LongIntMap>> LongIntMap = new SoftWrapper<Queue<LongIntMap>>();


    public static LongIntMap popLongIntMap() {
        Queue<LongIntMap> ref = LongIntMap.get();
        if (ref == null) {
            return ESCollections.newLongIntMap();
        }
        LongIntMap map = ref.poll();
        if (map == null) {
            return ESCollections.newLongIntMap();
        }
        return map;
    }

    public static void pushLongIntMap(LongIntMap map) {
        Queue<LongIntMap> ref = LongIntMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            LongIntMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }

    // ------ TObjectIntMap -----

    private final static SoftWrapper<Queue<ObjectIntMap<?>>> ObjectIntMap = new SoftWrapper<Queue<ObjectIntMap<?>>>();


    @SuppressWarnings({"unchecked"})
    public static <T> ObjectIntMap<T> popObjectIntMap() {
        Queue<ObjectIntMap<?>> ref = ObjectIntMap.get();
        if (ref == null) {
            return ESCollections.newObjectIntMap();
        }
        ObjectIntMap map = ref.poll();
        if (map == null) {
            return ESCollections.newObjectIntMap();
        }
        return map;
    }

    public static <T> void pushObjectIntMap(ObjectIntMap<T> map) {
        Queue<ObjectIntMap<?>> ref = ObjectIntMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            ObjectIntMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }

    // ------ TIntObjectMap -----

    private final static SoftWrapper<Queue<IntObjectMap<?>>> IntObjectMap = new SoftWrapper<Queue<IntObjectMap<?>>>();


    @SuppressWarnings({"unchecked"})
    public static <T> IntObjectMap<T> popIntObjectMap() {
        Queue<IntObjectMap<?>> ref = IntObjectMap.get();
        if (ref == null) {
            return ESCollections.newIntObjectMap();
        }
        IntObjectMap map = ref.poll();
        if (map == null) {
            return ESCollections.newIntObjectMap();
        }
        return map;
    }

    public static <T> void pushIntObjectMap(IntObjectMap<T> map) {
        Queue<IntObjectMap<?>> ref = IntObjectMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            IntObjectMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }

    // ------ TObjectFloatMap -----

    private final static SoftWrapper<Queue<ObjectFloatMap<?>>> ObjectFloatMap = new SoftWrapper<Queue<ObjectFloatMap<?>>>();

    @SuppressWarnings({"unchecked"})
    public static <T> ObjectFloatMap<T> popObjectFloatMap() {
        Queue<ObjectFloatMap<?>> ref = ObjectFloatMap.get();
        if (ref == null) {
            return ESCollections.newObjectFloatMap();
        }
        ObjectFloatMap map = ref.poll();
        if (map == null) {
            return ESCollections.newObjectFloatMap();
        }
        return map;
    }

    public static <T> void pushObjectFloatMap(ObjectFloatMap<T> map) {
        Queue<ObjectFloatMap<?>> ref = ObjectFloatMap.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            ObjectFloatMap.set(ref);
        }
        map.clear();
        ref.add(map);
    }

    // ----- int[] -----

    private final static SoftWrapper<Queue<Object[]>> objectArray = new SoftWrapper<Queue<Object[]>>();

    public static Object[] popObjectArray(int size) {
        size = size < 100 ? 100 : size;
        Queue<Object[]> ref = objectArray.get();
        if (ref == null) {
            return new Object[size];
        }
        Object[] objects = ref.poll();
        if (objects == null) {
            return new Object[size];
        }
        if (objects.length < size) {
            return new Object[size];
        }
        return objects;
    }

    public static void pushObjectArray(Object[] objects) {
        Queue<Object[]> ref = objectArray.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            objectArray.set(ref);
        }
        Arrays.fill(objects, null);
        ref.add(objects);
    }


    private final static SoftWrapper<Queue<int[]>> intArray = new SoftWrapper<Queue<int[]>>();

    public static int[] popIntArray(int size) {
        return popIntArray(size, 0);
    }

    public static int[] popIntArray(int size, int sentinal) {
        size = size < 100 ? 100 : size;
        Queue<int[]> ref = intArray.get();
        if (ref == null) {
            int[] ints = new int[size];
            if (sentinal != 0) {
                Arrays.fill(ints, sentinal);
            }
            return ints;
        }
        int[] ints = ref.poll();
        if (ints == null) {
            ints = new int[size];
            if (sentinal != 0) {
                Arrays.fill(ints, sentinal);
            }
            return ints;
        }
        if (ints.length < size) {
            ints = new int[size];
            if (sentinal != 0) {
                Arrays.fill(ints, sentinal);
            }
            return ints;
        }
        return ints;
    }

    public static void pushIntArray(int[] ints) {
        pushIntArray(ints, 0);
    }

    public static void pushIntArray(int[] ints, int sentinal) {
        Queue<int[]> ref = intArray.get();
        if (ref == null) {
            ref = ConcurrentCollections.newQueue();
            intArray.set(ref);
        }
        Arrays.fill(ints, sentinal);
        ref.add(ints);
    }
}