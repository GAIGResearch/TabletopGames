package core.components.grid;

import org.junit.Test;

import java.util.*;

import static core.components.grid.HexCoord.Offset.EVEN_R;
import static core.components.grid.HexCoord.Offset.ODD_R;
import static org.junit.Assert.*;

public class HexCoordTest {

    static HexCoord hex(int q, int r) {
        return new HexCoord(q, r);
    }

    @Test
    public void neighboursRunClockwiseFromEast() {
        assertEquals(List.of(hex(1, 0), hex(0, 1), hex(-1, 1), hex(-1, 0), hex(0, -1), hex(1, -1)),
                HexCoord.ORIGIN.neighbours());
        assertEquals(hex(3, -2), hex(2, -1).neighbour(HexCoord.NE));
    }

    @Test
    public void oppositeDirectionLeadsBack() {
        assertEquals(HexCoord.W, HexCoord.ORIGIN.opposite(HexCoord.E));
        assertEquals(HexCoord.SE, HexCoord.ORIGIN.opposite(HexCoord.NW));
        HexCoord h = hex(2, -3);
        for (int d = 0; d < 6; d++)
            assertEquals(h, h.neighbour(d).neighbour(h.opposite(d)));
    }

    @Test
    public void distanceCountsSteps() {
        assertEquals(0, hex(1, 1).distance(hex(1, 1)));
        assertEquals(1, HexCoord.ORIGIN.distance(hex(1, -1)));
        // a step east, then one north-east
        assertEquals(2, HexCoord.ORIGIN.distance(hex(2, -1)));
        // along r only
        assertEquals(3, HexCoord.ORIGIN.distance(hex(0, 3)));
        // from (-1,0): two steps south-east and two east
        assertEquals(4, hex(1, 2).distance(hex(-1, 0)));
        assertEquals(3, hex(3, -3).distance(HexCoord.ORIGIN));
    }

    @Test
    public void oddROffsetRowsAreShiftedRight() {
        // odd rows are shifted half a hex right, so column 0 of row 1 is south-east of column 0 of row 0
        assertEquals(hex(0, 1), HexCoord.fromOffset(0, 1, ODD_R));
        assertEquals(hex(0, 0).neighbour(HexCoord.SE), HexCoord.fromOffset(0, 1, ODD_R));
        assertEquals(hex(0, 3), HexCoord.fromOffset(1, 3, ODD_R));
        assertEquals(hex(1, -1), HexCoord.fromOffset(0, -1, ODD_R));
        assertEquals(hex(-1, 2), HexCoord.fromOffset(0, 2, ODD_R));
    }

    @Test
    public void evenROffsetRowsAreShiftedRight() {
        // even rows are shifted half a hex right, so column 0 of row 1 is south-west of column 0 of row 0
        assertEquals(hex(-1, 1), HexCoord.fromOffset(0, 1, EVEN_R));
        assertEquals(hex(0, 0).neighbour(HexCoord.SW), HexCoord.fromOffset(0, 1, EVEN_R));
        // row -1 is not shifted, so its column 1 is north-east of column 0 of row 0
        assertEquals(hex(0, 0).neighbour(HexCoord.NE), HexCoord.fromOffset(1, -1, EVEN_R));
    }

    @Test
    public void offsetColumnInvertsFromOffset() {
        for (HexCoord.Offset offset : HexCoord.Offset.values())
            for (int row = -3; row <= 3; row++)
                for (int col = -3; col <= 3; col++) {
                    HexCoord h = HexCoord.fromOffset(col, row, offset);
                    assertEquals(col, h.offsetCol(offset));
                    assertEquals(row, h.r());
                }
    }

    @Test
    public void oddROffsetNeighboursMatchTheOffsetTables() {
        // offset (dcol, drow) of each neighbour, for even and odd rows, from the standard odd-r tables
        int[][][] table = {{{1, 0}, {0, -1}, {-1, -1}, {-1, 0}, {-1, 1}, {0, 1}},
                {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {0, 1}, {1, 1}}};
        checkOffsetNeighbours(ODD_R, table);
    }

    @Test
    public void evenROffsetNeighboursMatchTheOffsetTablesInDirectionOrder() {
        // in direction order E, SE, SW, W, NW, NE, for even and odd rows
        int[][][] table = {{{1, 0}, {1, 1}, {0, 1}, {-1, 0}, {0, -1}, {1, -1}},
                {{1, 0}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}}};
        for (int row = -2; row <= 2; row++)
            for (int col = -2; col <= 2; col++) {
                HexCoord h = HexCoord.fromOffset(col, row, EVEN_R);
                for (int d = 0; d < 6; d++) {
                    HexCoord n = h.neighbour(d);
                    int[] delta = table[row & 1][d];
                    assertEquals(col + delta[0], n.offsetCol(EVEN_R));
                    assertEquals(row + delta[1], n.r());
                }
            }
    }

    private static void checkOffsetNeighbours(HexCoord.Offset offset, int[][][] table) {
        for (int row = -2; row <= 2; row++)
            for (int col = -2; col <= 2; col++) {
                Set<List<Integer>> expected = new HashSet<>();
                for (int[] delta : table[row & 1])
                    expected.add(List.of(col + delta[0], row + delta[1]));
                Set<List<Integer>> actual = new HashSet<>();
                for (HexCoord n : HexCoord.fromOffset(col, row, offset).neighbours())
                    actual.add(List.of(n.offsetCol(offset), n.r()));
                assertEquals("neighbours of (" + col + "," + row + ")", expected, actual);
            }
    }

    @Test
    public void hexagonHoldsEveryHexWithinTheRadiusInOrder() {
        // 1 + 6 + 12 hexes
        List<HexCoord> hexes = HexCoord.hexagon(2);
        assertEquals(19, hexes.size());
        int within = 0;
        for (int q = -3; q <= 3; q++)
            for (int r = -3; r <= 3; r++)
                if (HexCoord.ORIGIN.distance(hex(q, r)) <= 2) {
                    within++;
                    assertTrue(hexes.contains(hex(q, r)));
                }
        assertEquals(19, within);
        List<HexCoord> sorted = new ArrayList<>(hexes);
        Collections.sort(sorted);
        assertEquals(sorted, hexes);
        assertEquals(List.of(HexCoord.ORIGIN), HexCoord.hexagon(0));
    }

    @Test
    public void ringRunsClockwiseFromTheNorthWest() {
        // from the hex north-west of (1,1), stepping E, SE, SW, W, NW, NE
        assertEquals(List.of(hex(2, 0), hex(2, 1), hex(1, 2), hex(0, 2), hex(0, 1), hex(1, 0)), hex(1, 1).ring(1));
        List<HexCoord> ring = HexCoord.ORIGIN.ring(2);
        assertEquals(12, ring.size());
        assertEquals(12, new HashSet<>(ring).size());
        for (int k = 0; k < ring.size(); k++) {
            assertEquals(2, HexCoord.ORIGIN.distance(ring.get(k)));
            // the k-th hex is a step in direction k / 2 from the one before it (the last before the first is the
            // start, two steps north-west)
            HexCoord before = k == 0 ? hex(0, -2) : ring.get(k - 1);
            assertEquals(ring.get(k), before.neighbour(k / 2));
        }
    }

    @Test
    public void hexesAreOrderedRowByRow() {
        List<HexCoord> hexes = new ArrayList<>(List.of(hex(1, -5), hex(0, 2), hex(-1, 3), hex(-3, 2), hex(0, -1)));
        Collections.sort(hexes);
        assertEquals(List.of(hex(1, -5), hex(0, -1), hex(-3, 2), hex(0, 2), hex(-1, 3)), hexes);
    }

    @Test
    public void connectedGroupsSplitByAdjacency() {
        // (0,0) and its east neighbour touch; (3,0) is on its own; (0,2) touches (0,1)
        List<Set<HexCoord>> groups = GridCoord.connectedGroups(List.of(hex(0, 0), hex(1, 0), hex(3, 0), hex(0, 1),
                hex(0, 2)));
        assertEquals(Set.of(Set.of(hex(0, 0), hex(1, 0), hex(0, 1), hex(0, 2)), Set.of(hex(3, 0))),
                new HashSet<>(groups));
        assertTrue(GridCoord.connectedGroups(List.<HexCoord>of()).isEmpty());
    }
}
