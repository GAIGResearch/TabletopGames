package games.risk;

import games.risk.gui.RiskBoardShapes;
import games.risk.gui.RiskOdds;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * The world map's drawing (each territory's shape, from data/risk/worldMap.svg) and the battle odds the GUI shows.
 */
public class RiskMapDrawingTest {

    @Test
    public void everyTerritoryHasAShapeWithItsLabelInside() {
        RiskBoardShapes shapes = new RiskBoardShapes(MAP);
        Rectangle2D view = shapes.viewBox;
        for (RiskTerritory t : MAP.territories()) {
            Shape shape = shapes.shape(t);
            Point2D label = shapes.label(t);
            assertTrue(t.name(), shape.contains(label));
            assertTrue(t.name(), view.contains(label));
        }
    }

    @Test
    public void neighboursByLandAreDrawnNextToEachOther() {
        RiskBoardShapes shapes = new RiskBoardShapes(MAP);
        // a point of Alberta's shape lies close to Alaska's; Alaska's to Kamchatka's does not (across the sea)
        assertTrue(near(shapes.shape(ALASKA), shapes.shape(ALBERTA)));
        assertFalse(near(shapes.shape(ALASKA), shapes.shape(KAMCHATKA)));
    }

    private static boolean near(Shape a, Shape b) {
        Rectangle2D r = a.getBounds2D();
        for (double x = r.getMinX(); x <= r.getMaxX(); x += 1)
            for (double y = r.getMinY(); y <= r.getMaxY(); y += 1)
                if (a.contains(x, y) && b.intersects(x - 3, y - 3, 6, 6))
                    return true;
        return false;
    }

    @Test
    public void oneRollHasTheOddsOfTheDice() {
        // 3 dice against 2: of 7776 rolls, the attacker loses no army in 2890, one in 2611, two in 2275
        double[] p = RiskOdds.roll(3, 2);
        assertEquals(2890 / 7776.0, p[0], 1e-9);
        assertEquals(2611 / 7776.0, p[1], 1e-9);
        assertEquals(2275 / 7776.0, p[2], 1e-9);
        // 1 against 1: the defender wins ties, so the attacker wins 15 of 36
        assertEquals(21 / 36.0, RiskOdds.roll(1, 1)[1], 1e-9);
    }

    @Test
    public void attackingUntilATerritoryFallsHasTheOddsOfItsRolls() {
        // 2 armies attack with 1 die, and one loss ends the attack
        assertEquals(15 / 36.0, RiskOdds.capture(2, 1, 3, 2), 1e-9);
        assertEquals(0, RiskOdds.capture(1, 5, 3, 2), 0);
        assertEquals(1, RiskOdds.capture(5, 0, 3, 2), 0);
        // more attackers, better odds
        assertTrue(RiskOdds.capture(10, 5, 3, 2) > RiskOdds.capture(6, 5, 3, 2));
        assertTrue(RiskOdds.capture(10, 5, 3, 2) > 0.5);
    }
}
