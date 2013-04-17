package org.elasticsearch.common.geo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
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
    
    class Intersection {
        String string;
        Coordinate coordinate;
        public Intersection(String string, Coordinate coordinate) {
            super();
            this.string = string;
            this.coordinate = coordinate;
        }
        
    }
    
    private static Coordinate datelineIntersection(Coordinate p1, Coordinate p2, double dateline) {
        if(p1.x == p2.x) {
            return null;
        } else {
            final double t = (dateline - p1.x) / (p2.x - p1.x);

            if(t > 1 || t <= 0) {
                return null;
            } else {
                final double x = p1.x + t * (p2.x - p1.x); 
                final double y = p1.y + t * (p2.y - p1.y);
                return new Coordinate(x, y);
            }
        }
    }
    
    private static int topIndex(Coordinate...coordinates) {
        int result = 0;
        for (int i = 1; i < coordinates.length; i++) {
            if(coordinates[i].y < coordinates[result].y) {
                result = i;
            }
        }
        return result;
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
    
    public static Coordinate[][] split(double dateline, Coordinate...points) {
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
        Coordinate[] full = new Coordinate[edges.length + 2*intersections.size()];
        while(!intersections.isEmpty()) {
            System.out.println("Intersections: " + intersections.size());
            Edge in = intersections.pollFirst();
            Edge out = intersections.pollFirst();
            
            Edge e1 = new Edge(j++, in.intersection, in.next);
            Edge e2 = new Edge(j++, out.intersection, out.next);
            out.next = new Edge(j++, out.intersection, e1);
            in.next = new Edge(j++, in.intersection, e2);
            
            components.add(in.next);
            components.add(out.next);
            
            System.out.println("Out: " + out);
            
        }
        
        for (Edge component : components) {
            System.out.print("Component: p" + component.index);
            Edge current = component.next;
            while(current != component) {
                System.out.print(" p" + current.index);
                current = current.next;
            }
            System.out.println();
        }
        
        Edge current = edges[0];
        for (int i = 0; i < full.length; i++) {
            full[i] = (current = current.next).coordinate;
        }
        
        return new Coordinate[][] {full};
    }

    private static class Edge implements Comparable<Edge> {
        final Coordinate coordinate;
        final int index;
        Edge next;
        Coordinate intersection;
        
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
            final double x = coordinate.x + position * (next.coordinate.x - coordinate.x);
            final double y = coordinate.y + position * (next.coordinate.y - coordinate.y);
            return intersection = new Coordinate(x, y);
        }
        
        @Override
        public int compareTo(Edge o) {
            return Double.compare(intersection.y, o.intersection.y);
        }

    }
    
    public static Coordinate[][] _split(double dateline, Coordinate...points) {
        final int top = topIndex(points);
        final double shift = points[top].x;
        
        final double left = -dateline;
        final double right = +dateline;
        
        ArrayList<List<Coordinate>> parts = new ArrayList<List<Coordinate>>();
        List<Coordinate> current = new ArrayList<Coordinate>();
        
        parts.add(current);
        current.add(points[top]);
        
        boolean negative = false;
        boolean positive = false;
        
        for (int i = 1; i <= points.length; i++) {
            Coordinate p = points[(top + i - 1) % points.length];
            Coordinate c = points[(top + i + 0) % points.length];
            
            Coordinate s0 = datelineIntersection(p, c, right);
            Coordinate s1 = datelineIntersection(p, c, left);
            
            if(s0 == null && s1 == null) {
                if(negative) {
                    current.add(shift(c, left));
                } else if(positive) {
                    current.add(shift(c, right));
                } else {
                    current.add(c);
                }
            } else if (s1 == null) {
                if(!positive) {
                    current.add(s0);
                    current = new ArrayList<Coordinate>();
                    current.add(shift(s0, right));
                    current.add(shift(c, right));
                    parts.add(current);
                    positive = true;
                } else {
                    current.add(shift(s0, right));
                    current = parts.remove(parts.size()-2);
                    current.add(s0);
                    current.add(c);
                    parts.add(current);
                    positive = false;
                }
            } else if (s0 == null) {
                if(!negative) {
                    current.add(s1);
                    current = new ArrayList<Coordinate>();
                    current.add(shift(s1, left));
                    current.add(shift(c, left));
                    parts.add(current);
                    negative = true;
                } else {
                    current.add(shift(s1, left));
                    current = parts.remove(parts.size()-2);
                    current.add(s1);
                    current.add(c);
                    parts.add(current);
                    negative = false;
                }
            } else {
                if(negative) {
                    current.add(shift(s1, left));
                    current = parts.remove(parts.size()-2);
                    current.add(s1);
                    current.add(s0);
                    parts.add(current);
                    current = new ArrayList<Coordinate>();
                    current.add(shift(s0, right));
                    current.add(shift(c, right));
                    parts.add(current);
                    negative = false;
                    positive = true;
                } else if(positive) {
                    current.add(shift(s0, right));
                    current = parts.remove(parts.size()-2);
                    current.add(s0);
                    current.add(s1);
                    parts.add(current);
                    current = new ArrayList<Coordinate>();
                    current.add(shift(s1, left));
                    current.add(shift(c, left));
                    parts.add(current);
                    negative = true;
                    positive = false;
                }
            }
        }
        
        
        System.out.println("Parts: " + parts.size());
        
        Coordinate[][] result = new Coordinate[parts.size()][];
        for (int i = 0; i < result.length; i++) {
            result[i] = parts.get(i).toArray(new Coordinate[parts.get(i).size()]);
            System.out.println("\t" + i + ": " + Arrays.toString(result[i]));
        }
        
        
        return result;
    }
    
    public static Coordinate shift(Coordinate coordinate, double dateline) {
        return new Coordinate(-2*dateline + coordinate.x, coordinate.y);
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
