package games.ventlife;

import games.ventlife.components.Species;
import games.ventlife.components.VentTile;
import org.junit.Test;

import java.util.*;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

public class VentlifeSetupTest {

    @Test
    public void tilesInPlayFollowTheDistributionForEachPlayerCount() {
        // tiles.json columns 2P / 3P / 4P; a game uses every column up to its player count
        // mixed kinds: B/D 5,2,1  B/M 5,2,1  D/B 5,1,2  D/M 5,2,1  M/B 5,1,2  M/D 5,1,2; doubles 2,1,1 each
        Map<VentTile, Integer> two = Map.of(tile(BASALT, DIFFUSE), 5, tile(BASALT, MAT), 5, tile(DIFFUSE, BASALT), 5,
                tile(DIFFUSE, MAT), 5, tile(MAT, BASALT), 5, tile(MAT, DIFFUSE), 5,
                tile(BASALT, BASALT), 2, tile(DIFFUSE, DIFFUSE), 2, tile(MAT, MAT), 2);           // 30 + 6 = 36
        Map<VentTile, Integer> three = Map.of(tile(BASALT, DIFFUSE), 7, tile(BASALT, MAT), 7, tile(DIFFUSE, BASALT), 6,
                tile(DIFFUSE, MAT), 7, tile(MAT, BASALT), 6, tile(MAT, DIFFUSE), 6,
                tile(BASALT, BASALT), 3, tile(DIFFUSE, DIFFUSE), 3, tile(MAT, MAT), 3);           // 39 + 9 = 48
        Map<VentTile, Integer> four = Map.of(tile(BASALT, DIFFUSE), 8, tile(BASALT, MAT), 8, tile(DIFFUSE, BASALT), 8,
                tile(DIFFUSE, MAT), 8, tile(MAT, BASALT), 8, tile(MAT, DIFFUSE), 8,
                tile(BASALT, BASALT), 4, tile(DIFFUSE, DIFFUSE), 4, tile(MAT, MAT), 4);           // 48 + 12 = 60
        List<Map<VentTile, Integer>> expected = List.of(two, three, four);
        int[] totals = {36, 48, 60};
        for (int n = 2; n <= 4; n++) {
            VentlifeGameState state = newState(n, 11);
            assertEquals("tile composition at " + n + " players", expected.get(n - 2), unplacedTiles(state));
            for (int p = 0; p < n; p++)
                assertEquals("one tile in hand of player " + p, 1, state.getHand(p).getSize());
            assertEquals(totals[n - 2] - n, state.getDrawDeck().getSize());
            assertTrue(state.getField().isEmpty());
            assertEquals(0, state.getTilesPlaced());
            assertEquals(0, state.getCurrentPlayer());
            assertEquals(VentlifeGameState.Phase.PLACE_TILE, state.getGamePhase());
        }
    }

    @Test
    public void fourDistinctRandomSpeciesWithSixTokensEachAndNoneOfTheOthers() {
        Set<Set<Species>> seen = new HashSet<>();
        for (long seed = 0; seed < 20; seed++) {
            VentlifeGameState state = newState(3, seed);
            Set<Species> inPlay = new HashSet<>(state.getSpeciesInPlay());
            assertEquals("4 distinct species", 4, state.getSpeciesInPlay().size());
            assertEquals("4 distinct species", 4, inPlay.size());
            for (int p = 0; p < 3; p++)
                for (Species s : Species.values())
                    assertEquals(s + " tokens of player " + p, inPlay.contains(s) ? 6 : 0, state.getSupply(p, s));
            seen.add(inPlay);
        }
        // 35 possible sets of 4 from 7: 20 seeds giving a single set would mean no random choice
        assertTrue("species choice does not vary with the seed", seen.size() > 1);
    }

    @Test
    public void drawDeckIsShuffled() {
        // two set-ups differing only in seed: with 34 tiles of 9 kinds, the same order by chance is negligible
        VentlifeGameState a = newState(2, 1);
        VentlifeGameState b = newState(2, 2);
        List<VentTile> deckA = new ArrayList<>(a.getDrawDeck().getComponents());
        deckA.add(a.getHand(0).get(0));
        deckA.add(a.getHand(1).get(0));
        List<VentTile> deckB = new ArrayList<>(b.getDrawDeck().getComponents());
        deckB.add(b.getHand(0).get(0));
        deckB.add(b.getHand(1).get(0));
        assertNotEquals(deckA, deckB);
    }
}
