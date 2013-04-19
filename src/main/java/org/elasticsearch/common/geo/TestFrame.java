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

import com.vividsolutions.jts.geom.Coordinate;

public class TestFrame extends JFrame implements ComponentListener {

    private ArrayList<Coordinate> coordinates = new ArrayList<>();
    private double dateline = 180;
    
    public TestFrame() {
        super("polygon intersection");
        
        this.setSize(800, 600);
        this.setVisible(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        coordinates.add(new Coordinate(0, 0));
        coordinates.add(new Coordinate(-250, 250));
        coordinates.add(new Coordinate(250, 100));
        coordinates.add(new Coordinate(300, -50));
        coordinates.add(new Coordinate(100, -100));
        coordinates.add(new Coordinate(230, 10));
    }
    
    public static void main(String[] args) {
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
    
    private static int intersectingEdges(String prefix, double dateline, Edge[] edges) {
        int numIntersections = 0;
        for(int i=0; i<edges.length; i++) {
            Coordinate p0 = edges[(i+edges.length-1) % edges.length].coordinate;
            Coordinate p1 = edges[i].coordinate;
            Coordinate p2 = edges[i].next.coordinate;
            
            if(p1.x == dateline) {
                System.out.println(prefix + "On Dateline: p" + edges[i].index);
                if(Double.compare(p0.x, dateline) == Double.compare(p2.x, dateline)) {
                }
                continue;
            }

            double intersection = intersection(p1, p2, dateline);
            if(!Double.isNaN(intersection)) {
                
                if(intersection == 1) {
                    System.out.println(prefix + "Endpoint intersection " + edges[i]);
                    if()
                    if(Double.compare(p1.x, dateline) == Double.compare(edges[i].next.next.coordinate.x, dateline)) {
                        // Ignore the ear
                        System.out.println(prefix + "\tEar: e" + edges[i].index);
                        continue;
                    }
                    if(p1.x == dateline) {
                        System.out.println(prefix + "\tDateline: e" + edges[i].index);
                        continue;
                    }
                }
                edges[i].pointAt(intersection);
                System.out.println(prefix + "Add intersection: " + edges[i]);
                numIntersections++;
            }
        }
        Arrays.sort(edges, IntersectionOrder.INSTANCE);
        
        System.out.println(prefix + "Intersections: " + Arrays.toString(edges));
        
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
                System.out.print(prefix + "Component "+component.coordinate.x+":");
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
    
    private static Coordinate[][] decompose(double left, double right, Coordinate...points) {
        System.out.println("==== ==== ==== ==== ==== ==== ==== ==== ====");
        Edge[] edges = Edge.edges(points);
        Edge[] candidates = insertIntersections(edges, intersectingEdges("", right, edges));
        Coordinate[][] components = compose("", candidates, 0, right);
        System.out.println("---- ---- ---- ---- ---- ---- ---- ---- ----");

        ArrayList<Coordinate[]> polygons = new ArrayList<>();
        for(Coordinate[] component : components) {
            Edge[] subedges = Edge.edges(component);
            System.out.println("Polygon: " + Arrays.toString(subedges));
            Edge[] subcandidates = insertIntersections(subedges, intersectingEdges("\t", left, subedges));
            System.out.println("\tParts:" +Arrays.toString(subcandidates));
            for(Coordinate[] subcomponent : compose("\t", subcandidates, left, 0)) {
                polygons.add(subcomponent);
            }
        }
        
        return polygons.toArray(new Coordinate[polygons.size()][]);
    }
    
    public static Coordinate[][] split(double dateline, Coordinate...points) {
        return decompose(-dateline, dateline, points);
    }
    
    public static Coordinate[][] __split(double dateline, double s, Coordinate...points) {
        Edge[] edges = Edge.edges(points);
        TreeSet<Edge> intersections = new TreeSet<Edge>();
        
        for(int i=0; i<edges.length; i++) {
            Coordinate p1 = edges[i].coordinate;
            Coordinate p2 = edges[i].next.coordinate;
            
            double intersection = intersection(p1, p2, dateline);
            if(!Double.isNaN(intersection)) {
                System.out.println("Intersection p"+ edges[i].index + " and p" + edges[i].next.index + " " + intersection);
                edges[i].pointAt(intersection);
                intersections.add(edges[i]);
            }
        }

        ArrayList<Edge> components = new ArrayList<>();
        
        int j = edges.length;
        while(!intersections.isEmpty()) {
            System.out.println("Intersections: " + intersections.size());
            Edge in = intersections.pollFirst();
            Edge out = intersections.pollFirst();
            
            Edge e1 = new Edge(j++, in.intersection, in.next);
            Edge e2 = new Edge(j++, out.intersection, out.next);
            out.next = new Edge(j++, out.intersection, e1);
            in.next = new Edge(j++, in.intersection, e2);
            
            components.add(in);
            components.add(out);
        }

        int cid = 0;
        ArrayList<Coordinate[]> polygons = new ArrayList<Coordinate[]>();
        for (Edge component : components) {
            if(component.component >= 0) {
                continue;
            } else {
                double shift = component.coordinate.x > dateline ? s : 0;
                ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
                component.component = cid++;
                System.out.print("Component "+component.coordinate.x+":");
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

    private static class Edge {
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
        
        public static Edge[] edges(Coordinate...points) {
            Edge[] edges = new Edge[points.length];
            edges[0] = new Edge(0, points[0], null);
            for (int i = 1; i < points.length; i++) {
                edges[i-1].next = edges[i] = new Edge(i, points[i], null); 
            }
            edges[points.length - 1].next = edges[0];
            return edges;
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
            return "e"+index + " " + intersection;
        }
        
    }
    
    public static Coordinate shift(Coordinate coordinate, double dateline) {
        if(dateline == 0) {
            return coordinate;
        } else {
            return new Coordinate(-2*dateline + coordinate.x, coordinate.y);
        }
    }
    
    public void drawPolygon(Graphics g, Coordinate...points) {
        drawPolygon(g, points, 0, points.length);
    }
    
    public void drawPolygon(Graphics g, Coordinate[] points, int offset, int length) {
        Color color = g.getColor();
        for (int i = 0; i < length; i++) {
            Coordinate p0 = points[(offset+((i+0) % length)) % points.length];
            Coordinate p1 = points[(offset+((i+1) % length)) % points.length];
            
            int[] p = convert(p0);
            int[] q = convert(p1);

            g.drawLine(p[0], p[1], q[0], q[1]);
            g.fillOval(p[0]-4, p[1]-4, 8, 8);
            g.setColor(Color.BLACK);
//            g.drawString("p"+i + " = ("+p0.x+", "+p0.y+")", p[0], p[1]);
            g.setColor(color);
        }        
    }
    
    @Override
    public void paint(Graphics g) {

        Coordinate[] points = new Coordinate[coordinates.size()];
        coordinates.toArray(points);
        
        g.clearRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.RED);
        int[] ds1 = convert(new Coordinate(dateline, -getHeight()/2));
        int[] de1 = convert(new Coordinate(dateline, +getHeight()/2));
        g.drawLine(ds1[0], ds1[1], de1[0], de1[1]);
        
        int[] ds2 = convert(new Coordinate(-dateline, -getHeight()/2));
        int[] de2 = convert(new Coordinate(-dateline, +getHeight()/2));
        g.drawLine(ds2[0], ds2[1], de2[0], de2[1]);
        
        g.setColor(Color.BLUE);
        
        drawPolygon(g, points, 0, points.length);
        
        g.setColor(Color.GREEN);
        
        Coordinate[][] coord = split(dateline, points);
        for(Coordinate[] poly : coord) {
            drawPolygon(g, poly);
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
