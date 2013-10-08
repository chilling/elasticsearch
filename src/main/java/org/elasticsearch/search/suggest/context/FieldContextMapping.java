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
import java.util.*;

/**
 * The {@link FieldContextMapping} is used to define a {@link CompletionContextMapping} that
 * references a field within a document. The value of the field in turn will be used to setup
 * the suggestions made by the completion suggester. 
 */
public class FieldContextMapping extends CompletionContextMapping {

    public static final String TYPE = "field";
    public static final String FIELD = "field";
    public static final String DEFAULT_FIELDNAME = "_type";
    public static final String DEFAULT_VALUE = "default";
    
    private final String fieldname;
    private final Iterable<? extends CharSequence> defaultValues;
    
    /**
     * Create a new {@link FieldContextMapping} with the default field <code>[_type]</code>
     */
    @SuppressWarnings("unchecked")
    public FieldContextMapping() {
        this(DEFAULT_FIELDNAME, Collections.EMPTY_LIST);
    }

    /**
     * Create a new {@link FieldContextMapping} with the default field <code>[_type]</code>
     */
    @SuppressWarnings("unchecked")
    public FieldContextMapping(String fieldname) {
        this(fieldname, Collections.EMPTY_LIST);
    }

    /**
     * Create a new {@link FieldContextMapping} with the default field <code>[_type]</code>
     */
    public FieldContextMapping(Iterable<? extends CharSequence> defaultValues) {
        super(TYPE);
        this.fieldname = DEFAULT_FIELDNAME;
        this.defaultValues = defaultValues;
    }

    /**
     * Create a new {@link FieldContextMapping} with the default field <code>[_type]</code>
     */
    public FieldContextMapping(String fieldname, Iterable<? extends CharSequence> defaultValues) {
        super(TYPE);
        this.fieldname = fieldname;
        this.defaultValues = defaultValues;
        
        if(DEBUG) {
            logger.info("Field Context Mapping [field={}; default={}]", fieldname, defaultValues);
        }
    }

    /**
     * Name of the field used by this {@link FieldContextMapping} 
     */
    public String fieldname() {
        return fieldname;
    }
    
    public Iterable<? extends CharSequence> defaultValues() {
        return defaultValues;
    }
    
    public static FieldContextMapping parseFieldMapping(XContentParser parser) throws IOException, ElasticSearchParseException {
        FieldMappingBuilder builder = new FieldMappingBuilder();
                
        Token token = parser.nextToken();
        
        if(token == Token.START_OBJECT) {
            while((token = parser.nextToken()) != Token.END_OBJECT) {
                if(token == Token.FIELD_NAME) {
                    String currentfield = parser.text();
                    if(FIELD.equals(currentfield)) {
                        parser.nextToken();
                        builder.field(parser.text());
                    } else if(DEFAULT_VALUE.equals(currentfield)) {
                        token = parser.nextToken();
                        if(token == Token.START_ARRAY) {
                            while((token = parser.nextToken()) != Token.END_ARRAY) {
                                if(token.isValue()) {
                                    builder.defaultValue(parser.text());
                                } else {
                                    throw new ElasticSearchParseException("value expected");
                                }
                            }
                        } else if(token.isValue()) {
                            builder.defaultValue(parser.text());
                        } else {
                            throw new ElasticSearchParseException("[" + DEFAULT_VALUE + "] must be array or value");
                        }
                    }
                } else {
                    throw new ElasticSearchParseException("fieldname expected");
                }
            }
        } else if(token == Token.VALUE_STRING) {
            builder.field(parser.text());
        }        
        
        return builder.build();
    }

    /**
     * Load the specification of a {@link FieldContextMapping} 
     * @param field name of the field to use. If <code>null</code> default field will be used
     * @return new {@link FieldContextMapping}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected static FieldContextMapping load(Object data) throws ElasticSearchParseException {
        Map<String, Object> config = (Map<String, Object>)data;
        Object field = config.get(FIELD);
        
        if(field == null) {
            return new FieldContextMapping();
        } else {
            if(field instanceof String) {
                return new FieldContextMapping(field.toString());
            } else if (field instanceof List) {
                ArrayList<String> fields = new ArrayList<String>();
                for (Object fieldname : (List)field) {
                    fields.add(fieldname.toString());
                }
                return new FieldContextMapping(fields);
            } else {
                throw new ElasticSearchParseException("field context must be String or List of Strings");
            }
        }
    }

    @Override
    protected XContentBuilder toInnerXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(FIELD, fieldname);
        builder.startArray(DEFAULT_VALUE);
        for (CharSequence value : defaultValues) {
            builder.value(value);
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }
    
    @Override
    public FieldConfig parseConfig(XContentParser parser) throws IOException, ElasticSearchParseException {
        return new FieldConfig(fieldname, defaultValues);
    }

    @Override
    public FieldQuery parseQuery(XContentParser parser) throws IOException, ElasticSearchParseException {
        Iterable<? extends CharSequence> values;
        
        Token token = parser.nextToken();
        if(token == Token.START_ARRAY) {
            ArrayList<String> list = new ArrayList<String>();
            while ((token = parser.nextToken()) != Token.END_ARRAY) {
                list.add(parser.text());

            }
            values = list;
        } else if(token == Token.VALUE_NULL) {
            values = defaultValues;
        } else {
            values = Collections.singleton(parser.text());
        }
        
        return new FieldQuery(values);
    }
    
    public static FieldQuery query(CharSequence...fieldvalues) {
        return query(Arrays.asList(fieldvalues));
    }

    public static FieldQuery query(Iterable<? extends CharSequence> fieldvalues) {
        return new FieldQuery(fieldvalues);
    }

    static class FieldConfig extends ContextConfig {
        
        private final String fieldname;
        private final Iterable<? extends CharSequence> defaultValues;

        public FieldConfig(String fieldname, Iterable<? extends CharSequence> defaultValues) {
            super();
            this.fieldname = fieldname;
            this.defaultValues = defaultValues;

            if(DEBUG) {
                logger.info("Field Context Config [field={}; default={}]", fieldname, defaultValues);
            }
        }

        @Override
        protected TokenStream wrapTokenStream(Document doc, TokenStream stream) {
            String[] values = doc.getValues(fieldname);
            
            if(DEBUG) {
                logger.info("Field Context Config [field={}; docvalues={}]", fieldname, (Object)values);
            }
            
            if(values != null) {
                return new PrefixAnalyzer.PrefixTokenFilter(stream, Arrays.asList(values));
            } else {
                return new PrefixAnalyzer.PrefixTokenFilter(stream, defaultValues);
            }
        }
        
    }
    
    static class FieldQuery extends ContextQuery {

        private final Iterable<? extends CharSequence> values;
        
        public FieldQuery(Iterable<? extends CharSequence> values) {
            super();
            this.values = values;
            
            if(DEBUG) {
                logger.info("Field Context Query [values={}]", values);
            }

        }

        @Override
        public Analyzer wrapSearchAnalyzer(Analyzer searchAnalyzer) {
            return new PrefixAnalyzer(searchAnalyzer, values);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startArray();
            for (CharSequence value : values) {
                builder.value(value);
            }
            builder.endArray();
            return builder;
        }
        
    }  
}
