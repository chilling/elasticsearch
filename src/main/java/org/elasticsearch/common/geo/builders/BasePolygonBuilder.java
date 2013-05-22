package org.elasticsearch.common.geo.builders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.elasticsearch.common.geo.GeoShapeConstants;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.jts.JtsGeometry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public abstract class BasePolygonBuilder<E extends BasePolygonBuilder<?>> extends GeoShapeBuilder {
    
    public static final String TYPE = "polygon";
    
    protected Ring<E> shell;
    protected final ArrayList<LineStringBuilder> holes = new ArrayList<LineStringBuilder>();

    public BasePolygonBuilder<E> point(double longitude, double latitude) {
        shell.point(latitude, longitude);
        return this;
    }
    
    public BasePolygonBuilder<E> point(Coordinate coordinate) {
        shell.point(coordinate);
        return this;
    }
    
    public BasePolygonBuilder<E> points(Coordinate...coordinates) {
        shell.points(coordinates);
        return this;
    }
    
    public BasePolygonBuilder<E> hole(LineStringBuilder hole) {
        holes.add(hole);
        return this;
    }
    
    public abstract Ring<E> hole();

    public GeoShapeBuilder close() {
        return shell.close();
    }
    
    public Coordinate[][][] coordinates() {
        int numEdges = shell.points.size();
        for (int i = 0; i < holes.size(); i++) {
            numEdges += holes.get(i).points.size();
        }
        
        Edge[] edges = new Edge[numEdges];
        Edge[] holeComponents = new Edge[holes.size()];
        
        int offset = shell.toArray(0, true, edges, 0);
        for (int i = 0; i < holes.size(); i++) {
            int length = this.holes.get(i).toArray(i+1, false, edges, offset);
            holeComponents[i] = edges[offset];
            offset += length;
        }

        int numHoles = holeComponents.length;
        numHoles = merge(edges, 0, intersections(+DATELINE, edges), holeComponents, numHoles);
        numHoles = merge(edges, 0, intersections(-DATELINE, edges), holeComponents, numHoles);
 
        return compose(edges, holeComponents, numHoles);
    }
    
    @Override
    public Shape buildShape() {
        Geometry geometry = buildGeometry(FACTORY, wrapdateline);
        return new JtsGeometry(geometry, GeoShapeConstants.SPATIAL_CONTEXT, true);
    }
    
    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(FIELD_TYPE, TYPE);
        builder.startArray(FIELD_COORDINATES);

        shell.coordinatesToXcontent(builder, true);
        for(LineStringBuilder hole : holes) {
            hole.coordinatesToXcontent(builder, true);
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }
    
    public Geometry buildGeometry(GeometryFactory factory, boolean fixDateline) {
        if(fixDateline) {
            Coordinate[][][] polygons = coordinates();
            return polygons.length==1
                    ? polygon(factory, polygons[0])
                    : multipolygon(factory, polygons);
        } else {
            return toPolygon(factory);
        }
    }

    public Polygon toPolygon() {
        return toPolygon(FACTORY);
    }

    protected Polygon toPolygon(GeometryFactory factory) {
        LinearRing shell = linearRing(factory, this.shell.points);
        LinearRing[] holes;
        if(this.holes.size()>=1) {
            holes = new LinearRing[this.holes.size()];
            for (int i = 0; i < holes.length; i++) {
                holes[i] = linearRing(factory, this.holes.get(i).points);
            }
        } else {
            holes = null;
        }
        return factory.createPolygon(shell, holes);
    }
    
    protected static LinearRing linearRing(GeometryFactory factory, ArrayList<Coordinate> coordinates) {
        return factory.createLinearRing(coordinates.toArray(new Coordinate[coordinates.size()]));
    }
    
    protected static Polygon polygon(GeometryFactory factory, Coordinate[][] polygon) {
        LinearRing shell = factory.createLinearRing(polygon[0]);
        LinearRing[] holes;
        
        if(polygon.length > 1) {
            holes = new LinearRing[polygon.length-1];
            for (int i = 0; i < holes.length; i++) {
                holes[i] = factory.createLinearRing(polygon[i+1]);
            }
        } else {
            holes = null;
        }
        return factory.createPolygon(shell, holes);
    }
    
    protected static MultiPolygon multipolygon(GeometryFactory factory, Coordinate[][][] polygons) {
        Polygon[] polygonSet = new Polygon[polygons.length];
        for (int i = 0; i < polygonSet.length; i++) {
            polygonSet[i] = polygon(factory, polygons[i]);
        }
        return factory.createMultiPolygon(polygonSet);
    }

    private static int component(final Edge edge, final int id, final ArrayList<Edge> edges) {
        // find a coordinate that is not part of the dateline 
        Edge any = edge;
        while(any.coordinate.x == +DATELINE || any.coordinate.x == -DATELINE) {
            if((any = any.next) == edge) {
                break;   
            }
        }
        
        double shift = any.coordinate.x>DATELINE?DATELINE:(any.coordinate.x<-DATELINE?-DATELINE:0);
        logger.debug("shift: {[]}", shift);

        // run along the border of the component, collect the
        // edges, shift them according to the dateline and
        // update the component id
        int length = 0;
        Edge current = edge;
        do {

            current.coordinate = shift(current.coordinate, shift); 
            current.component = id;
            if(edges != null) {
                edges.add(current);
            }
            
            length++;
        } while((current = current.next) != edge);

        return length;
    }
    
    private static Coordinate[] coordinates(Edge component, Coordinate[] coordinates) {
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = (component = component.next).coordinate;
        }
        return coordinates;
    }
    
    private static Coordinate[][][] buildCoordinates(ArrayList<ArrayList<Coordinate[]>> components) {
        Coordinate[][][] result = new Coordinate[components.size()][][];
        for (int i = 0; i < result.length; i++) {
            ArrayList<Coordinate[]> component = components.get(i);
            result[i] = component.toArray(new Coordinate[component.size()][]);
        }
        
        if(logger.isDebugEnabled()) {
            for (int i = 0; i < result.length; i++) {
                logger.debug("Component {[]}:", i);
                for (int j = 0; j < result[i].length; j++) {
                    logger.debug("\t" + Arrays.toString(result[i][j]));
                }
            }
        }
        
        return result;
    } 
    
    private static Coordinate[][] holes(Edge[] holes, int numHoles) {
        Coordinate[][] points = new Coordinate[numHoles][];
        
        for (int i = 0; i < numHoles; i++) {
            int length = component(holes[i], -(i+1), null);
            points[i] = coordinates(holes[i], new Coordinate[length+1]);
        }
        
        return points;
    } 
    
    private static Edge[] edges(Edge[] edges, int numHoles, ArrayList<ArrayList<Coordinate[]>> components) {
        ArrayList<Edge> mainEdges = new ArrayList<Edge>(edges.length);

        for (int i = 0; i < edges.length; i++) {
            if(edges[i].component>=0) {
                int length = component(edges[i], -(components.size()+numHoles+1), mainEdges);
                ArrayList<Coordinate[]> component = new ArrayList<Coordinate[]>();
                component.add(coordinates(edges[i], new Coordinate[length+1]));
                components.add(component);
            }
        }

        return mainEdges.toArray(new Edge[mainEdges.size()]);
    }
    
    private static Coordinate[][][] compose(Edge[] edges, Edge[] holes, int numHoles) {
        final ArrayList<ArrayList<Coordinate[]>> components = new ArrayList<ArrayList<Coordinate[]>>();
        assign(holes, holes(holes, numHoles), numHoles, edges(edges, numHoles, components), components);
        return buildCoordinates(components);
    }
    
    private static void assign(Edge[] holes, Coordinate[][] points, int numHoles, Edge[] edges, ArrayList<ArrayList<Coordinate[]>> components) {
        // Assign Hole to related components
        // To find the new component the hole belongs to all intersections of the
        // polygon edges with a vertical line are calculated. This vertical line
        // is an arbitrary point of the hole. The polygon edge next to this point
        // is part of the polygon the hole belongs to.
        logger.debug("Holes: " + Arrays.toString(holes));
        for (int i = 0; i < numHoles; i++) {
            final Edge current = holes[i];
            final int intersections = intersections(current.coordinate.x, edges);
            final int pos = Arrays.binarySearch(edges, 0, intersections, current, INTERSECTION_ORDER);
            final int index = -(pos+2);
            final int component = -edges[index].component - numHoles - 1;

            if(logger.isDebugEnabled()) {
                logger.debug("\tposition ("+index+") of edge "+current+": " + edges[index]);
                logger.debug("\tComponent: " + component);
                logger.debug("\tHole intersections ("+current.coordinate.x+"): " + Arrays.toString(edges));
            }
            
            components.get(component).add(points[i]);
        }
    }
    
    private static int merge(Edge[] intersections, int offset, int length, Edge[] holes, int numHoles) {
        // Intersections appear pairwise. On the first edge the inner of
        // of the polygon is entered. On the second edge the outer face
        // is entered. Other kinds of intersections are discard by the
        // intersection function
        
        for (int i = 0; i < length; i+=2) {
            Edge e1 = intersections[offset + i + 0];
            Edge e2 = intersections[offset + i + 1];

            // If two segments are connected maybe a hole must be deleted
            // Since Edges of components appear pairwise we need to check
            // the second edge only (the first edge is either polygon or
            // already handled)
            if(e2.component>0) {
                numHoles--;
                holes[e2.component-1] = holes[numHoles];
                holes[numHoles] = null;
            }

            connect(e1, e2);
        }
        
        return numHoles;
    }
    
    private static void connect(Edge in, Edge out) {
        // Connecting two Edges by inserting the point at
        // dateline intersection and connect these by adding
        // two edges between this points. One per direction
        if(in.intersection != in.next.coordinate) {
            // first edge has no point on dateline
            Edge e1 = new Edge(in.intersection, in.next);
            
            if(out.intersection != out.next.coordinate) {
                // second edge has no point on dateline
                Edge e2 = new Edge(out.intersection, out.next);
                in.next = new Edge(in.intersection, e2, in.intersection);
            } else {
                // second edge intersects with dateline
                in.next = new Edge(in.intersection, out.next, in.intersection);
            }
            out.next = new Edge(out.intersection, e1, out.intersection);
        } else {
            // first edge intersects with dateline
            Edge e2 = new Edge(out.intersection, in.next, out.intersection);

            if(out.intersection != out.next.coordinate) {
                // second edge has no point on dateline
                Edge e1 = new Edge(out.intersection, out.next);
                in.next = new Edge(in.intersection, e1, in.intersection);
                
            } else {
                // second edge intersects with dateline
                in.next = new Edge(in.intersection, out.next, in.intersection);
            }
            out.next = e2;
        }
    }
    
    public static class Ring<P extends BasePolygonBuilder<?>> extends LineStringBuilder {

        private final P parent;
        
        protected Ring(P parent) {
            this(parent, new ArrayList<Coordinate>());
        }
        
        protected Ring(P parent, ArrayList<Coordinate> points) {
            super(points);
            this.parent = parent;
        }
        
        @Override
        public Ring<P> point(Coordinate coordinate) {
            super.point(coordinate);
            return this;
        }
        
        @Override
        public Ring<P> point(double longitude, double latitude) {
            super.point(longitude, latitude);
            return this;
        }
        
        @Override
        public Ring<P> points(Coordinate... coordinates) {
            super.points(coordinates);
            return this;
        }
        
        public P close() {
            Coordinate start = points.get(0);
            Coordinate end = points.get(points.size()-1);
            if(start.x != end.x || start.y != end.y) {
                points.add(start);
            }
            return parent;
        }
        
    }


}
