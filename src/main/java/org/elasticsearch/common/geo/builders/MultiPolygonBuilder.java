package org.elasticsearch.common.geo.builders;

import java.io.IOException;
import java.util.ArrayList;

import org.elasticsearch.common.geo.GeoShapeConstants;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

public class MultiPolygonBuilder extends GeoShapeBuilder {
    
    public static final String TYPE = "multipolygon";

    protected final ArrayList<BasePolygonBuilder<?>> polygons = new ArrayList<BasePolygonBuilder<?>>();
    
    public MultiPolygonBuilder polygon(BasePolygonBuilder<?> polygon) {
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
        for(BasePolygonBuilder<?> polygon : polygons) {
            polygon.toXContent(builder, params);
        }
        builder.endArray();
        return builder.endObject();
    }
    
    @Override
    public Shape buildShape() {
        if(wrapdateline) {
            ArrayList<Polygon> polygons = new ArrayList<Polygon>(this.polygons.size());
            for (BasePolygonBuilder<?> polygon : this.polygons) {
                for(Coordinate[][] part : polygon.coordinates()) {
                    polygons.add(PolygonBuilder.polygon(FACTORY, part));
                }
            }

            Geometry geometry = FACTORY.createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
            return new JtsGeometry(geometry, GeoShapeConstants.SPATIAL_CONTEXT, true);
        } else {
            Polygon[] polygons = new Polygon[this.polygons.size()];
            for (int i = 0; i < polygons.length; i++) {
                polygons[i] = this.polygons.get(i).toPolygon(FACTORY);
            }
            
            Geometry geometry = FACTORY.createMultiPolygon(polygons);
            return new JtsGeometry(geometry, GeoShapeConstants.SPATIAL_CONTEXT, true);
        }
        
    }
    
   public class IPolygonBuilder extends BasePolygonBuilder<IPolygonBuilder> {

       public IPolygonBuilder() {
           super();
           this.shell = new Ring<IPolygonBuilder>(this);
       }
      
        @Override
        public Ring<IPolygonBuilder> hole() {
            Ring<IPolygonBuilder> hole = new Ring<IPolygonBuilder>(this);
            super.hole(hole);
            return hole;
        }
        
        @Override
        public MultiPolygonBuilder close() {
            return MultiPolygonBuilder.this;
        }
    }
}
