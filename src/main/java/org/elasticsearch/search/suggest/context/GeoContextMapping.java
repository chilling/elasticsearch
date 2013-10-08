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
import org.apache.lucene.analysis.PrefixAnalyzer.PrefixTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.util.fst.FST;
import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.common.geo.GeoHashUtils;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.GeoUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentParser.Token;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The {@link GeoContextMapping} allows to take GeoInfomation into account during building suggestions. The
 * mapping itself works with geohashes explicitly and is configured by three parameters:
 * <ul>
 *   <li><code>precision</code>: length of the geohash indexed as prefix of the completion field</li>
 *   <li><code>separator</code>: Character separating the geohash from the suggestions</li>
 *   <li><code>neighbors</code>: Should the neighbor cells of the deepest geohash level also be indexed as
 *                               alternatives to the actual geohash</li>
 * </ul>
 * Internally this mapping wraps the suggestions into a form <code>[geohash][separator][suggestion]</code>.
 * If the neighbor option is set the cells next to the cell on the deepest geohash level
 * (<code>precision</code>) will be indexed as well. The {@link TokenStream} used to build the {@link FST}
 * for suggestion will be wrapped into a {@link PrefixTokenFilter} managing these geohases as prefixes.
 */
public class GeoContextMapping extends CompletionContextMapping {
    
    public static final String TYPE = "geo";
    
    public static final String PRECISION = "precision";
    public static final String NEIGHBORS = "neighbors";
    public static final String DEFAULT = "default";

    private final boolean useDefault;
    private final String defaultValue;
    private final int precision;
    private final boolean neighbors;
    
    /**
     * Create a new {@link GeoContextMapping} with default parameters
     * <ul>
     *   <li><code>precision</code>: 12</li>
     *   <li><code>neighbors</code>: true</li>
     * </ul>
     * 
     * @param name name of the context
     */
    public GeoContextMapping() {
        this(GeoHashUtils.PRECISION, true, null);
    }
    
    public GeoContextMapping(String defaultValue) {
        this(GeoHashUtils.PRECISION, true, defaultValue);
    }

    /**
     * Create a new {@link GeoContextMapping} with a given precision
     * @param name name of the context
     * @param precision precision in meters
     */
    public GeoContextMapping(double precision, String defaultValue) {
        this(precision, true, defaultValue);
    }

    /**
     * Create a new {@link GeoContextMapping} with a given precision
     * @param name name of the context
     * @param precision precision in meters
     */
    public GeoContextMapping(double precision, boolean neighbors) {
        this(precision, true, null);
    }

    /**
     * Create a new {@link GeoContextMapping} with a given precision
     * @param name name of the context
     * @param precision precision in meters
     * @param neighbors should neighbors be indexed 
     */
    public GeoContextMapping(double precision, boolean neighbors, String defaultValue) {
        this(GeoUtils.geoHashLevelsForPrecision(precision), neighbors, defaultValue);
    }

    /**
     * Create a new {@link GeoContextMapping} with a given precision
     * @param precision length of the geohashes
     * @param name name of the mapping
     * @param neighbors should neighbors be indexed 
     */
    public GeoContextMapping(int precision, boolean neighbors, String defaultValue) {
        super(TYPE);
        this.precision = precision;
        this.neighbors = neighbors;
        this.defaultValue = defaultValue;
        this.useDefault = defaultValue != null;
        
        if(DEBUG) {
            logger.info("Geo Context Mapping [precision={}; neighbors={}; default={}]", precision, neighbors, defaultValue);
        }
    }


    /**
     * load a {@link GeoContextMapping} by configuration. Such a configuration
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
     * @param config Configuration for {@link GeoContextMapping}
     * @return new {@link GeoContextMapping} configured by the parameters of
     *           <code>config</code>
     */
    protected static GeoContextMapping load(Object data) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> config = (Map<String, Object>)data;
        
        final GeoMappingBuilder builder = new GeoMappingBuilder();

        if(config != null) {
            final Object configPrecision = config.get(PRECISION);
            if(configPrecision == null) {
                // ignore precision
            } else if(configPrecision instanceof Integer) {
                builder.precision((Integer)configPrecision);
            } else if(configPrecision instanceof Double) {
                builder.precision((Double)configPrecision);
            } else if(configPrecision instanceof Float) {
                builder.precision((Float)configPrecision);
            } else {
                builder.precision(configPrecision.toString());
            }
            
            final Object configNeighbors = config.get(NEIGHBORS);
            if(configNeighbors != null) {
                builder.neighbors((Boolean)configNeighbors);
            }

            final Object def = config.get(DEFAULT);
            if(def != null) {
                builder.defaultLocation(def.toString());
            }
}        
        return builder.build();
    }

    @Override
    protected XContentBuilder toInnerXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(PRECISION, precision);
        builder.field(NEIGHBORS, neighbors);
        if(useDefault) {
            builder.field(DEFAULT, neighbors);
        }
        builder.endObject();
        return builder;
    }
    
    public static GeoContextMapping parseGeoContextMapping(XContentParser parser) throws IOException, ElasticSearchParseException {
        GeoMappingBuilder builder = new GeoMappingBuilder();
        Token token = parser.nextToken();
        if((token == Token.START_OBJECT)) {
            while((token = parser.nextToken()) != Token.END_OBJECT) {
                final String fieldname = parser.text();
                
                if(PRECISION.equals(fieldname)) {
                    switch (parser.nextToken()) {
                    case VALUE_STRING:
                        builder.precision(parser.text());
                        break;
                    case VALUE_NUMBER:
                        switch (parser.numberType()) {
                        case DOUBLE:
                            builder.precision(parser.doubleValue());
                            break;

                        case FLOAT:
                            builder.precision(parser.floatValue());
                            break;

                        case INT:
                            builder.precision(parser.intValue());
                            break;

                        default:
                            throw new ElasticSearchParseException("invalid precision type. valid types [String, Float, Double, Integer]");
                        }
                        break;
                    default:
                        throw new ElasticSearchParseException("invalid precision type. valid types [String, Float, Double, Integer]");
                    }
                } else if(NEIGHBORS.equals(fieldname)) {
                    parser.nextToken();
                    builder.neighbors(parser.booleanValue());
                } else if(DEFAULT.equals(fieldname)) {
                    parser.nextToken();
                    builder.defaultLocation(GeoPoint.parse(parser).geohash());
                } else {
                    throw new ElasticSearchParseException("invalid field");
                }
            }
            return builder.build();
        } else {
            throw new ElasticSearchParseException("mapping of type " + TYPE + " must be object");
        }
    }
    
    @Override
    public GeoConfig parseConfig(XContentParser parser) throws IOException, ElasticSearchParseException {
        String geohash = GeoPoint.parse(parser).getGeohash();
        
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

        return new GeoConfig(locations);
    }
    
    public static GeoQuery query(GeoPoint point) {
        return query(point.getGeohash());
    }
    
    public static GeoQuery query(double lat, double lon) {
        return query(GeoHashUtils.encode(lat, lon));
    }
    
    public static GeoQuery query(String geohash) {
        return new GeoQuery(geohash);
    }
    
    @Override
    public ContextQuery parseQuery(XContentParser parser) throws IOException, ElasticSearchParseException {
        String location = GeoPoint.parse(parser).getGeohash();
        location = location.substring(0, Math.min(precision, location.length()));
        return new GeoQuery(location);
    }
    
    private static class GeoConfig extends ContextConfig {

        private final Iterable<? extends CharSequence> locations;
        
        public GeoConfig(Iterable<? extends CharSequence> locations) {
            super();
            this.locations = locations;
            
            if(DEBUG) {
                logger.info("Geo Context Config [locations={}]", locations);
            }
        }

        @Override
        protected TokenStream wrapTokenStream(Document doc, TokenStream stream) {
            return new PrefixTokenFilter(stream, locations);
        }
    }

    private static class GeoQuery extends ContextQuery {
        private final String location;
        
        public GeoQuery(String location) {
            super();
            this.location = location;
            
            if(DEBUG) {
                logger.info("Geo Context Query [location={}]", location);
            }

        }

        @Override
        public Analyzer wrapSearchAnalyzer(Analyzer searchAnalyzer) {
            if(DEBUG) {
                logger.info("Geo Context Query: analyzing [{}]", location);
            }
            return new PrefixAnalyzer(searchAnalyzer, location);
        }
        
        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return builder.value(location);
        }
    }

}
