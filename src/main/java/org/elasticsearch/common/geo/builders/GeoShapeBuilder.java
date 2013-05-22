package org.elasticsearch.common.geo.builders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.elasticsearch.ElasticSearchParseException;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.unit.DistanceUnit.Distance;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;

import com.spatial4j.core.shape.Shape;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public abstract class GeoShapeBuilder implements ToXContent {

    protected static final double DATELINE = 180;
    protected static final GeometryFactory FACTORY = new GeometryFactory();
    
    protected final boolean fixDateline = true;
    
    protected GeoShapeBuilder() {

    }

    protected static Coordinate coordinate(double longitude, double latitude) {
        return new Coordinate(longitude, latitude);
    }
    
    public static PointBuilder newPoint(double longitude, double latitude) {
        return newPoint(new Coordinate(longitude, latitude));
    }
    
    public static PointBuilder newPoint(Coordinate coordinate) {
        return new PointBuilder().coordinate(coordinate);
    }

    public static MultiPointBuilder newMultiPoint() {
        return new MultiPointBuilder();
    }

    public static LineStringBuilder newLineString() {
        return new LineStringBuilder();
    }

    public static MultiLineStringBuilder newMultiLinestring() {
        return new MultiLineStringBuilder();
    } 

    public static PolygonBuilder newPolygon() {
        return new PolygonBuilder();
    } 

    public static MultiPolygonBuilder newMultiPolygon() {
        return new MultiPolygonBuilder();
    } 

    public static CircleBuilder newCircleBuilder() {
        return new CircleBuilder();
    }

    public static EnvelopeBuilder newEnvelope() {
        return new EnvelopeBuilder();
    }
    
    @Override
    public String toString() {
        try {
            XContentBuilder xcontent = JsonXContent.contentBuilder();
            return toXContent(xcontent, EMPTY_PARAMS).prettyPrint().string();
        } catch (IOException e) {
            return super.toString();
        }
        
    }
    
    public abstract Shape buildShape();
    
    /**
     * Recursive method which parses the arrays of coordinates used to define Shapes
     *
     * @param parser Parser that will be read from
     * @return CoordinateNode representing the start of the coordinate tree
     * @throws IOException Thrown if an error occurs while reading from the XContentParser
     */
    private static CoordinateNode parseCoordinates(XContentParser parser) throws IOException {
        XContentParser.Token token = parser.nextToken();

        // Base case
        if (token != XContentParser.Token.START_ARRAY) {
            double lon = parser.doubleValue();
            token = parser.nextToken();
            double lat = parser.doubleValue();
            token = parser.nextToken();
            return new CoordinateNode(new Coordinate(lon, lat));
        }

        List<CoordinateNode> nodes = new ArrayList<CoordinateNode>();
        while (token != XContentParser.Token.END_ARRAY) {
            nodes.add(parseCoordinates(parser));
            token = parser.nextToken();
        }

        return new CoordinateNode(nodes);
    }

    public static GeoShapeBuilder parse(XContentParser parser) throws IOException {
        if(parser.currentToken() == XContentParser.Token.VALUE_NULL) {
            return null;
        } else if (parser.currentToken() != XContentParser.Token.START_OBJECT) {
            throw new ElasticSearchParseException("Shape must be an object consisting of type and coordinates");
        }
        
        String shapeType = null;
        Distance radius = null;
        CoordinateNode node = null;

        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                String fieldName = parser.currentName();

                if (FIELD_TYPE.equals(fieldName)) {
                    parser.nextToken();
                    shapeType = parser.text().toLowerCase(Locale.ROOT);
                    if (shapeType == null) {
                        throw new ElasticSearchParseException("Unknown Shape type [" + parser.text() + "]");
                    }
                } else if (FIELD_COORDINATES.equals(fieldName)) {
                    parser.nextToken();
                    node = parseCoordinates(parser);
                } else if (CircleBuilder.FIELD_RADIUS.equals(fieldName)) {
                    parser.nextToken();
                    radius = Distance.parseDistance(parser.text(), DistanceUnit.METERS);
                } else {
                    parser.nextToken();
                    parser.skipChildren();
                }
            }
        }

        if (shapeType == null) {
            throw new ElasticSearchParseException("Shape type not included");
        } else if (node == null) {
            throw new ElasticSearchParseException("Coordinates not included");
        } else if(radius != null && !CircleBuilder.TYPE.equals(shapeType)) {
            throw new ElasticSearchParseException("Field ["+CircleBuilder.FIELD_RADIUS+"] is supported for ["+CircleBuilder.TYPE+"] only");
        }
        
        if(PointBuilder.TYPE.equals(shapeType)) {
            return parsePoint(node);
        } else if(CircleBuilder.TYPE.equals(shapeType)) {
            return parseCircle(node, radius);
        } else if (EnvelopeBuilder.TYPE.equals(shapeType)) {
            return parseEnvelope(node);
        } else if(MultiPointBuilder.TYPE.equals(shapeType)) {
            return parseMultiPoint(node);
        } else if(LineStringBuilder.TYPE.equals(shapeType)) {
            return parseLineString(node);
        } else if(MultiLineStringBuilder.TYPE.equals(shapeType)) {
            return parseMultiLine(node);
        } else if(PolygonBuilder.TYPE.equals(shapeType)) {
            return parsePolygon(node);
        } else if(MultiPolygonBuilder.TYPE.equals(shapeType)) {
            return parseMultiPolygon(node);
        } else {
            throw new ElasticSearchParseException("Shape type [" + shapeType + "] not included");
        }

    }
    
    protected static PointBuilder parsePoint(CoordinateNode node) {
        return newPoint(node.coordinate);
    }
    
    protected static CircleBuilder parseCircle(CoordinateNode coordinates, Distance radius) {
        return newCircleBuilder().center(coordinates.coordinate).radius(radius);
    }
    
    protected static EnvelopeBuilder parseEnvelope(CoordinateNode coordinates) {
        return newEnvelope().topLeft(coordinates.children.get(0).coordinate).bottomRight(coordinates.children.get(1).coordinate);
    }

    protected static MultiPointBuilder parseMultiPoint(CoordinateNode coordinates) {
        MultiPointBuilder points = new MultiPointBuilder();
        for (CoordinateNode node : coordinates.children) {
            points.point(node.coordinate);
        }
        return points;
    }

    protected static LineStringBuilder parseLineString(CoordinateNode coordinates) {
        LineStringBuilder line = newLineString();
        for (CoordinateNode node : coordinates.children) {
            line.point(node.coordinate);
        }
        return line;
    }
    
    protected static MultiLineStringBuilder parseMultiLine(CoordinateNode coordinates) {
        MultiLineStringBuilder multiline = newMultiLinestring();
        for (CoordinateNode node : coordinates.children) {
            multiline.linestring(parseLineString(node));
        }
        return multiline;
    }
    
    protected static PolygonBuilder parsePolygon(CoordinateNode coordinates) {
        LineStringBuilder shell = parseLineString(coordinates.children.get(0));
        PolygonBuilder polygon = new PolygonBuilder(shell.points);
        for(int i = 1; i < coordinates.children.size(); i++) {
            polygon.hole(parseLineString(coordinates.children.get(i)));
        }
        return polygon;
    }

    protected static MultiPolygonBuilder parseMultiPolygon(CoordinateNode coordinates) {
        MultiPolygonBuilder polygons = newMultiPolygon();
        for(CoordinateNode node : coordinates.children) {
            polygons.polygon(parsePolygon(node));
        }
        return polygons;
    }
    
    protected static XContentBuilder toXContent(XContentBuilder builder, Coordinate coordinate) throws IOException {
        return builder.startArray().value(coordinate.x).value(coordinate.y).endArray();
    }

    protected static Coordinate shift(Coordinate coordinate, double dateline) {
        if(dateline == 0) {
            return coordinate;
        } else {
            return new Coordinate(-2*dateline + coordinate.x, coordinate.y);
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
    protected static final double intersection(Coordinate p1, Coordinate p2, double dateline) {
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
    protected static int intersections(double dateline, Edge[] edges) {
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
        Arrays.sort(edges, INTERSECTION_ORDER);
        return numIntersections;
    }
    
    /**
     * Node used to represent a tree of coordinates.
     * <p/>
     * Can either be a leaf node consisting of a Coordinate, or a parent with children
     */
    protected static class CoordinateNode implements ToXContent {

        protected final Coordinate coordinate;
        protected final List<CoordinateNode> children;

        /**
         * Creates a new leaf CoordinateNode
         *
         * @param coordinate Coordinate for the Node
         */
        protected CoordinateNode(Coordinate coordinate) {
            this.coordinate = coordinate;
            this.children = null;
        }

        /**
         * Creates a new parent CoordinateNode
         *
         * @param children Children of the Node
         */
        protected CoordinateNode(List<CoordinateNode> children) {
            this.children = children;
            this.coordinate = null;
        }
        
        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            if(children == null) {
                builder.startArray().value(coordinate.x).value(coordinate.y).endArray();
            } else {
                builder.startArray();
                for (int i = 0; i < children.size(); i++) {
                    children.get(i).toXContent(builder, params);
                }
                builder.endArray();
            }
            return builder;
        }
    }
    
    protected static final class Edge {
        Coordinate coordinate;       // coordinate of the start point
        Edge next;                   // next segment
        Coordinate intersection;     // potential intersection with dateline
        int component = -1;          // id of the component this edge belongs to 
        
        protected Edge(Coordinate coordinate, Edge next, Coordinate intersection) {
            this.coordinate = coordinate;
            this.next = next;
            this.intersection = intersection;
            if(next != null) {
                this.component = next.component;
            }
        }
        
        protected Edge(Coordinate coordinate, Edge next) {
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
            return intersection = position(coordinate, next.coordinate, position);
        }
        
        public static Coordinate position(Coordinate p1, Coordinate p2, double position) {
            if(position == 0) {
                return p1;
            } else if(position == 1) {
                return p2;
            } else {
                final double x = p1.x + position * (p2.x - p1.x);
                final double y = p1.y + position * (p2.y - p1.y);
                return new Coordinate(x, y);
            }
        }
     
        @Override
        public String toString() {
            return "Edge[C"+component+"; x="+coordinate.x+" "+"; "+(intersection==null?"":intersection.y)+"]";
        }
    }

    protected static final IntersectionOrder INTERSECTION_ORDER = new IntersectionOrder(); 

    private static final class IntersectionOrder implements Comparator<Edge> {
        
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
    
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_COORDINATES = "coordinates";
    
}
