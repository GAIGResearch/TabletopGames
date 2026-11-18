package games.ventlife;

import org.junit.Before;
import org.junit.Test;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Scoring of Tube Worms and Vent Sponges, the game score and the tiebreaks, on the six-tile standard field (built with
 * no species in play, then creatures put on it directly, taken from the supplies). Top hexes:
 * (0,0) S3, (-1,1) M3, (0,1) D3, (1,0) B2, (1,1) D1, (2,0) M1, (-1,0) S2, (-2,0) B1, (-2,1) D2; all else uncovered.
 * Sponge values (2 per distinct terrain among the covered neighbours, own hex not counted):
 * (0,1): (1,1) D, (-1,1) M, (0,0) S, (1,0) B -> 4 types = 8;
 * (1,1): (0,1) D, (1,0) B, (2,0) M -> 3 types = 6;
 * (2,0): (1,1) D, (1,0) B -> 2 types = 4;
 * (-1,1): (0,1) D, (-2,1) D, (-1,0) S, (0,0) S -> 2 types = 4;
 * (-2,0): (-1,0) S, (-2,1) D -> 2 types = 4;
 * (-2,1): (-1,1) M, (-2,0) B, (-1,0) S -> 3 types = 6.
 */
public class VentlifeScoringTest {

    VentlifeGameState state;
    VentlifeForwardModel fm;

    @Before
    public void setup() {
        arrange(new VentlifeParameters());
    }

    private void arrange(VentlifeParameters params) {
        state = newState(2, 42, params);
        fm = new VentlifeForwardModel();
        useSpecies(state);
        buildStandardField(state, fm, 6);
        useSpecies(state, WORM, SPONGE);
    }

    @Test
    public void eachWormScoresTheLevelOfItsSmoker() {
        putCreature(state, hex(0, 0), WORM, 0, 1);
        putCreature(state, hex(-1, 0), WORM, 1, 2);
        // player 0: 1 worm on a level-3 Smoker = 3; player 1: 2 worms on a level-2 Smoker = 2 x 2 = 4
        assertEquals(3, state.speciesScore(0, WORM));
        assertEquals(4, state.speciesScore(1, WORM));
        assertEquals(0, state.speciesScore(0, SPONGE));
        assertEquals(3, state.getGameScore(0), 0);
        assertEquals(4, state.getGameScore(1), 0);
    }

    @Test
    public void eachSpongeScoresForTheDistinctTerrainsAroundIt() {
        putCreature(state, hex(0, 1), SPONGE, 0, 1);
        putCreature(state, hex(2, 0), SPONGE, 0, 1);
        putCreature(state, hex(1, 1), SPONGE, 1, 1);
        putCreature(state, hex(-1, 1), SPONGE, 1, 1);
        // player 0: 8 + 4 = 12; player 1: 6 + 4 = 10
        assertEquals(12, state.speciesScore(0, SPONGE));
        assertEquals(10, state.speciesScore(1, SPONGE));
        assertEquals(0, state.speciesScore(1, WORM));
        assertEquals(12, state.getGameScore(0), 0);
        assertEquals(10, state.getGameScore(1), 0);
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(1));
    }

    @Test
    public void spongePointsPerTerrainParameterScalesTheSpongeScore() {
        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("spongePointsPerTerrain", 3);
        arrange(params);
        putCreature(state, hex(0, 1), SPONGE, 0, 1);
        putCreature(state, hex(2, 0), SPONGE, 0, 1);
        // 4 types x 3 + 2 types x 3
        assertEquals(18, state.speciesScore(0, SPONGE));
    }

    @Test
    public void tiedScoresAreBrokenByTheHighestSingleSpeciesScoreFirst() {
        // player 0: worm on (-1,0) level 2 = 2, sponge on (0,1) = 8: total 10, best species 8, worms 2
        putCreature(state, hex(-1, 0), WORM, 0, 1);
        putCreature(state, hex(0, 1), SPONGE, 0, 1);
        // player 1: 2 worms on (0,0) level 3 = 6, sponge on (2,0) = 4: total 10, best species 6, worms 6
        putCreature(state, hex(0, 0), WORM, 1, 2);
        putCreature(state, hex(2, 0), SPONGE, 1, 1);
        assertEquals(10, state.getGameScore(0), 0);
        assertEquals(10, state.getGameScore(1), 0);
        assertEquals(8, state.getTiebreak(0, 1), 0);
        assertEquals(6, state.getTiebreak(1, 1), 0);
        assertEquals(2, state.getTiebreak(0, 2), 0);
        assertEquals(6, state.getTiebreak(1, 2), 0);
        // tier 1 decides before the Tube Worm tier is looked at
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(1));
    }

    @Test
    public void tiedOnScoreAndBestSpeciesTheTubeWormScoreDecides() {
        // player 0: 2 worms on (-1,0) level 2 = 4, sponge on (1,1) = 6: total 10, best 6, worms 4
        putCreature(state, hex(-1, 0), WORM, 0, 2);
        putCreature(state, hex(1, 1), SPONGE, 0, 1);
        // player 1: 2 worms on (0,0) level 3 = 6, sponge on (2,0) = 4: total 10, best 6, worms 6
        putCreature(state, hex(0, 0), WORM, 1, 2);
        putCreature(state, hex(2, 0), SPONGE, 1, 1);
        assertEquals(10, state.getGameScore(0), 0);
        assertEquals(10, state.getGameScore(1), 0);
        assertEquals(6, state.getTiebreak(0, 1), 0);
        assertEquals(6, state.getTiebreak(1, 1), 0);
        assertEquals(4, state.getTiebreak(0, 2), 0);
        assertEquals(6, state.getTiebreak(1, 2), 0);
        assertEquals(2, state.getOrdinalPosition(0));
        assertEquals(1, state.getOrdinalPosition(1));
    }

    @Test
    public void playersTiedOnEverythingShareThePlace() {
        // player 0: sponges (1,1) 6 + (2,0) 4 = 10; player 1: sponges (-2,1) 6 + (-2,0) 4 = 10; no worms
        putCreature(state, hex(1, 1), SPONGE, 0, 1);
        putCreature(state, hex(2, 0), SPONGE, 0, 1);
        putCreature(state, hex(-2, 1), SPONGE, 1, 1);
        putCreature(state, hex(-2, 0), SPONGE, 1, 1);
        assertEquals(10, state.getGameScore(0), 0);
        assertEquals(10, state.getGameScore(1), 0);
        // tier 1 is the best species total: each player's only species, Sponges, 6 + 4 = 10
        assertEquals(10, state.getTiebreak(0, 1), 0);
        assertEquals(10, state.getTiebreak(1, 1), 0);
        assertEquals(0, state.getTiebreak(0, 2), 0);
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(1, state.getOrdinalPosition(1));
    }
}
