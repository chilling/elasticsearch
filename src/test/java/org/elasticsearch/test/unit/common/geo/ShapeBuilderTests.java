package org.elasticsearch.test.unit.common.geo;

import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Rectangle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

import org.elasticsearch.common.geo.builders.GeoShapeBuilder;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link ShapeBuilder}
 */
public class ShapeBuilderTests {

    @Test
    public void testNewPoint() {
        Point point = GeoShapeBuilder.newPoint(-100, 45).buildShape();
        assertEquals(-100D, point.getX());
        assertEquals(45D, point.getY());
    }

    @Test
    public void testNewRectangle() {
        Rectangle rectangle = GeoShapeBuilder.newEnvelope().topLeft(-45, 30).bottomRight(45, -30).buildShape();
        assertEquals(-45D, rectangle.getMinX());
        assertEquals(-30D, rectangle.getMinY());
        assertEquals(45D, rectangle.getMaxX());
        assertEquals(30D, rectangle.getMaxY());
    }

    @Test
    public void testNewPolygon() {
        Polygon polygon = GeoShapeBuilder.newPolygon()
                .point(-45, 30)
                .point(45, 30)
                .point(45, -30)
                .point(-45, -30)
                .point(-45, 30).toPolygon();

        LineString exterior = polygon.getExteriorRing();
        assertEquals(exterior.getCoordinateN(0), new Coordinate(-45, 30));
        assertEquals(exterior.getCoordinateN(1), new Coordinate(45, 30));
        assertEquals(exterior.getCoordinateN(2), new Coordinate(45, -30));
        assertEquals(exterior.getCoordinateN(3), new Coordinate(-45, -30));
    }

//    @Test
//    public void testToJTSGeometry() {
//        PolygonBuilder polygonBuilder = GeoShapeBuilder.newPolygon()
//                .point(-45, 30)
//                .point(45, 30)
//                .point(45, -30)
//                .point(-45, -30)
//                .close();
//
//        Shape polygon = polygonBuilder.buildShape();
//        Geometry polygonGeometry = ShapeBuilder.toJTSGeometry(polygon);
//        assertEquals(polygonBuilder.toPolygon(), polygonGeometry);
//
//        Rectangle rectangle = ShapeBuilder.newRectangle().topLeft(-45, 30).bottomRight(45, -30).build();
//        Geometry rectangleGeometry = ShapeBuilder.toJTSGeometry(rectangle);
//        assertEquals(rectangleGeometry, polygonGeometry);
//
//        Point point = ShapeBuilder.newPoint(-45, 30);
//        Geometry pointGeometry = ShapeBuilder.toJTSGeometry(point);
//        assertEquals(pointGeometry.getCoordinate(), new Coordinate(-45, 30));
//    }
}
