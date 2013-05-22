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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFrame;

import org.elasticsearch.common.geo.builders.GeoShapeBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.ToXContent.Params;

import com.spatial4j.core.shape.Shape;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class TestFrame extends JFrame implements ComponentListener, MouseListener, MouseWheelListener, MouseMotionListener {

    private int currentPolygon = 0;
    
    private double scale = 1.50;
    private final GeoShapeBuilder polygons[];
    private double dateline = 180;
    private double[] center = new double[2];
    
    private final Collection<Geometry> geometires = new ArrayList<Geometry>();
    
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
        
        polygons = new GeoShapeBuilder[3];
        
        polygons[0] = GeoShapeBuilder.newPolygon()
                .point(0, -200)
                .point(250, -200)
                .point(250, 200)
                .point(0, 200)
                    .hole()
                        .point(100, -150)
                        .point(200, -150)
                        .point(200, -100)
                        .point(100, -100)
                    .close()
                    .hole()
                        .point(100, +100)
                        .point(200, +100)
                        .point(200, +150)
                        .point(100, +150)
                    .close()
                    .hole()
                        .point(100, -50)
                        .point(200, -50)
                        .point(200, +50)
                        .point(100, +50)
                    .close()
                    .hole()
                        .point(10, 10)
                        .point(20, 10)
                        .point(20, 20)
                        .point(10, 20)
                    .close()
                    .hole()
                        .point(210, 10)
                        .point(220, 10)
                        .point(220, 20)
                        .point(210, 20)
                    .close()
                    .hole()
                        .point(150, 160)
                        .point(160, 160)
                        .point(160, 170)
                        .point(150, 170)
                    .close()
                .close();

        polygons[1] = GeoShapeBuilder.newPolygon()
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

        polygons[2] = GeoShapeBuilder.newPolygon()
                .point(0, -100)
                .point(250, -100)
                .point(250, 200)
                .point(0, 200)
                .point(0, 150)
                .point(190, 150)
                .point(190, -50)
                .point(0, -50)
            .close();
    }
    
    public static void main(String[] args) {
        new TestFrame();
    }

    public Collection<Geometry> geomeries() {
        return geometires;
    }
    
    public void drawGeometry(Graphics g, Geometry geometry, boolean info) {
        if(geometry instanceof GeometryCollection) {
            GeometryCollection collection = (GeometryCollection) geometry;
            int numGeometry = collection.getNumGeometries();
            for (int n = 0; n < numGeometry; n++) {
                drawGeometry(g, collection.getGeometryN(n), info);
            }
        } else if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry;
            g.setColor(Color.BLUE);
            drawGeometry(g, polygon.getExteriorRing(), info);
            g.setColor(Color.RED);
            int numRings = polygon.getNumInteriorRing();
            for (int i = 0; i < numRings; i++) {
                drawGeometry(g, polygon.getInteriorRingN(i), info);
            }
        } else if (geometry instanceof LineString) {
            LineString linestring = (LineString) geometry;
            drawPolygon(g, info, 0, linestring.getCoordinates());
        }
        
    }
    
    public void drawPolygon(Graphics g, boolean info, int shift, Coordinate...points) {
        drawPolygon(g, points, 0, points.length, info, shift);
    }
    
    public void drawPolygon(Graphics g, Coordinate[] points, int offset, int length, boolean info, int shift) {
        Color color = g.getColor();
        int s = 2;
        for (int i = 0; i < length; i++) {
            Coordinate p0 = points[(offset+((i+0) % length)) % points.length];
            Coordinate p1 = points[(offset+((i+1) % length)) % points.length];
            
            int[] p = convert(p0);
            int[] q = convert(p1);

            g.drawLine(p[0]+shift, p[1]+shift, q[0]+shift, q[1]+shift);
            g.fillOval(p[0]-s+shift, p[1]-s+shift, 2*s, 2*s);
            if(info) {
                String text = "p"+(offset+i) + " = ("+p0.x+", "+p0.y+")";
                
//                int w = g.getFontMetrics().stringWidth(text);
//                int h = g.getFontMetrics().getHeight();
                
//                g.setColor(Color.BLACK);
//                g.fillRect(p[0]+4, p[1]+4-h, w, h);
                
                g.setColor(Color.WHITE);
                g.drawString(text, p[0]+s+shift, p[1]+s+shift);
                g.setColor(color);
            }
        }        
    }
    
    @Override
    public void paint(Graphics g) {
        if(polygons == null) {
            repaint();
            return;
        }
        
        
        g.setFont(g.getFont().deriveFont(10f));
        
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.LIGHT_GRAY);
        int[] ds1 = convert(new Coordinate(dateline, -getHeight()/2));
        int[] de1 = convert(new Coordinate(dateline, +getHeight()/2));
        g.drawLine(ds1[0], 0, de1[0], getHeight());
        
        int[] ds2 = convert(new Coordinate(-dateline, -getHeight()/2));
        int[] de2 = convert(new Coordinate(-dateline, +getHeight()/2));
        g.drawLine(ds2[0], 0, de2[0], getHeight());

        for (Geometry geometry : geometires) {
            drawGeometry(g, geometry, false);
        }

        drawGeometry(g, polygons[currentPolygon].build(new GeometryFactory()), true);

        
//        if(decompose) {
//            Coordinate[][] coord = split(dateline, points);
//            g.setColor(Color.GREEN);
//            for (int i = 0; i < coord.length; i++) {
//                drawPolygon(g, coord[i], 0, coord[i].length-1, true, 1);
//            }
//        }
        
    }
    
    private int[] convert(Coordinate coordinate) {
        return new int[] {
                Math.round(Math.round(scale*(center[0]+coordinate.x))),
                Math.round(Math.round(scale*(center[1]-coordinate.y))),
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
        if(e.getButton() == MouseEvent.BUTTON1) {
            decompose = !decompose;
        } else if(e.getButton() == MouseEvent.BUTTON3) {
            currentPolygon = (currentPolygon+1) % polygons.length;
        }
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
        scale = scale + 0.1 * e.getPreciseWheelRotation();
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
