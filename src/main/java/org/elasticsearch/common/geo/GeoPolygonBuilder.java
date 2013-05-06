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

public class GeoPolygonBuilder {

    private final GeoRingBuilder<GeoPolygonBuilder> polygon = new GeoRingBuilder<GeoPolygonBuilder>(this, 0);
    private final ArrayList<GeoRingBuilder<GeoPolygonBuilder>> holes = new ArrayList<GeoRingBuilder<GeoPolygonBuilder>>(); 
    
    public static void main(String[] args) {
        GeoPolygonBuilder builder = new GeoPolygonBuilder();
        builder.point(0, 0)
        .point(-250, 250)
        .point(250, 100)
        .point(300, -50)
        .point(-180, -100)
        .point(230, 10)
        .hole()
            .point(-200, 225)
            .point(-150, 215)
            .point(-200, 205)
        .close()
        .hole()
            .point(200, -50)
            .point(150, -30)
            .point(200, -20)
        .close()
        .close();
        
        builder.coordinates();
    }
    
    public GeoPolygonBuilder point(double lat, double lon) {
        polygon.point(lat, lon);
        return this;
    }
    
    public GeoRingBuilder<GeoPolygonBuilder> hole() {
        GeoRingBuilder<GeoPolygonBuilder> hole = new GeoRingBuilder<GeoPolygonBuilder>(this, holes.size()+1);
        this.holes.add(hole);
        return hole;
    }
    
    public GeoPolygonBuilder close() {
        this.polygon.close();
        return this;
    }
    
    public Coordinate[][] coordinates() {
        int numEdges = polygon.coordinates.size();
        for (int i = 0; i < holes.size(); i++) {
            numEdges += holes.get(i).coordinates.size();
        }
        
        Edge[] edges = new Edge[numEdges];
        int offset = polygon.toArray(true, edges, 0);
        for (int i = 0; i < holes.size(); i++) {
            offset += holes.get(i).toArray(true, edges, offset);
        }
        System.out.println("Edges: " + Arrays.toString(edges));
        
        merge(edges, 0, intersections(+180, edges));
//        merge(edges, 0, intersections(-180, edges));
 
        Coordinate[][] components = components(edges);
        
        for (int i = 0; i < components.length; i++) {
            System.out.println("Component " + i + " " + Arrays.toString(components[i]));
        }
        
        return components;
    }
    
    private static Coordinate[][] components(Edge[] edges) {
        ArrayList<Coordinate[]> components = new ArrayList<Coordinate[]>(); 
        for (int i = 0; i < edges.length; i++) {
            if(edges[i].component>=0) {

                Edge any = edges[i];
                while(any.coordinate.x == +180 || any.coordinate.x == -180)
                    any = any.next;
                
//                double shift = any.coordinate.x>180?180:(any.coordinate.x<-180?-180:0);
                double shift = 0;
                System.out.println("shift: " + shift);
                
                
                ArrayList<Coordinate> component = new ArrayList<Coordinate>();
                Edge current = edges[i];
                do {
                    component.add(shift(current.coordinate, shift));
                    current.component = -1;
                } while((current = current.next) != edges[i]);
                components.add(component.toArray(new Coordinate[component.size()]));
            }
        }
        return components.toArray(new Coordinate[components.size()][]);
    }
    
    private static void merge(Edge[] intersections, int offset, int length) {
        System.out.println("merging edges: " + Arrays.toString(intersections));
        for (int i = 0; i < length; i+=2) {
            Edge e1 = intersections[offset + i + 0]; 
            Edge e2 = intersections[offset + i + 1];
            
            if(e1.component == 0 && e2.component == 0) {
                connect(e1, e2);
            } else {
                Edge e3 = intersections[offset + i + 2]; 
                Edge e4 = intersections[offset + i + 3];
                cut(e1, e2, e3, e4);
                i+=2;
            }
        }
        System.out.println("merged edges: " + Arrays.toString(intersections));
    }
    
    private static void cut(Edge in1, Edge out1, Edge in2, Edge out2) {
        System.out.println("\tCUT " + in1 + " " + out1 + " " + in2 + " " + out2);
        
        Edge e3 = in1.split(in1.intersection);
        Edge e4 = out1.split(out1.intersection);
        
        Edge e2 = new Edge(out1.intersection, e3, in1.intersection);
        Edge e1 = new Edge(in1.intersection, e4, out1.intersection);
        
        Edge e8 = in2.split(in2.intersection);
        Edge e7 = out2.split(out2.intersection);
        
        Edge e5 = new Edge(in2.intersection, e7, out2.intersection);
        Edge e6 = new Edge(out2.intersection, e8, in2.intersection);
        
        out1.next = e2;
        in1.next = e1;
        out2.next = e6;
        in2.next = e5;

    }
    
    private static void connect(Edge in, Edge out) {
        System.out.println("\tCONNECT " + in + " " + out);

        if(in.intersection != in.next.coordinate) {
            Edge e1 = new Edge(in.intersection, in.next);
            
            if(out.intersection != out.next.coordinate) {
                Edge e2 = new Edge(out.intersection, out.next);
                in.next = new Edge(in.intersection, e2, in.intersection);
            } else {
                in.next = new Edge(in.intersection, out.next, in.intersection);
            }
            out.next = new Edge(out.intersection, e1, out.intersection);
        } else {
            Edge e2 = new Edge(out.intersection, in.next, out.intersection);

            if(out.intersection != out.next.coordinate) {
                Edge e1 = new Edge(out.intersection, out.next);
                in.next = new Edge(in.intersection, e1, in.intersection);
                
            } else {
                in.next = new Edge(in.intersection, out.next, in.intersection);
            }
            out.next = e2;
        }
    }
    
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
    
    private static int intersections(double dateline, Edge[] edges) {
        int numIntersections = 0;
        for(int i=0; i<edges.length; i++) {
            Coordinate p1 = edges[i].coordinate;
            Coordinate p2 = edges[i].next.coordinate;

            edges[i].intersection = null;

            System.out.println(p1 + "\t" + p2);
            
            double intersection = intersection(p1, p2, dateline);
            if(!Double.isNaN(intersection)) {
                
                if(intersection == 1) {
                    if(Double.compare(p1.x, dateline) == Double.compare(edges[i].next.next.coordinate.x, dateline)) {
                        // Ignore the ear
                        continue;
                    }else if(p2.x == dateline) {
                        // Ignore Linesegments on dateline 
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

    protected static final class Edge {
        final Coordinate coordinate; // coordinate of the start point
        Edge next;                   // next segment
        Coordinate intersection;     // potential intersection with dateline
        int component = -1;          // id of the component this edge belongs to 
        
        private Edge(Coordinate coordinate, Edge next, Coordinate intersection) {
            if(coordinate == null)
                throw new NullPointerException();
            
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
        
        private static final int top(Coordinate...points) {
            int top = 0;
            for (int i = 1; i < points.length-1; i++) {
                if(points[i].y < points[top].y) {
                    top = i;
                }
            }
            return top;
        }
        
        private Edge split(Coordinate point) {
            next = new Edge(point, next);
            next.component = component;
            return next;
        }
        
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
                edges[toffset].next = edges[length - 1];
                edges[toffset].component = component;
            } else {
                edges[toffset + length - 1].next = edges[toffset];
                edges[toffset + length - 1].component = component;
            }
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
        protected static Edge[] ring(int component, boolean direction, Coordinate[] points, int offset, int length) {
            return ring(component, direction, points, offset, new Edge[length], 0, length);
        }
        
        protected static Edge[] ring(int component, boolean direction, Coordinate[] points, int offset, Edge[] edges, int toffset, int length) {
            final int top = top(points);
            final int prev = (top + points.length - 1) % (points.length-1);
            final int next = (top + 1) % (points.length-1);
            return concat(component, direction ^ (points[prev].x > points[next].x), points, offset, edges, toffset, length);
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
