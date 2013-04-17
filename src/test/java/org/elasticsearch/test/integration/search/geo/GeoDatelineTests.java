package org.elasticsearch.test.integration.search.geo;

import org.elasticsearch.common.geo.ShapeBuilder;
import org.elasticsearch.common.geo.ShapeBuilder.PolygonBuilder;
import org.testng.annotations.Test;

public class GeoDatelineTests {

    @Test
    public void polygonDatelineTest() {
        // Building a triangle crossing the dateline
        PolygonBuilder builder = ShapeBuilder.newPolygon()
            .point(0, 90).point(-190, 0).point(0, 0)
        .close();
        
        builder.build();


    }
    
}
