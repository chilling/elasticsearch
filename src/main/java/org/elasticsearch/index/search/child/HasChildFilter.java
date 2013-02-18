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

package org.elasticsearch.index.search.child;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Bits;
import org.elasticsearch.ElasticSearchIllegalStateException;
import org.elasticsearch.common.CacheRecycler;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.bytes.HashedBytesArray;
import org.elasticsearch.common.lucene.docset.MatchDocIdSet;
import org.elasticsearch.common.lucene.search.NoopCollector;
import org.elasticsearch.index.cache.id.IdReaderTypeCache;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
public abstract class HasChildFilter extends Filter implements SearchContext.Rewrite {

    final Query childQuery;
    final String parentType;
    final String childType;
    final SearchContext searchContext;

    protected HasChildFilter(Query childQuery, String parentType, String childType, SearchContext searchContext) {
        this.searchContext = searchContext;
        this.parentType = parentType;
        this.childType = childType;
        this.childQuery = childQuery;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("child_filter[").append(childType).append("/").append(parentType).append("](").append(childQuery).append(')');
        return sb.toString();
    }

    public static HasChildFilter create(Query childQuery, String parentType, String childType, SearchContext searchContext) {
        return new Uid(childQuery, parentType, childType, searchContext);
    }

    static class Uid extends HasChildFilter {

        Set<HashedBytesArray> collectedUids;

        Uid(Query childQuery, String parentType, String childType, SearchContext searchContext) {
            super(childQuery, parentType, childType, searchContext);
        }

        public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
            if (collectedUids == null) {
                throw new ElasticSearchIllegalStateException("has_child filter hasn't executed properly");
            }

            IdReaderTypeCache idReaderTypeCache = searchContext.idCache().reader(context.reader()).type(parentType);
            if (idReaderTypeCache != null) {
                return new ParentDocSet(context.reader(), acceptDocs, collectedUids, idReaderTypeCache);
            } else {
                return null;
            }
        }

        @Override
        public void contextRewrite(SearchContext searchContext) throws Exception {
            searchContext.idCache().refresh(searchContext.searcher().getTopReaderContext().leaves());
            collectedUids = CacheRecycler.popHashSet();
            UidCollector collector = new UidCollector(parentType, searchContext, collectedUids);
            searchContext.searcher().search(childQuery, collector);
        }

        @Override
        public void contextClear() {
            if (collectedUids != null) {
                CacheRecycler.pushHashSet(collectedUids);
            }
            collectedUids = null;
        }

        static class ParentDocSet extends MatchDocIdSet {

            final IndexReader reader;
            final Set<HashedBytesArray> parents;
            final IdReaderTypeCache typeCache;

            ParentDocSet(IndexReader reader, @Nullable Bits acceptDocs, Set<HashedBytesArray> parents, IdReaderTypeCache typeCache) {
                super(reader.maxDoc(), acceptDocs);
                this.reader = reader;
                this.parents = parents;
                this.typeCache = typeCache;
            }

            @Override
            protected boolean matchDoc(int doc) {
                return parents.contains(typeCache.idByDoc(doc));
            }
        }

        static class UidCollector extends NoopCollector {

            final String parentType;
            final SearchContext context;
            final Set<HashedBytesArray> collectedUids;

            private IdReaderTypeCache typeCache;

            UidCollector(String parentType, SearchContext context, Set<HashedBytesArray> collectedUids) {
                this.parentType = parentType;
                this.context = context;
                this.collectedUids = collectedUids;
            }

            @Override
            public void collect(int doc) throws IOException {
                // It can happen that for particular segment no document exist for an specific type. This prevents NPE
                if (typeCache != null) {
                    collectedUids.add(typeCache.parentIdByDoc(doc));
                }

            }

            @Override
            public void setNextReader(AtomicReaderContext readerContext) throws IOException {
                typeCache = context.idCache().reader(readerContext.reader()).type(parentType);
            }
        }
    }
}
