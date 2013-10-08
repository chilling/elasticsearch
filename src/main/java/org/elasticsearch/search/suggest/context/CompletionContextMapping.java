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

package org.elasticsearch.search.suggest.context;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;

/**
 * A {@link CompletionContextMapping} is used t define a context that may used
 * in conjunction with a suggester. To define a suggester that depends on a
 * specific context derived class of {@link CompletionContextMapping} will be
 * used to specify the kind of additional information required in order to make
 * suggestions.
 */
public abstract class CompletionContextMapping implements ToXContent {

    protected static final boolean DEBUG;
    protected static ESLogger logger = ESLoggerFactory.getLogger(CompletionContextMapping.class.getCanonicalName());
    
    protected final String type;

    static {
        boolean debug = false;
        assert debug = true;
        DEBUG = debug;
    }
    
    /**
     * Define a new context mapping of a specific type
     * 
     * @param type
     *            name of the new context mapping
     */
    protected CompletionContextMapping(String type) {
        super();
        this.type = type;
    }

    /**
     * return the type name of the context
     */
    public String type() {
        return type;
    }

    @Override
    public final XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(type);
        toInnerXContent(builder, params);
        builder.endObject();
        return builder;
    }

    public abstract ContextConfig parseConfig(XContentParser parser) throws IOException, ElasticSearchParseException;
    public abstract ContextQuery parseQuery(XContentParser parser) throws IOException, ElasticSearchParseException;

    protected abstract XContentBuilder toInnerXContent(XContentBuilder builder, Params params) throws IOException;

    public static class Context {

        final Iterable<ContextConfig> contexts;
        final Document doc;

        public Context(Iterable<ContextConfig> contexts, Document doc) {
            super();
            this.contexts = contexts;
            this.doc = doc;
        }

        public TokenStream wrapTokenStream(TokenStream tokenStream) {
            for (ContextConfig context : contexts) {
                tokenStream = context.wrapTokenStream(doc, tokenStream);
            }
            return tokenStream;
        }
    }
}
