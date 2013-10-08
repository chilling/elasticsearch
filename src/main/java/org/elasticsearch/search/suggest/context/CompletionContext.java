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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.suggest.completion.AnalyzingCompletionLookupProvider;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A {@link CompletionContext} is used t define a context that may used in conjunction
 * with a suggester. To define a suggester that depends on a specific context derived
 * class of {@link CompletionContext} will be used to specify the kind of additional
 * information required in order to make suggestions.  
 */
public abstract class CompletionContext<E extends ContextInformation<?>> implements ToXContent {
    
    private final String type;

    protected CompletionContext(String type) {
        super();
        this.type = type;
    }
    
    /**
     * return the type name of the context
     */
    public String type() {
        return type;
    } 

    /**
     * Given a completion request and this completion context this method creates 
     * {@link Builder} that combines both to the effective specification of a
     * completion.
     *     
     * @param context Parser to use for reading the context information from
     * @return {@link Builder} for the context
     * @throws IOException if thrown by the parser used
     */
    public abstract Builder parseContext(XContentParser context) throws IOException;

    /**
     * Wraps the Search{@link Analyzer} in the {@link AnalyzingCompletionLookupProvider}
     * according to the definition of the {@link CompletionContext} and the actual
     * {@link ContextInformation}
     * 
     * @param searchAnalyzer {@link Analyzer} to modify
     * @param contextInfo configuration of the context
     * 
     * @return the Search{@link Analyzer} used for completion
     */
    public abstract Analyzer wrapSearchAnalyzer(Analyzer searchAnalyzer, E contextInfo);

    /**
     * Build a {@link CompletionContext} from a configuration map
     * @param config configuration of the context
     * @return {@link CompletionContext} configured by <code>config</code>
     */
    @SuppressWarnings("unchecked")
    public static CompletionContext<?> build(Map<String, Object> config) {
        Iterator<Entry<String, Object>> iterator = config.entrySet().iterator();
        if(!iterator.hasNext()) {
            return null;
        }

        Entry<String, Object> item = iterator.next();
        String name = item.getKey();
        Object value = item.getValue();

        if (GeoLocationContext.TYPE.equals(name)) {
            return GeoLocationContext.load((Map<String, Object>)value);
        } else {
            throw new ElasticSearchIllegalArgumentException("No such context [" + config + "]");
        }
    }

    /**
     * The {@link Builder} interface is used to wrap the token stream used to generate
     * suggestions according to the specifications set by the {@link CompletionContext}. 
     */
    public static interface Builder {
        
        /**
         * wrap a given {@link TokenStream} by the specifications of the {@link ContextInformation}
         */
        public TokenStream wrapTokenStream(TokenStream stream);
    }

}
