package org.elasticsearch.common.geo.builders;

import java.io.IOException;
import java.util.Collection;

import org.elasticsearch.common.geo.GeoShapeConstants;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;

public class MultiPointBuilder extends PointCollection {

    public static final String TYPE = "multipoint";

    @Override
    public MultiPointBuilder point(Coordinate coordinate) {
        super.point(coordinate);
        return this;
    }

    @Override
    public MultiPointBuilder point(double latitude, double longitude) {
        super.point(latitude, longitude);
        return this;
    }
    
    @Override
    public MultiPointBuilder points(Collection<? extends Coordinate> coordinates) {
       super.points(coordinates);
       return this;
    }
    
    @Override
    public MultiPointBuilder points(Coordinate... coordinates) {
        super.points(coordinates);
        return this;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(FIELD_TYPE, TYPE);
        builder.startArray(FIELD_COORDINATES);
        for(Coordinate point : points) {
            toXContent(builder, point);
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    @Override
    public Shape buildShape(GeometryFactory factory, boolean fixDateline) {
        MultiPoint geometry = factory.createMultiPoint(points.toArray(new Coordinate[points.size()]));
        return new JtsGeometry(geometry, GeoShapeConstants.SPATIAL_CONTEXT, true);
    }
    
}
