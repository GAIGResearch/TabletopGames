package games.ventlife;

import games.ventlife.components.Species;
import org.junit.Test;

import java.util.*;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Where Blind Vent Shrimp, Eelpout Fish and Yeti Crabs may be placed, on the six-tile standard field (built with no
 * species in play), with only the species under test in play and the creature step started for player 0.
 * Top hexes: (0,0) S3, (-1,1) M3, (0,1) D3, (1,0) B2, (1,1) D1, (2,0) M1, (-1,0) S2, (-2,0) B1, (-2,1) D2.
 */
public class VentlifeSpeciesPlacementTest {

    VentlifeGameState state;
    VentlifeForwardModel fm;

    /** The standard field with only the species in play, player 0 to place creatures. */
    private void arrange(VentlifeParameters params, Species... species) {
        state = newState(2, 42, params);
        fm = new VentlifeForwardModel();
        useSpecies(state);
        buildStandardField(state, fm, 6);
        useSpecies(state, species);
        assertEquals(0, state.getCurrentPlayer());
    }

    private static VentlifeParameters smokersForAll() {
        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("smokersOnlyForWormsAndShrimp", false);
        return params;
    }

    @Test
    public void shrimpGoOnAnyEmptyHexExceptBasaltSmokersIncluded() {
        arrange(null, SHRIMP);
        putCreature(state, hex(2, 0), SHRIMP, 1, 1);
        startCreatureStep(state);
        // not (1,0) B2, (-2,0) B1 (Basalt), nor (2,0) (occupied); the Smokers (0,0) and (-1,0) are allowed
        Set<Object> expected = Set.of(shrimp(hex(0, 0)), shrimp(hex(-1, 1)), shrimp(hex(0, 1)), shrimp(hex(1, 1)),
                shrimp(hex(-1, 0)), shrimp(hex(-2, 1)));
        assertEquals(expected, new HashSet<>(legalSet(state, fm)));
    }

    @Test
    public void fishGoOnSeafloorHexesOrNextToTheirOwnersFishButNeverOnASmoker() {
        // the same with Smokers open to every species: Fish still never go on one
        for (VentlifeParameters params : Arrays.asList(null, smokersForAll())) {
            arrange(params, FISH);
            putCreature(state, hex(-2, 1), FISH, 0, 1);
            putCreature(state, hex(1, 1), FISH, 1, 1);
            startCreatureStep(state);
            // level 1: (2,0) M1, (-2,0) B1 [(1,1) occupied]; next to player 0's Fish at (-2,1): (-1,1) M3 (level 3
            // allowed), (-2,0), not the Smoker (-1,0); not (0,1) or (1,0), next only to player 1's Fish at (1,1)
            Set<Object> expected = Set.of(fish(hex(2, 0)), fish(hex(-2, 0)), fish(hex(-1, 1)));
            assertEquals("params " + params, expected, new HashSet<>(legalSet(state, fm)));
        }
    }

    @Test
    public void crabsGoOnEmptyHexesOfLevelTwoOrMoreNotOnSmokersByDefault() {
        arrange(null, CRAB);
        putCreature(state, hex(0, 1), CRAB, 1, 1);
        startCreatureStep(state);
        // level >= 2: (0,0) S3, (-1,1) M3, (0,1) D3 [occupied], (1,0) B2, (-1,0) S2, (-2,1) D2; minus the Smokers
        Set<Object> expected = Set.of(crab(hex(-1, 1)), crab(hex(1, 0)), crab(hex(-2, 1)));
        assertEquals(expected, new HashSet<>(legalSet(state, fm)));
    }

    @Test
    public void crabsMayGoOnHighSmokersWhenSmokersAreOpenToEverySpecies() {
        arrange(smokersForAll(), CRAB);
        putCreature(state, hex(0, 1), CRAB, 1, 1);
        startCreatureStep(state);
        Set<Object> expected = Set.of(crab(hex(-1, 1)), crab(hex(1, 0)), crab(hex(-2, 1)),
                crab(hex(0, 0)), crab(hex(-1, 0)));
        assertEquals(expected, new HashSet<>(legalSet(state, fm)));
    }
}
