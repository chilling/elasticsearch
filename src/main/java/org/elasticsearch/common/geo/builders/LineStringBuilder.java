package org.elasticsearch.common.geo.builders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.elasticsearch.common.geo.GeoShapeConstants;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class LineStringBuilder extends PointCollection {

    public static final String TYPE = "linestring";

    protected LineStringBuilder() {
        this(new ArrayList<Coordinate>());
    }
    
    protected LineStringBuilder(ArrayList<Coordinate> points) {
        super(points);
    }

    @Override
    public LineStringBuilder point(Coordinate coordinate) {
        super.point(coordinate);
        return this;
    }

    @Override
    public LineStringBuilder point(double latitude, double longitude) {
        super.point(latitude, longitude);
        return this;
    }
    
    @Override
    public LineStringBuilder points(Collection<? extends Coordinate> coordinates) {
       super.points(coordinates);
       return this;
    }
    
    @Override
    public LineStringBuilder points(Coordinate...coordinates) {
        super.points(coordinates);
        return this;
    }
    
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(FIELD_TYPE, TYPE);
        builder.field(FIELD_COORDINATES);
        coordinatesToXcontent(builder, false);
        builder.endObject();
        return builder;
    }
    
    @Override
    public Shape buildShape() {
        Coordinate[] coordinates = points.toArray(new Coordinate[points.size()]);
        Geometry geometry;
        if(wrapdateline) {
            ArrayList<LineString> strings = decompose(FACTORY, coordinates, new ArrayList<LineString>());

            if(strings.size() == 1) {
                geometry = strings.get(0);
            } else {
                LineString[] linestrings = strings.toArray(new LineString[strings.size()]);
                geometry = FACTORY.createMultiLineString(linestrings);
            }

        } else {
            geometry = FACTORY.createLineString(coordinates);
        }
        return new JtsGeometry(geometry, GeoShapeConstants.SPATIAL_CONTEXT, wrapdateline);
    }
    
    protected static ArrayList<LineString> decompose(GeometryFactory factory, Coordinate[] coordinates, ArrayList<LineString> strings) {
        int offset = 0;
        
        for (int i = 1; i < coordinates.length; i++) {
            double t = intersection(coordinates[i-1], coordinates[i], DATELINE);
            if(!Double.isNaN(t)) {
                Coordinate[] part;
                if(t<1) {
                    coordinates[i] = Edge.position(coordinates[i-1], coordinates[i], t);
                    part = Arrays.copyOfRange(coordinates, offset, i);
                    offset = i-1;
                } else {
                    part = Arrays.copyOfRange(coordinates, offset, i);
                    offset = i;
                }
                strings.add(factory.createLineString(part));
            }
        }

        if(offset > 0) {
            Coordinate[] part = Arrays.copyOfRange(coordinates, offset, coordinates.length-1);
            strings.add(factory.createLineString(part));
        } else {
            strings.add(factory.createLineString(coordinates));
        }
        return strings;
    }
    
    protected int toArray(int component, boolean direction, Edge[] edges, int offset) {
        Coordinate[] points = this.points.toArray(new Coordinate[this.points.size()]);
        Edge.ring(component, direction, points, 0, edges, offset, points.length);
        return points.length;
    }
    
    
}
