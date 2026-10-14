package games.catan;

import games.catan.components.Building;
import games.catan.components.CatanTile;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class HexSeedTests {

    CatanForwardModel fm = new CatanForwardModel();

    @Test
    public void hexSeedFixesBoardAndHarbours() {
        CatanParameters params = new CatanParameters();
        params.setParameterValue("hexShuffleSeed", 39392);
        CatanGameState state = new CatanGameState(params, 4);
        fm.setup(state);
        List<CatanTile> boardCopy = copyTiles(state);
        for (int loop = 0; loop < 9; loop++) {
            params.setRandomSeed(loop);
            fm.setup(state);
            List<CatanTile> board = state.getTiles();
            assertEquals(boardCopy.size(), board.size());
            for (int i = 0; i < board.size(); i++) {
                assertEquals(boardCopy.get(i).hex, board.get(i).hex);
                assertEquals(boardCopy.get(i).getTileType(), board.get(i).getTileType());
                assertEquals(boardCopy.get(i).getNumber(), board.get(i).getNumber());
            }
        }
    }

    @Test
    public void noSeedRandomizesBoardAndHarbours() {
        CatanParameters params = new CatanParameters();
        CatanGameState state = new CatanGameState(params, 4);
        fm.setup(state);
        List<CatanTile> boardCopy = copyTiles(state);
        // the initial setup used the default seed of 0, so start from seed 1
        for (int loop = 1; loop < 10; loop++) {
            params.setRandomSeed(loop);
            fm.setup(state);
            List<CatanTile> board = state.getTiles();
            assertEquals(boardCopy.size(), board.size());
            int landTiles = 0;
            int numberMatches = 0;
            int typeMatches = 0;
            int bothMatch = 0;
            for (int i = 0; i < board.size(); i++) {
                assertEquals(boardCopy.get(i).hex, board.get(i).hex);
                if (board.get(i).getTileType() != CatanTile.TileType.SEA) {
                    landTiles++;
                    boolean numberMatch = boardCopy.get(i).getNumber() == board.get(i).getNumber();
                    boolean typeMatch = boardCopy.get(i).getTileType() == board.get(i).getTileType();
                    if (numberMatch) numberMatches++;
                    if (typeMatch) typeMatches++;
                    if (numberMatch && typeMatch) bothMatch++;
                }
            }
            // a random shuffle will leave some tiles matching, but nowhere near the whole board
            assertTrue("seed " + loop + ": " + numberMatches + " of " + landTiles + " numbers match", numberMatches <= landTiles / 2);
            assertTrue("seed " + loop + ": " + typeMatches + " of " + landTiles + " types match", typeMatches <= landTiles / 2);
            assertTrue("seed " + loop + ": " + bothMatch + " of " + landTiles + " tiles match", bothMatch <= landTiles / 4);
        }
    }

    @Test
    public void hexSeedGivesTheRecordedBoard() {
        // the land tiles in order of position (initial and number), then the harbours by the vertices of the tiles in
        // order of position; the same in every run, whatever the JVM has done before
        CatanParameters params = new CatanParameters();
        params.setParameterValue("hexShuffleSeed", 39392);
        CatanGameState state = new CatanGameState(params, 4);
        fm.setup(state);
        StringBuilder land = new StringBuilder(), harbours = new StringBuilder();
        for (CatanTile t : state.getTiles())
            if (t.getTileType() != CatanTile.TileType.SEA)
                land.append(t.getTileType().name().charAt(0)).append(t.getNumber()).append(' ');
        Set<Building> seen = new HashSet<>();
        for (CatanTile t : state.getTiles())
            for (Building b : state.getBuildings(t))
                if (b.getHarbour() != null && seen.add(b))
                    harbours.append(b.getHarbour().name().charAt(0));
        assertEquals("H3 F4 H6 M8 M11 P5 P11 F12 M9 H2 D0 F8 F4 P10 F10 F9 F3 F6 P5 ", land.toString());
        assertEquals("WWBBWWWLWLOOWWWWGG", harbours.toString());
    }

    private static List<CatanTile> copyTiles(CatanGameState state) {
        List<CatanTile> retValue = new ArrayList<>();
        for (CatanTile tile : state.getTiles())
            retValue.add(tile.copy());
        return retValue;
    }

    @Test
    public void diceAreNotPredictable() {
        CatanParameters params = new CatanParameters();
        params.setParameterValue("diceSeed", -1);
        params.setRandomSeed(39392);
        CatanGameState state = new CatanGameState(params, 4);
        fm.setup(state);

        int[] diceRolls = new int[20];
        for (int loop = -0; loop < 20; loop++) {
            fm.rollDiceAndAllocateResources(state, params);
            diceRolls[loop] = state.getRollValue();
        }

        // Now we repeat this with a different seed
        params.setRandomSeed(392);
        fm.setup(state);
        int matches = 0;
        for (int loop = 0; loop < 20; loop++) {
            fm.rollDiceAndAllocateResources(state, params);
          //  System.out.println("Checking " + diceRolls[loop] + " vs " + state.getRollValue());
            if (diceRolls[loop] == state.getRollValue()) {
                matches++;
            }
        }
        assertEquals(0, matches, 2);
    }

    @Test
    public void fixingDiceSeedFixesDice() {
        CatanParameters params = new CatanParameters();
        params.setParameterValue("diceSeed", 39392);
        params.setRandomSeed(39392);
        CatanGameState state = new CatanGameState(params, 4);
        fm.setup(state);

        int[] diceRolls = new int[20];
        for (int loop = -0; loop < 20; loop++) {
            fm.rollDiceAndAllocateResources(state, params);
            diceRolls[loop] = state.getRollValue();
        }

        // Now we repeat this with the same dice seed, but different random seed
        params.setRandomSeed(392);
        fm.setup(state);
        for (int loop = 0; loop < 20; loop++) {
            fm.rollDiceAndAllocateResources(state, params);
            assertEquals(diceRolls[loop], state.getRollValue());
        }

        // Now we repeat this with a different seed
        params.setRandomSeed(392);
        params.setParameterValue("diceSeed", 427858);
        fm.setup(state);
        int matches = 0;
        for (int loop = 0; loop < 20; loop++) {
            fm.rollDiceAndAllocateResources(state, params);
            if (diceRolls[loop] == state.getRollValue()) {
                matches++;
            }
            assertEquals(0, matches, 2);
        }
    }
}
