package org.elasticsearch.common.geo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.JFrame;

import org.elasticsearch.common.geo.ShapeBuilder.PolygonBuilder;

import com.vividsolutions.jts.geom.Coordinate;

public class TestFrame extends JFrame implements ComponentListener, MouseListener, MouseWheelListener, MouseMotionListener {

    private double scale = 1.2;
    private PolygonBuilder polygon;
    private double dateline = 180;
    private double[] center = new double[2]; 
    
    public TestFrame() {
        super("polygon intersection");
        
        this.setSize(800, 600);
        this.setVisible(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        this.addMouseListener(this);
        this.addMouseWheelListener(this);
        this.addMouseMotionListener(this);

        this.center[0] = getWidth() / 2;
        this.center[1] = getHeight() / 2;
        
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
            for(int i=0; i<numIntersections; i+=2) {
                Edge in = edges[i+0];
                Edge out = edges[i+1];

                candidates[i+0] = in;
                candidates[i+1] = out;

                if(in.intersection != in.next.coordinate) {
                    Edge e1 = new Edge(j--, in.intersection, in.next);
                    
                    if(out.intersection != out.next.coordinate) {
                        Edge e2 = new Edge(j--, out.intersection, out.next);
                        candidates[i+0] = in.next = new Edge(j--, in.intersection, e2, in.intersection);
                    } else {
                        candidates[i+0] = in.next = new Edge(j--, in.intersection, out.next, in.intersection);
                    }
                    candidates[i+1] = out.next = new Edge(j--, out.intersection, e1, out.intersection);
                } else {
                    Edge e2 = candidates[i+0] = new Edge(j--, out.intersection, in.next, out.intersection);

                    if(out.intersection != out.next.coordinate) {
                        Edge e1 = new Edge(j--, out.intersection, out.next);
                        candidates[i+1] = in.next = new Edge(j--, in.intersection, e1, in.intersection);
                        
                    } else {
                        candidates[i+1] = in.next = new Edge(j--, in.intersection, out.next, in.intersection);
                    }
                    out.next = e2;
                }
            }

            System.out.println("DATELINE: " + Arrays.toString(candidates));
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

                
                while (component.coordinate.x == left || component.coordinate.x == right) {
                    component = component.next;
                    System.out.println("=========== shiftx = "+component.coordinate.x+" ============== ("+left+ ":"+right+")");
                }
                
                double shift = component.coordinate.x > right ? right : (component.coordinate.x < left ? left : 0);
                System.out.println("=========== shift = "+shift+" ("+component.coordinate.x+")"+" ============== ("+left+ ":"+right+")");
                
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

                coordinates.add(shift(current.coordinate, shift));

                polygons.add(coordinates.toArray(new Coordinate[coordinates.size()]));
                
                System.out.println();
            }
        }

        return polygons.toArray(new Coordinate[polygons.size()][]); 
    }
    
    private static Edge split(Edge edge, Coordinate point) {
        return edge.next = new Edge(-200, point, edge.next);
    }
    
    private static Edge[] merge(double dateline, Edge[] hole, Edge[] segments) {
        int holeIntersections = intersectingEdges("", dateline, hole);
        System.out.println("\t\tCMP:  "+Arrays.toString(segments));
        System.out.println("\t\tHole: "+Arrays.toString(hole));

        if(holeIntersections == 0) {
//            Edge component = closest(hole[0].coordinate, edges[0]);
//            System.out.println("\t\t\tComponent of " + component);
        } else {
            for (int j = 0; j < holeIntersections; j+=2) {
                int segment = Arrays.binarySearch(segments, hole[j], IntersectionOrder.INSTANCE);
                if (segment<0) {
                    segment = 2+segment;
                    System.out.println("\t\t\tFound: " + segments[segment].coordinate + " - " + segments[segment].next.coordinate +" at "  + segment);
                    
                    int l = segment | 1;
                    Edge[] newSegments = new Edge[segments.length+2];
                    System.arraycopy(segments, 0, newSegments, 0, l);
                    System.arraycopy(segments, l, newSegments, l+2, segments.length-l);

                    Edge e1 = newSegments[l+0] = new Edge(-300, hole[j+0].intersection, segments[segment+1].next, hole[j+0].intersection);
                    Edge e2 = newSegments[l+1] = new Edge(-300, hole[j+1].intersection, segments[segment+0].next, hole[j+1].intersection);
                    Edge e3 = split(hole[j+1], hole[j+1].intersection);
                    Edge e4 = split(hole[j+0], hole[j+0].intersection);
                    
                    hole[j+0].next = e1;
                    hole[j+1].next = e2;
                    
                    segments[segment+0].next = e4; 
                    segments[segment+1].next = e3; 

                    segments = newSegments;
                    
                    System.out.println("\t\t\tSegment ("+segment+"): " + segments[segment]);
                } else {
                    System.out.println("\t\t\tDirect hit: " + segments[j]);
                }
            }
        }
        return segments;
    }
    
    private static Coordinate[][] decompose(double left, double right, Coordinate[]...rings) {
        System.out.println("==== ==== ==== ==== ==== ==== ==== ==== ====");
        Edge[] edges = Edge.ring(true, rings[0]);
        
        int numIntersections = intersectingEdges("", right, edges);
        Edge[] dateline = insertIntersections(edges, numIntersections);
        
        for (int i = 1; i < rings.length; i++) {
            dateline = merge(right, Edge.ring(true, rings[i]), dateline);
        }


        Coordinate[][] components = compose("", dateline, 0, right);
        System.out.println("---- ---- ---- ---- ---- ---- ---- ---- ----");

        
        ArrayList<Coordinate[]> polygons = new ArrayList<Coordinate[]>();
        for(Coordinate[] component : components) {
            Edge[] subedges = Edge.ring(true, component);
            System.out.println("Component: " + Arrays.toString(subedges));
            int subnum = intersectingEdges("\t", left, subedges);
            Edge[] subcandidates = insertIntersections(subedges, subnum);

            if(subnum>1) {
                for (int i = 1; i < rings.length; i++) {
                    subcandidates = merge(left, Edge.ring(true, rings[i]), subcandidates);
                }
            }

            System.out.println("\tParts:" +Arrays.toString(subcandidates));
            for(Coordinate[] subcomponent : compose("\t", subcandidates, left, right)) {
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
        
        public Edge(int index, Coordinate coordinate, Edge next, Coordinate intersection) {
            this.coordinate = coordinate;
            this.next = next;
            this.index = index;
            this.intersection = intersection;
        }
        
        public Edge(int index, Coordinate coordinate, Edge next) {
            this(index, coordinate, next, null);
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
        
        private static Edge[] concat(boolean direction, Coordinate...points) {
            Edge[] edges = new Edge[points.length-1];
            edges[0] = new Edge(0, points[0], null);
            for (int i = 1; i < edges.length; i++) {
                if(direction) {
                    edges[i] = new Edge(i, points[i], edges[i-1]); 
                } else {
                    edges[i-1].next = edges[i] = new Edge(i, points[i], null); 
                }
            }
            
            if(direction) {
                edges[0].next = edges[edges.length - 1];
            } else {
                edges[edges.length - 1].next = edges[0];
            }
            return edges;
        }
        
        public static Edge[] ring(boolean direction, Coordinate...points) {
            final int top = top(points);
            final int prev = (top + points.length - 1) % (points.length-1);
            final int next = (top + 1) % (points.length-1);
            return concat(direction ^ (points[prev].x > points[next].x), points);
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
    
    public void drawPolygon(Graphics g, boolean info, int shift, Coordinate...points) {
        drawPolygon(g, points, 0, points.length, info, shift);
    }
    
    public void drawPolygon(Graphics g, Coordinate[] points, int offset, int length, boolean info, int shift) {
        Color color = g.getColor();
        for (int i = 0; i < length; i++) {
            Coordinate p0 = points[(offset+((i+0) % length)) % points.length];
            Coordinate p1 = points[(offset+((i+1) % length)) % points.length];
            
            int[] p = convert(p0);
            int[] q = convert(p1);

            g.drawLine(p[0]+shift, p[1]+shift, q[0]+shift, q[1]+shift);
            g.fillOval(p[0]-4+shift, p[1]-4+shift, 8, 8);
            if(info) {
                String text = "p"+(offset+i) + " = ("+p0.x+", "+p0.y+")";
                
                int w = g.getFontMetrics().stringWidth(text);
                int h = g.getFontMetrics().getHeight();
                
//                g.setColor(Color.BLACK);
//                g.fillRect(p[0]+4, p[1]+4-h, w, h);
                
                g.setColor(Color.WHITE);
                g.drawString(text, p[0]+4+shift, p[1]+2+shift);
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
        drawPolygon(g, points[0], 0, points[0].length-1, !decompose, 0);
        g.setColor(Color.RED);
        for (int i = 1; i < points.length; i++) {
            drawPolygon(g, points[i], 0, points[i].length-1, !decompose, 1);
        }
        
        if(decompose) {
            Coordinate[][] coord = split(dateline, points);
            g.setColor(Color.GREEN);
            for (int i = 0; i < coord.length; i++) {
                drawPolygon(g, coord[i], 0, coord[i].length-1, true, 1);
            }
        }
        
    }
    
    private int[] convert(Coordinate coordinate) {
        int cx = getWidth()/2;
        int cy = getHeight()/2;
        return new int[] {
                Math.round(Math.round(scale*(center[0]+coordinate.x))),
                Math.round(Math.round(scale*(center[1]+coordinate.y))),
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

    private boolean decompose;
    
    @Override
    public void mouseClicked(MouseEvent e) {
        decompose = !decompose;
        this.repaint();
    }

    private int[] pos = null; 
    
    @Override
    public void mousePressed(MouseEvent e) {
        pos = new int[] {e.getX(), e.getY()};
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(pos != null) {
            int dx = e.getX() - pos[0];
            int dy = e.getY() - pos[1];
            
            this.center[0] += dx/scale;
            this.center[1] += dy/scale;
            pos = null;
            
            this.repaint();
        }
        
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        scale = scale + 0.01 * e.getPreciseWheelRotation();
        this.repaint();
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
    }
    
}
