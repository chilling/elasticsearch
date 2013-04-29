package org.elasticsearch.common.geo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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

import org.elasticsearch.common.geo.ShapeBuilder.PolygonBuilder;

import com.vividsolutions.jts.geom.Coordinate;

public class TestFrame extends JFrame implements ComponentListener, MouseListener {

    private PolygonBuilder polygon;
    private double dateline = 180;
    
    public TestFrame() {
        super("polygon intersection");
        
        this.setSize(800, 600);
        this.setVisible(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        this.addMouseListener(this);
        
        polygon = ShapeBuilder.newPolygon()
                .point(-100, -190).point(-250, 190).point(100, 190).point(250, -190)
                .hole()
                    .point(-50, -50).point(-50, 50).point(50, 50).point(50, -50)
                .close()
                .hole()
                    .point(-230, 170).point(50, 160).point(50, 140).point(-220, 150)
                .close()
                .hole()
                    .point(185, -160).point(230, -160).point(200, -80).point(185, -120)
                .close()
            .close();
    }
    
    public static void main(String[] args) {
        new TestFrame();
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
        if(polygon == null) {
            this.repaint();
            return;
        }
        
        Coordinate[] points = polygon.coordinates();
        
        g.clearRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.RED);
        int[] ds1 = convert(new Coordinate(dateline, -getHeight()/2));
        int[] de1 = convert(new Coordinate(dateline, +getHeight()/2));
        g.drawLine(ds1[0], ds1[1], de1[0], de1[1]);
        
        int[] ds2 = convert(new Coordinate(-dateline, -getHeight()/2));
        int[] de2 = convert(new Coordinate(-dateline, +getHeight()/2));
        g.drawLine(ds2[0], ds2[1], de2[0], de2[1]);
        
        g.setColor(Color.BLUE);
        drawPolygon(g, points, 1, points.length);
        
        
        g.setColor(Color.RED);
        for(Coordinate[] hole : polygon.inner()) {
            drawPolygon(g, hole, 1, hole.length);
        }
        
        if(decompose) {
            g.setColor(Color.GREEN);
            for(Coordinate[] poly : polygon.asMultipolygon()) {
                drawPolygon(g, poly);
            }
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

    private boolean decompose;
    
    @Override
    public void mouseClicked(MouseEvent e) {
        decompose = !decompose;
        this.repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }
    
    
}
