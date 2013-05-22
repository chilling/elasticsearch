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

package org.elasticsearch.test.unit.common.geo;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.geo.GeoShapeConstants;
import org.elasticsearch.common.geo.TestFrame;
import org.elasticsearch.common.geo.builders.CircleBuilder;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.common.geo.builders.GeoShapeBuilder;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.XContentParser.Token;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.testng.annotations.Test;

import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;

public class GeoWrappingTests {

    @Test
    public void wrappingTest() throws IOException {
        XContentBuilder mapping = JsonXContent.contentBuilder()
                .startObject()
                    .startObject("collection")
                        .startObject("properties")
                            .startObject("type")
                                .field("type", "string")
                            .endObject()
                            .startObject("id")
                                .field("type", "string")
                            .endObject()
                            .startObject("features")
                                .startObject("properties")
                                    .startObject("name")
                                        .field("type", "string")
                                    .endObject()
                                    .startObject("geometry")
                                        .field("type", "geo_shape")
                                    .endObject()
                                .endObject()
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
        
        System.out.println("Mapping + " + mapping.prettyPrint().string());

        
//        FileInputStream in = new FileInputStream("/home/schilling/Desktop/countries.geo.json.1");
//
////        StringBuilder sb = new StringBuilder();
////        int c;
////        while((c=in.read())>=0) {
////            sb.append((char)c);
////        }
////        System.out.println(sb.toString());
//
//        XContentParser parser = JsonXContent.jsonXContent.createParser(in);
//
//        if(parser.nextToken() == Token.START_OBJECT) {
//            TestFrame testFrame = new TestFrame();
//            parseObject(parser, testFrame.geomeries());
//
//            
//            try {
//                Thread.sleep(200000);
//            } catch (InterruptedException e) {
//                // TODO: handle exception
//            }
//        }
//        
//        if(true) return;
//        
        

        
        String clusterName = "test1";
        Node node1 = NodeBuilder.nodeBuilder().clusterName(clusterName).build();
        Node node2 = NodeBuilder.nodeBuilder().clusterName(clusterName).build();

        node1.start();
        node2.start();
        
        Client client = node1.client();

        if(client.admin().indices().prepareExists("collections").execute().actionGet().isExists()) {
            client.admin().indices().prepareDelete("collections").execute().actionGet();
        }
        
        
        System.out.println(client.admin().indices().prepareCreate("collections").addMapping("collection", mapping));
        
        CreateIndexResponse create = client.admin().indices().prepareCreate("collections").addMapping("collection", mapping).execute().actionGet();
        
        client.admin().indices().prepareOpen("collections").execute().actionGet();
        
        client.admin().indices().prepareRefresh("collections").execute().actionGet();
        
        
        System.out.println(create);
        
//        IndexResponse indexed = client.prepareIndex("collections", "collection").setCreate(true).setSource(sb.toString()).execute().actionGet();
        
//        System.out.println(indexed);
        
//        GetResponse object = client.prepareGet("collections", "collection", indexed.getId()).execute().actionGet();
        
//        System.out.println(object.getSourceAsString());
        
        client.close();
        node1.close();
        node2.close();
    }
    
    public static Shape shape(Geometry geometry) {
        return new JtsGeometry(
                geometry,
                GeoShapeConstants.SPATIAL_CONTEXT,
                true);
    }
    
    public static GeoShapeBuilder parseGeometry(XContentParser parser, Collection<Geometry> geometries) throws IOException {
        return GeoShapeBuilder.parse(parser);
    }
    
    public static Map<String, Object> parseObject(XContentParser parser, Collection<Geometry> geometries) throws IOException {
        Map<String, Object> result = new TreeMap<String, Object>();
        for(Token token = parser.nextToken(); token != Token.END_OBJECT; token = parser.nextToken()) {
            if(token == Token.START_OBJECT) {
                String name = parser.currentName();
                if("geometry".equals(name)) {
//                    Geometry geometry = parseGeometry(parser, geometries);
//                    
//                    try {
//                        shape(geometry);
//                    } catch (Throwable e) {
//                        geometries.add(geometry);
//                        e.printStackTrace();
//                    }
                    
                    result.put(name, parseGeometry(parser, geometries));
                } else {
                    result.put(name, parseObject(parser, geometries));
                }
            } else if (token == Token.START_ARRAY) {
                String name = parser.currentName();
                result.put(name, parseArray(parser, geometries));
            } else if (token == Token.VALUE_STRING) {
                System.out.println(parser.text());
                result.put(parser.currentName(), parser.text());
            } else if (token == Token.VALUE_NUMBER) {
                result.put(parser.currentName(), parser.doubleValue());
            }
        }
        return result;
    }
    
    public static Object parseArray(XContentParser parser, Collection<Geometry> geometries) throws IOException {
        ArrayList<Object> result = new ArrayList<Object>();

        boolean isCoord = true;
        boolean isCoordArray = true;
        boolean isCoordArrayArray = true;
        boolean isCoordArrayArrayArray = true;
        
        for(Token token = parser.nextToken(); token != Token.END_ARRAY; token = parser.nextToken()) {
            if(token == Token.START_OBJECT) {
                result.add(parseObject(parser, geometries));
                isCoord = false;
                isCoordArray = false;
                isCoordArrayArray = false;
                isCoordArrayArrayArray = false;
            } else if (token == Token.START_ARRAY) {
                
                Object array = parseArray(parser, geometries);
                
                if(!(array instanceof Coordinate)) {
                    isCoordArray = false;
                }
                if(!(array instanceof Coordinate[])) {
                    isCoordArrayArray = false;
                }
                if(!(array instanceof Coordinate[][])) {
                    isCoordArrayArrayArray = false;
                }
                
                result.add(array);
                isCoord = false;
            } else if (token == Token.VALUE_NUMBER) {
                result.add(parser.doubleValue());
                isCoordArray = false;
                isCoordArrayArray = false;
                isCoordArrayArrayArray = false;
            }
        }
        
        if(isCoord) {
            double[] coords = new double[result.size()];
            for (int i = 0; i < coords.length; i++) {
                coords[i] = (Double)(result.get(i));
            }
            
            Coordinate c = new Coordinate(coords[0], coords[1]);
            
            return c;
        } else {
            
            if(isCoordArray) {
                Coordinate[] coords = result.toArray(new Coordinate[result.size()]);
                return coords;
            } else if(isCoordArrayArray) {
                Coordinate[][] coords = result.toArray(new Coordinate[result.size()][]);
                return coords;
            } else if(isCoordArrayArrayArray) {
                Coordinate[][][] coords = result.toArray(new Coordinate[result.size()][][]);
                return coords;
            } else {
                Object[] test = result.toArray(new Object[result.size()]);
                return test;
            }
        }
    }
    
}
