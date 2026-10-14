package games.battlelore;

import games.battlelore.components.MapTile;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * The board's odd rows are shifted half a hex right, so (x, y) is column x of row y.
 */
public class MapTileTest {

    static int distance(int x1, int y1, int x2, int y2) {
        return new MapTile(x1, y1, new ArrayList<>()).getHex().distance(new MapTile(x2, y2, new ArrayList<>()).getHex());
    }

    @Test
    public void distanceCountsHexSteps() {
        // along a row
        assertEquals(3, distance(1, 4, 4, 4));
        // row 1 is shifted right, so (0,1) and (1,1) both touch (1,0) and (1,2)
        assertEquals(1, distance(1, 0, 0, 1));
        assertEquals(1, distance(1, 0, 1, 1));
        assertEquals(1, distance(0, 1, 1, 2));
        // from row 0 to row 2 is two steps whether straight down or one column across
        assertEquals(2, distance(3, 0, 3, 2));
        assertEquals(2, distance(3, 0, 2, 2));
        // (2,0) to (0,1): a step down-left to (1,1), then left; 2.24 apart as the crow flies over the offsets
        assertEquals(2, distance(2, 0, 0, 1));
        // two columns across and four rows down: the rows absorb the columns
        assertEquals(4, distance(0, 0, 2, 4));
    }
}
