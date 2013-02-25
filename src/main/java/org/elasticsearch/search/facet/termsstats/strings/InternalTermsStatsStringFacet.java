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

package org.elasticsearch.search.facet.termsstats.strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.CacheRecycler;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.lucene.HashedBytesRef;
import org.elasticsearch.common.text.BytesText;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.termsstats.InternalTermsStatsFacet;

import com.google.common.collect.ImmutableList;

public class InternalTermsStatsStringFacet extends InternalTermsStatsFacet {

    private static final String STREAM_TYPE = "tTS";

    public static void registerStream() {
        Streams.registerStream(STREAM, STREAM_TYPE);
    }

    static Stream STREAM = new Stream() {
        @Override
        public Facet readFacet(String type, StreamInput in) throws IOException {
            return readTermsStatsFacet(in);
        }
    };

    @Override
    public String streamType() {
        return STREAM_TYPE;
    }

    public InternalTermsStatsStringFacet() {
    }

    public static class StringEntry implements Entry {

        Text term;
        long count;
        long totalCount;
        double total;
        double min;
        double max;

        public StringEntry(HashedBytesRef term, long count, long totalCount, double total, double min, double max) {
            this(new BytesText(new BytesArray(term.bytes)), count, totalCount, total, min, max);
        }

        public StringEntry(Text term, long count, long totalCount, double total, double min, double max) {
            this.term = term;
            this.count = count;
            this.totalCount = totalCount;
            this.total = total;
            this.min = min;
            this.max = max;
        }

        @Override
        public Text getTerm() {
            return term;
        }

        @Override
        public Number getTermAsNumber() {
            return Double.parseDouble(term.string());
        }

        @Override
        public long getCount() {
            return count;
        }

        @Override
        public long getTotalCount() {
            return this.totalCount;
        }

        @Override
        public double getMin() {
            return this.min;
        }

        @Override
        public double getMax() {
            return this.max;
        }

        @Override
        public double getTotal() {
            return total;
        }

        @Override
        public double getMean() {
            if (totalCount == 0) {
                return 0;
            }
            return total / totalCount;
        }

        @Override
        public int compareTo(Entry other) {
            return term.compareTo(other.getTerm());
        }
    }

    int requiredSize;
    long missing;
    Collection<StringEntry> entries = ImmutableList.of();
    ComparatorType comparatorType;

    public InternalTermsStatsStringFacet(String name, ComparatorType comparatorType, int requiredSize, Collection<StringEntry> entries, long missing) {
        super(name);
        this.comparatorType = comparatorType;
        this.requiredSize = requiredSize;
        this.entries = entries;
        this.missing = missing;
    }

    @Override
    public List<StringEntry> getEntries() {
        if (!(entries instanceof List)) {
            entries = ImmutableList.copyOf(entries);
        }
        return (List<StringEntry>) entries;
    }

    List<StringEntry> mutableList() {
        if (!(entries instanceof List)) {
            entries = new ArrayList<StringEntry>(entries);
        }
        return (List<StringEntry>) entries;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public Iterator<Entry> iterator() {
        return (Iterator) entries.iterator();
    }

    @Override
    public long getMissingCount() {
        return this.missing;
    }

    @Override
    public Facet reduce(List<Facet> facets) {
        if (facets.size() == 1) {
            if (requiredSize == 0) {
                // we need to sort it here!
                InternalTermsStatsStringFacet tsFacet = (InternalTermsStatsStringFacet) facets.get(0);
                if (!tsFacet.entries.isEmpty()) {
                    List<StringEntry> entries = tsFacet.mutableList();
                    Collections.sort(entries, comparatorType.comparator());
                }
            }
            return facets.get(0);
        }
        int missing = 0;
        Map<Text, StringEntry> map = CacheRecycler.popHashMap();
        for (Facet facet : facets) {
            InternalTermsStatsStringFacet tsFacet = (InternalTermsStatsStringFacet) facet;
            missing += tsFacet.missing;
            for (Entry entry : tsFacet) {
                StringEntry stringEntry = (StringEntry) entry;
                StringEntry current = map.get(stringEntry.getTerm());
                if (current != null) {
                    current.count += stringEntry.count;
                    current.totalCount += stringEntry.totalCount;
                    current.total += stringEntry.total;
                    if (stringEntry.min < current.min) {
                        current.min = stringEntry.min;
                    }
                    if (stringEntry.max > current.max) {
                        current.max = stringEntry.max;
                    }
                } else {
                    map.put(stringEntry.getTerm(), stringEntry);
                }
            }
        }

        // sort
        if (requiredSize == 0) { // all terms
            StringEntry[] entries1 = map.values().toArray(new StringEntry[map.size()]);
            Arrays.sort(entries1, comparatorType.comparator());
            CacheRecycler.pushHashMap(map);
            return new InternalTermsStatsStringFacet(getName(), comparatorType, requiredSize, Arrays.asList(entries1), missing);
        } else {
            Object[] values = map.values().toArray();
            Arrays.sort(values, (Comparator) comparatorType.comparator());
            List<StringEntry> ordered = new ArrayList<StringEntry>(map.size());
            for (int i = 0; i < requiredSize; i++) {
                StringEntry value = (StringEntry) values[i];
                if (value == null) {
                    break;
                }
                ordered.add(value);
            }
            CacheRecycler.pushHashMap(map);
            return new InternalTermsStatsStringFacet(getName(), comparatorType, requiredSize, ordered, missing);
        }
    }

    static final class Fields {
        static final XContentBuilderString _TYPE = new XContentBuilderString("_type");
        static final XContentBuilderString MISSING = new XContentBuilderString("missing");
        static final XContentBuilderString TERMS = new XContentBuilderString("terms");
        static final XContentBuilderString TERM = new XContentBuilderString("term");
        static final XContentBuilderString COUNT = new XContentBuilderString("count");
        static final XContentBuilderString TOTAL_COUNT = new XContentBuilderString("total_count");
        static final XContentBuilderString TOTAL = new XContentBuilderString("total");
        static final XContentBuilderString MIN = new XContentBuilderString("min");
        static final XContentBuilderString MAX = new XContentBuilderString("max");
        static final XContentBuilderString MEAN = new XContentBuilderString("mean");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(getName());
        builder.field(Fields._TYPE, InternalTermsStatsFacet.TYPE);
        builder.field(Fields.MISSING, missing);
        builder.startArray(Fields.TERMS);
        for (Entry entry : entries) {
            builder.startObject();
            builder.field(Fields.TERM, entry.getTerm());
            builder.field(Fields.COUNT, entry.getCount());
            builder.field(Fields.TOTAL_COUNT, entry.getTotalCount());
            builder.field(Fields.MIN, entry.getMin());
            builder.field(Fields.MAX, entry.getMax());
            builder.field(Fields.TOTAL, entry.getTotal());
            builder.field(Fields.MEAN, entry.getMean());
            builder.endObject();
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    public static InternalTermsStatsStringFacet readTermsStatsFacet(StreamInput in) throws IOException {
        InternalTermsStatsStringFacet facet = new InternalTermsStatsStringFacet();
        facet.readFrom(in);
        return facet;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        comparatorType = ComparatorType.fromId(in.readByte());
        requiredSize = in.readVInt();
        missing = in.readVLong();

        int size = in.readVInt();
        entries = new ArrayList<StringEntry>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new StringEntry(in.readText(), in.readVLong(), in.readVLong(), in.readDouble(), in.readDouble(), in.readDouble()));
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeByte(comparatorType.id());
        out.writeVInt(requiredSize);
        out.writeVLong(missing);

        out.writeVInt(entries.size());
        for (Entry entry : entries) {
            out.writeText(entry.getTerm());
            out.writeVLong(entry.getCount());
            out.writeVLong(entry.getTotalCount());
            out.writeDouble(entry.getTotal());
            out.writeDouble(entry.getMin());
            out.writeDouble(entry.getMax());
        }
    }
}