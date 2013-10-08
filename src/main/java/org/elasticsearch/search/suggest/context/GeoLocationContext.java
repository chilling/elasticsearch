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
import org.elasticsearch.common.geo.GeoHashUtils;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.GeoUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentParser.Token;
import org.elasticsearch.search.suggest.context.GeoLocationContext.GeoLocation;

import java.io.IOException;
import java.util.*;

public class GeoLocationContext extends CompletionContext<GeoLocation> {
    
    public static final String TYPE = "geolocation";
    
    public static final String PRECISION = "precision";
    public static final String SEPARATOR = "separator";
    public static final String NEIGHBORS = "neighbors";
    
    private final int precision;
    private final char separator;
    private final boolean neighbors;
    
    protected GeoLocationContext() {
        this(GeoHashUtils.PRECISION, PrefixAnalyzer.DEFAULT_SEPARATOR, true);
    }

    protected GeoLocationContext(int precision, char separator, boolean neighbors) {
        super(TYPE);
        this.separator = separator;
        this.precision = precision;
        this.neighbors = neighbors;
    }

    /**
     * load a {@link GeoLocationContext} by configuration. Such a configuration
     * can set the parameters
     * <ul>
     *   <li>precision [<code>String</code>, <code>Double</code>, <code>Float</code> or <code>Integer</code>] defines the length
     *       of the underlying geohash</li>
     *   <li>separator [<code>Character</code>] defines the {@link Character} used to
     *       separate geohash and effective data</li>
     *   <li>neighbors [<code>Boolean</code>] defines if the last level of the geohash
     *       should be extended by neighbor cells</li>
     * </ul>
     * 
     * @param config Configuration for {@link GeoLocationContext}
     * @return new {@link GeoLocationContext} configured by the parameters of
     *           <code>config</code>
     */
    protected static GeoLocationContext load(Map<String, Object> config) {
        if(config == null) {
            return new GeoLocationContext();
        }
        
        final int precision;
        final Object configPrecision = config.get(PRECISION);
        if(configPrecision == null) {
            precision = GeoHashUtils.PRECISION;
        } else if(configPrecision instanceof Integer) {
            precision = (Integer)configPrecision;
        } else if(configPrecision instanceof Double) {
            precision = GeoUtils.geoHashLevelsForPrecision((Double)configPrecision);
        } else if(configPrecision instanceof Float) {
            precision = GeoUtils.geoHashLevelsForPrecision((Float)configPrecision);
        } else {
            precision = GeoUtils.geoHashLevelsForPrecision(configPrecision.toString());
        }
        
        final char separator;
        final Object configSeparator = config.get(SEPARATOR);
        if(configSeparator == null) {
            separator = PrefixAnalyzer.DEFAULT_SEPARATOR;
        } else {
            separator = configSeparator.toString().charAt(0);
        }
        
        final Object configNeighbors = config.get(NEIGHBORS);
        final boolean neighbors;
        if(configNeighbors == null) {
            neighbors = true;
        } else {
            neighbors = (Boolean)configNeighbors;
        }
        
        return new GeoLocationContext(precision, separator, neighbors);
    }

    @Override
    public Builder parseContext(XContentParser parser) throws IOException {
        String geohash;
        if(parser.currentToken() == XContentParser.Token.VALUE_STRING) {
            geohash = parser.text();
        } else {
            geohash = GeoPoint.parse(parser).geohash();
        }
        
        int precision = Math.min(this.precision, geohash.length());
        geohash = geohash.substring(0, precision);
        
        Set<String> locations = new TreeSet<String>();
        if(neighbors) {
             List<String> surround = GeoHashUtils.neighbors(geohash);
             for (String neighbor : surround) {
                 for (String cell : GeoHashUtils.path(neighbor)) {
                     locations.add(cell);
                }
            }
        }
        
        for (String cell : GeoHashUtils.path(geohash)) {
            locations.add(cell);
        }

        return new GeoBuilder(separator, locations); 
    }
    
    @Override
    public Analyzer wrapSearchAnalyzer(Analyzer searchAnalyzer, GeoLocation location) {
        final String geohash = location.getValue();
        final int precision = Math.min(this.precision, geohash.length());
        final String position = geohash.substring(0, precision);
        Collection<String> neighbors;
        if(this.neighbors) {
            neighbors = GeoHashUtils.neighbors(position);
            neighbors.add(position);
        } else {
            neighbors = Collections.singleton(position);
        }
        return new PrefixAnalyzer(searchAnalyzer, separator, neighbors);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(TYPE);
        builder.field(SEPARATOR, Character.toString(separator));
        builder.field(PRECISION, precision);
        builder.field(NEIGHBORS, neighbors);
        builder.endObject();
        return builder;
    }
    
    public static class GeoBuilder implements Builder {
        private final Iterable<String> locations;
        private final char separator;

        public GeoBuilder(char separator, Iterable<String> locations) {
            super();
            this.locations = locations;
            this.separator = separator;
        }

        @Override
        public TokenStream wrapTokenStream(TokenStream stream) {
            return new PrefixAnalyzer.PrefixTokenFilter(stream, separator, locations);
        }

    }
    
    public static class GeoLocation extends ContextInformation<String> {
        
        public GeoLocation(String value) {
            super(GeoLocationContext.TYPE, value);
        }
        
        public GeoLocation(double lat, double lon) {
            this(GeoHashUtils.encode(lat, lon));
        }
        
        public static GeoLocation parseGeoLocation(XContentParser parser) throws IOException {
            XContentParser.Token token = parser.currentToken();
            if(token == Token.VALUE_STRING) {
                return new GeoLocation(parser.text());
            } else {
                return new GeoLocation(GeoPoint.parse(parser).geohash());
            }
        }
        
        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.value(getValue());
        }
        
    }
    
}
