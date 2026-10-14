package core.components.grid;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static core.components.grid.HexCoordTest.hex;
import static org.junit.Assert.*;

public class HexVertexAndSideTest {

    @Test
    public void aCornerIsTheSameVertexFromEachHexSharingIt() {
        HexCoord h = hex(1, -2);
        for (int c = 0; c < 6; c++) {
            HexVertex v = new HexVertex(h, c);
            // corner c is also corner c + 2 of the hex in direction c - 1, and corner c + 4 of the one in direction c
            assertEquals(v, new HexVertex(h.neighbour((c + 5) % 6), (c + 2) % 6));
            assertEquals(v, new HexVertex(h.neighbour(c), (c + 4) % 6));
            assertTrue(v.corner() == 0 || v.corner() == 1);
        }
        // the top corner of (0,1), south-east of (0,0), is the lower right corner of (0,0); the bottom corner of
        // (0,0) is the upper left corner of (0,1)
        assertEquals(new HexVertex(hex(0, 0), 1), new HexVertex(hex(0, 1), 5));
        assertEquals(new HexVertex(hex(0, 0), 2), new HexVertex(hex(0, 1), 4));
    }

    @Test
    public void aHexHasSixDistinctVerticesAndSides() {
        Set<HexVertex> vertices = new HashSet<>();
        Set<HexSide> sides = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            vertices.add(new HexVertex(HexCoord.ORIGIN, i));
            sides.add(new HexSide(HexCoord.ORIGIN, i));
        }
        assertEquals(6, vertices.size());
        assertEquals(6, sides.size());
    }

    @Test
    public void aSideIsTheSameFromBothHexes() {
        HexCoord h = hex(-2, 1);
        for (int d = 0; d < 6; d++) {
            HexSide s = new HexSide(h, d);
            assertEquals(s, new HexSide(h.neighbour(d), (d + 3) % 6));
            assertTrue(s.direction() < 3);
        }
    }

    @Test
    public void theStandardCatanBoardHas54VerticesAnd72Sides() {
        // the 19 land hexes of the base game: 54 intersections and 72 paths
        Set<HexVertex> vertices = new HashSet<>();
        Set<HexSide> sides = new HashSet<>();
        for (HexCoord h : HexCoord.hexagon(2))
            for (int i = 0; i < 6; i++) {
                vertices.add(new HexVertex(h, i));
                sides.add(new HexSide(h, i));
            }
        assertEquals(54, vertices.size());
        assertEquals(72, sides.size());
    }
}
