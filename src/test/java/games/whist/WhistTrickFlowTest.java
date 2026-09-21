package games.whist;

import core.Game;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static core.components.FrenchCard.Suite.Diamonds;
import static core.components.FrenchCard.Suite.Hearts;
import static games.tricktaking.TrickTakingTestUtils.*;
import static games.whist.WhistTestUtils.*;
import static org.junit.Assert.*;

/**
 * Scripted walk-through of a trick and the start of the next in a real game, driven by fm.next.
 */
public class WhistTrickFlowTest {

    WhistGameState state;
    WhistForwardModel fm;

    @Before
    public void setup() {
        Game game = newGame(7);
        state = (WhistGameState) game.getGameState();
        fm = (WhistForwardModel) game.getForwardModel();
        // three cards each (the other 40 on the discard pile); spades trumps, the turned-up 7S with dealer 3
        giveHand(state, 0, "10H", "3D", "4C");
        giveHand(state, 1, "KH", "9D", "JC");
        giveHand(state, 2, "2S", "5D", "6C");
        giveHand(state, 3, "AH", "7S", "8C");
        setTrumps(state, "7S");
        assertEquals(40, state.getDiscardPile().getSize());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void playPassesClockwiseAndTheTrumpWinnerLeadsTheNextTrick() {
        playCards(state, fm, "10H");
        assertEquals(1, state.getCurrentPlayer());
        playCards(state, fm, "KH");
        assertEquals(2, state.getCurrentPlayer());
        playCards(state, fm, "2S");                  // player 2, void in hearts, trumps
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(cards("10H", "KH", "2S"), cardsOf(state.getCurrentTrick()));
        assertEquals(Set.of(Hearts), state.getKnownVoids().get(2));

        playCards(state, fm, "AH");
        // the 2 of Spades (trump) beats the Ace of Hearts led: player 2 wins and leads
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(2, state.getCurrentTrick().getLeader());
        assertEquals(44, state.getDiscardPile().getSize());   // 40 + the 4 trick cards
        for (String c : new String[]{"10H", "KH", "2S", "AH"})
            assertTrue(c + " on the discard pile", state.getDiscardPile().contains(card(c)));
        assertArrayEquals(new int[]{0, 0, 1, 0}, state.tricksTaken);
        assertEquals(1, state.getTeamTricks(0));
        assertEquals(0, state.getTeamTricks(1));
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);

        // trick 2: player 2 leads, then play wraps round 3 -> 0
        playCards(state, fm, "5D");
        assertEquals(3, state.getCurrentPlayer());
        playCards(state, fm, "8C");                  // player 3 holds 7S and 8C: void in diamonds
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(Diamonds), state.getKnownVoids().get(3));
        assertEquals(cards("5D", "8C"), cardsOf(state.getCurrentTrick()));
    }
}
