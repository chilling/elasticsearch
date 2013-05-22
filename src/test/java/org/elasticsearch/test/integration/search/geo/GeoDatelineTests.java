package org.elasticsearch.test.integration.search.geo;

import org.elasticsearch.common.geo.builders.GeoShapeBuilder;
import org.testng.annotations.Test;

public class GeoDatelineTests {

    @Test
    public void polygonDatelineTest() {
        // Building a triangle crossing the dateline
        GeoShapeBuilder builder = GeoShapeBuilder.newPolygon()
            .point(0, 90).point(-190, 0).point(0, 0)
        .close();
        
        builder.buildShape();


    }
    
}
