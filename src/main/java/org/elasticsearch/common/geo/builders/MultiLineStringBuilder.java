package org.elasticsearch.common.geo.builders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.elasticsearch.common.geo.GeoShapeConstants;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class MultiLineStringBuilder extends GeoShapeBuilder {

    public static final String TYPE = "multilinestring";

    private final ArrayList<LineStringBuilder> lines = new ArrayList<LineStringBuilder>();
    
    public ILineStringBuilder linestring() {
        ILineStringBuilder line = new ILineStringBuilder();
        this.lines.add(line);
        return line;
    }
    
    public MultiLineStringBuilder linestring(LineStringBuilder line) {
        this.lines.add(line);
        return this;
    }

    public Coordinate[][] coordinates() {
        Coordinate[][] result = new Coordinate[lines.size()][];
        for (int i = 0; i < result.length; i++) {
            result[i] = lines.get(i).coordinates(false);
        }
        return result;
    }
    
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(FIELD_TYPE, TYPE);
        builder.field(FIELD_COORDINATES);
        builder.startArray();
        for(LineStringBuilder line : lines) {
            line.coordinatesToXcontent(builder, false);
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }
    
    @Override
    public Shape buildShape() {
        final Geometry geometry;
        if(fixDateline) {
            ArrayList<LineString> parts = new ArrayList<LineString>();
            for (LineStringBuilder line : lines) {
                LineStringBuilder.decompose(FACTORY, line.coordinates(false), parts);
            }
            if(parts.size() == 1) {
                geometry = parts.get(0);
            } else {
                LineString[] lineStrings = parts.toArray(new LineString[parts.size()]);
                geometry = FACTORY.createMultiLineString(lineStrings);
            }
        } else {
            LineString[] lineStrings = new LineString[lines.size()];
            for (int i = 0; i < lineStrings.length; i++) {
                lineStrings[i] = FACTORY.createLineString(lines.get(i).coordinates(false));
            }
            geometry = FACTORY.createMultiLineString(lineStrings);
        }
        
        return new JtsGeometry(geometry, GeoShapeConstants.SPATIAL_CONTEXT, true);
    }

    class ILineStringBuilder extends LineStringBuilder {
        
        public MultiLineStringBuilder end() {
            return MultiLineStringBuilder.this;
        }
        
        public Coordinate[] coordinates() {
            return super.coordinates(false);
        }
        
        @Override
        public ILineStringBuilder point(Coordinate coordinate) {
            super.point(coordinate);
            return this;
        }

        @Override
        public ILineStringBuilder point(double latitude, double longitude) {
            super.point(latitude, longitude);
            return this;
        }
        
        @Override
        public ILineStringBuilder points(Collection<? extends Coordinate> coordinates) {
           super.points(coordinates);
           return this;
        }
        
        @Override
        public ILineStringBuilder points(Coordinate... coordinates) {
            super.points(coordinates);
            return this;
        }
    }
}
