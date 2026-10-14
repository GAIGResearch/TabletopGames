package games.chinesecheckers;

import core.Game;
import core.actions.AbstractAction;
import core.components.grid.HexCoord;
import games.GameType;
import games.chinesecheckers.actions.MovePeg;
import games.chinesecheckers.components.Peg;
import games.chinesecheckers.components.StarBoard;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * The star is a hexagram of 121 hexes round the origin, with node 0 at the top point and node 60 in the middle.
 */
public class StarBoardGeometryTest {

    StarBoard board = new StarBoard();

    List<Integer> neighbours(int node) {
        List<Integer> retValue = new ArrayList<>();
        for (HexCoord h : board.getPosition(node).neighbours())
            if (board.getNode(h) >= 0)
                retValue.add(board.getNode(h));
        return retValue;
    }

    @Test
    public void nodesAreNumberedRowByRowFromTheTopPoint() {
        assertEquals(121, board.nNodes());
        assertEquals(new HexCoord(4, -8), board.getPosition(0));
        assertEquals(HexCoord.ORIGIN, board.getPosition(60));
        assertEquals(new HexCoord(-4, 8), board.getPosition(120));
        for (int i = 0; i < board.nNodes(); i++) {
            assertEquals(i, board.getNode(board.getPosition(i)));
            if (i > 0)
                assertTrue(board.getPosition(i - 1).compareTo(board.getPosition(i)) < 0);
        }
        // beyond the top point, and between two points
        assertEquals(-1, board.getNode(new HexCoord(4, -9)));
        assertEquals(-1, board.getNode(new HexCoord(6, -6)));
    }

    @Test
    public void nodesAreJoinedToTheirHexNeighbours() {
        int links = 0;
        for (int i = 0; i < board.nNodes(); i++) {
            List<Integer> n = neighbours(i);
            assertTrue(n.size() >= 2 && n.size() <= 6);
            for (int m : n)
                assertTrue(neighbours(m).contains(i));
            links += n.size();
        }
        // six points of 10 nodes and a hexagon of 61 have 312 links between them
        assertEquals(2 * 312, links);
        // the points have two neighbours each
        assertEquals(List.of(1, 2), neighbours(0).stream().sorted().toList());
        // the hand-built board had no link from 84 to 73
        assertTrue(neighbours(84).contains(73));
    }

    @Test
    public void distancesAreAlongTheBoard() {
        // from point to opposite point, straight through the middle
        assertEquals(16, board.distanceBetween(0, 120));
        for (int i = 0; i < board.nNodes(); i++) {
            assertEquals(0, board.distanceBetween(i, i));
            for (int m : neighbours(i))
                assertEquals(1, board.distanceBetween(i, m));
        }
        // every distance is the shortest path over the board's links
        for (int from = 0; from < board.nNodes(); from++) {
            int[] steps = new int[board.nNodes()];
            Arrays.fill(steps, -1);
            steps[from] = 0;
            Deque<Integer> open = new ArrayDeque<>(List.of(from));
            while (!open.isEmpty()) {
                int n = open.poll();
                for (int m : neighbours(n))
                    if (steps[m] < 0) {
                        steps[m] = steps[n] + 1;
                        open.add(m);
                    }
            }
            for (int to = 0; to < board.nNodes(); to++)
                assertEquals(steps[to], board.distanceBetween(from, to));
        }
    }

    @Test
    public void copiesAreEqualAndIndependent() {
        board.setPeg(5, new Peg(Peg.Colour.red, false));
        StarBoard copy = board.copy();
        assertEquals(board, copy);
        assertEquals(board.hashCode(), copy.hashCode());
        copy.setPeg(5, null);
        copy.setPeg(60, new Peg(Peg.Colour.blue, true));
        assertEquals(new Peg(Peg.Colour.red, false), board.getPeg(5));
        assertFalse(board.isOccupied(60));
        assertNotEquals(board, copy);
    }

    @Test
    public void aPegJumpsOverANeighbourInEveryDirection() {
        // a purple peg in the middle, ringed by red pegs: it can only jump, to the six nodes two steps away
        Game game = GameType.ChineseCheckers.createGameInstance(2, new CCParameters());
        CCGameState state = (CCGameState) game.getGameState();
        StarBoard b = state.getStarBoard();
        for (int i = 0; i < b.nNodes(); i++)
            b.setPeg(i, null);
        b.setPeg(60, new Peg(Peg.Colour.purple, false));
        Set<AbstractAction> expected = new HashSet<>();
        for (int d = 0; d < 6; d++) {
            b.setPeg(b.getNode(HexCoord.ORIGIN.neighbour(d)), new Peg(Peg.Colour.red, false));
            expected.add(new MovePeg(60, b.getNode(HexCoord.ORIGIN.neighbour(d).neighbour(d))));
        }
        assertEquals(Peg.Colour.purple, state.getPlayerColour(state.getCurrentPlayer()));
        assertEquals(expected, new HashSet<>(new CCForwardModel().computeAvailableActions(state)));
    }
}
