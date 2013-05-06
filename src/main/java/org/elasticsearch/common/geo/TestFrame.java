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

import org.elasticsearch.common.geo.GeoPolygonBuilder.Edge;
import org.elasticsearch.common.geo.ShapeBuilder.PolygonBuilder;

import com.vividsolutions.jts.geom.Coordinate;

public class TestFrame extends JFrame implements ComponentListener, MouseListener, MouseWheelListener, MouseMotionListener {

    private double scale = 1.2;
    private Coordinate[][] points;
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
        
//        GeoPolygonBuilder polygon = new GeoPolygonBuilder();
//
//        this.points = polygon
//                .point(0, 0)
//                .point(-250, 250)
//                .point(250, 100)
//                .point(300, -50)
//                .point(-180, -100)
//                .point(230, 10)
//                .hole()
//                    .point(-200, 225)
//                    .point(-150, 215)
//                    .point(-200, 205)
//                .close()
//                .hole()
//                    .point(200, -50)
//                    .point(150, -30)
//                    .point(200, -20)
//                .close()
//                .close()
//                .coordinates();
        
        int spikes = 3;
        double innerRadius = 50;
        double outerRadius = 170;
        
        GeoPolygonBuilder polygon = new GeoPolygonBuilder();
        
        for (int i = 0; i < spikes; i++) {
            double alpha = 2*Math.PI * (1.0f*i) / (1.0f * spikes);
            double beta = 2*Math.PI * (1.0f*(i+1)) / (1.0f * spikes);
            
            double x1 = outerRadius * Math.cos(alpha);
            double y1 = outerRadius * Math.sin(alpha);
            polygon.point(x1, y1);

            double x2 = innerRadius * Math.cos(beta);
            double y2 = innerRadius * Math.sin(beta);
            polygon.point(x2, y2);
        }
        
        points = polygon.close().coordinates();
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
        
        GeoPolygonBuilder.main(args);
        
        new TestFrame();
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
        if(points == null) {
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
        
        
        if(points.length < 1)
            return;
        
        g.setColor(Color.BLUE);
        drawPolygon(g, points[0], 0, points[0].length, !decompose, 0);
        g.setColor(Color.RED);
        for (int i = 1; i < points.length; i++) {
            drawPolygon(g, points[i], 0, points[i].length, !decompose, 1);
        }
        
//        if(decompose) {
//            Coordinate[][] coord = split(dateline, points);
//            g.setColor(Color.GREEN);
//            for (int i = 0; i < coord.length; i++) {
//                drawPolygon(g, coord[i], 0, coord[i].length-1, true, 1);
//            }
//        }
        
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
