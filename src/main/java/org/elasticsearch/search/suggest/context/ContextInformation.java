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

import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentParser.Token;

import java.io.IOException;

/**
 * The {@link ContextInformation} implements the effective data of
 * a {@link CompletionContext}. In the usual case a {@link CompletionContext}
 * implements its own context information associated with it.  
 */
public abstract class ContextInformation<E> implements ToXContent {

    private final String type;
    private final E value;
    
    public ContextInformation(String type, E value) {
        super();
        this.type = type;
        this.value = value;
    }

    /**
     * Type of {@link ContextInformation}
     */
    public String getType() {
        return type;
    }

    /**
     * Value of {@link ContextInformation}
     */
    public E getValue() {
        return value;
    }

    /**
     * Parses a {@link ContextInformation} from the parsers current position. The current token
     * is assumed to be a field name which refers to the name of the {@link CompletionContext}.
     *   
     * @param parser Parser used to parse the {@link ContextInformation}
     * @return {@link ContextInformation} parsed
     * 
     * @throws IOException if an exception raises during parsing
     * @throws ElasticSearchIllegalArgumentException if the context type is unknown or parsing
     *         the underlying {@link ContextInformation} raises it 
     * @throws ElasticSearchIllegalArgumentException if the current token is not a field name or
     *         parsing the underlying {@link ContextInformation} raises it
     */
    public static ContextInformation<?> parseContext(XContentParser parser) throws IOException {
        XContentParser.Token token = parser.currentToken();
        if(token == Token.FIELD_NAME) {
            final String type = parser.text();
            if(GeoLocationContext.TYPE.equals(type)) {
                parser.nextToken();
                return GeoLocationContext.GeoLocation.parseGeoLocation(parser);
            } else {
                throw new ElasticSearchIllegalArgumentException("no such context type [" + type + "]");
            }
        } else {
            throw new ElasticSearchParseException("field name expected");
        } 
    }
}
