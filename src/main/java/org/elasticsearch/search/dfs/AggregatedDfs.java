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

package org.elasticsearch.search.dfs;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;

import com.carrotsearch.hppc.ObjectObjectOpenHashMap;

import java.io.IOException;

/**
 *
 */
public class AggregatedDfs implements Streamable {

    private ObjectObjectOpenHashMap<Term, TermStatistics> termStatistics;
    private ObjectObjectOpenHashMap<String, CollectionStatistics> fieldStatistics;
    private long maxDoc;

    private AggregatedDfs() {

    }

    public AggregatedDfs(ObjectObjectOpenHashMap<Term, TermStatistics> termStatistics, ObjectObjectOpenHashMap<String, CollectionStatistics> fieldStatistics, long maxDoc) {
        this.termStatistics = termStatistics;
        this.fieldStatistics = fieldStatistics;
        this.maxDoc = maxDoc;
    }

    public ObjectObjectOpenHashMap<Term, TermStatistics> termStatistics() {
        return termStatistics;
    }

    public ObjectObjectOpenHashMap<String, CollectionStatistics> fieldStatistics() {
        return fieldStatistics;
    }

    public long maxDoc() {
        return maxDoc;
    }

    public static AggregatedDfs readAggregatedDfs(StreamInput in) throws IOException {
        AggregatedDfs result = new AggregatedDfs();
        result.readFrom(in);
        return result;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        int size = in.readVInt();
        termStatistics = new ObjectObjectOpenHashMap<Term, TermStatistics>(size, ObjectObjectOpenHashMap.DEFAULT_LOAD_FACTOR);
        for (int i = 0; i < size; i++) {
            Term term = new Term(in.readString(), in.readBytesRef());
            TermStatistics stats = new TermStatistics(in.readBytesRef(), in.readVLong(), in.readVLong());
            termStatistics.put(term, stats);
        }
        size = in.readVInt();
        fieldStatistics = new ObjectObjectOpenHashMap<String, CollectionStatistics>(size, ObjectObjectOpenHashMap.DEFAULT_LOAD_FACTOR);
        for (int i = 0; i < size; i++) {
            String field = in.readString();
            CollectionStatistics stats = new CollectionStatistics(field, in.readVLong(), in.readVLong(), in.readVLong(), in.readVLong());
            fieldStatistics.put(field, stats);
        }
        maxDoc = in.readVLong();
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        out.writeVInt(termStatistics.size());
        
        final boolean[] tStatsAllocated = termStatistics.allocated;
        final Term[] tStatsKeys = termStatistics.keys;
        final TermStatistics[] tStatsValues = termStatistics.values;
        
        for(int i=0; i<tStatsAllocated.length; i++) {
            if(tStatsAllocated[i]) {
                Term term = tStatsKeys[i];
                out.writeString(term.field());
                out.writeBytesRef(term.bytes());
                TermStatistics stats = tStatsValues[i];
                out.writeBytesRef(stats.term());
                out.writeVLong(stats.docFreq());
                out.writeVLong(stats.totalTermFreq());
            }
        }
        
        out.writeVInt(fieldStatistics.size());

        final boolean[] fStatsAllocated = fieldStatistics.allocated;
        final String[] fStatsKeys = fieldStatistics.keys;
        final CollectionStatistics[] fStatsValues = fieldStatistics.values;

        for (int i = 0; i < fStatsAllocated.length; i++) {
            if(fStatsAllocated[i]) {
                out.writeString(fStatsKeys[i]);
                out.writeVLong(fStatsValues[i].maxDoc());
                out.writeVLong(fStatsValues[i].docCount());
                out.writeVLong(fStatsValues[i].sumTotalTermFreq());
                out.writeVLong(fStatsValues[i].sumDocFreq());
            }
        }

        out.writeVLong(maxDoc);
    }
}
