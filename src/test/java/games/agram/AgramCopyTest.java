package games.agram;

import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static games.agram.AgramTestUtils.*;
import static org.junit.Assert.*;

public class AgramCopyTest {

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
     * One complete trick (won by player 2, who then leads) and one card of the next: the trick in progress,
     * discard pile and trick leader are all non-trivial.
     */
    private void playATrickAndAHalf() {
        giveHand(state, 0, "4H", "6C", "8C");
        giveHand(state, 1, "9H", "5C", "7D");
        giveHand(state, 2, "AH", "AC", "10S");
        playCards(state, fm, "4H", "9H", "AH", "AC");
        // arrangement guard: player 2 won the first trick and led the Ace of Clubs to the second
        assertEquals(2, state.getCurrentTrick().getLeader());
        assertEquals(cards("AC"), cardsOf(state.getCurrentTrick()));
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void fullCopyIsEqualAndIndependentOfTheOriginal() {
        AgramGameState copy = (AgramGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        int originalHash = state.hashCode();
        fm.next(copy, new PlayCard(copy.getPlayerHands().get(0).get(0)));   // player 0 leads any card
        assertEquals(originalHash, state.hashCode());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(6, state.getPlayerHands().get(0).getSize());
        assertNotEquals(state, copy);
    }

    @Test
    public void aCopyMadeMidTrickKeepsTheTrickAndLeaderAndPlaysOnIdentically() {
        playATrickAndAHalf();
        AgramGameState copy = (AgramGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(cards("AC"), cardsOf(copy.getCurrentTrick()));
        assertEquals(2, copy.getCurrentTrick().getLeader());
        assertEquals(0, copy.getCurrentPlayer());

        // playing the same cards in both keeps them equal: player 2's Ace of Clubs wins trick 2 in each
        playCards(state, fm, "8C", "5C");
        playCards(copy, fm, "8C", "5C");
        assertEquals(state, copy);
        assertEquals(2, copy.getCurrentTrick().getLeader());
        assertEquals(2, copy.getCurrentPlayer());
    }

    private Set<FrenchCard> hiddenFromPlayerZero(AgramGameState s) {
        Set<FrenchCard> hidden = new HashSet<>(s.getDrawDeck().getComponents());
        for (int p = 1; p < s.getNPlayers(); p++)
            hidden.addAll(s.getPlayerHands().get(p).getComponents());
        return hidden;
    }

    @Test
    public void redeterminisedCopyKeepsWhatThePlayerCanSeeAndShufflesTheRest() {
        playATrickAndAHalf();
        int handsChanged = 0;
        for (int i = 0; i < 20; i++) {
            AgramGameState copy = (AgramGameState) state.copy(0);
            assertEquals(cardsOf(state.getPlayerHands().get(0)), cardsOf(copy.getPlayerHands().get(0)));
            assertEquals(cardsOf(state.getCurrentTrick()), cardsOf(copy.getCurrentTrick()));
            assertEquals(cardsOf(state.getDiscardPile()), cardsOf(copy.getDiscardPile()));
            assertEquals(state.getCurrentTrick().getLeader(), copy.getCurrentTrick().getLeader());
            assertEquals(state.getCurrentPlayer(), copy.getCurrentPlayer());
            for (int p = 1; p < 3; p++)
                assertEquals(state.getPlayerHands().get(p).getSize(), copy.getPlayerHands().get(p).getSize());
            assertEquals(state.getDrawDeck().getSize(), copy.getDrawDeck().getSize());
            assertEquals(hiddenFromPlayerZero(state), hiddenFromPlayerZero(copy));
            assertAllCardsPresent(copy);

            if (!cardsOf(copy.getPlayerHands().get(1)).equals(cardsOf(state.getPlayerHands().get(1))))
                handsChanged++;
        }
        assertTrue("player 1's hand was never redeterminised", handsChanged > 0);
    }
}
