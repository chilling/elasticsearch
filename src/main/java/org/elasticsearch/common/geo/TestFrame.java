package org.elasticsearch.common.geo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JFrame;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.geo.ShapeBuilder.PolygonBuilder;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.vividsolutions.jts.geom.Coordinate;

public class TestFrame extends JFrame implements ComponentListener {

    private PolygonBuilder polygon;
    private double dateline = 180;
    
    public TestFrame() {
        super("polygon intersection");
        
        this.setSize(800, 600);
        this.setVisible(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        
        polygon = ShapeBuilder.newPolygon()
                .point(0, 0)
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
    }
    
    public static void main(String[] args) {
        
//        Node node = NodeBuilder.nodeBuilder().node();
//        Client client = node.client();
//                
//        String mapping = JsonXContent.contentBuilder()
//                .startObject()
//                .startObject("properties")
//                .field("type", "geo_shape")
//                .endObject()
//                .endObject()
//                .string();
//            
//        client.admin().indices().prepareCreate("geo").addMapping("polygon", mapping).execute().actionGet();
//        
//        client.prepareIndex("geo", "polygons").setCreate(true).setSource(
//                ).;
        
        new TestFrame();
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
    
    private static double distance(Coordinate point, Edge edge) {
        Coordinate p1 = edge.coordinate;
        Coordinate p2 = edge.next.coordinate;
        
        final double dx = p2.x - p1.x; 
        final double dy = p2.y - p1.y;
        final double s = dx*dx + dy*dy;
        final double t = ((point.x - p1.x) * dx + (point.y * p1.y) * dy) / s;
        final double u = Math.max(Math.min(t, 1), 0);
        final double x = (p1.x + u * dx) - point.x; 
        final double y = (p1.y + u * dy) - point.y; 
        
        return x*x + y*y;
    }
    
    private static Edge closest(Coordinate point, Edge component) {
        Edge closest = component;
        double distance = distance(point, closest);
        
        Edge current = component;
        while((current = current.next) != component) {
            double dist = distance(point, current);
            if(dist < distance) {
                closest = current;
                distance = dist;
            }
        }

        return closest;
    }
    
    private static int intersectingEdges(String prefix, double dateline, Edge[] edges) {
        int numIntersections = 0;
        for(int i=0; i<edges.length; i++) {
            Coordinate p1 = edges[i].coordinate;
            Coordinate p2 = edges[i].next.coordinate;
            
            double intersection = intersection(p1, p2, dateline);
            if(!Double.isNaN(intersection)) {
                
                if(intersection == 1) {
                    System.out.print(prefix + "Endpoint intersection " + edges[i]+": ");
                    if(Double.compare(p1.x, dateline) == Double.compare(edges[i].next.next.coordinate.x, dateline)) {
                        // Ignore the ear
                        System.out.println("Ear (ignored)");
                        continue;
                    }else if(p2.x == dateline) {
                        // Ignore Linesegments on dateline 
                        System.out.println("Dateline (ignored)");
                        continue;
                    } else {
                        System.out.println("taken");
                    }
                }
                edges[i].pointAt(intersection);
                System.out.println(prefix + "Add intersection: " + edges[i]);
                numIntersections++;
            }
        }
        Arrays.sort(edges, IntersectionOrder.INSTANCE);
        
        return numIntersections;
    }
    
    private static Edge[] insertIntersections(Edge[] edges, int numIntersections) {
        if(numIntersections < 1) {
            return Arrays.copyOf(edges, 1);
        } else {
            int j = -1;
            Edge[] candidates = new Edge[numIntersections];
            for(int i=0; i<numIntersections; i++) {
                Edge in = edges[i];
                candidates[i] = in;

                Edge out = edges[++i];
                candidates[i] = out;

                if(in.intersection != in.next.coordinate) {
                    Edge e1 = new Edge(j--, in.intersection, in.next);
                    
                    if(out.intersection != out.next.coordinate) {
                        Edge e2 = new Edge(j--, out.intersection, out.next);
                        in.next = new Edge(j--, in.intersection, e2);
                    } else {
                        in.next = new Edge(j--, in.intersection, out.next);
                    }
                    out.next = new Edge(j--, out.intersection, e1);
                } else {
                    Edge e2 = new Edge(j--, out.intersection, in.next);

                    if(out.intersection != out.next.coordinate) {
                        Edge e1 = new Edge(j--, out.intersection, out.next);
                        in.next = new Edge(j--, in.intersection, e1);
                        
                    } else {
                        in.next = new Edge(j--, in.intersection, out.next);
                    }
                    out.next = e2;
                }
            }

//            System.out.println(Arrays.toString(candidates));
            return candidates;
        }
    }
    
    private static Coordinate[][] compose(String prefix, Edge[] candidates, double left, double right) {
        int cid = 0;
        ArrayList<Coordinate[]> polygons = new ArrayList<Coordinate[]>();
        for (Edge component : candidates) {
            if(component.component >= 0) {
                continue;
            } else {
                double shift = component.coordinate.x > right ? right : (component.coordinate.x < left ? left : 0);
                ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
                component.component = cid++;
                System.out.print(prefix + "sub-component "+component.coordinate.x+":");
                Edge current = component;
                do {
                    coordinates.add(shift(current.coordinate, shift));
                    current.component = component.component;
                    System.out.print(" p" + current.index);
                    current = current.next;
                } while(current != component);
                
                polygons.add(coordinates.toArray(new Coordinate[coordinates.size()]));
                
                System.out.println();
            }
        }

        return polygons.toArray(new Coordinate[polygons.size()][]); 
    }
    
    private static Edge split(Edge edge, Coordinate point) {
        return edge.next = new Edge(-200, point, edge.next);
    }
    
    private static void merge(double dateline, Edge[] hole, int numIntersections, Edge[] edges) {
        int holeIntersections = intersectingEdges("", dateline, hole);
        System.out.println("\t\tHole: "+Arrays.toString(hole));

        if(holeIntersections == 0) {
            Edge component = closest(hole[0].coordinate, edges[0]);
            System.out.println("\t\t\tComponent of " + component);
        } else {
            for (int j = 0; j < holeIntersections; j++) {
                int segment = Arrays.binarySearch(edges, 0, numIntersections, hole[j], IntersectionOrder.INSTANCE);
                if (segment<0) {
                    segment = -(segment+1);
//                    Edge e1 = split(hole[j], hole[j].intersection);
                    Edge e2 = split(edges[segment], edges[segment].intersection);

                    j++;
//                    Edge e3 = split(hole[j], hole[j].intersection);
                    Edge e4 = split(edges[segment-1], edges[segment-1].intersection);

//                    e3.next = e4;
//                    edges[segment].next = e1;
                    
                    System.out.println("\t\t\tSegment ("+hole[j].intersection+"): " + edges[segment] + " " + edges[segment].next);
                    
                } else {
                    System.out.println("\t\t\tDirect hit: " + edges[j]);
                }
            }
        } 
    }
    
    private static Coordinate[][] decompose(double left, double right, Coordinate[]...rings) {
        System.out.println("==== ==== ==== ==== ==== ==== ==== ==== ====");
        Edge[] edges = Edge.ring(rings[0]);
        
        int numIntersections = intersectingEdges("", right, edges);
        
        for (int i = 1; i < rings.length; i++) {
            merge(right, Edge.ring(rings[i]), numIntersections, edges);
        }

//      Edge[] candidates = insertIntersections(edges, numIntersections);
        Edge[] candidates = edges;


        
        Coordinate[][] components = compose("", candidates, 0, right);
        System.out.println("---- ---- ---- ---- ---- ---- ---- ---- ----");

        ArrayList<Coordinate[]> polygons = new ArrayList<Coordinate[]>();
        for(Coordinate[] component : components) {
            Edge[] subedges = Edge.ring(component);
            System.out.println("Component: " + Arrays.toString(subedges));
            Edge[] subcandidates = insertIntersections(subedges, intersectingEdges("\t", left, subedges));
            System.out.println("\tParts:" +Arrays.toString(subcandidates));
            for(Coordinate[] subcomponent : compose("\t", subcandidates, left, 0)) {
                polygons.add(subcomponent);
            }
        }
        
        return polygons.toArray(new Coordinate[polygons.size()][]);
    }
    
    public static Coordinate[][] split(double dateline, Coordinate[][] points) {
        return decompose(-dateline, dateline, points);
    }

    private static final class Edge {
        final Coordinate coordinate;
        final int index;
        Edge next;
        Coordinate intersection;
        int component = -1;
        
        public Edge(int index, Coordinate coordinate, Edge next) {
            super();
            this.coordinate = coordinate;
            this.next = next;
            this.index = index;
        }
        
        private static final int top(Coordinate...points) {
            int top = 0;
            for (int i = 1; i < points.length; i++) {
                if(points[i].y < points[top].y) {
                    top = i;
                }
            }
            return top;
        }
        
        private static Edge[] ring(boolean direction, Coordinate...points) {
            Edge[] edges = new Edge[points.length];
            edges[0] = new Edge(0, points[0], null);
            for (int i = 1; i < points.length; i++) {
                if(direction) {
                    edges[i] = new Edge(i, points[i], edges[i-1]); 
                } else {
                    edges[i-1].next = edges[i] = new Edge(i, points[i], null); 
                }
            }
            
            if(direction) {
                edges[0].next = edges[points.length - 1];
            } else {
                edges[points.length - 1].next = edges[0];
            }
            return edges;
        }
        
        public static Edge[] ring(Coordinate...points) {
            final int top = top(points);
            final int prev = (top + points.length - 1) % points.length;
            final int next = (top + 1) % points.length;
            return ring(points[prev].x > points[next].x, points);
        }

        public Coordinate pointAt(double position) {
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
            return "e"+index + (intersection==null?"":(" (" + intersection.y+")"));
        }
        
    }
    
    public static Coordinate shift(Coordinate coordinate, double dateline) {
        if(dateline == 0) {
            return coordinate;
        } else {
            return new Coordinate(-2*dateline + coordinate.x, coordinate.y);
        }
    }
    
    public void drawPolygon(Graphics g, boolean info, Coordinate...points) {
        drawPolygon(g, points, 0, points.length, info);
    }
    
    public void drawPolygon(Graphics g, Coordinate[] points, int offset, int length, boolean info) {
        Color color = g.getColor();
        for (int i = 0; i < length; i++) {
            Coordinate p0 = points[(offset+((i+0) % length)) % points.length];
            Coordinate p1 = points[(offset+((i+1) % length)) % points.length];
            
            int[] p = convert(p0);
            int[] q = convert(p1);

            g.drawLine(p[0], p[1], q[0], q[1]);
            g.fillOval(p[0]-4, p[1]-4, 8, 8);
            if(info) {
                g.setColor(Color.WHITE);
                g.drawString("p"+(offset+i) + " = ("+p0.x+", "+p0.y+")", p[0]+4, p[1]+4);
                g.setColor(color);
            }
        }        
    }
    
    @Override
    public void paint(Graphics g) {
        if(polygon == null) {
            repaint();
            return;
        }
        
        g.setFont(g.getFont().deriveFont(10f));
        
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.RED);
        int[] ds1 = convert(new Coordinate(dateline, -getHeight()/2));
        int[] de1 = convert(new Coordinate(dateline, +getHeight()/2));
        g.drawLine(ds1[0], ds1[1], de1[0], de1[1]);
        
        int[] ds2 = convert(new Coordinate(-dateline, -getHeight()/2));
        int[] de2 = convert(new Coordinate(-dateline, +getHeight()/2));
        g.drawLine(ds2[0], ds2[1], de2[0], de2[1]);
        
        
        Coordinate[][] points = polygon.coordinates();
        g.setColor(Color.BLUE);
        drawPolygon(g, points[0], 0, points[0].length, false);
        g.setColor(Color.RED);
        for (int i = 1; i < points.length; i++) {
            drawPolygon(g, points[i], 0, points[i].length, true);
        }
        
        
        g.setColor(Color.GREEN);
        
        Coordinate[][] coord = split(dateline, points);
        for(Coordinate[] poly : coord) {
            drawPolygon(g, true, poly);
        }
        
    }
    
    private int[] convert(Coordinate coordinate) {
        int cx = getWidth()/2;
        int cy = getHeight()/2;
        return new int[] {
                Math.round(Math.round(cx+coordinate.x)),
                Math.round(Math.round(cy+coordinate.y)),
        };
    }

    @Override public void componentHidden(ComponentEvent e) {}
    @Override public void componentMoved(ComponentEvent e) {}

    @Override
    public void componentResized(ComponentEvent e) {
        System.out.println(e);
        this.repaint();
    }
    
    @Override
    public void componentShown(ComponentEvent e) {
        System.out.println(e);
        this.repaint();
    }
}
