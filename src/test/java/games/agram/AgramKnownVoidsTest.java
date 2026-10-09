package games.agram;

import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.agram.AgramTestUtils.*;
import static org.junit.Assert.*;

/**
 * A player who fails to follow suit is publicly known to be void in the suit led, and redeterminisation
 * never deals them a card of that suit.
 */
public class AgramKnownVoidsTest {

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

    @Test
    public void aFollowerWhoCannotFollowSuitIsKnownVoidInTheSuitLed() {
        for (int p = 0; p < 3; p++)
            assertEquals("no voids are known at the start, player " + p, Set.of(), state.getKnownVoids().get(p));

        giveHand(state, 0, "4H", "8D");
        giveHand(state, 1, "5C", "7D");     // no Hearts
        giveHand(state, 2, "9H", "3S");

        fm.next(state, play("4H"));
        assertEquals("the leader records nothing", Set.of(), state.getKnownVoids().get(0));

        fm.next(state, play("5C"));
        // recorded as soon as the card is played, and in the suit led (Hearts), not the suit played (Clubs)
        assertEquals(Set.of(Hearts), state.getKnownVoids().get(1));
        assertEquals(Set.of(), state.getKnownVoids().get(0));
        assertEquals(Set.of(), state.getKnownVoids().get(2));

        fm.next(state, play("9H"));
        assertEquals("a follower who follows suit records nothing", Set.of(), state.getKnownVoids().get(2));
        assertEquals(Set.of(), state.getKnownVoids().get(0));
        assertEquals("the void outlasts the trick", Set.of(Hearts), state.getKnownVoids().get(1));
    }

    @Test
    public void theLastPlayerToATrickIsRecordedVoidWhenTheTrickCompletes() {
        giveHand(state, 0, "4H", "8D");
        giveHand(state, 1, "9H", "7D");
        giveHand(state, 2, "5C", "3S");     // no Hearts

        playCards(state, fm, "4H", "9H", "5C");
        // arrangement guard: the trick is complete and has been cleared away
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(3, state.getDiscardPile().getSize());

        assertEquals(Set.of(Hearts), state.getKnownVoids().get(2));
        assertEquals(Set.of(), state.getKnownVoids().get(0));
        assertEquals(Set.of(), state.getKnownVoids().get(1));
    }

    @Test
    public void knownVoidsAreCopiedIndependentlyAndIncludedInEqualsAndHash() {
        giveHand(state, 0, "4H", "6C");
        giveHand(state, 1, "5C", "7D");     // no Hearts; no Clubs once the Five is played
        giveHand(state, 2, "9H", "AC");
        playCards(state, fm, "4H", "5C", "9H");   // player 2 wins and leads
        assertEquals(Set.of(Hearts), state.getKnownVoids().get(1));

        AgramGameState copy = (AgramGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        for (int p = 0; p < 3; p++)
            assertEquals("player " + p, state.getKnownVoids().get(p), copy.getKnownVoids().get(p));

        // knownVoids is public, so a redeterminised copy keeps it too
        AgramGameState redeterminised = (AgramGameState) state.copy(0);
        for (int p = 0; p < 3; p++)
            assertEquals("redeterminised, player " + p, state.getKnownVoids().get(p), redeterminised.getKnownVoids().get(p));

        // a new void recorded in the copy (player 1 cannot follow Clubs) leaves the original alone
        int originalHash = state.hashCode();
        playCards(copy, fm, "AC", "6C", "7D");
        assertEquals(Set.of(Hearts, Clubs), copy.getKnownVoids().get(1));
        assertEquals(Set.of(Hearts), state.getKnownVoids().get(1));
        assertEquals(originalHash, state.hashCode());

        // two states differing only in knownVoids are neither equal nor hashed alike
        AgramGameState other = (AgramGameState) state.copy();
        other.knownVoids.get(0).add(Spades);
        assertNotEquals(state, other);
        assertNotEquals(state.hashCode(), other.hashCode());
    }

    /**
     * Two tricks: player 2 fails to follow Clubs, then player 1 fails to follow Hearts. Player 0 wins both and is
     * to lead the third, with three cards left each and 20 undealt.
     */
    private void arrangeVoidsForPlayersOneAndTwo() {
        giveHand(state, 0, "6C", "4H", "8D", "9D", "10D");
        giveHand(state, 1, "5C", "7D", "3S", "4S", "6S");
        giveHand(state, 2, "9H", "3H", "10S", "5S", "7S");
        playCards(state, fm, "6C", "5C", "9H");     // player 0 wins with the Six of Clubs
        playCards(state, fm, "4H", "7D", "3H");     // player 0 wins with the Four of Hearts
        // arrangement guard
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(), state.getKnownVoids().get(0));
        assertEquals(Set.of(Hearts), state.getKnownVoids().get(1));
        assertEquals(Set.of(Clubs), state.getKnownVoids().get(2));
    }

    private static boolean holdsSuit(AgramGameState s, int player, FrenchCard.Suite suit) {
        return s.getPlayerHands().get(player).getComponents().stream().anyMatch(c -> c.suite == suit);
    }

    @Test
    public void redeterminisationNeverDealsAKnownVoidPlayerThatSuitButMovesEverythingElse() {
        arrangeVoidsForPlayersOneAndTwo();
        List<FrenchCard> ownHand = cardsOf(state.getPlayerHands().get(0));
        int playerOneHandChanged = 0, playerOneGotAClub = 0, playerTwoGotAHeart = 0;
        for (int i = 0; i < 200; i++) {
            AgramGameState copy = (AgramGameState) state.copy(0);
            String label = "copy " + i;
            assertEquals(label, ownHand, cardsOf(copy.getPlayerHands().get(0)));
            for (int p = 1; p < 3; p++)
                assertEquals(label, 3, copy.getPlayerHands().get(p).getSize());
            assertEquals(label, 20, copy.getDrawDeck().getSize());
            assertAllCardsPresent(copy);

            // the 6 Hearts and 7 Clubs still hidden may go anywhere except to the player known to be void in them
            assertFalse(label + ": player 1 is void in Hearts", holdsSuit(copy, 1, Hearts));
            assertFalse(label + ": player 2 is void in Clubs", holdsSuit(copy, 2, Clubs));
            assertHandsRespectKnownVoids(label, copy, state);

            if (!cardsOf(copy.getPlayerHands().get(1)).equals(cardsOf(state.getPlayerHands().get(1))))
                playerOneHandChanged++;
            if (holdsSuit(copy, 1, Clubs)) playerOneGotAClub++;
            if (holdsSuit(copy, 2, Hearts)) playerTwoGotAHeart++;
        }
        assertTrue("player 1's hand was never redeterminised", playerOneHandChanged > 0);
        assertTrue("player 1 (void only in Hearts) was never dealt a Club", playerOneGotAClub > 0);
        assertTrue("player 2 (void only in Clubs) was never dealt a Heart", playerTwoGotAHeart > 0);
    }
}
