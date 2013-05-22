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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class MultiPolygonBuilder extends GeoShapeBuilder {
    
    public static final String TYPE = "multipolygon";

    protected final ArrayList<PolygonBuilder> polygons = new ArrayList<PolygonBuilder>();
    
    public MultiPolygonBuilder polygon(PolygonBuilder polygon) {
        this.polygons.add(polygon);
        return this;
    }
    
    public IPolygonBuilder polygon() {
        IPolygonBuilder polygon = new IPolygonBuilder();
        this.polygon(polygon);
        return polygon;
    }
    
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(FIELD_TYPE, TYPE);
        builder.startArray(FIELD_COORDINATES);
        for(PolygonBuilder polygon : polygons) {
            polygon.toXContent(builder, params);
        }
        builder.endArray();
        return builder.endObject();
    }
    
    @Override
    public Shape buildShape(GeometryFactory factory, boolean fixDateline) {
        if(fixDateline) {
            ArrayList<Polygon> polygons = new ArrayList<Polygon>(this.polygons.size());
            for (PolygonBuilder polygon : this.polygons) {
                for(Coordinate[][] part : polygon.coordinates()) {
                    polygons.add(PolygonBuilder.polygon(factory, part));
                }
            }

            Geometry geometry = factory.createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
            return new JtsGeometry(geometry, GeoShapeConstants.SPATIAL_CONTEXT, true);
        } else {
            Polygon[] polygons = new Polygon[this.polygons.size()];
            for (int i = 0; i < polygons.length; i++) {
                polygons[i] = this.polygons.get(i).asIs(factory);
            }
            
            Geometry geometry = factory.createMultiPolygon(polygons);
            return new JtsGeometry(geometry, GeoShapeConstants.SPATIAL_CONTEXT, true);
        }
        
    }
    
    class IPolygonBuilder extends PolygonBuilder {
        
        @Override
        public PolygonLineStringBuilder hole() {
            PolygonLineStringBuilder hole = new PolygonLineStringBuilder();
            super.hole(hole);
            return hole;
        }
        
        class PolygonLineStringBuilder extends ILineStringBuilder {
            
            public IPolygonBuilder close() {
                Coordinate start = points.get(0);
                Coordinate end = points.get(points.size()-1);
                if(start.x != end.x || start.y != end.y) {
                    points.add(start);
                }
                return IPolygonBuilder.this;
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
}
