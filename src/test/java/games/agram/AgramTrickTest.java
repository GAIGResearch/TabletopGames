package games.agram;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static games.agram.AgramTestUtils.*;
import static org.junit.Assert.*;

public class AgramTrickTest {

    AgramGameState state;
    AgramForwardModel fm;

    @Before
    public void setup() {
        AgramParameters params = new AgramParameters();
        params.setRandomSeed(42);
        state = new AgramGameState(params, 3);
        fm = new AgramForwardModel();
        fm.setup(state);
    }

    /**
     * After a completed trick that is not the last: the winner is to act and leads, the trick is empty and its
     * cards are on the discard pile.
     */
    private void assertTrickWonBy(int winner, List<?> trickCards) {
        assertTrue("the game is not over after the first of two tricks", state.isNotTerminal());
        assertEquals("winner is to act", winner, state.getCurrentPlayer());
        assertEquals("winner leads the next trick", winner, state.getTrickLeader());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(new HashSet<>(trickCards), new HashSet<>(state.getDiscardPile().getComponents()));
        assertEquals(trickCards.size(), state.getDiscardPile().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void theHighestCardOfTheSuitLedWinsEvenWhenAHigherCardOfAnotherSuitIsPlayed() {
        giveHand(state, 0, "7H", "3S");
        giveHand(state, 1, "AC", "4C");   // void in Hearts
        giveHand(state, 2, "9H", "5D");
        playCards(state, fm, "7H", "AC", "9H");

        // the Ace of Clubs is off-suit, so the Nine of Hearts (player 2) beats the Seven of Hearts
        assertTrickWonBy(2, cards("7H", "AC", "9H"));
        // player 2 now leads, so may play anything left in hand
        assertEquals(List.of(play("5D")), fm.computeAvailableActions(state));
    }

    @Test
    public void anAceBeatsTheTen() {
        giveHand(state, 0, "10D", "3S");
        giveHand(state, 1, "AD", "4C");
        giveHand(state, 2, "3D", "5H");
        playCards(state, fm, "10D", "AD", "3D");

        assertTrickWonBy(1, cards("10D", "AD", "3D"));
    }

    @Test
    public void inSpadesTheTenIsTheHighestCard() {
        // there is no Ace of Spades; an Ace of another suit discarded to a spade lead does not win
        giveHand(state, 0, "5S", "3H");
        giveHand(state, 1, "10S", "4C");
        giveHand(state, 2, "AD", "5H");   // void in Spades
        playCards(state, fm, "5S", "10S", "AD");

        assertTrickWonBy(1, cards("5S", "10S", "AD"));
    }

    @Test
    public void theLeaderCanWinTheirOwnTrick() {
        giveHand(state, 0, "AC", "3S");
        giveHand(state, 1, "3C", "4H");
        giveHand(state, 2, "10C", "5H");
        playCards(state, fm, "AC", "3C", "10C");

        assertTrickWonBy(0, cards("AC", "3C", "10C"));
    }

    @Test
    public void theWinnerLeadsTheNextTrickAndPlayContinuesInPlayerOrderFromThem() {
        giveHand(state, 0, "4H", "6C", "8C");
        giveHand(state, 1, "9H", "5C", "7D");
        giveHand(state, 2, "3H", "AC", "10S");
        playCards(state, fm, "4H", "9H", "3H");
        assertTrickWonBy(1, cards("4H", "9H", "3H"));

        // trick 2: player 1 leads the Five of Clubs, then player 2, then player 0
        fm.next(state, play("5C"));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(List.of(play("AC")), fm.computeAvailableActions(state));
        fm.next(state, play("AC"));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(plays("6C", "8C"), new HashSet<>(fm.computeAvailableActions(state)));
        fm.next(state, play("8C"));

        // player 2's Ace of Clubs wins trick 2; both tricks are now on the discard pile
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(2, state.getTrickLeader());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(6, state.getDiscardPile().getSize());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }
}
