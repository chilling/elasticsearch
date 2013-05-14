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

import org.apache.lucene.util.BytesRef;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.common.xcontent.json.JsonXContentParser;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.testng.annotations.Test;

public class GeoWrappingTests {

    @Test
    public void wrappingTest() throws IOException {
        FileInputStream in = new FileInputStream("/home/schilling/Desktop/countries.geo.json");

        StringBuilder sb = new StringBuilder();
        int c;
        while((c=in.read())>=0) {
            sb.append((char)c);
        }


        System.out.println(sb.toString());

        
        String clusterName = "test1";
        Node node1 = NodeBuilder.nodeBuilder().clusterName(clusterName).build();
        Node node2 = NodeBuilder.nodeBuilder().clusterName(clusterName).build();

        node1.start();
        node2.start();
        
        Client client = node1.client();

        if(client.admin().indices().prepareExists("collections").execute().actionGet().isExists()) {
            client.admin().indices().prepareDelete("collections").execute().actionGet();
        }
        
        XContentBuilder mapping = JsonXContent.contentBuilder().startObject()
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
        
        System.out.println(client.admin().indices().prepareCreate("collections").addMapping("collection", mapping));
        
        CreateIndexResponse create = client.admin().indices().prepareCreate("collections").addMapping("collection", mapping).execute().actionGet();
        
        client.admin().indices().prepareOpen("collections").execute().actionGet();
        
        client.admin().indices().prepareRefresh("collections").execute().actionGet();
        
        
        System.out.println(create);
        
        IndexResponse indexed = client.prepareIndex("collections", "collection").setCreate(true).setSource(sb.toString()).execute().actionGet();
        
        System.out.println(indexed);
        
        GetResponse object = client.prepareGet("collections", "collection", indexed.getId()).execute().actionGet();
        
        System.out.println(object.getSourceAsString());
        
        client.close();
        node1.close();
        node2.close();
    } 
    
}
