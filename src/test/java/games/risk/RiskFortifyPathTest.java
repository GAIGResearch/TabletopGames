package games.risk;

import core.actions.AbstractAction;
import games.risk.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * The expert rule fortifyAlongPath (pdf p.15).
 * Base: 3 players, fortifyAlongPath on; player 1 holds everything with 1 army but Argentina (player 2); each test
 * gives player 0 its part and starts player 0's FORTIFY.
 * Adjacency used: South Africa - Congo, East Africa, Madagascar; Congo - North Africa, East Africa;
 * East Africa - North Africa; North Africa - Brazil; Brazil - Venezuela, Peru, Argentina.
 */
public class RiskFortifyPathTest {

    RiskForwardModel fm = new RiskForwardModel();
    RiskGameState state;

    @Before
    public void setup() {
        state = newState(3, 7, params(true));
        arrangeBase(state);
    }

    private static RiskParameters params(boolean alongPath) {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("fortifyAlongPath", alongPath);
        return params;
    }

    private static void arrangeBase(RiskGameState s) {
        fillBoard(s, 1);
        give(s, 2, 1, ARGENTINA);
    }

    /** The pdf's example: South Africa 5, Congo 1, North Africa 1, Brazil 3, and Indonesia 4 (no neighbour held). */
    private static void arrangePdfPath(RiskGameState s) {
        give(s, 0, 5, SOUTH_AFRICA);
        give(s, 0, 1, CONGO, NORTH_AFRICA);
        give(s, 0, 3, BRAZIL);
        give(s, 0, 4, INDONESIA);
        startPlay(s, 0, RiskGamePhase.FORTIFY, 0);
    }

    @Test
    public void pdfExampleSouthAfricaToBrazilThroughCongoAndNorthAfrica() {
        arrangePdfPath(state);
        // South Africa -> Congo -> North Africa -> Brazil is one held chain; Congo and North Africa have 1 army
        // (nothing to move); Indonesia's neighbours are all player 1's
        Set<AbstractAction> expected = Set.of(
                new Fortify(SOUTH_AFRICA, CONGO), new Fortify(SOUTH_AFRICA, NORTH_AFRICA),
                new Fortify(SOUTH_AFRICA, BRAZIL),
                new Fortify(BRAZIL, NORTH_AFRICA), new Fortify(BRAZIL, CONGO), new Fortify(BRAZIL, SOUTH_AFRICA),
                new EndTurn());
        assertEquals(expected, actionSet(fm, state));
    }

    @Test
    public void noPathWhenNorthAfricaIsHeldBySomeoneElse() {
        // North Africa stays player 1's: Brazil is cut off from Congo and South Africa (no other held route)
        give(state, 0, 5, SOUTH_AFRICA);
        give(state, 0, 1, CONGO);
        give(state, 0, 3, BRAZIL);
        startPlay(state, 0, RiskGamePhase.FORTIFY, 0);
        assertEquals(Set.of(new Fortify(SOUTH_AFRICA, CONGO), new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void anyHeldPathWillDoThroughEastAfricaInsteadOfCongo() {
        // Congo is player 1's; South Africa -> East Africa -> North Africa -> Brazil is held
        give(state, 0, 5, SOUTH_AFRICA);
        give(state, 0, 1, EAST_AFRICA, NORTH_AFRICA, BRAZIL);
        startPlay(state, 0, RiskGamePhase.FORTIFY, 0);
        Set<AbstractAction> expected = Set.of(
                new Fortify(SOUTH_AFRICA, EAST_AFRICA), new Fortify(SOUTH_AFRICA, NORTH_AFRICA),
                new Fortify(SOUTH_AFRICA, BRAZIL), new EndTurn());
        assertEquals(expected, actionSet(fm, state));
    }

    @Test
    public void withoutFortifyAlongPathOnlyAdjacentTerritories() {
        state = newState(3, 7, params(false));
        arrangeBase(state);
        arrangePdfPath(state);
        // adjacent held pairs from a territory with 2+ armies: South Africa - Congo, Brazil - North Africa
        assertEquals(Set.of(new Fortify(SOUTH_AFRICA, CONGO), new Fortify(BRAZIL, NORTH_AFRICA), new EndTurn()),
                actionSet(fm, state));
    }

    @Test
    public void fortifyingAlongAPathIsOneMoveThatEndsTheTurn() {
        arrangePdfPath(state);
        assertTrue(fm.computeAvailableActions(state).contains(new Fortify(SOUTH_AFRICA, BRAZIL)));
        fm.next(state, new Fortify(SOUTH_AFRICA, BRAZIL));
        assertEquals(0, state.getCurrentPlayer());
        // 1 .. 5 - 1
        assertEquals(Set.of(new MoveArmies(SOUTH_AFRICA, BRAZIL, 1), new MoveArmies(SOUTH_AFRICA, BRAZIL, 2),
                new MoveArmies(SOUTH_AFRICA, BRAZIL, 3), new MoveArmies(SOUTH_AFRICA, BRAZIL, 4)),
                actionSet(fm, state));
        fm.next(state, new MoveArmies(SOUTH_AFRICA, BRAZIL, 4));
        assertEquals(1, state.getArmies(SOUTH_AFRICA)); // 5 - 4
        assertEquals(7, state.getArmies(BRAZIL));       // 3 + 4
        assertEquals(1, state.getArmies(CONGO));        // the territories in between are unchanged
        assertEquals(1, state.getArmies(NORTH_AFRICA));
        assertFalse(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
    }
}
