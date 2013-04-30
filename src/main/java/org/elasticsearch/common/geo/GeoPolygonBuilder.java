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
import java.util.Comparator;


import com.vividsolutions.jts.geom.Coordinate;

public class GeoPolygonBuilder {
    
    private final ArrayList<Edge> edges = new ArrayList<Edge>();
    
    public GeoPolygonBuilder point(double lat, double lon) {
        Edge edge = new Edge(new Coordinate(lon, lat), null);
        if(edges.size()>0) {
            edges.get(edges.size()-1).next = edge;
        }
        edges.add(edge);
        return this;
    }
    
    public void close() {
        
    }
    

    private static final class Edge {
        final Coordinate coordinate; // coordinate of the start point
        Edge next;                   // next segment
        Coordinate intersection;     // potential intersection with dateline
        int component = -1;          // id of the component this edge belongs to 
        
        private Edge(Coordinate coordinate, Edge next, Coordinate intersection) {
            this.coordinate = coordinate;
            this.next = next;
            this.intersection = intersection;
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
            return next = new Edge(point, next);
        }
        
        private static Edge[] concat(boolean direction, Coordinate[] points, int offset, int length) {
            Edge[] edges = new Edge[points.length];
            edges[0] = new Edge(points[offset], null);
            for (int i = 1; i < edges.length; i++) {
                if(direction) {
                    edges[i] = new Edge(points[offset + i], edges[i-1]); 
                } else {
                    edges[i-1].next = edges[i] = new Edge(points[offset + i], null); 
                }
            }
            
            if(direction) {
                edges[0].next = edges[edges.length - 1];
            } else {
                edges[edges.length - 1].next = edges[0];
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
        protected static Edge[] ring(Coordinate[] points, int offset, int length) {
            final int top = top(points);
            final int prev = (top + points.length - 1) % (points.length-1);
            final int next = (top + 1) % (points.length-1);
            return concat(points[prev].x > points[next].x, points, offset, length);
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
