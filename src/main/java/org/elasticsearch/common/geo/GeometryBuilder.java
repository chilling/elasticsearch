/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.geo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public abstract class GeometryBuilder {

    private static final double DATELINE = 180d;
    
    public static PolygonBuilder newPolygon() {
        return new PolygonBuilder();
    }
    
    public static MultiPolygonBuilder newMultiPolygon() {
        return new MultiPolygonBuilder();
    }

    public abstract Geometry build(GeometryFactory factory);
    
    private static MultiPolygon multipolygon(GeometryFactory factory, Coordinate[][][] polygons) {
        Polygon[] polygonSet = new Polygon[polygons.length];
        for (int i = 0; i < polygonSet.length; i++) {
            polygonSet[i] = polygon(factory, polygons[i]);
        }
        return factory.createMultiPolygon(polygonSet);
    }
    
    private static Polygon polygon(GeometryFactory factory, Coordinate[][] polygon) {

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

    private static int component(final Edge edge, final int id, final ArrayList<Edge> edges) {
        System.out.print("Search shift...");
        // find a coordinate that is not part of the dateline 
        Edge any = edge;
        while(any.coordinate.x == +DATELINE || any.coordinate.x == -DATELINE) {
            if((any = any.next) == edge) {
                break;   
            }
        }
        
        double shift = any.coordinate.x>DATELINE?DATELINE:(any.coordinate.x<-DATELINE?-DATELINE:0);
        System.out.println("shift: " + shift);

        // run along the border of the component, collect the
        // edges, shift them according to the dateline and
        // update the component id
        int length = 0;
        Edge current = edge;
        System.out.print("building Component C"+id+"... ");
        do {
            System.out.print(current.component + " ");

            current.coordinate = shift(current.coordinate, shift); 
            current.component = id;
            if(edges != null) {
                edges.add(current);
            }
            
            length++;
        } while((current = current.next) != edge);

        System.out.println("done");
        return length;
    }
    
    private static Coordinate[] coordinates(Edge component, Coordinate[] coordinates) {
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = (component = component.next).coordinate;
        }
        return coordinates;
    }
    
    private static Coordinate[][] holes(Edge[] holes, int numHoles) {
        Coordinate[][] points = new Coordinate[numHoles][];
        
        for (int i = 0; i < numHoles; i++) {
            int length = component(holes[i], -(i+1), null);
            points[i] = coordinates(holes[i], new Coordinate[length]);
        }
        
        return points;
    } 
    
    private static Edge[] edges(Edge[] edges, int numHoles, ArrayList<ArrayList<Coordinate[]>> components) {
        ArrayList<Edge> mainEdges = new ArrayList<Edge>(edges.length);

        for (int i = 0; i < edges.length; i++) {
            if(edges[i].component>=0) {
                int length = component(edges[i], -(components.size()+numHoles+1), mainEdges);
                ArrayList<Coordinate[]> component = new ArrayList<Coordinate[]>();
                component.add(coordinates(edges[i], new Coordinate[length]));
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
        System.out.println("Holes ("+numHoles+"): " + Arrays.toString(holes));
        for (int i = 0; i < numHoles; i++) {
            final Edge current = holes[i];
            final int intersections = intersections(current.coordinate.x, edges);
            final int pos = Arrays.binarySearch(edges, 0, intersections, current, IntersectionOrder.INSTANCE);
            final int index = -(pos+2);
            final int component = -edges[index].component - numHoles - 1;

            System.out.println("\tposition ("+index+") of edge "+current+": " + edges[index]);
            System.out.println("\tComponent: " + component);
            System.out.println("\tHole intersections ("+current.coordinate.x+"): " + Arrays.toString(edges));
            System.out.println();
            
            components.get(component).add(points[i]);
        }
    }
    
    private static Coordinate[][][] buildCoordinates(ArrayList<ArrayList<Coordinate[]>> components) {
        Coordinate[][][] result = new Coordinate[components.size()][][];
        for (int i = 0; i < result.length; i++) {
            ArrayList<Coordinate[]> component = components.get(i);
            result[i] = component.toArray(new Coordinate[component.size()][]);
        }
//        
//        for (int i = 0; i < result.length; i++) {
//            System.out.println("Component " + i + ":");
//            for (int j = 0; j < result[i].length; j++) {
//                System.out.println("\t" + Arrays.toString(result[i][j]));
//            }
//        }

        return result;
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
    
    /**
     * Calculate the intersection of a line segment and a vertical dateline.
     *  
     * @param p1 start-point of the line segment
     * @param p2 end-point of the line segment
     * @param dateline x-coordinate of the vertical dateline 
     * @return position of the intersection in the open range [0..1) if the line
     * segment intersects with the line segment. Otherwise this method returns Nan
     */
    private static final double intersection(Coordinate p1, Coordinate p2, double dateline) {
        if(p1.x == p2.x) {
            return Double.NaN;
        } else {
            final double t = (dateline - p1.x) / (p2.x - p1.x);

            if(t > 1 || t <= 0) {
                return Double.NaN;
            } else {
                return t;
            }
        }
    }
    
    /**
     * Calculate all intersections of line segments and a vertical line.
     * The Array of edges will be ordered asc by the y-coordinate of the
     * intersections of edges.
     *  
     * @param dateline x-coordinate of the dateline
     * @param edges set of edges that may intersect with the dateline
     * @return number of intersecting edges
     */
    private static int intersections(double dateline, Edge[] edges) {
        int numIntersections = 0;
        for(int i=0; i<edges.length; i++) {
            Coordinate p1 = edges[i].coordinate;
            Coordinate p2 = edges[i].next.coordinate;

            edges[i].intersection = null;

            double intersection = intersection(p1, p2, dateline);
            if(!Double.isNaN(intersection)) {
                
                if(intersection == 1) {
                    if(Double.compare(p1.x, dateline) == Double.compare(edges[i].next.next.coordinate.x, dateline)) {
                        // Ignore the ear
                        continue;
                    }else if(p2.x == dateline) {
                        // Ignore Linesegment on dateline
                        continue;
                    }
                }
                edges[i].intersection(intersection);
                numIntersections++;
            }
        }
        Arrays.sort(edges, IntersectionOrder.INSTANCE);
        return numIntersections;
    }
    
    private static Coordinate shift(Coordinate coordinate, double dateline) {
        if(dateline == 0) {
            return coordinate;
        } else {
            return new Coordinate(-2*dateline + coordinate.x, coordinate.y);
        }
    }
    
    public static class GeoRingBuilder<E> {
    
        private final E parent;
        private final int component;
        private final ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
        
        private GeoRingBuilder(E parent, int component) {
            this.parent = parent;
            this.component = component;
        }
        
        public GeoRingBuilder<E> point(double lat, double lon) {
            this.coordinates.add(new Coordinate(lat, lon));
            return this;
        }
        
        public E close() {
            return parent;
        }
        
        private int toArray(boolean direction, Edge[] edges, int offset) {
            Coordinate[] points = coordinates.toArray(new Coordinate[coordinates.size()]);
            Edge.ring(component, direction, points, 0, edges, offset, points.length);
            return points.length;
        } 
    }

    public static class MultiPolygonBuilder extends GeometryBuilder {
        private final ArrayList<EmbeddedPolygonBuilder<MultiPolygonBuilder>> polygons = new ArrayList<EmbeddedPolygonBuilder<MultiPolygonBuilder>>();
        
        public EmbeddedPolygonBuilder<MultiPolygonBuilder> polygon() {
            EmbeddedPolygonBuilder<MultiPolygonBuilder> polygon = new EmbeddedPolygonBuilder<MultiPolygonBuilder>(this);
            this.polygons.add(polygon);
            return polygon;
        }
        
        @Override
        public Geometry build(GeometryFactory factory) {
            ArrayList<Polygon> polygons = new ArrayList<Polygon>();
            for (int i = 0; i < this.polygons.size(); i++) {
                Coordinate[][][] coordinates = this.polygons.get(i).coordinates();
                for (Coordinate[][] polygon : coordinates) {
                    polygons.add(polygon(factory, polygon));
                }
            }
            return factory.createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
        }
    }

    public static class PolygonBuilder extends EmbeddedPolygonBuilder<PolygonBuilder> {
        public PolygonBuilder() {
            super(null);
        }
        
        @Override
        public PolygonBuilder close() {
            super.close();
            return this;
        }
    }

    public static class EmbeddedPolygonBuilder<E extends GeometryBuilder> extends GeometryBuilder {
        
        private final E parent;
        private final GeoRingBuilder<EmbeddedPolygonBuilder<E>> polygon = new GeoRingBuilder<EmbeddedPolygonBuilder<E>>(this, 0);
        private final ArrayList<GeoRingBuilder<EmbeddedPolygonBuilder<E>>> holes = new ArrayList<GeoRingBuilder<EmbeddedPolygonBuilder<E>>>(); 

        private EmbeddedPolygonBuilder(E parent) {
            this.parent = parent;
        }
        
        public EmbeddedPolygonBuilder<E> point(double lat, double lon) {
            polygon.point(lat, lon);
            return this;
        }
    
        public GeoRingBuilder<EmbeddedPolygonBuilder<E>> hole() {
            GeoRingBuilder<EmbeddedPolygonBuilder<E>> hole = new GeoRingBuilder<EmbeddedPolygonBuilder<E>>(this, holes.size()+1);
            this.holes.add(hole);
            return hole;
        }
        
        public E close() {
            this.polygon.close();
            return parent;
        }
    
        public Coordinate[][] points() {
            Coordinate[][] points = new Coordinate[1+holes.size()][];
            points[0] = polygon.coordinates.toArray(new Coordinate[polygon.coordinates.size()]);
            for (int i = 0; i < holes.size(); i++) {
                GeoRingBuilder<?> hole = holes.get(i);
                points[1+i] = hole.coordinates.toArray(new Coordinate[hole.coordinates.size()]);
            }
            return points;
        }
        
        public Coordinate[][][] coordinates() {
            int numEdges = polygon.coordinates.size();
            for (int i = 0; i < holes.size(); i++) {
                numEdges += holes.get(i).coordinates.size();
            }
            
            Edge[] edges = new Edge[numEdges];
            Edge[] holeComponents = new Edge[holes.size()];
            
            int offset = polygon.toArray(true, edges, 0);
            for (int i = 0; i < holes.size(); i++) {
                int length = this.holes.get(i).toArray(false, edges, offset);
                holeComponents[i] = edges[offset];
                offset += length;
            }

            int numHoles = holeComponents.length;
            numHoles = merge(edges, 0, intersections(+DATELINE, edges), holeComponents, numHoles);
            numHoles = merge(edges, 0, intersections(-DATELINE, edges), holeComponents, numHoles);
     
            return compose(edges, holeComponents, numHoles);
        }
        
        @Override
        public Geometry build(GeometryFactory factory) {
            Coordinate[][][] polygons = coordinates();
            return polygons.length==1
                    ? polygon(factory, polygons[1])
                    : multipolygon(factory, polygons);
        }

    }
    
    protected static final class Edge {
        Coordinate coordinate;       // coordinate of the start point
        Edge next;                   // next segment
        Coordinate intersection;     // potential intersection with dateline
        int component = -1;          // id of the component this edge belongs to 
        
        private Edge(Coordinate coordinate, Edge next, Coordinate intersection) {
            this.coordinate = coordinate;
            this.next = next;
            this.intersection = intersection;
            if(next != null) {
                this.component = next.component;
            }
        }
        
        private Edge(Coordinate coordinate, Edge next) {
            this(coordinate, next, null);
        }
        
        private static final int top(Coordinate[] points, int offset, int length) {
            int top = 0;
            for (int i = 1; i < length; i++) {
                if(points[offset + i].y < points[offset + top].y) {
                    top = i;
                }
            }
            return top;
        }
        
        /**
         * Concatenate a set of points to a polygon 
         * @param component component id of the polygon
         * @param direction direction of the ring
         * @param points list of points to concatenate
         * @param offset index of the first point
         * @param edges Array of edges to write the result to 
         * @param toffset index of the first edge in the result 
         * @param length number of points to use
         * @return the edges creates 
         */
        private static Edge[] concat(int component, boolean direction, Coordinate[] points, int offset, Edge[] edges, int toffset, int length) {
            edges[toffset] = new Edge(points[offset], null);
            for (int i = 1; i < length; i++) {
                if(direction) {
                    edges[toffset + i] = new Edge(points[offset + i], edges[toffset + i - 1]);
                    edges[toffset + i].component = component;
                } else {
                    edges[toffset + i - 1].next = edges[toffset + i] = new Edge(points[offset + i], null); 
                    edges[toffset + i - 1].component = component;
                }
            }
            
            if(direction) {
                edges[toffset].next = edges[toffset + length - 1];
                edges[toffset].component = component;
            } else {
                edges[toffset + length - 1].next = edges[toffset];
                edges[toffset + length - 1].component = component;
            }
            
            System.out.println("Ring("+direction+"): " + edges[toffset] + " " + edges[toffset].next);
            
            return edges;
        }
        
        /**
         * Create a connected list of a list of coordinates
         * 
         * @param points array of point
         * @param offset index of the first point
         * @param length number of points
         * @return Array of edges 
         */
        protected static Edge[] ring(int component, boolean direction, Coordinate[] points, int offset, Edge[] edges, int toffset, int length) {
            // calculate the direction of the points:
            // find the point a the top of the set and check its
            // neighbors orientation. So direction is equivalent
            // to clockwise/counterclockwise
            final int top = top(points, offset, length);
            final int prev = (offset + ((top + length - 1) % length));
            final int next = (offset + ((top + 1) % length));
            return concat(component, direction ^ (points[offset + prev].x > points[offset + next].x), points, offset, edges, toffset, length);
        }

        /**
         * Set the intersection of this line segment to the given position 
         * @param position position of the intersection [0..1]
         * @return the {@link Coordinate} of the intersection
         */
        protected Coordinate intersection(double position) {
            if(position == 0) {
                return intersection = coordinate;
            } else if(position == 1) {
                return intersection = next.coordinate;
            } else {
                final double x = coordinate.x + position * (next.coordinate.x - coordinate.x);
                final double y = coordinate.y + position * (next.coordinate.y - coordinate.y);
                return intersection = new Coordinate(x, y);
            }
        }
     
        @Override
        public String toString() {
            return "Edge[C"+component+"; x="+coordinate.x+" "+"; "+(intersection==null?"":intersection.y)+"]";
        }
    }

    private static final class IntersectionOrder implements Comparator<Edge> {

        private static final IntersectionOrder INSTANCE = new IntersectionOrder(); 
        
        @Override
        public int compare(Edge o1, Edge o2) {
            if(o1.intersection == null && o2.intersection == null) {
              return 0;  
            } else if(o1.intersection == null) {
                return 1;
            } else if(o2.intersection == null) {
                return -1;
            } else {
                return Double.compare(o1.intersection.y, o2.intersection.y);
            }
        }
        
    }
    
}
