package org.elasticsearch.common.geo.builders;

import java.io.IOException;

import org.elasticsearch.common.geo.GeoShapeConstants;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.spatial4j.core.shape.Shape;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class EnvelopeBuilder extends GeoShapeBuilder {

    public static final String TYPE = "envelope"; 

    protected Coordinate northWest;
    protected Coordinate southEast;
    
    public EnvelopeBuilder northWest(Coordinate northWest) {
        this.northWest = northWest;
        return this;
    }
    
    public EnvelopeBuilder northWest(double longitude, double latitude) {
        return northWest(coordinate(longitude, latitude));
    }

    public EnvelopeBuilder southEast(Coordinate southEast) {
        this.southEast = southEast;
        return this;
    }

    public EnvelopeBuilder southEast(double longitude, double latitude) {
        return southEast(coordinate(longitude, latitude));
    }
    
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(FIELD_TYPE, TYPE);
        builder.startArray(FIELD_COORDINATES);
        toXContent(builder, northWest);
        toXContent(builder, southEast);
        builder.endArray();
        return builder.endObject();
    }

    @Override
    public Shape buildShape(GeometryFactory factory, boolean fixDateline) {
        return GeoShapeConstants.SPATIAL_CONTEXT.makeRectangle(
                northWest.x, southEast.x,
                northWest.y, southEast.y);
    }
    
}
