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

import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentParser.Token;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public abstract class ContextBuilder<E extends CompletionContextMapping> {
    
    public abstract E build();
    
    public static List<CompletionContextMapping> parseMappings(XContentParser parser) throws IOException, ElasticSearchParseException {
        final Token token = parser.currentToken();
        if(token == Token.START_ARRAY) {
            List<CompletionContextMapping> mappings = new ArrayList<CompletionContextMapping>();
            while(parser.nextToken() != Token.END_ARRAY) {
                mappings.add(parseMapping(parser));
            }
            return mappings;
        } else {
            return Collections.singletonList(parseMapping(parser));
        }
    }

    protected static CompletionContextMapping parseMapping(XContentParser parser) throws IOException, ElasticSearchParseException {
        Token token = parser.currentToken();
        if(token == Token.START_OBJECT) {
            final String typename = parser.text();
            if(GeoContextMapping.TYPE.equals(typename)) {
                return GeoContextMapping.parseGeoContextMapping(parser);
                
            } else if(CategoryContextMapping.TYPE.equals(typename)) {
                return CategoryContextMapping.parseCategoryMapping(parser);
                
            } else if(FieldContextMapping.TYPE.equals(typename)) {
                return FieldContextMapping.parseFieldMapping(parser);
                
            } else {
                throw new ElasticSearchParseException("unknown context type [ " + typename + " ]");
            }
        } else {
            throw new ElasticSearchParseException("context mapping expects object");
        }
    }

    public static GeoMappingBuilder location() {
        return new GeoMappingBuilder();
    }

    public static GeoMappingBuilder location(String name, int precision, boolean neighbors) {
        return new GeoMappingBuilder(precision, neighbors);
    }

    public static CategoryMappingBuilder category() {
        return new CategoryMappingBuilder();
    }
    
    public static CategoryMappingBuilder category(String name, String defaultValue) {
        return new CategoryMappingBuilder().addDefaultValue(defaultValue);
    }
    
    public static FieldMappingBuilder reference(String field) {
        return new FieldMappingBuilder(field);
    }
    
    public static FieldMappingBuilder reference(String field, String defaultValue) {
        return new FieldMappingBuilder(field).defaultValue(defaultValue);
    }
    
    public static Iterable<CompletionContextMapping> loadMappings(List configurations) throws ElasticSearchParseException {
        ArrayList<CompletionContextMapping> mappings = new ArrayList<CompletionContextMapping>(configurations.size());
        for (Object config : configurations) {
            mappings.add(loadMapping((Map<String, Object>)config));
        }
        return mappings;
    }

    protected static CompletionContextMapping loadMapping(Map<String, Object> data) throws ElasticSearchParseException {
        final Entry<String, Object> entry = data.entrySet().iterator().next();
        final String type = entry.getKey();
        final Object config = entry.getValue(); 
        
        if(GeoContextMapping.TYPE.equals(type)) {
            return GeoContextMapping.load(config);
        } else if (CategoryContextMapping.TYPE.equals(type)) {
            return CategoryContextMapping.load(config);
        } else if(FieldContextMapping.TYPE.equals(type)) {
            return FieldContextMapping.load(config);
        } else {
            throw new ElasticSearchParseException("unknown context type[" + type + "]");
        }
    }
}
