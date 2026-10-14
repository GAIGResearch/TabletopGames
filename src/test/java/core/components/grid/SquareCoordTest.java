package core.components.grid;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SquareCoordTest {

    @Test
    public void neighboursRunClockwiseFromEast() {
        // y increases downwards, so south is +1
        assertEquals(List.of(new SquareCoord(3, 2), new SquareCoord(2, 3), new SquareCoord(1, 2), new SquareCoord(2, 1)),
                new SquareCoord(2, 2).neighbours());
        assertEquals(SquareCoord.W, new SquareCoord(0, 0).opposite(SquareCoord.E));
        assertEquals(SquareCoord.S, new SquareCoord(0, 0).opposite(SquareCoord.N));
    }

    @Test
    public void distanceIsOrthogonalSteps() {
        assertEquals(5, new SquareCoord(1, -1).distance(new SquareCoord(-1, 2)));
    }

    @Test
    public void squaresAreOrderedRowByRow() {
        List<SquareCoord> squares = new ArrayList<>(List.of(new SquareCoord(0, 1), new SquareCoord(5, 0),
                new SquareCoord(-1, 1)));
        Collections.sort(squares);
        assertEquals(List.of(new SquareCoord(5, 0), new SquareCoord(-1, 1), new SquareCoord(0, 1)), squares);
    }
}
