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
import org.apache.lucene.analysis.PrefixAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentParser.Token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The {@link CategoryContextMapping} configures the suggester to take a category name
 * into account. A category is defined as an arbitrary String.
 */
public class CategoryContextMapping extends CompletionContextMapping {

    protected static final String TYPE = "category";
    
    private final Iterable<? extends CharSequence> defaultCategories;

    public CategoryContextMapping() {
        this(Collections.EMPTY_LIST);
    }
    
    public CategoryContextMapping(Iterable<? extends CharSequence> defaultCategories) {
        super(TYPE);
        this.defaultCategories = defaultCategories;
        
        if(DEBUG) {
            logger.info("Category Context Mapping[defaults={}]", defaultCategories);
        }
        
    }

    @Override
    protected XContentBuilder toInnerXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startArray();
        for (CharSequence value : defaultCategories) {
            builder.value(value);
        }
        builder.endArray();
        return builder;
    }
    
    public static CategoryContextMapping parseCategoryMapping(XContentParser parser) throws IOException, ElasticSearchParseException {
        Token token = parser.nextToken();
        CategoryMappingBuilder builder = new CategoryMappingBuilder();
        if(token == Token.START_ARRAY) {
            while((token = parser.nextToken()) != Token.END_ARRAY) {
                builder.addDefaultValue(parser.text());
            }
        } else {
            builder.addDefaultValue(parser.text());
        }
        return builder.build();
    }
    
    @Override
    public CategoryConfig parseConfig(XContentParser parser) throws IOException, ElasticSearchParseException {
        return CategoryConfig.parseConfig(parser, defaultCategories);
    }
    
    @Override
    public ContextQuery parseQuery(XContentParser parser) throws IOException, ElasticSearchParseException {
        Iterable<? extends CharSequence> categories;
        
        Token token = parser.nextToken();
        if(token == Token.START_ARRAY) {
            ArrayList<String> values = new ArrayList<String>();
            while ((token = parser.nextToken()) != Token.END_ARRAY) {
                values.add(parser.text());
            }
            categories = values;
        } else if(token == Token.VALUE_NULL) {
            categories = defaultCategories;
        } else {
            categories = Collections.singleton(parser.text());
        }
        
        return new CategoryQuery(categories);
    }
    

    public static CategoryContextMapping load(Object config) {
        Iterable<? extends CharSequence> defaultCategories = Collections.emptySet();
        if(config instanceof List) {
            defaultCategories = (Iterable)config;
        } else {
            defaultCategories = Collections.singleton(config.toString());
        }
        return new CategoryContextMapping(defaultCategories);
    }

    public static CategoryQuery query(CharSequence...categories) {
           return query(Arrays.asList(categories));
    }

    public static CategoryQuery query(Iterable<? extends CharSequence> categories) {
        return new CategoryQuery(categories);
    }
    

    static class CategoryConfig extends ContextConfig {

        private final Iterable<? extends CharSequence> categories;
        
        public CategoryConfig(Iterable<? extends CharSequence> categories) {
            this.categories = categories;

            if(DEBUG) {
                logger.info("Category Context Config[categories={}]", categories);
            }
        }

        @Override
        protected TokenStream wrapTokenStream(Document doc, TokenStream stream) {
            return new PrefixAnalyzer.PrefixTokenFilter(stream, categories);
        }

        public static CategoryConfig parseConfig(XContentParser parser, Iterable<? extends CharSequence> defaultCategories) throws IOException, ElasticSearchParseException {
            Iterable<? extends CharSequence> categories;
            
            Token token = parser.currentToken();
            if(token == Token.START_ARRAY) {
                ArrayList<String> values = new ArrayList<String>();
                while ((token = parser.nextToken()) != Token.END_ARRAY) {
                    values.add(parser.text());
                }
                categories = values;
            } else if(token == Token.VALUE_NULL) {
                categories = defaultCategories;
                
            } else if (token == Token.START_OBJECT) {
                throw new ElasticSearchParseException("category can not be an object");
            } else {
                categories = Collections.singleton(parser.text());
            }
            
            return new CategoryConfig(categories);
        }
        
    }
    
    private static class CategoryQuery extends ContextQuery {

        final Iterable<? extends CharSequence> categories;
        
        public CategoryQuery(Iterable<? extends CharSequence> categories) {
            this.categories = categories;
            
            if(DEBUG) {
                logger.info("Category Context Query[categories={}]", categories);
            }

        }

        @Override
        public Analyzer wrapSearchAnalyzer(Analyzer searchAnalyzer) {
            return new PrefixAnalyzer(searchAnalyzer, categories);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startArray();
            for(CharSequence category : categories) {
                builder.value(category);
            }
            builder.endArray();
            return builder;
        }

    }
    
    
    
}
