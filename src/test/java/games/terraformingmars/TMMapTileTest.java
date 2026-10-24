package games.terraformingmars;

import core.Game;
import games.GameType;
import games.terraformingmars.actions.ReserveTile;
import games.terraformingmars.components.TMMapTile;
import org.junit.Test;

import static org.junit.Assert.*;

public class TMMapTileTest {

    Game game = GameType.TerraformingMars.createGameInstance(2, 3);
    TMGameState state = (TMGameState) game.getGameState();

    // an empty ground space on Mars with something printed on it
    TMMapTile groundWithResources() {
        for (TMMapTile mt : state.getBoard().asMap().values())
            if (mt.getTileType() == TMTypes.MapTileType.Ground && mt.getTilePlaced() == null && mt.getResources().length > 0)
                return mt;
        throw new AssertionError("no such space");
    }

    @Test
    public void placingATileReplacesTheSpace() {
        TMMapTile mt = groundWithResources();
        int player = state.getCurrentPlayer();
        TMTypes.Resource res = mt.getResources()[0];
        int before = state.getPlayerResources()[player].get(res).getValue();
        // getComponentById would give the space as it was first seen
        state.getComponentById(mt.getComponentID());

        assertTrue(state.placeTile(mt, TMTypes.Tile.City));

        TMMapTile placed = state.getTile(mt.getX(), mt.getY());
        assertNotSame(mt, placed);
        assertNull(mt.getTilePlaced());
        assertEquals(TMTypes.Tile.City, placed.getTilePlaced());
        assertEquals(player, placed.getOwnerId());
        assertEquals(mt.getComponentID(), placed.getComponentID());
        assertSame(placed, state.getMapTile(mt.getComponentID()));
        assertEquals(1, state.getPlayerTilesPlaced()[player].get(TMTypes.Tile.City).getValue());
        assertTrue(state.getPlayerResources()[player].get(res).getValue() > before);
        // a second tile cannot go there
        assertFalse(state.placeTile(placed, TMTypes.Tile.Greenery));
    }

    @Test
    public void reservingASpaceReplacesIt() {
        TMMapTile mt = groundWithResources();
        new ReserveTile(state.getCurrentPlayer(), mt.getComponentID(), true).execute(state);
        assertEquals(state.getCurrentPlayer(), state.getTile(mt.getX(), mt.getY()).getReserved());
        assertFalse(mt.isReserved());
    }

    @Test
    public void placingOffMarsReplacesTheSpaceInPlace() {
        TMMapTile first = state.getExtraTiles().get(0);
        TMMapTile second = state.getExtraTiles().get(1);
        assertTrue(state.placeTile(first, TMTypes.Tile.City));
        assertEquals(TMTypes.Tile.City, state.getExtraTiles().get(0).getTilePlaced());
        assertEquals(first.getComponentID(), state.getExtraTiles().get(0).getComponentID());
        assertSame(second, state.getExtraTiles().get(1));
        // a state whose spaces have changed still equals its copy
        assertEquals(state, state.copy());
    }

    @Test
    public void copiesAreEqualAndIndependent() {
        TMGameState copy = (TMGameState) state.copy();
        assertEquals(state.getBoard(), copy.getBoard());
        TMMapTile mt = groundWithResources();
        copy.placeTile(copy.getMapTile(mt.getComponentID()), TMTypes.Tile.Greenery);
        assertNull(state.getTile(mt.getX(), mt.getY()).getTilePlaced());
        assertEquals(TMTypes.Tile.Greenery, copy.getTile(mt.getX(), mt.getY()).getTilePlaced());
        assertNotEquals(state.getBoard(), copy.getBoard());
    }
}
