package games.blackjack;

import core.actions.AbstractAction;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

public class BlackjackPlayTest {

    static final Set<AbstractAction> HIT_OR_STAND = Set.of(new Hit(), new Stand());

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
        state = new BlackjackGameState(params, 3);
        fm = new BlackjackForwardModel();
        fm.setup(state);
    }

    private Set<AbstractAction> legalActions() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    @Test
    public void aPlayerMayHitOrStand() {
        arrangePlay(state, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "6S", "10D", "");
        assertEquals(HIT_OR_STAND, legalActions());
    }

    @Test
    public void hitTakesTheTopDrawDeckCardAndThePlayerContinues() {
        arrangePlay(state, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "6S", "10D", "5H 3C");
        int drawDeckSize = state.getDrawDeck().getSize();
        fm.next(state, new Hit());

        assertEquals(setOf("10H 2C 5H"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(drawDeckSize - 1, state.getDrawDeck().getSize());
        assertEquals(card("3C"), state.getDrawDeck().peek());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Play, state.getGamePhase());
        assertEquals(HIT_OR_STAND, legalActions());
        assertAllCardsPresent(state);
    }

    @Test
    public void aPlayerOnTwentyOneIsStillOfferedHitOrStand() {
        // by default a two-card 21 is an ordinary 21: no automatic stand
        arrangePlay(state, new int[]{2, 4, 6}, new String[]{"AH KC", "9S 8D", "7C 7D"}, "6S", "10D", "");
        assertEquals(HIT_OR_STAND, legalActions());

        // a 21 reached by hitting: 10 + 2 + 9
        state = new BlackjackGameState(params, 3);
        fm.setup(state);
        arrangePlay(state, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "6S", "10D", "9H");
        fm.next(state, new Hit());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(HIT_OR_STAND, legalActions());
    }

    @Test
    public void standPassesTheTurnToTheNextPlayer() {
        arrangePlay(state, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "6S", "10D", "");
        int drawDeckSize = state.getDrawDeck().getSize();
        fm.next(state, new Stand());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Play, state.getGamePhase());
        assertEquals(setOf("10H 2C"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(drawDeckSize, state.getDrawDeck().getSize());
        assertEquals(HIT_OR_STAND, legalActions());

        fm.next(state, new Stand());
        assertEquals(2, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void goingOverTwentyOneIsBustAndPassesTheTurn() {
        // 10 + 6 + K = 26
        arrangePlay(state, new int[]{2, 4, 6}, new String[]{"10H 6C", "9S 8D", "7C 7D"}, "6S", "10D", "KH");
        fm.next(state, new Hit());
        assertEquals(setOf("10H 6C KH"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Play, state.getGamePhase());
        assertTrue(state.isNotTerminal());
        assertEquals(HIT_OR_STAND, legalActions());
        assertAllCardsPresent(state);
    }

    @Test
    public void theDealerDoesNotPlayUntilTheLastPlayerHasFinished() {
        arrangePlay(state, new int[]{2, 4, 6}, new String[]{"10H 6C", "9S 8D", "7C 7D"}, "6S", "10D", "KH");
        fm.next(state, new Hit());      // player 0 bust
        fm.next(state, new Stand());    // player 1 stands
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(setOf("10D"), setOf(state.getHoleCard()));
        assertEquals(setOf("6S"), setOf(state.getDealerHand()));
        assertTrue(state.isNotTerminal());
    }
}
