package org.elasticsearch.common.geo.builders;

import java.io.IOException;

import org.elasticsearch.common.geo.GeoShapeConstants;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.spatial4j.core.shape.Rectangle;
import com.vividsolutions.jts.geom.Coordinate;

public class EnvelopeBuilder extends GeoShapeBuilder {

    public static final String TYPE = "envelope"; 

    protected Coordinate topLeft;
    protected Coordinate bottomRight;
    
    public EnvelopeBuilder topLeft(Coordinate topLeft) {
        this.topLeft = topLeft;
        return this;
    }
    
    public EnvelopeBuilder topLeft(double longitude, double latitude) {
        return topLeft(coordinate(longitude, latitude));
    }

    public EnvelopeBuilder bottomRight(Coordinate bottomRight) {
        this.bottomRight = bottomRight;
        return this;
    }

    public EnvelopeBuilder bottomRight(double longitude, double latitude) {
        return bottomRight(coordinate(longitude, latitude));
    }
    
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(FIELD_TYPE, TYPE);
        builder.startArray(FIELD_COORDINATES);
        toXContent(builder, topLeft);
        toXContent(builder, bottomRight);
        builder.endArray();
        return builder.endObject();
    }

    @Override
    public Rectangle buildShape() {
        return GeoShapeConstants.SPATIAL_CONTEXT.makeRectangle(
                topLeft.x, bottomRight.x,
                topLeft.y, bottomRight.y);
    }
    
}
