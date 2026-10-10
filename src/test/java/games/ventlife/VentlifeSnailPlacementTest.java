package games.ventlife;

import games.ventlife.actions.StopPlacing;
import games.ventlife.components.Creature;
import games.ventlife.components.Species;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static games.ventlife.VentlifeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Volcano Snail placement: the first Snail, the follow-on Snails of the same turn, and when the turn ends.
 * Most tests use the Basalt chain field (VentlifeTestUtils.buildBasaltChain): Basalt
 * (0,1)-(1,0)-(2,0)-(3,0) in a chain and (-1,0) on its own, all level 1; player 1 places D/M at ((1,-2),0)
 * ((1,-2) S, (1,-1) D, (2,-2) M) and then has the creature step.
 */
public class VentlifeSnailPlacementTest {

    VentlifeGameState state;
    VentlifeForwardModel fm;

    @Before
    public void setup() {
        state = newState(2, 42);
        fm = new VentlifeForwardModel();
    }

    /** The Basalt chain field built with no species, then only the given species; player 1's tile placed. */
    private void chainCreatureStep(Species... species) {
        useSpecies(state);
        buildBasaltChain(state, fm);
        useSpecies(state, species);
    }

    /** Player 1's tile D/M at ((1,-2),0), after which they have the creature step. */
    private void chainTurnTile() {
        placeChainTurnTile(state, fm);
        assertCreatureStepOf(1);
    }

    private void assertCreatureStepOf(int player) {
        assertEquals("creature step expected", VentlifeGameState.Phase.PLACE_CREATURES, state.getGamePhase());
        assertEquals(player, state.getCurrentPlayer());
    }

    /** Player 0 to place a tile: player 1's turn is over and they have drawn. */
    private void assertTurnPassedToPlayer0() {
        assertEquals(VentlifeGameState.Phase.PLACE_TILE, state.getGamePhase());
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, state.getHand(1).getSize());
    }

    @Test
    public void snailsMayBePlacedOnAnyEmptyBasaltHexOfAnyLevel() {
        useSpecies(state);
        buildStandardField(state, fm, 6);
        useSpecies(state, SNAIL);
        startCreatureStep(state);
        // Basalt on the standard field: (1,0) B2 and (-2,0) B1; no StopPlacing before the first Snail
        assertEquals(Set.of(snail(hex(1, 0)), snail(hex(-2, 0))), legalSet(state, fm));
        putCreature(state, hex(-2, 0), SNAIL, 1, 1);
        assertEquals(Set.of(snail(hex(1, 0))), legalSet(state, fm));
    }

    @Test
    public void afterEachSnailTheSamePlayerMayAddOneNextToASnailOfThisTurnUpToThree() {
        chainCreatureStep(SNAIL);
        chainTurnTile();
        assertCreatureStepOf(1);
        assertEquals(Set.of(snail(hex(0, 1)), snail(hex(1, 0)), snail(hex(2, 0)), snail(hex(3, 0)), snail(hex(-1, 0))),
                legalSet(state, fm));

        fm.next(state, snail(hex(1, 0)));
        // the follow-on comes before the draw: player 1 still holds no tile
        assertCreatureStepOf(1);
        assertEquals(0, state.getHand(1).getSize());
        // empty Basalt next to (1,0): (0,1) and (2,0); not (3,0) or (-1,0)
        assertEquals(Set.of(snail(hex(0, 1)), snail(hex(2, 0)), new StopPlacing()), legalSet(state, fm));

        fm.next(state, snail(hex(2, 0)));
        assertCreatureStepOf(1);
        // next to either Snail of this turn: (0,1) by (1,0), (3,0) by (2,0)
        assertEquals(Set.of(snail(hex(0, 1)), snail(hex(3, 0)), new StopPlacing()), legalSet(state, fm));

        fm.next(state, snail(hex(3, 0)));
        // the third Snail ends the turn with no further decision, although (0,1) is still free
        assertTurnPassedToPlayer0();
        Creature s = new Creature(SNAIL, 1, 1);
        assertEquals(Map.of(hex(1, 0), s, hex(2, 0), s, hex(3, 0), s), state.getCreatures());
        assertEquals(3, state.getSupply(1, SNAIL));
        assertTokensConserved(state, "after three snails");
    }

    @Test
    public void stopPlacingEndsTheTurnAfterTheFirstSnail() {
        chainCreatureStep(SNAIL);
        chainTurnTile();
        fm.next(state, snail(hex(1, 0)));
        assertTrue(legalSet(state, fm).contains(new StopPlacing()));
        fm.next(state, new StopPlacing());
        assertTurnPassedToPlayer0();
        assertEquals(Map.of(hex(1, 0), new Creature(SNAIL, 1, 1)), state.getCreatures());
        assertEquals(5, state.getSupply(1, SNAIL));
    }

    @Test
    public void theTurnEndsWithoutADecisionWhenNoBasaltIsNextToTheSnails() {
        chainCreatureStep(SNAIL);
        chainTurnTile();
        // (-1,0) has no Basalt neighbour: (0,0) S, (-1,1) uncovered, (-1,-1) D, (0,-1) S, ...
        fm.next(state, snail(hex(-1, 0)));
        assertTurnPassedToPlayer0();
        assertEquals(5, state.getSupply(1, SNAIL));
    }

    @Test
    public void snailsFromEarlierTurnsDoNotExtendTheGroup() {
        chainCreatureStep(SNAIL);
        // player 1's Snail from an earlier turn on (2,0)
        putCreature(state, hex(2, 0), SNAIL, 1, 1);
        chainTurnTile();
        assertCreatureStepOf(1);
        fm.next(state, snail(hex(1, 0)));
        // next to (1,0): only (0,1) ((2,0) is taken); (3,0) is next to the earlier Snail only
        assertEquals(Set.of(snail(hex(0, 1)), new StopPlacing()), legalSet(state, fm));
    }

    @Test
    public void theFollowOnOffersOnlySnailsEvenWithOtherSpeciesInPlay() {
        chainCreatureStep(SNAIL, SHRIMP);
        chainTurnTile();
        assertCreatureStepOf(1);
        assertTrue(legalSet(state, fm).contains(shrimp(hex(1, -1))));
        fm.next(state, snail(hex(1, 0)));
        assertEquals(Set.of(snail(hex(0, 1)), snail(hex(2, 0)), new StopPlacing()), legalSet(state, fm));
    }

    @Test
    public void theTurnEndsWhenTheSnailSupplyRunsOut() {
        chainCreatureStep(SNAIL);
        state.supply[1][SNAIL.ordinal()] = 2;
        chainTurnTile();
        fm.next(state, snail(hex(1, 0)));
        assertEquals(Set.of(snail(hex(0, 1)), snail(hex(2, 0)), new StopPlacing()), legalSet(state, fm));
        fm.next(state, snail(hex(2, 0)));
        // no Snail left: the turn ends, although (0,1) and (3,0) are free
        assertTurnPassedToPlayer0();
        assertEquals(0, state.getSupply(1, SNAIL));
        assertEquals(2, state.getCreatures().size());
    }

    @Test
    public void maxSnailsPerTurnLimitsTheGroup() {
        VentlifeParameters params = new VentlifeParameters();
        params.setParameterValue("maxSnailsPerTurn", 2);
        state = newState(2, 42, params);
        chainCreatureStep(SNAIL);
        chainTurnTile();
        fm.next(state, snail(hex(1, 0)));
        assertEquals(Set.of(snail(hex(0, 1)), snail(hex(2, 0)), new StopPlacing()), legalSet(state, fm));
        fm.next(state, snail(hex(2, 0)));
        assertTurnPassedToPlayer0();
        assertEquals(4, state.getSupply(1, SNAIL));
    }
}
