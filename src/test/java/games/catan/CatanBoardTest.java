package games.catan;

import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.components.Edge;
import core.components.grid.HexCoord;
import core.components.grid.HexVertex;
import games.catan.actions.robber.MoveRobber;
import games.catan.actions.robber.MoveRobberAndSteal;
import games.catan.actions.setup.DeepPlaceSettlementThenRoad;
import games.catan.actions.setup.PlaceSettlementWithRoad;
import games.catan.components.Building;
import games.catan.components.CatanTile;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.catan.CatanGameState.CatanGamePhase.Setup;
import static games.catan.components.CatanTile.TileType.DESERT;
import static games.catan.components.CatanTile.TileType.SEA;
import static org.junit.Assert.*;

/**
 * The standard board: 19 land tiles within two steps of the centre, ringed by 18 sea tiles.
 */
public class CatanBoardTest {

    CatanParameters params;
    CatanGameState state;
    CatanForwardModel fm;

    @Before
    public void setup() {
        params = new CatanParameters();
        params.setRandomSeed(7);
        state = new CatanGameState(params, 3);
        fm = new CatanForwardModel();
        fm.setup(state);
    }

    @Test
    public void landIsWithinTwoStepsOfTheCentreAndTheSeaRingsIt() {
        List<CatanTile> tiles = state.getTiles();
        assertEquals(37, tiles.size());
        int land = 0, deserts = 0;
        for (CatanTile tile : tiles) {
            int distance = tile.hex.distance(HexCoord.ORIGIN);
            assertTrue(distance <= 3);
            assertEquals(tile + " is sea", distance == 3, tile.getTileType() == SEA);
            if (distance < 3) land++;
            if (tile.getTileType() == DESERT) {
                deserts++;
                assertTrue("the robber starts on the desert", state.hasRobber(tile));
            }
            assertSame(tile, state.getTile(tile.hex));
        }
        assertEquals(19, land);
        assertEquals(1, deserts);
        assertNull(state.getTile(new HexCoord(4, 0)));
    }

    @Test
    public void tilesShareTheBuildingsAtTheirVertices() {
        int checks = 0;
        for (CatanTile tile : state.getTiles())
            for (int v = 0; v < 6; v++) {
                HexCoord[] others = tile.getNeighboursOnVertex(v);
                // the vertex is vertex v + 2 of the first other hex and v + 4 of the second
                for (int k = 0; k < 2; k++) {
                    CatanTile other = state.getTile(others[k]);
                    if (other == null) continue;
                    assertSame(state.getBuilding(tile, v), state.getBuilding(other, (v + 2 * (k + 1)) % 6));
                    checks++;
                }
            }
        assertTrue(checks > 0);
        // 37 hexes have 6 * 37 = 222 corners: the vertices inside the hexagon of radius 3 are shared by three hexes
        // and those on its edge by one or two, giving 96 vertices as on any hexagon of 37 hexes
        assertEquals(96, state.getSettlements().size());
    }

    @Test
    public void landTilesShareTheRoadsAlongTheirSides() {
        int checks = 0;
        for (CatanTile tile : state.getTiles()) {
            if (tile.getTileType() == SEA || tile.getTileType() == DESERT) continue;
            for (int side = 0; side < 6; side++) {
                Edge road = state.getRoad(tile, side, side);
                assertNotNull(tile + " side " + side, road);
                // the road joins the buildings at the ends of the side, and the tile across has it on the opposite side
                assertNotNull(state.getBuilding(tile, (side + 1) % 6).getEdgeByID(road.getComponentID()));
                CatanTile across = state.getTile(tile.hex.neighbour(side));
                assertEquals(road.getComponentID(), across.getEdgeID((side + 3) % 6));
                checks++;
            }
        }
        // the 18 land tiles other than the desert
        assertEquals(18 * 6, checks);
    }

    @Test
    public void harboursAreInPairsOnTheCoast() {
        // each harbour is on two buildings at neighbouring vertices of a sea tile, on the coast
        Map<CatanParameters.Resource, Integer> buildingsWithHarbour = new HashMap<>();
        for (CatanTile tile : state.getTiles())
            for (int v = 0; v < 6; v++) {
                Building b = state.getBuilding(tile, v);
                if (b.getHarbour() == null || new HexVertex(tile.hex, v).corner() != v) continue;
                buildingsWithHarbour.merge(b.getHarbour(), 1, Integer::sum);
                boolean land = false, paired = false;
                for (CatanTile t : tilesAt(new HexVertex(tile.hex, v))) {
                    land |= t.getTileType() != SEA;
                    if (t.getTileType() != SEA) continue;
                    for (int k = 0; k < 6; k++)
                        if (state.getBuilding(t, k) == b)
                            paired |= state.getBuilding(t, (k + 1) % 6).getHarbour() == b.getHarbour()
                                    || state.getBuilding(t, (k + 5) % 6).getHarbour() == b.getHarbour();
                }
                assertTrue(b + " is on the coast", land);
                assertTrue(b + " is one of a pair", paired);
            }
        Map<CatanParameters.Resource, Integer> expected = new HashMap<>();
        params.harborCount.forEach((r, n) -> expected.put(r, 2 * n));
        assertEquals(expected, buildingsWithHarbour);
    }

    @Test
    public void theRobberStopsItsTileProducing() {
        // a settlement of player 0 on the first numbered tile, at a vertex where no other tile has the same number
        CatanTile tile = null;
        int vertex = -1;
        search:
        for (CatanTile t : state.getTiles()) {
            if (t.getNumber() == 0) continue;
            for (int v = 0; v < 6; v++) {
                int same = 0;
                for (CatanTile o : tilesAt(new HexVertex(t.hex, v)))
                    if (o.getNumber() == t.getNumber()) same++;
                if (same == 1) {
                    tile = t;
                    vertex = v;
                    break search;
                }
            }
        }
        assertNotNull(tile);
        state.getBuilding(tile, vertex).setOwnerId(0);
        CatanParameters.Resource produced = params.productMapping.get(tile.getTileType());

        state.setRobber(tile.hex);
        assertEquals(0, gainOnRoll(tile.getNumber(), produced));
        state.setRobber(tile.hex.neighbour(HexCoord.E).neighbour(HexCoord.E).neighbour(HexCoord.E));
        assertEquals(1, gainOnRoll(tile.getNumber(), produced));
    }

    @Test
    public void movingTheRobberPutsItOnTheTile() {
        HexCoord from = state.getRobber().hex, to = from.equals(HexCoord.ORIGIN) ? new HexCoord(1, 0) : HexCoord.ORIGIN;
        new MoveRobberAndSteal(to, 0, -1).execute(state);
        assertEquals(to, state.getRobber().hex);
        assertFalse(state.hasRobber(state.getTile(from)));
        new MoveRobber(from, 0).execute(state);
        assertEquals(from, state.getRobber().hex);
    }

    /**
     * Rolls until the dice give the number, returning what player 0 gains of the resource on that roll.
     */
    private int gainOnRoll(int number, CatanParameters.Resource resource) {
        for (int i = 0; i < 1000; i++) {
            int before = state.getPlayerResources(0).get(resource).getValue();
            fm.rollDiceAndAllocateResources(state, params);
            if (state.getRollValue() == number)
                return state.getPlayerResources(0).get(resource).getValue() - before;
        }
        throw new AssertionError("never rolled " + number);
    }

    @Test
    public void theSecondSettlementGivesTheResourcesOfTheTilesAroundIt() {
        PlaceSettlementWithRoad place = (PlaceSettlementWithRoad) secondRoundPlacement(ActionSpace.Default);
        int player = state.getCurrentPlayer();
        Map<CatanParameters.Resource, Integer> before = resources(player);
        fm.next(state, place);
        assertEquals(expectedGain(before, new HexVertex(place.hex, place.vertex)), resources(player));
    }

    @Test
    public void theSecondSettlementGivesTheResourcesOfTheTilesAroundItInTheDeepActionSpace() {
        DeepPlaceSettlementThenRoad place = (DeepPlaceSettlementThenRoad) secondRoundPlacement(new ActionSpace(ActionSpace.Structure.Deep));
        int player = state.getCurrentPlayer();
        Map<CatanParameters.Resource, Integer> before = resources(player);
        fm.next(state, place);
        assertTrue(state.isActionInProgress());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(expectedGain(before, new HexVertex(place.hex, place.vertex)), resources(player));
    }

    /**
     * Plays the first round of setup with the first legal placement of each player, then returns the first placement
     * in the action space, of the next player, whose vertex touches three land tiles.
     */
    private AbstractAction secondRoundPlacement(ActionSpace actionSpace) {
        while (state.getRoundCounter() == 0) {
            assertEquals(Setup, state.getGamePhase());
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        // CatanForwardModel offers the deep action space only through this method
        for (AbstractAction a : fm._computeAvailableActions(state, actionSpace)) {
            PlaceSettlementWithRoad p = (PlaceSettlementWithRoad) a;
            int land = 0;
            for (CatanTile t : tilesAt(new HexVertex(p.hex, p.vertex)))
                if (t.getTileType() != SEA) land++;
            if (land == 3)
                return a;
        }
        throw new AssertionError("no placement touching three land tiles");
    }

    private List<CatanTile> tilesAt(HexVertex vertex) {
        List<CatanTile> retValue = new ArrayList<>();
        for (CatanTile t : state.getTiles())
            for (int v = 0; v < 6; v++)
                if (new HexVertex(t.hex, v).equals(vertex))
                    retValue.add(t);
        assertEquals(3, retValue.size());
        return retValue;
    }

    private Map<CatanParameters.Resource, Integer> expectedGain(Map<CatanParameters.Resource, Integer> before, HexVertex vertex) {
        Map<CatanParameters.Resource, Integer> expected = new HashMap<>(before);
        for (CatanTile t : tilesAt(vertex)) {
            CatanParameters.Resource res = params.productMapping.get(t.getTileType());
            if (res != null)
                expected.merge(res, 1, Integer::sum);
        }
        return expected;
    }

    private Map<CatanParameters.Resource, Integer> resources(int player) {
        Map<CatanParameters.Resource, Integer> retValue = new HashMap<>();
        state.getPlayerResources(player).forEach((r, c) -> retValue.put(r, c.getValue()));
        return retValue;
    }
}
