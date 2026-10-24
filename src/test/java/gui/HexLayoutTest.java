package gui;

import core.components.grid.HexCoord;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Hexes of size 10 have centres sqrt(3) * 10 = 17.32 apart along a row, and rows 15 apart.
 */
public class HexLayoutTest {

    static final double SQRT3 = Math.sqrt(3), EPS = 1e-9;
    final HexLayout layout = new HexLayout(10, 100, 50);

    @Test
    public void centresStepAlongRowsAndDownHalfAHexAcross() {
        assertEquals(new Point2D.Double(100, 50), layout.centre(HexCoord.ORIGIN));
        // east: a hex width along
        assertPoint(100 + 10 * SQRT3, 50, layout.centre(new HexCoord(1, 0)));
        // south-east: half a hex width along and 1.5 sizes down
        assertPoint(100 + 5 * SQRT3, 65, layout.centre(new HexCoord(0, 1)));
        assertEquals(new Point(109, 65), layout.pixel(new HexCoord(0, 1)));
    }

    @Test
    public void cornersRunClockwiseFromTheTopRight() {
        // corner 0 at 30 degrees above east, corner 5 straight up, corner 2 straight down
        assertPoint(100 + 5 * SQRT3, 45, layout.corner(HexCoord.ORIGIN, 0));
        assertPoint(100, 40, layout.corner(HexCoord.ORIGIN, 5));
        assertPoint(100, 60, layout.corner(HexCoord.ORIGIN, 2));
        assertPoint(100, 54, layout.corner(HexCoord.ORIGIN, 2, 4));
        // the side facing east, from corner 0 to corner 1, is shared with the east neighbour's corners 4 and 3
        assertPoint(layout.corner(new HexCoord(1, 0), 4).getX(), layout.corner(new HexCoord(1, 0), 4).getY(),
                layout.corner(HexCoord.ORIGIN, 0));
        assertPoint(layout.corner(new HexCoord(1, 0), 3).getX(), layout.corner(new HexCoord(1, 0), 3).getY(),
                layout.corner(HexCoord.ORIGIN, 1));
    }

    @Test
    public void outlineContainsTheCentreOnly() {
        assertTrue(layout.outline(new HexCoord(2, -1), 10).contains(layout.centre(new HexCoord(2, -1))));
        assertFalse(layout.outline(new HexCoord(2, -1), 10).contains(layout.centre(new HexCoord(2, 0))));
    }

    @Test
    public void hexAtFindsTheHexUnderAPoint() {
        for (HexCoord h : HexCoord.hexagon(3)) {
            Point2D c = layout.centre(h);
            assertEquals(h, layout.hexAt(c.getX(), c.getY()));
            // just inside its east side, and just inside its top corner
            assertEquals(h, layout.hexAt(c.getX() + 8.5, c.getY()));
            assertEquals(h, layout.hexAt(c.getX(), c.getY() - 9.5));
        }
        // just past the east side is the east neighbour
        assertEquals(new HexCoord(1, 0), layout.hexAt(100 + 9, 50));
    }

    @Test
    public void fittingCentresTheHexesInTheArea() {
        // (0,0) and (2,0): centres 2 * sqrt3 * size apart, plus a hex width, must fit 200 - 2 * 10 wide; 2 * size high
        // must fit 100 - 2 * 10
        HexLayout fit = HexLayout.fitting(List.of(HexCoord.ORIGIN, new HexCoord(2, 0)), 200, 100, 10, 1000);
        assertEquals(Math.min(180 / (3 * SQRT3), 80 / 2.0), fit.size(), EPS);
        Point2D a = fit.centre(HexCoord.ORIGIN), b = fit.centre(new HexCoord(2, 0));
        assertEquals(100, (a.getX() + b.getX()) / 2, EPS);
        assertEquals(50, a.getY(), EPS);
        // the size is capped
        assertEquals(12, HexLayout.fitting(List.of(HexCoord.ORIGIN), 200, 100, 10, 12).size(), EPS);
    }

    private static void assertPoint(double x, double y, Point2D p) {
        assertEquals(x, p.getX(), EPS);
        assertEquals(y, p.getY(), EPS);
    }
}
