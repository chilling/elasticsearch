package org.elasticsearch.common.geo.builders;

import java.io.IOException;

import org.elasticsearch.common.geo.GeoShapeConstants;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.spatial4j.core.shape.Point;
import com.vividsolutions.jts.geom.Coordinate;

public class PointBuilder extends GeoShapeBuilder {

    public static final String TYPE = "point";

    private Coordinate coordinate;
    
    public PointBuilder coordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
        return this;
    }
    
    public double longitude() {
        return coordinate.x;
    }
    
    public double latitude() {
        return coordinate.y;
    }
    
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
       builder.startObject();
       builder.field(FIELD_TYPE, TYPE);
       builder.field(FIELD_COORDINATES);
       toXContent(builder, coordinate);
       return builder.endObject(); 
    }
    
    @Override
    public Point buildShape() {
        return GeoShapeConstants.SPATIAL_CONTEXT.makePoint(coordinate.x, coordinate.y);
    }
    
}
