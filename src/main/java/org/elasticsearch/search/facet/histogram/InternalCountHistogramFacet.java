/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search.facet.histogram;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.elasticsearch.common.CacheRecycler;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.search.facet.Facet;

import com.carrotsearch.hppc.LongLongOpenHashMap;

/**
 *
 */
public class InternalCountHistogramFacet extends InternalHistogramFacet {

    private static final String STREAM_TYPE = "cHistogram";

    public static void registerStreams() {
        Streams.registerStream(STREAM, STREAM_TYPE);
    }

    static Stream STREAM = new Stream() {
        @Override
        public Facet readFacet(String type, StreamInput in) throws IOException {
            return readHistogramFacet(in);
        }
    };

    @Override
    public String streamType() {
        return STREAM_TYPE;
    }


    /**
     * A histogram entry representing a single entry within the result of a histogram facet.
     */
    public static class CountEntry implements Entry {
        private final long key;
        private final long count;

        public CountEntry(long key, long count) {
            this.key = key;
            this.count = count;
        }

        @Override
        public long getKey() {
            return key;
        }

        @Override
        public long getCount() {
            return count;
        }

        @Override
        public double getTotal() {
            return Double.NaN;
        }

        @Override
        public long getTotalCount() {
            return 0;
        }

        @Override
        public double getMean() {
            return Double.NaN;
        }

        @Override
        public double getMin() {
            return Double.NaN;
        }

        @Override
        public double getMax() {
            return Double.NaN;
        }
    }

    ComparatorType comparatorType;
    LongLongOpenHashMap counts;
    boolean cachedCounts;
    CountEntry[] entries = null;

    private InternalCountHistogramFacet() {
    }

    public InternalCountHistogramFacet(String name, ComparatorType comparatorType, LongLongOpenHashMap counts, boolean cachedCounts) {
        super(name);
        this.comparatorType = comparatorType;
        this.counts = counts;
        this.cachedCounts = cachedCounts;
    }

    @Override
    public List<CountEntry> getEntries() {
        return Arrays.asList(computeEntries());
    }

    @Override
    public Iterator<Entry> iterator() {
        return (Iterator) getEntries().iterator();
    }

    private CountEntry[] computeEntries() {
        if (entries != null) {
            return entries;
        }
        entries = new CountEntry[counts.size()];
        int j = 0;
        final boolean[] allocated = counts.allocated;
        final long[] keys = counts.keys;
        final long[] values = counts.values;
        int assigned = counts.assigned;

        for (int i = 0; j<assigned; i++) {
            if(allocated[i]) {
                entries[j++] = new CountEntry(keys[i], values[i]);
            }
        }
        releaseCache();
        Arrays.sort(entries, comparatorType.comparator());
        return entries;
    }

    void releaseCache() {
        if (cachedCounts) {
            CacheRecycler.pushLongLongMap(counts);
            cachedCounts = false;
            counts = null;
        }
    }

    @Override
    public Facet reduce(List<Facet> facets) {
        if (facets.size() == 1) {
            return facets.get(0);
        }
        LongLongOpenHashMap counts = CacheRecycler.popLongLongMap();

        for (Facet facet : facets) {
            InternalCountHistogramFacet histoFacet = (InternalCountHistogramFacet) facet;
            
            final boolean[] allocated = histoFacet.counts.allocated;
            final long[] keys = histoFacet.counts.keys;
            final long[] values = histoFacet.counts.values;
            int assigned = histoFacet.counts.assigned;

            for (int i = 0; assigned>0; i++) {
                if(allocated[i]) {
                    assigned--;
                    counts.putOrAdd(keys[i], values[i], values[i]);
                }
            }
            
            histoFacet.releaseCache();
        }

        return new InternalCountHistogramFacet(getName(), comparatorType, counts, true);
    }

    static final class Fields {
        static final XContentBuilderString _TYPE = new XContentBuilderString("_type");
        static final XContentBuilderString ENTRIES = new XContentBuilderString("entries");
        static final XContentBuilderString KEY = new XContentBuilderString("key");
        static final XContentBuilderString COUNT = new XContentBuilderString("count");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(getName());
        builder.field(Fields._TYPE, HistogramFacet.TYPE);
        builder.startArray(Fields.ENTRIES);
        for (Entry entry : computeEntries()) {
            builder.startObject();
            builder.field(Fields.KEY, entry.getKey());
            builder.field(Fields.COUNT, entry.getCount());
            builder.endObject();
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    public static InternalCountHistogramFacet readHistogramFacet(StreamInput in) throws IOException {
        InternalCountHistogramFacet facet = new InternalCountHistogramFacet();
        facet.readFrom(in);
        return facet;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        comparatorType = ComparatorType.fromId(in.readByte());

        int size = in.readVInt();
        counts = CacheRecycler.popLongLongMap();
        cachedCounts = true;
        for (int i = 0; i < size; i++) {
            long key = in.readLong();
            counts.put(key, in.readVLong());
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeByte(comparatorType.id());
        // optimize the write, since we know we have the same buckets as keys
        out.writeVInt(counts.size());
        
        final boolean[] allocated = counts.allocated;
        final long[] keys = counts.keys;
        final long[] values = counts.values;
        int assigned = counts.assigned;

        for (int i = 0; assigned>0; i++) {
            if(allocated[i]) {
                assigned--;
                out.writeLong(keys[i]);
                out.writeVLong(values[i]);
            }
        }
        releaseCache();
    }
}