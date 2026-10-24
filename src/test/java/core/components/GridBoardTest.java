package core.components;

import core.components.grid.SquareCoord;
import org.junit.Test;

import static org.junit.Assert.*;

public class GridBoardTest {

    @Test
    public void aNodeIsReadAndWrittenByXAndY() {
        GridBoard board = new GridBoard(3, 2);
        BoardNode node = new BoardNode("a");
        board.setElement(2, 1, node);
        assertSame(node, board.getElement(2, 1));
        assertSame(node, board.getElement(new SquareCoord(2, 1)));
        assertTrue(board.contains(2, 1));
        assertNull(board.getElement(1, 1));
        assertEquals(1, board.size());
        board.setElement(2, 1, null);
        assertTrue(board.isEmpty());
    }

    @Test
    public void puttingANodeOffTheBoardFails() {
        GridBoard board = new GridBoard(3, 2);
        assertFalse(board.setElement(3, 0, new BoardNode("a")));
        assertFalse(board.setElement(0, -1, new BoardNode("b")));
        assertTrue(board.isEmpty());
        assertNull(board.getElement(3, 0));
    }

    @Test
    public void shrinkingDropsTheNodesThatNoLongerFit() {
        GridBoard board = new GridBoard(3, 3);
        BoardNode kept = new BoardNode("a");
        board.setElement(0, 0, kept);
        board.setElement(2, 2, new BoardNode("b"));
        board.setWidthHeight(2, 2);
        assertEquals(1, board.size());
        assertSame(kept, board.getElement(0, 0));
    }

    @Test
    public void aCopyCopiesTheNodesAndTheirNeighbours() {
        GridBoard board = new GridBoard(2, 1);
        BoardNode a = new BoardNode("a"), b = new BoardNode("b");
        a.addNeighbourWithCost(b, 2.0);
        board.setElement(0, 0, a);
        board.setElement(1, 0, b);

        GridBoard copy = board.copy();
        assertEquals(board, copy);
        assertEquals(board.hashCode(), copy.hashCode());
        assertNotSame(a, copy.getElement(0, 0));
        assertSame(copy.getElement(1, 0), copy.getElement(0, 0).getNeighbours().keySet().iterator().next());
        assertEquals(2.0, copy.getElement(0, 0).getNeighbourCost(copy.getElement(1, 0)), 0.0);

        copy.setElement(0, 0, null);
        assertSame(a, board.getElement(0, 0));
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
        board.setElement(0, 0, shared);
        assertSame(shared, board.copy().getElement(0, 0));
    }

    @Test
    public void resizingMovesTheNodesByTheOffset() {
        GridBoard board = new GridBoard(2, 2);
        BoardNode node = new BoardNode("a");
        board.setElement(1, 1, node);
        board.setWidthHeight(4, 3, 2, 1);
        assertEquals(4, board.getWidth());
        assertEquals(3, board.getHeight());
        assertSame(node, board.getElement(3, 2));
        assertNull(board.getElement(1, 1));
    }

    @Test
    public void rotatingTurnsTheBoardClockwise() {
        GridBoard board = new GridBoard(2, 1);
        board.setElement(0, 0, new BoardNode("a"));
        board.setElement(1, 0, new BoardNode("b"));
        BoardNode[][] rotated = board.rotate(1);
        assertEquals(2, rotated.length);
        assertEquals(1, rotated[0].length);
        assertEquals("a", rotated[0][0].getComponentName());
        assertEquals("b", rotated[1][0].getComponentName());
    }
}
