package org.elasticsearch.common.geo;

import java.io.IOException;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.geo.ShapeBuilder.PolygonBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.testng.annotations.Test;

import com.spatial4j.core.shape.Shape;

public class PolygonBenchmark {

    @Test
    public void benchmark() throws IOException {
        Node node1 = NodeBuilder.nodeBuilder().build();
        Node node2 = NodeBuilder.nodeBuilder().build();
        
        node1.start();
        node2.start();

        Client client = node1.client();
        
        client.admin().cluster().prepareHealth("points").setWaitForGreenStatus().execute().actionGet();
        
        denseRaster(client, 1);
        
        System.out.println("points\tstar\ttook\thits\tcircle\ttook\thits");
        
        for (int i = 1000; i <= 10000; i+=1000) {
            
            System.out.print("" + i*2);
            
            GeoShapeQueryBuilder query1 = star("location", i);
            long start1 = System.currentTimeMillis();
            SearchResponse result1 = client.prepareSearch("points").setTypes("data").setQuery(query1).execute().actionGet();
            long end1 = System.currentTimeMillis();
            
            System.out.print("\t" + (end1-start1) + "\t" + result1.getTook() + "\t" + result1.getHits().getTotalHits());
            
            GeoShapeQueryBuilder query2 = circle("location", 2*i);
            long start2 = System.currentTimeMillis();
            SearchResponse result2 = client.prepareSearch("points").setTypes("data").setQuery(query2).execute().actionGet();
            long end2 = System.currentTimeMillis();
            System.out.print("\t" + (end2-start2) + "\t" + result2.getTook() + "\t" + result2.getHits().getTotalHits());
            
            System.out.println();
        }
        
        
        
        client.close();
        
        
        node1.stop();
        node2.stop();
    }
    
    public void denseRaster(Client client, double step) throws IOException {

        IndicesExistsResponse index = client.admin().indices().prepareExists("points").execute().actionGet();
        
        if(index.isExists()) {
            client.admin().indices().prepareDelete("points").execute().actionGet();
        }
        
        String mapping = JsonXContent.contentBuilder().startObject()
                .startObject("data")
                .startObject("properties")
                    .startObject("location")
                        .field("type", "geo_shape")
                    .endObject()
                .endObject()
                .endObject()
                .endObject().string();
        
        CreateIndexRequestBuilder addMapping = client.admin().indices().prepareCreate("points").addMapping("data", mapping);
        System.out.println(addMapping);
        addMapping.execute().actionGet();
        
        double height = 180d / step;
        double width = 360d*2 / step; 
        
        System.out.println(width + "x" + height + "("+(width * height)+")");
        
        for(double lat = -90; lat < 90; lat+=step) {
            for(double lon = -180; lon < 180; lon+=step/2) {
                BytesReference source = JsonXContent.contentBuilder()
                        .startObject()
                            .startObject("location")
                                .field("type", "point")
                                .startArray("coordinates")
                                    .value(lon).value(lat)
                                .endArray()
                            .endObject()
                        .endObject().bytes();

                
                client.prepareIndex("points", "data").setCreate(true).setSource(source).execute().actionGet();
            }
        }
        
        client.admin().indices().prepareRefresh("points");
        
    }

    public GeoShapeQueryBuilder circle(String name, int points) {
        double outerRadius = 40;
        
        PolygonBuilder polygon = ShapeBuilder.newPolygon();
        
        for (int i = 0; i < points; i++) {
            double alpha = 2*Math.PI * (1.0f*i) / (1.0f * points);
            
            double x1 = outerRadius * Math.cos(alpha);
            double y1 = outerRadius * Math.sin(alpha);
            polygon.point(x1, y1);
        }
        
        polygon.close();

        Shape shape = polygon.build();
        
        GeoShapeQueryBuilder query = QueryBuilders.geoShapeQuery(name, shape);
        
        return query;
    }

    public GeoShapeQueryBuilder star(String name, int spikes) {
        
        double innerRadius = 10;
        double outerRadius = 40;
        
        PolygonBuilder polygon = ShapeBuilder.newPolygon();
        
        for (int i = 0; i < spikes; i++) {
            double alpha = 2*Math.PI * (1.0f*i) / (1.0f * spikes);
            double beta = 2*Math.PI * (1.0f*(i+1)) / (1.0f * spikes);
            
            double x1 = outerRadius * Math.cos(alpha);
            double y1 = outerRadius * Math.sin(alpha);
            polygon.point(x1, y1);

            double x2 = innerRadius * Math.cos(beta);
            double y2 = innerRadius * Math.sin(beta);
            polygon.point(x2, y2);
        }
        
        polygon.close();

        Shape shape = polygon.build();
        
        GeoShapeQueryBuilder query = QueryBuilders.geoShapeQuery(name, shape);
        
        return query;
    }
    
}
