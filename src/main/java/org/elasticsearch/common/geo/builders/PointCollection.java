package org.elasticsearch.common.geo.builders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.elasticsearch.common.xcontent.XContentBuilder;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class PointCollection extends GeoShapeBuilder {

    protected final ArrayList<Coordinate> points;
    
    protected PointCollection() {
        this(new ArrayList<Coordinate>());
    }
    
    protected PointCollection(ArrayList<Coordinate> points) {
        this.points = points;
    }
    
    protected PointCollection point(double longitude, double latitude) {
        return this.point(coordinate(longitude, latitude));
    } 
    
    protected PointCollection point(Coordinate coordinate) {
        this.points.add(coordinate);
        return this;
    }
    
    protected PointCollection points(Coordinate...coordinates) {
        return this.points(Arrays.asList(coordinates));
    }
    
    protected PointCollection points(Collection<? extends Coordinate> coordinates) {
        this.points.addAll(coordinates);
        return this;
    }

    protected Coordinate[] coordinates(boolean closed) {
        Coordinate[] result = points.toArray(new Coordinate[points.size() + (closed?1:0)]);
        if(closed) {
            result[result.length-1] = result[0];
        }
        return result;
    }
    
    protected XContentBuilder coordinatesToXcontent(XContentBuilder builder, boolean closed) throws IOException {
        builder.startArray();
        for(Coordinate point : points) {
            toXContent(builder, point);
        }
        if(closed) {
            Coordinate start = points.get(0);
            Coordinate end = points.get(points.size()-1);
            if(start.x != end.x || start.y != end.y) {
                toXContent(builder, points.get(0));
            }
        }
        builder.endArray();
        return builder;
    }
    
}
