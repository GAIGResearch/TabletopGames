package games.ventlife;

import core.actions.AbstractAction;
import games.ventlife.components.Creature;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Covering of Tube Worms and Vent Sponges by a plateau tile: which climb onto the new Smoker and which return to their
 * owner's supply.
 * Field: tile 0 B/D at ((0,0),1) -> (0,0) S, (-1,1) B, (0,1) D; tile 1 D/M at ((1,0),0) -> (1,0) S, (1,1) D, (2,0) M.
 */
public class VentlifeCoveringTest {

    VentlifeGameState state;
    VentlifeForwardModel fm;

    @Before
    public void setup() {
        state = newState(2, 42);
        fm = new VentlifeForwardModel();
    }

    /** Player 0: tile 0 with 2 worms (bonus) on (0,0); player 1: tile 1 with 1 worm on (1,0). Player 0 to move. */
    private void wormsOnBothSeafloorSmokers() {
        useSpecies(state, WORM, SPONGE);
        place(state, fm, BASALT, DIFFUSE, hex(0, 0), 1, worm(hex(0, 0), 2));
        place(state, fm, DIFFUSE, MAT, hex(1, 0), 0, worm(hex(1, 0), 1));
        assertEquals(4, state.getSupply(0, WORM));
        assertEquals(5, state.getSupply(1, WORM));
    }

    @Test
    public void wormsUnderTheNewSmokerClimbAndWormsUnderItsOtherHexesReturn() {
        wormsOnBothSeafloorSmokers();
        // tile M/B at ((0,0),0): Smoker on (0,0) [player 0's 2 worms], left SE (0,1) M, right E (1,0) B [player 1's worm]
        giveTile(state, 0, MAT, BASALT);
        fm.next(state, placeTile(state, hex(0, 0), 0));
        assertEquals(Map.of(hex(0, 0), new Creature(WORM, 0, 2)), state.getCreatures());
        assertEquals(2, state.getLevel(hex(0, 0)));
        // player 0 keeps 4 in supply; player 1's worm returns: 5 + 1
        assertEquals(4, state.getSupply(0, WORM));
        assertEquals(6, state.getSupply(1, WORM));
        assertTokensConserved(state, "after covering");

        // then player 0's creature step: no empty Smoker, so sponges only, on the empty hexes - the new tile's
        // (0,1) and (1,0) among them
        assertEquals(VentlifeGameState.Phase.PLACE_CREATURES, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        Set<AbstractAction> expected = Set.of(sponge(hex(-1, 1)), sponge(hex(0, 1)), sponge(hex(1, 0)),
                sponge(hex(1, 1)), sponge(hex(2, 0)));
        assertEquals(expected, legalSet(state, fm));
    }

    @Test
    public void wormsUnderTheLeftHexReturnWhileAnotherPlayersWormsClimb() {
        wormsOnBothSeafloorSmokers();
        // player 0: tile B/M at ((1,0),2): Smoker on (1,0) [player 1's worm], left W (0,0) B [player 0's 2 worms],
        // right SW (0,1) M
        giveTile(state, 0, BASALT, MAT);
        fm.next(state, placeTile(state, hex(1, 0), 2));
        assertEquals(Map.of(hex(1, 0), new Creature(WORM, 1, 1)), state.getCreatures());
        assertEquals(2, state.getLevel(hex(1, 0)));
        // player 0's worms return: 4 + 2; player 1 keeps 5 in supply
        assertEquals(6, state.getSupply(0, WORM));
        assertEquals(5, state.getSupply(1, WORM));
        assertTokensConserved(state, "after covering");

        Set<AbstractAction> expected = Set.of(sponge(hex(-1, 1)), sponge(hex(0, 0)), sponge(hex(0, 1)),
                sponge(hex(1, 1)), sponge(hex(2, 0)));
        assertEquals(expected, legalSet(state, fm));
    }

    @Test
    public void coveredSpongesOfEveryPlayerReturnToTheirOwnersSupply() {
        useSpecies(state, WORM, SPONGE);
        place(state, fm, BASALT, DIFFUSE, hex(0, 0), 1, sponge(hex(0, 1)));
        place(state, fm, DIFFUSE, MAT, hex(1, 0), 0, sponge(hex(1, 1)));
        // player 0: tile D/B at ((1,0),1): Smoker (1,0), left SW (0,1) [player 0's sponge], right SE (1,1) [player 1's]
        giveTile(state, 0, DIFFUSE, BASALT);
        fm.next(state, placeTile(state, hex(1, 0), 1));
        assertTrue(state.getCreatures().isEmpty());
        assertEquals(6, state.getSupply(0, SPONGE));
        assertEquals(6, state.getSupply(1, SPONGE));
        assertTokensConserved(state, "after covering");
    }

    @Test
    public void aSpongeUnderTheNewSmokerReturnsRatherThanClimbing() {
        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("smokersOnlyForWormsAndShrimp", false);
        state = newState(2, 42, params);
        useSpecies(state, SPONGE);
        // sponges on both seafloor Smokers (allowed with the restriction off)
        place(state, fm, BASALT, DIFFUSE, hex(0, 0), 1, sponge(hex(0, 0)));
        place(state, fm, DIFFUSE, MAT, hex(1, 0), 0, sponge(hex(1, 0)));
        // tile M/B at ((0,0),0): Smoker on (0,0) [player 0's sponge], right E (1,0) [player 1's sponge]
        giveTile(state, 0, MAT, BASALT);
        fm.next(state, placeTile(state, hex(0, 0), 0));
        assertTrue(state.getCreatures().isEmpty());
        assertEquals(6, state.getSupply(0, SPONGE));
        assertEquals(6, state.getSupply(1, SPONGE));
        // every hex is empty again, the new Smoker included
        Set<AbstractAction> expected = Set.of(sponge(hex(0, 0)), sponge(hex(0, 1)), sponge(hex(1, 0)),
                sponge(hex(-1, 1)), sponge(hex(1, 1)), sponge(hex(2, 0)));
        assertEquals(expected, legalSet(state, fm));
    }
}
