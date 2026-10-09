package games.ventlife;

import games.ventlife.components.Species;
import org.junit.Before;
import org.junit.Test;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Snail and Octopus scoring on the standard 6-tile field (2 players): (0,0) S3, (-1,1) M3, (0,1) D3, (1,0) B2,
 * (1,1) D1, (2,0) M1, (-1,0) S2, (-2,0) B1, (-2,1) D2; every other position uncovered.
 * Snail: snailPointsPerHeightEdge per covered neighbour of a different level (uncovered ones do not count).
 * Octopus: octopusPointsPerSpecies per distinct species, other than Octopus, among the creatures (any owner) on its
 * neighbours.
 */
public class VentlifeSnailOctopusScoringTest {

    VentlifeGameState state;
    VentlifeForwardModel fm;

    @Before
    public void setup() {
        state = newState(2, 42);
        fm = new VentlifeForwardModel();
    }

    private void standardField(VentlifeParameters params, Species... species) {
        if (params != null)
            state = newState(2, 42, params);
        useSpecies(state);
        buildStandardField(state, fm, 6);
        useSpecies(state, species);
    }

    private void snailsOnStandardField() {
        // player 0: (1,0) B2 - neighbours (2,0) M1, (1,1) D1, (0,1) D3, (0,0) S3 all differ, (1,-1), (2,-1)
        // uncovered -> 4; (-2,0) B1 - (-1,0) S2, (-2,1) D2 differ, four uncovered -> 2. Total 6.
        // player 1: (-2,1) D2 - (-1,1) M3, (-2,0) B1 differ, (-1,0) S2 the same, three uncovered -> 2
        putCreature(state, hex(1, 0), SNAIL, 0, 1);
        putCreature(state, hex(-2, 0), SNAIL, 0, 1);
        putCreature(state, hex(-2, 1), SNAIL, 1, 1);
    }

    @Test
    public void eachSnailScoresItsCoveredNeighboursOfADifferentLevel() {
        standardField(null, SNAIL);
        snailsOnStandardField();
        assertEquals(6, state.speciesScore(0, SNAIL));
        assertEquals(2, state.speciesScore(1, SNAIL));
        assertEquals(6.0, state.getGameScore(0), 0.0);
        assertEquals(2.0, state.getGameScore(1), 0.0);
    }

    @Test
    public void snailPointsPerHeightEdgeMultipliesTheSnailScore() {
        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("snailPointsPerHeightEdge", 2);
        standardField(params, SNAIL);
        snailsOnStandardField();
        assertEquals(12, state.speciesScore(0, SNAIL));
        assertEquals(4, state.speciesScore(1, SNAIL));
    }

    private void octopusesOnStandardField() {
        // player 0's Octopus on (0,1) D3: neighbours (1,1) Shrimp of player 1, (1,0) Shrimp of player 0, (0,0) Worm of
        // player 0, (-1,1) Octopus of player 1 -> species Shrimp and Worm: 2 x 2 = 4.
        // player 1's Octopus on (-1,1) M3: neighbours (0,1) Octopus, (0,0) Worm, (-1,0), (-2,1) empty -> 1 x 2 = 2
        putCreature(state, hex(0, 1), OCTOPUS, 0, 1);
        putCreature(state, hex(-1, 1), OCTOPUS, 1, 1);
        putCreature(state, hex(1, 1), SHRIMP, 1, 1);
        putCreature(state, hex(1, 0), SHRIMP, 0, 1);
        putCreature(state, hex(0, 0), WORM, 0, 1);
    }

    @Test
    public void eachOctopusScoresTheDistinctOtherSpeciesAroundIt() {
        standardField(null, OCTOPUS, SHRIMP, WORM);
        octopusesOnStandardField();
        assertEquals(4, state.speciesScore(0, OCTOPUS));
        assertEquals(2, state.speciesScore(1, OCTOPUS));
    }

    @Test
    public void octopusPointsPerSpeciesMultipliesTheOctopusScore() {
        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("octopusPointsPerSpecies", 3);
        standardField(params, OCTOPUS, SHRIMP, WORM);
        octopusesOnStandardField();
        assertEquals(6, state.speciesScore(0, OCTOPUS));
        assertEquals(3, state.speciesScore(1, OCTOPUS));
    }
}
