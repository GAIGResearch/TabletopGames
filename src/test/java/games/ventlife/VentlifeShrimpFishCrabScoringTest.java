package games.ventlife;

import games.ventlife.components.Terrain;
import org.junit.Test;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Scoring of Blind Vent Shrimp (terrains around each), Eelpout Fish (shoals) and Yeti Crabs (Elevated Plateaus), with
 * creatures put directly on a field: the standard field (real tiles) or a field written cell by cell (putCell).
 */
public class VentlifeShrimpFishCrabScoringTest {

    VentlifeGameState state;
    VentlifeForwardModel fm = new VentlifeForwardModel();

    /**
     * The six-tile standard field plus a seventh tile B/B at ((-2,-1),3): (-2,-1) S1, left NW (-2,-2) B1, right W
     * (-3,-1) B1. Only Shrimp in play.
     */
    private void shrimpField(VentlifeParameters params) {
        state = newState(2, 42, params);
        useSpecies(state);
        buildStandardField(state, fm, 6);
        place(state, fm, BASALT, BASALT, hex(-2, -1), 3);
        useSpecies(state, SHRIMP);
    }

    /**
     * Shrimp around the standard field (own hex not counted; Smokers and Basalt count for nothing):
     * player 0: (1,1) next to (0,1) D, (1,0) B, (2,0) M -> both = 5; (2,0) next to (1,1) D, (1,0) B -> one = 2;
     * (-2,-1) next to (-2,0) B, (-3,-1) B, (-2,-2) B -> neither = 0. Player 1: (-2,1) D2 next to (-1,1) M,
     * (-2,0) B, (-1,0) S -> one = 2.
     */
    private void arrangeShrimp() {
        putCreature(state, hex(1, 1), SHRIMP, 0, 1);
        putCreature(state, hex(2, 0), SHRIMP, 0, 1);
        putCreature(state, hex(-2, -1), SHRIMP, 0, 1);
        putCreature(state, hex(-2, 1), SHRIMP, 1, 1);
    }

    @Test
    public void eachShrimpScoresForDiffuseVentsAndMicrobialMatAroundIt() {
        shrimpField(null);
        arrangeShrimp();
        // player 0: 5 + 2 + 0; player 1: 2
        assertEquals(7, state.speciesScore(0, SHRIMP));
        assertEquals(2, state.speciesScore(1, SHRIMP));
        assertEquals(7, state.getGameScore(0), 0);
        assertEquals(2, state.getGameScore(1), 0);
    }

    @Test
    public void shrimpPointParametersSetTheOneAndBothTerrainScores() {
        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("shrimpPointsOneTerrain", 3);
        params.setParameterValue("shrimpPointsBothTerrains", 7);
        shrimpField(params);
        arrangeShrimp();
        // player 0: 7 + 3 + 0; player 1: 3
        assertEquals(10, state.speciesScore(0, SHRIMP));
        assertEquals(3, state.speciesScore(1, SHRIMP));
    }

    /** A row of level-1 Microbial Mat hexes (q, 0) for q = 0 .. length - 1, with only Fish in play. */
    private void fishRow(VentlifeParameters params, int length) {
        state = newState(2, 42, params);
        useSpecies(state, FISH);
        for (int q = 0; q < length; q++)
            putCell(state, hex(q, 0), MAT, 1);
    }

    @Test
    public void aShoalOfNFishScoresFiveTimesNMinusOneAndASingleFishNothing() {
        // rulebook 7.3: 1 -> 0, 2 -> 5, 3 -> 10, 4 -> 15, 5 -> 20, 6 -> 25
        int[] expected = {0, 5, 10, 15, 20, 25};
        for (int n = 1; n <= 6; n++) {
            fishRow(null, 6);
            for (int q = 0; q < n; q++)
                putCreature(state, hex(q, 0), FISH, 0, 1);
            assertEquals("shoal of " + n, expected[n - 1], state.speciesScore(0, FISH));
            assertEquals(0, state.speciesScore(1, FISH));
        }
    }

    @Test
    public void shoalsScoreSeparatelyAndAnotherPlayersFishSplitsAShoal() {
        fishRow(null, 7);
        // row q = 0..6: P0 P0 P0 P1 P0 P0 P1
        for (int q : new int[]{0, 1, 2, 4, 5})
            putCreature(state, hex(q, 0), FISH, 0, 1);
        putCreature(state, hex(3, 0), FISH, 1, 1);
        putCreature(state, hex(6, 0), FISH, 1, 1);
        // and a pair of player 1's apart from the row (two steps away from it)
        putCell(state, hex(0, 2), MAT, 1);
        putCell(state, hex(1, 2), MAT, 1);
        putCreature(state, hex(0, 2), FISH, 1, 1);
        putCreature(state, hex(1, 2), FISH, 1, 1);
        // player 0: shoals {0,1,2} = 5 x 2 and {4,5} = 5 x 1 -> 15
        // player 1: (3,0) and (6,0) single -> 0 each; the pair -> 5
        assertEquals(15, state.speciesScore(0, FISH));
        assertEquals(5, state.speciesScore(1, FISH));
        assertEquals(15, state.getGameScore(0), 0);
    }

    @Test
    public void shoalPointsParameterScalesEveryShoal() {
        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("shoalPoints", 3);
        fishRow(params, 4);
        for (int q = 0; q < 4; q++)
            putCreature(state, hex(q, 0), FISH, 0, 1);
        // 3 x (4 - 1)
        assertEquals(9, state.speciesScore(0, FISH));
    }

    private static VentlifeParameters smokersForAll() {
        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("smokersOnlyForWormsAndShrimp", false);
        return params;
    }

    /**
     * Crabs on the standard field at 3 players (Smokers open to Crabs, so every plateau hex may hold one). Elevated
     * Plateaus: level 3 {(0,0) S, (-1,1) M, (0,1) D} size 3; level 2 {(1,0)} size 1 (its neighbours (0,0), (0,1) are
     * level 3, the others level 1 or uncovered); level 2 {(-1,0), (-2,1)} size 2 (adjacent; their other neighbours
     * level 1, 3 or uncovered).
     */
    @Test
    public void crabsScoreEachElevatedPlateauOfOneConnectedLevel() {
        state = newState(3, 42, smokersForAll());
        useSpecies(state);
        buildStandardField(state, fm, 6);
        useSpecies(state, CRAB);
        putCreature(state, hex(-1, 1), CRAB, 0, 1);
        putCreature(state, hex(0, 1), CRAB, 0, 1);
        putCreature(state, hex(0, 0), CRAB, 1, 1);
        putCreature(state, hex(1, 0), CRAB, 1, 1);
        putCreature(state, hex(-1, 0), CRAB, 2, 1);
        putCreature(state, hex(-2, 1), CRAB, 2, 1);
        // level-3 plateau (size 3): player 0 has 2 -> first, 3; player 1 has 1 -> second, floor(3 / 2) = 1
        // (1,0) (size 1): player 1 alone -> 1
        // {(-1,0), (-2,1)} (size 2): player 2 has 2 -> 2
        assertEquals(3, state.speciesScore(0, CRAB));
        assertEquals(1 + 1, state.speciesScore(1, CRAB));
        assertEquals(2, state.speciesScore(2, CRAB));
        assertEquals(2, state.getGameScore(2), 0);
    }

    /**
     * Crabs at 3 players on a field written cell by cell: level 2 at (q, 0) for q = 0..6 (D, M, S, B, D, M, B - one
     * plateau of size 7, its Smoker included); level 3 Microbial Mat at (7,0) (a plateau of size 1); level 1 Diffuse
     * Vents at (-1,0) (not a plateau).
     */
    private void crabStrip() {
        state = newState(3, 42);
        useSpecies(state, CRAB);
        Terrain[] row = {DIFFUSE, MAT, SMOKER, BASALT, DIFFUSE, MAT, BASALT};
        for (int q = 0; q < row.length; q++)
            putCell(state, hex(q, 0), row[q], 2);
        putCell(state, hex(7, 0), MAT, 3);
        putCell(state, hex(-1, 0), DIFFUSE, 1);
    }

    @Test
    public void playersTiedForMostCrabsAllScoreTheSizeAndNoSecondPlaceIsAwarded() {
        crabStrip();
        putCreature(state, hex(0, 0), CRAB, 0, 1);
        putCreature(state, hex(1, 0), CRAB, 0, 1);
        putCreature(state, hex(3, 0), CRAB, 1, 1);
        putCreature(state, hex(4, 0), CRAB, 1, 1);
        putCreature(state, hex(5, 0), CRAB, 2, 1);
        // size 7: players 0 and 1 tie with 2 -> 7 each; player 2's single crab is not a second place -> 0
        assertEquals(7, state.speciesScore(0, CRAB));
        assertEquals(7, state.speciesScore(1, CRAB));
        assertEquals(0, state.speciesScore(2, CRAB));
    }

    @Test
    public void playersTiedForSecondMostCrabsAllScoreHalfTheSizeRoundedDown() {
        crabStrip();
        putCreature(state, hex(0, 0), CRAB, 0, 1);
        putCreature(state, hex(1, 0), CRAB, 0, 1);
        putCreature(state, hex(3, 0), CRAB, 1, 1);
        putCreature(state, hex(4, 0), CRAB, 2, 1);
        // size 7: player 0 first with 2 -> 7; players 1 and 2 tie second with 1 -> floor(7 / 2) = 3 each
        assertEquals(7, state.speciesScore(0, CRAB));
        assertEquals(3, state.speciesScore(1, CRAB));
        assertEquals(3, state.speciesScore(2, CRAB));
    }

    @Test
    public void aPlateauEndsWhereTheLevelChanges() {
        crabStrip();
        putCreature(state, hex(7, 0), CRAB, 0, 1);
        putCreature(state, hex(0, 0), CRAB, 1, 1);
        // player 0 alone on the level-3 plateau (7,0) of size 1 -> 1; player 1 alone on the level-2 plateau of size
        // 7 -> 7; the two are adjacent at (6,0)-(7,0) but at different levels
        assertEquals(1, state.speciesScore(0, CRAB));
        assertEquals(7, state.speciesScore(1, CRAB));
        assertEquals(0, state.speciesScore(2, CRAB));
    }
}
