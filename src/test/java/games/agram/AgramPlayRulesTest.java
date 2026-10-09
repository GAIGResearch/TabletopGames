package games.agram;

import core.actions.AbstractAction;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static games.agram.AgramTestUtils.*;
import static org.junit.Assert.*;

public class AgramPlayRulesTest {

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

    private void assertOffered(HashSet<AbstractAction> expected) {
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(expected, new HashSet<>(actions));
        assertEquals("one action per card, no duplicates", expected.size(), actions.size());
    }

    @Test
    public void theLeaderIsOfferedEveryCardInHand() {
        giveHand(state, 0, "AH", "3C", "10S");
        giveHand(state, 1, "9H", "5C", "4D");
        giveHand(state, 2, "7H", "8C", "6D");
        assertEquals(0, state.getCurrentPlayer());
        assertOffered(plays("AH", "3C", "10S"));
    }

    @Test
    public void aFollowerHoldingTheSuitLedIsOfferedOnlyThoseCardsHighOrLow() {
        giveHand(state, 0, "9H", "3C");
        giveHand(state, 1, "3H", "AH", "5C", "10S");
        giveHand(state, 2, "7H", "8C", "6D", "4S");
        fm.next(state, play("9H"));

        // Hearts led: both hearts, including the Three that cannot beat the Nine (no obligation to beat)
        assertEquals(1, state.getCurrentPlayer());
        assertOffered(plays("3H", "AH"));
    }

    @Test
    public void aFollowerVoidInTheSuitLedIsOfferedEveryCard() {
        giveHand(state, 0, "9H", "3C");
        giveHand(state, 1, "5C", "10S", "4D");
        giveHand(state, 2, "7H", "8C", "6D");
        fm.next(state, play("9H"));

        assertEquals(1, state.getCurrentPlayer());
        assertOffered(plays("5C", "10S", "4D"));
    }

    @Test
    public void theSuitLedIsSetByTheLeadCardNotByAnOffSuitCardPlayedAfterIt() {
        giveHand(state, 0, "9H", "3C");
        giveHand(state, 1, "5C", "10S");
        giveHand(state, 2, "7H", "8C");
        fm.next(state, play("9H"));
        fm.next(state, play("5C"));   // player 1 is void in Hearts and discards a Club

        // player 2 must still follow Hearts, not Clubs
        assertEquals(2, state.getCurrentPlayer());
        assertOffered(plays("7H"));
    }

    @Test
    public void playingACardMovesItFromHandToTheEndOfTheTrickAndPassesTheTurn() {
        giveHand(state, 0, "9H", "3C");
        giveHand(state, 1, "3H", "5C");
        giveHand(state, 2, "7H", "8C");
        int undealt = state.getDrawDeck().getSize();

        fm.next(state, play("9H"));
        assertEquals(cards("3C"), cardsOf(state.getPlayerHands().get(0)));
        assertEquals(cards("9H"), cardsOf(state.getCurrentTrick()));
        assertEquals(1, state.getCurrentPlayer());

        fm.next(state, play("3H"));
        assertEquals(cards("5C"), cardsOf(state.getPlayerHands().get(1)));
        // in play order: index 0 is the lead card
        assertEquals(cards("9H", "3H"), cardsOf(state.getCurrentTrick()));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals("trick still open, so the leader is unchanged", 0, state.getCurrentTrick().getLeader());

        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals("the undealt cards are never used", undealt, state.getDrawDeck().getSize());
        assertEquals(2, state.getPlayerHands().get(2).getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void aPlayCardBuiltFromTheCardValueEqualsTheGeneratedOneAndAppliesToACopy() {
        giveHand(state, 0, "AH", "3C");
        giveHand(state, 1, "9H", "5C");
        giveHand(state, 2, "7H", "8C");

        // the generated actions are equal to (and hash as) actions built by hand from the card values
        List<AbstractAction> generated = fm.computeAvailableActions(state);
        assertTrue("hand-built Play Ace of Hearts not among " + generated, generated.contains(play("AH")));
        assertEquals(plays("AH", "3C"), new HashSet<>(generated));

        // the same hand-built action can be played in a copy of the state, leaving the original untouched
        AgramGameState copy = (AgramGameState) state.copy();
        fm.next(copy, play("AH"));
        assertEquals(cards("3C"), cardsOf(copy.getPlayerHands().get(0)));
        assertEquals(cards("AH"), cardsOf(copy.getCurrentTrick()));
        assertEquals(1, copy.getCurrentPlayer());

        assertEquals(2, state.getPlayerHands().get(0).getSize());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(0, state.getCurrentPlayer());
    }
}
