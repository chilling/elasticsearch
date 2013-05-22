package org.elasticsearch.common.geo.builders;

import java.io.IOException;

import org.elasticsearch.common.geo.GeoShapeConstants;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.unit.DistanceUnit.Distance;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.spatial4j.core.shape.Shape;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class CircleBuilder extends GeoShapeBuilder {
    
    public static final String FIELD_RADIUS = "radius";
    public static final String TYPE = "circle";

    
    private DistanceUnit unit;
    private double radius;
    private Coordinate center;
    
    public CircleBuilder center(Coordinate center) {
        this.center = center;
        return this;
    }
    
    public CircleBuilder center(double lat, double lon) {
        return center(new Coordinate(lon, lat));
    }
    
    public CircleBuilder radius(String radius) {
        return radius(DistanceUnit.Distance.parseDistance(radius, DistanceUnit.METERS));
    }
    
    public CircleBuilder radius(Distance radius) {
        return radius(radius.value, radius.unit);
    }
    
    public CircleBuilder radius(double radius, String unit) {
        return radius(radius, DistanceUnit.fromString(unit));
    }

    public CircleBuilder radius(double radius, DistanceUnit unit) {
        this.unit = unit;
        this.radius = radius;
        return this;
    }
    
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(FIELD_TYPE, TYPE);
        builder.field(FIELD_RADIUS, unit.toString(radius));
        builder.field(FIELD_COORDINATES);
        toXContent(builder, center);
        return builder.endObject();
    }

    @Override
    public Shape buildShape(GeometryFactory factory, boolean fixDateline) {
        return GeoShapeConstants.SPATIAL_CONTEXT.makeCircle(center.x, center.y, 180 * radius / unit.getEarthCircumference());
    }

}
