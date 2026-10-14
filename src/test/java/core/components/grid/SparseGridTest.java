package core.components.grid;

import core.CoreConstants;
import core.components.Token;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static core.components.grid.HexCoordTest.hex;
import static org.junit.Assert.*;

public class SparseGridTest {

    SparseGrid<HexCoord, String> grid;

    @Before
    public void setup() {
        grid = new SparseGrid<>("Grid");
        grid.set(hex(0, 0), "a");
        grid.set(hex(1, 0), "b");
        grid.set(hex(0, 1), "c");
    }

    @Test
    public void setGetAndRemove() {
        assertEquals("b", grid.get(hex(1, 0)));
        assertNull(grid.get(hex(5, 5)));
        assertNull(grid.get(hex(-500, 300)));
        assertEquals(3, grid.size());
        grid.set(hex(1, 0), "B");
        assertEquals("B", grid.get(hex(1, 0)));
        assertEquals(3, grid.size());
        assertEquals("B", grid.remove(hex(1, 0)));
        assertFalse(grid.contains(hex(1, 0)));
        assertNull(grid.remove(hex(1, 0)));
        assertNull(grid.remove(hex(-500, 300)));
        // setting null takes the value away
        grid.set(hex(0, 1), null);
        assertEquals(Set.of(hex(0, 0)), grid.positions());
        assertFalse(grid.isEmpty());
        grid.remove(hex(0, 0));
        assertTrue(grid.isEmpty());
        assertEquals(0, grid.size());
    }

    @Test
    public void theGridGrowsInEveryDirection() {
        // far beyond the first positions, on every side
        List<HexCoord> far = List.of(hex(40, 0), hex(-37, 2), hex(3, 55), hex(-2, -61), hex(100, -100), hex(-100, 100));
        for (int i = 0; i < far.size(); i++)
            grid.set(far.get(i), "far" + i);
        assertEquals(9, grid.size());
        for (int i = 0; i < far.size(); i++)
            assertEquals("far" + i, grid.get(far.get(i)));
        assertEquals("a", grid.get(hex(0, 0)));
        assertEquals("b", grid.get(hex(1, 0)));
        assertEquals("c", grid.get(hex(0, 1)));
        Set<HexCoord> expected = new HashSet<>(far);
        expected.addAll(List.of(hex(0, 0), hex(1, 0), hex(0, 1)));
        assertEquals(expected, grid.positions());
    }

    @Test
    public void positionsAreSortedOnRequest() {
        grid.set(hex(-2, 4), "d");
        grid.set(hex(-7, 1), "e");
        assertEquals(List.of(hex(0, 0), hex(1, 0), hex(-7, 1), hex(0, 1), hex(-2, 4)), grid.sortedPositions());
        assertEquals(grid.sortedPositions(), new ArrayList<>(grid.positions()));
    }

    @Test
    public void theMapViewMatchesTheGrid() {
        Map<HexCoord, String> expected = Map.of(hex(0, 0), "a", hex(1, 0), "b", hex(0, 1), "c");
        assertEquals(expected, grid.asMap());
        assertEquals(grid.asMap(), expected);
        assertEquals(expected.hashCode(), grid.asMap().hashCode());
        assertTrue(grid.asMap().containsKey(hex(0, 1)));
        assertFalse(grid.asMap().containsKey(hex(1, 1)));
        // a square at the same pair of ints is not a hex on the grid
        assertFalse(grid.asMap().containsKey(new SquareCoord(0, 1)));
        assertNull(grid.asMap().get(new SquareCoord(0, 1)));
        // the view follows the grid
        Map<HexCoord, String> view = grid.asMap();
        grid.set(hex(9, 9), "z");
        assertEquals("z", view.get(hex(9, 9)));
        assertEquals(4, view.size());
    }

    @Test
    public void viewsAreReadOnly() {
        assertThrows(UnsupportedOperationException.class, () -> grid.positions().clear());
        assertThrows(UnsupportedOperationException.class, () -> grid.asMap().put(hex(4, 4), "x"));
    }

    @Test
    public void borderPositionsHaveAnEmptyNeighbour() {
        assertTrue(grid.isOnBorder(hex(0, 0)));
        assertFalse("not on the grid", grid.isOnBorder(hex(1, 1)));
        // fill the ring round (0,0)
        for (HexCoord n : hex(0, 0).neighbours())
            grid.set(n, "n");
        assertFalse(grid.isOnBorder(hex(0, 0)));
        assertTrue(grid.isOnBorder(hex(1, 0)));
    }

    @Test
    public void copyIsEqualAndIndependent() {
        SparseGrid<HexCoord, String> copy = grid.copy();
        assertEquals(grid, copy);
        assertEquals(grid.hashCode(), copy.hashCode());
        assertEquals(grid.getComponentID(), copy.getComponentID());
        copy.set(hex(0, 0), "changed");
        copy.set(hex(4, 4), "new");
        copy.remove(hex(1, 0));
        // and far enough away to grow the copy
        copy.set(hex(-50, 50), "far");
        assertEquals("a", grid.get(hex(0, 0)));
        assertEquals("b", grid.get(hex(1, 0)));
        assertFalse(grid.contains(hex(4, 4)));
        assertFalse(grid.contains(hex(-50, 50)));
        assertEquals(3, grid.size());
        assertNotEquals(grid, copy);
    }

    @Test
    public void gridsThatGrewDifferentlyAreEqualByContent() {
        SparseGrid<HexCoord, String> copy = grid.copy();
        copy.set(hex(70, -70), "far");
        copy.remove(hex(70, -70));
        assertEquals(grid, copy);
        assertEquals(grid.hashCode(), copy.hashCode());
    }

    @Test
    public void gridsMadeSeparatelyAreNotEqual() {
        SparseGrid<HexCoord, String> other = new SparseGrid<>("Grid");
        for (HexCoord h : grid.positions())
            other.set(h, grid.get(h));
        assertNotEquals(grid, other);
    }

    @Test
    public void aFullyObservableGridIsVisibleToAll() {
        assertTrue(grid.isVisible(hex(0, 0), 0));
        assertTrue(grid.isVisible(hex(0, 0), 7));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> grid.setVisible(hex(0, 0), 0, false));
        assertEquals("Grid is fully observable", e.getMessage());
    }

    @Test
    public void positionsCanBeHiddenFromEachPlayer() {
        SparseGrid<SquareCoord, String> cards = new SparseGrid<>("Cards", 3);
        SquareCoord goal = new SquareCoord(8, 2);
        cards.set(goal, "gold");
        for (int p = 0; p < 3; p++)
            assertTrue(cards.isVisible(goal, p));
        cards.setVisible(goal, 0, false);
        cards.setVisible(goal, 2, false);
        assertFalse(cards.isVisible(goal, 0));
        assertTrue(cards.isVisible(goal, 1));
        assertFalse(cards.isVisible(goal, 2));
        // visibility belongs to the position, with or without a value
        cards.remove(goal);
        assertFalse(cards.isVisible(goal, 0));
        cards.setVisible(goal, 0, true);
        assertTrue(cards.isVisible(goal, 0));
    }

    @Test
    public void visibilityIsCopiedIndependentlyAndCountsForEquality() {
        SparseGrid<SquareCoord, String> cards = new SparseGrid<>("Cards", 2);
        SquareCoord goal = new SquareCoord(1, 1);
        cards.set(goal, "coal");
        cards.setVisible(goal, 1, false);
        SparseGrid<SquareCoord, String> copy = cards.copy();
        assertEquals(cards, copy);
        assertFalse(copy.isVisible(goal, 1));
        copy.setVisible(goal, 1, true);
        assertFalse(cards.isVisible(goal, 1));
        assertNotEquals(cards, copy);
    }

    @Test
    public void componentValuesAreContained() {
        SparseGrid<SquareCoord, Token> tokens = new SparseGrid<>("Tokens", 2);
        Token a = new Token("a"), b = new Token("b");
        tokens.set(new SquareCoord(5, 1), a);
        tokens.set(new SquareCoord(0, 0), b);
        assertEquals(Set.of(a, b), new HashSet<>(tokens.getComponents()));
        assertEquals(CoreConstants.VisibilityMode.MIXED_VISIBILITY, tokens.getVisibilityMode());
        // values that are not components are not
        assertTrue(grid.getComponents().isEmpty());
        assertEquals(CoreConstants.VisibilityMode.VISIBLE_TO_ALL, grid.getVisibilityMode());
    }
}
