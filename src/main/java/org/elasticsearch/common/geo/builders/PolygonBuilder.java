package org.elasticsearch.common.geo.builders;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;

public class PolygonBuilder extends BasePolygonBuilder<PolygonBuilder> {

    public PolygonBuilder() {
        this(new ArrayList<Coordinate>());
    }
    
    protected PolygonBuilder(ArrayList<Coordinate> points) {
        super();
        this.shell = new Ring<PolygonBuilder>(this, points);
    }

    @Override
    public PolygonBuilder point(Coordinate coordinate) {
        super.point(coordinate);
        return this;
    }
    
    @Override
    public PolygonBuilder point(double longitude, double latitude) {
        super.point(longitude, latitude);
        return this;
    }
    
    @Override
    public PolygonBuilder points(Coordinate... coordinates) {
        super.points(coordinates);
        return this;
    }
    
    @Override
    public PolygonBuilder hole(LineStringBuilder hole) {
        super.hole(hole);
        return this;
    }
    
    public Ring<PolygonBuilder> hole() {
        Ring<PolygonBuilder> hole = new Ring<PolygonBuilder>(this);
        this.holes.add(hole);
        return hole;
    }
    
    @Override
    public PolygonBuilder close() {
        super.close();
        return this;
    }
}
