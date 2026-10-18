package core.components;

import core.components.grid.SquareCoord;
import org.junit.Test;

import static org.junit.Assert.*;

public class GridBoardTest {

    @Test
    public void aNodeIsReadAndWrittenByXAndY() {
        GridBoard board = new GridBoard(3, 2);
        BoardNode node = new BoardNode("a");
        board.set(2, 1, node);
        assertSame(node, board.get(2, 1));
        assertSame(node, board.get(new SquareCoord(2, 1)));
        assertTrue(board.contains(2, 1));
        assertNull(board.get(1, 1));
        assertEquals(1, board.size());
        board.set(2, 1, null);
        assertTrue(board.isEmpty());
    }

    @Test
    public void puttingANodeOffTheBoardThrows() {
        GridBoard board = new GridBoard(3, 2);
        assertThrows(IllegalArgumentException.class, () -> board.set(3, 0, new BoardNode("a")));
        assertThrows(IllegalArgumentException.class, () -> board.set(0, -1, new BoardNode("b")));
        assertTrue(board.isEmpty());
        assertNull(board.get(3, 0));
    }

    @Test
    public void shrinkingDropsTheNodesThatNoLongerFit() {
        GridBoard board = new GridBoard(3, 3);
        BoardNode kept = new BoardNode("a");
        board.set(0, 0, kept);
        board.set(2, 2, new BoardNode("b"));
        board.setWidthHeight(2, 2);
        assertEquals(1, board.size());
        assertSame(kept, board.get(0, 0));
    }

    @Test
    public void aCopyCopiesTheNodesAndTheirNeighbours() {
        GridBoard board = new GridBoard(2, 1);
        BoardNode a = new BoardNode("a"), b = new BoardNode("b");
        a.addNeighbourWithCost(b, 2.0);
        board.set(0, 0, a);
        board.set(1, 0, b);

        GridBoard copy = board.copy();
        assertEquals(board, copy);
        assertEquals(board.hashCode(), copy.hashCode());
        assertNotSame(a, copy.get(0, 0));
        assertSame(copy.get(1, 0), copy.get(0, 0).getNeighbours().keySet().iterator().next());
        assertEquals(2.0, copy.get(0, 0).getNeighbourCost(copy.get(1, 0)), 0.0);

        copy.set(0, 0, null);
        assertSame(a, board.get(0, 0));
        assertNotEquals(board, copy);
    }

    @Test
    public void anImmutableNodeIsSharedByTheCopies() {
        BoardNode shared = new BoardNode("x") {
            @Override
            public BoardNode copy() {
                return this;
            }
        };
        GridBoard board = new GridBoard(1, 1);
        board.set(0, 0, shared);
        assertSame(shared, board.copy().get(0, 0));
    }

    @Test
    public void resizingMovesTheNodesByTheOffset() {
        GridBoard board = new GridBoard(2, 2);
        BoardNode node = new BoardNode("a");
        board.set(1, 1, node);
        board.setWidthHeight(4, 3, 2, 1);
        assertEquals(4, board.getWidth());
        assertEquals(3, board.getHeight());
        assertSame(node, board.get(3, 2));
        assertNull(board.get(1, 1));
    }

    @Test
    public void rotatingTurnsTheBoardClockwise() {
        GridBoard board = new GridBoard(2, 1);
        board.set(0, 0, new BoardNode("a"));
        board.set(1, 0, new BoardNode("b"));
        BoardNode[][] rotated = board.rotate(1);
        assertEquals(2, rotated.length);
        assertEquals(1, rotated[0].length);
        assertEquals("a", rotated[0][0].getComponentName());
        assertEquals("b", rotated[1][0].getComponentName());
    }
}
