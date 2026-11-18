package games.ventlife;

import core.actions.AbstractAction;
import games.ventlife.actions.PlaceTile;
import games.ventlife.components.Hex;
import games.ventlife.components.HexCell;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Tile placement: the first tile, seafloor expansion, plateau formation and what a placed tile records.
 * Directions (axial, clockwise from east): 0 E(1,0) 1 SE(0,1) 2 SW(-1,1) 3 W(-1,0) 4 NW(0,-1) 5 NE(1,-1).
 * A tile (S, o) covers S (Black Smoker), its left terrain at S + dir(o+1) and its right terrain at S + dir(o).
 */
public class VentlifeTilePlacementTest {

    VentlifeGameState state;
    VentlifeForwardModel fm;

    @Before
    public void setup() {
        state = newState(2, 42);
        fm = new VentlifeForwardModel();
    }

    private Set<AbstractAction> legal() {
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        Set<AbstractAction> retValue = new HashSet<>(actions);
        assertEquals("duplicate actions in " + actions, actions.size(), retValue.size());
        return retValue;
    }

    /**
     * Tile 0: B/D at ((0,0), 1) -> (0,0) Smoker, (-1,1) Basalt, (0,1) Diffuse.
     * Tile 1: D/M at ((1,0), 0) -> (1,0) Smoker, (1,1) Diffuse (left, SE), (2,0) Mat (right, E).
     */
    private void placeTwoSeafloorTiles() {
        place(state, fm, BASALT, DIFFUSE, hex(0, 0), 1);
        place(state, fm, DIFFUSE, MAT, hex(1, 0), 0);
    }

    @Test
    public void firstTileOffersOnlyTheSixOrientationsWithItsSmokerAtTheOrigin() {
        Set<AbstractAction> expected = new HashSet<>();
        for (int o = 0; o < 6; o++)
            expected.add(placeTile(state, hex(0, 0), o));
        assertEquals(expected, legal());
    }

    @Test
    public void firstTilePutsItsTerrainsInTheRightPlacesOnTheSeafloor() {
        VentlifeGameState other = (VentlifeGameState) state.copy();

        // orientation 1: right at SE (0,1), left at SW (-1,1) - the printed picture
        place(state, fm, BASALT, DIFFUSE, hex(0, 0), 1);
        assertEquals(Map.of(hex(0, 0), new HexCell(SMOKER, 1, 0),
                hex(-1, 1), new HexCell(BASALT, 1, 0),
                hex(0, 1), new HexCell(DIFFUSE, 1, 0)), state.getField());
        assertEquals(1, state.getTilesPlaced());

        // orientation 4: right at NW (0,-1), left at NE (1,-1) - the tile turned upside down
        place(other, fm, DIFFUSE, MAT, hex(0, 0), 4);
        assertEquals(Map.of(hex(0, 0), new HexCell(SMOKER, 1, 0),
                hex(1, -1), new HexCell(DIFFUSE, 1, 0),
                hex(0, -1), new HexCell(MAT, 1, 0)), other.getField());
    }

    @Test
    public void seafloorPlacementsMustTouchTheFieldAlongAnEdgeWithoutOverlapping() {
        place(state, fm, BASALT, DIFFUSE, hex(0, 0), 1);
        // field: (0,0), (-1,1), (0,1); one tile only, so no plateau is possible yet
        Set<AbstractAction> actions = legal();

        // (1,0) E of the Smoker: (1,0), (1,1), (2,0) all uncovered, (1,0) shares an edge with (0,0)
        assertTrue(actions.contains(placeTile(state, hex(1, 0), 0)));
        // (2,0), (2,1), (3,0): nearest covered hexes (0,0) and (0,1) are 2 steps away - not touching
        assertFalse(actions.contains(placeTile(state, hex(2, 0), 0)));
        // (1,0), (0,0) [left, W], (0,1) [right, SW]: overlaps two covered positions
        assertFalse(actions.contains(placeTile(state, hex(1, 0), 2)));
        // (1,0), (1,-1) [left, NW], (0,0) [right, W]: overlaps the Smoker, though touching the field
        assertFalse(actions.contains(placeTile(state, hex(1, 0), 3)));
        // a second tile is not limited to the origin
        assertFalse(actions.contains(placeTile(state, hex(0, 0), 0)));

        Set<Hex> covered = Set.of(hex(0, 0), hex(-1, 1), hex(0, 1));
        assertEquals(new HashSet<AbstractAction>(seafloorPlacements(heldTile(state), covered)), actions);
    }

    @Test
    public void seafloorTileIsLevelOneAndRecordsItsTileId() {
        placeTwoSeafloorTiles();
        assertEquals(new HexCell(SMOKER, 1, 1), state.getCell(hex(1, 0)));
        assertEquals(new HexCell(DIFFUSE, 1, 1), state.getCell(hex(1, 1)));
        assertEquals(new HexCell(MAT, 1, 1), state.getCell(hex(2, 0)));
        assertEquals(new HexCell(SMOKER, 1, 0), state.getCell(hex(0, 0)));
        assertEquals(6, state.getField().size());
        assertEquals(2, state.getTilesPlaced());
    }

    @Test
    public void plateauNeedsSmokerOnSmokerFullSupportAtOneLevelAndTwoTilesBeneath() {
        placeTwoSeafloorTiles();
        // covered: tile 0 (0,0)S (-1,1) (0,1); tile 1 (1,0)S (1,1) (2,0); all level 1
        Set<AbstractAction> actions = legal();

        // Smoker (0,0): o=0 covers (0,1) [tile 0] and (1,0) [tile 1] - two tiles: legal
        PlaceTile a = placeTile(state, hex(0, 0), 0);
        // Smoker (1,0): o=1 covers (1,1) [tile 1] and (0,1) [tile 0]: legal
        PlaceTile b = placeTile(state, hex(1, 0), 1);
        // Smoker (1,0): o=2 covers (0,1) and (0,0) [both tile 0], Smoker on tile 1: legal
        PlaceTile c = placeTile(state, hex(1, 0), 2);
        assertTrue(actions.containsAll(List.of(a, b, c)));

        // exactly on top of tile 0: all three beneath are one tile
        assertFalse(actions.contains(placeTile(state, hex(0, 0), 1)));
        // exactly on top of tile 1 ((1,0), o=0 covers (1,1), (2,0))
        assertFalse(actions.contains(placeTile(state, hex(1, 0), 0)));
        // Smoker on Diffuse (0,1): o=5 covers (1,1) [left, E] and (1,0) [right, NE] - level 1, tiles 0 and 1
        assertFalse(actions.contains(placeTile(state, hex(0, 1), 5)));
        // Smoker (1,0), o=3: right W (0,0) covered, left NW (1,-1) uncovered - unsupported
        assertFalse(actions.contains(placeTile(state, hex(1, 0), 3)));

        // every other legal action is a seafloor placement
        Set<Hex> covered = Set.of(hex(0, 0), hex(-1, 1), hex(0, 1), hex(1, 0), hex(1, 1), hex(2, 0));
        Set<AbstractAction> expected = new HashSet<>(seafloorPlacements(heldTile(state), covered));
        expected.addAll(List.of(a, b, c));
        assertEquals(expected, actions);
    }

    @Test
    public void plateauTileIsOneLevelUpAndRecordsItsTileId() {
        placeTwoSeafloorTiles();
        // tile 2: M/B at ((0,0), 0): left SE (0,1) Mat, right E (1,0) Basalt, all at level 1 + 1
        place(state, fm, MAT, BASALT, hex(0, 0), 0);
        assertEquals(new HexCell(SMOKER, 2, 2), state.getCell(hex(0, 0)));
        assertEquals(new HexCell(MAT, 2, 2), state.getCell(hex(0, 1)));
        assertEquals(new HexCell(BASALT, 2, 2), state.getCell(hex(1, 0)));
        // uncovered parts of tiles 0 and 1 stay as they were
        assertEquals(new HexCell(BASALT, 1, 0), state.getCell(hex(-1, 1)));
        assertEquals(new HexCell(DIFFUSE, 1, 1), state.getCell(hex(1, 1)));
        assertEquals(new HexCell(MAT, 1, 1), state.getCell(hex(2, 0)));
        // no new positions: the field still has the 6 covered by the two seafloor tiles
        assertEquals(6, state.getField().size());
        assertEquals(3, state.getTilesPlaced());
    }

    @Test
    public void overhangIsIllegalUntilTheLevelsMatchThenAThirdLevelIsBuilt() {
        placeTwoSeafloorTiles();
        place(state, fm, MAT, BASALT, hex(0, 0), 0);   // tile 2: (0,0)S (0,1) (1,0) at level 2
        // ((0,0), 1): (0,0) level 2, left SW (-1,1) level 1, right SE (0,1) level 2 - levels differ
        PlaceTile overhang = placeTile(state, hex(0, 0), 1);
        assertFalse(legal().contains(overhang));

        // tile 3: B/M at ((-1,0), 2): (-1,0) Smoker, left W (-2,0) Basalt, right SW (-2,1) Mat - seafloor next to (0,0)
        place(state, fm, BASALT, MAT, hex(-1, 0), 2);
        assertEquals(new HexCell(BASALT, 1, 3), state.getCell(hex(-2, 0)));
        // tile 4: D/B at ((-1,0), 1): left SW (-2,1) [tile 3], right SE (-1,1) [tile 0], Smoker on tile 3's Smoker
        place(state, fm, DIFFUSE, BASALT, hex(-1, 0), 1);
        assertEquals(new HexCell(SMOKER, 2, 4), state.getCell(hex(-1, 0)));
        assertEquals(new HexCell(DIFFUSE, 2, 4), state.getCell(hex(-2, 1)));
        assertEquals(new HexCell(BASALT, 2, 4), state.getCell(hex(-1, 1)));

        // now (0,0) [tile 2 Smoker], (-1,1) [tile 4], (0,1) [tile 2] are all level 2: the same placement is legal
        assertTrue(legal().contains(placeTile(state, hex(0, 0), 1)));
        // tile 5: M/D: left SW (-1,1) Mat, right SE (0,1) Diffuse, level 2 + 1
        place(state, fm, MAT, DIFFUSE, hex(0, 0), 1);
        assertEquals(new HexCell(SMOKER, 3, 5), state.getCell(hex(0, 0)));
        assertEquals(new HexCell(MAT, 3, 5), state.getCell(hex(-1, 1)));
        assertEquals(new HexCell(DIFFUSE, 3, 5), state.getCell(hex(0, 1)));
        assertEquals(new HexCell(BASALT, 2, 2), state.getCell(hex(1, 0)));
        // positions: tiles 0, 1 (6) + tile 3's (-1,0), (-2,0), (-2,1) = 9
        assertEquals(9, state.getField().size());
        assertEquals(6, state.getTilesPlaced());
    }

    @Test
    public void placeTileDescriptionNamesTheTileItsPositionAndOrientation() {
        giveTile(state, 0, BASALT, DIFFUSE);
        String s = placeTile(state, hex(0, 0), 4).getString(state);
        assertTrue(s, s.contains("Basalt Ridge"));
        assertTrue(s, s.contains("Diffuse Vents"));
        assertTrue(s, s.contains("(0,0)"));
        assertTrue(s, s.contains("4"));
    }

    @Test
    public void placingATileThePlayerDoesNotHoldIsRejected() {
        giveTile(state, 0, BASALT, DIFFUSE);
        PlaceTile wrong = new PlaceTile(tile(MAT, MAT), hex(0, 0), 1);
        assertThrows(IllegalStateException.class, () -> wrong.execute(state));
        assertTrue(state.getField().isEmpty());
        assertEquals(List.of(tile(BASALT, DIFFUSE)), state.getHand(0).getComponents());
    }
}
