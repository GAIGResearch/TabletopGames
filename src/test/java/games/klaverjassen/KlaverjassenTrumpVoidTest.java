package games.klaverjassen;

import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.klaverjassen.KlaverjassenTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.cards;
import static games.tricktaking.TrickTakingTestUtils.cardsOf;
import static org.junit.Assert.*;

/**
 * The void in trumps inferred from play (KlaverjassenForwardModel.inferTrumpVoid), through fm.next, and its use
 * in redeterminisation. Hearts are trumps throughout.
 */
public class KlaverjassenTrumpVoidTest {

    KlaverjassenGameState state;
    KlaverjassenForwardModel fm;

    @Before
    public void setup() {
        state = newState(42);
        fm = new KlaverjassenForwardModel();
        setTrumps(state, Hearts);
    }

    private Set<FrenchCard.Suite> voids(int player) {
        return state.knownVoids.get(player);
    }

    @Test
    public void discardingWhileAnOpponentWinsWithAPlainCardShowsAVoidInTrumps() {
        giveHand(state, 2, "KD", "8C");
        arrangeTrick(state, 1, "AS");                  // opponent 1's AS is winning; player 2 has no spade
        playCards(state, fm, "KD");
        // no spade (lead-suit void) and no trump (otherwise a trump was compulsory)
        assertEquals(Set.of(Spades, Hearts), voids(2));
    }

    @Test
    public void discardingAfterTheWinnerChangesToAnOpponentsPlainCardShowsAVoidInTrumps() {
        giveHand(state, 3, "QD", "7C");
        arrangeTrick(state, 1, "KS", "AS");            // 1 led KS, opponent 2 is winning with AS; player 3 to play
        playCards(state, fm, "QD");
        assertEquals(Set.of(Spades, Hearts), voids(3));
    }

    @Test
    public void discardingWhilePartnerWinsShowsOnlyTheLeadSuitVoid() {
        giveHand(state, 2, "7H", "KD");
        arrangeTrick(state, 0, "AS", "7S");            // partner 0's AS is winning: player 2 may discard with trumps
        playCards(state, fm, "KD");
        assertEquals(Set.of(Spades), voids(2));
    }

    @Test
    public void trumpingAnOpponentsPlainCardShowsOnlyTheLeadSuitVoid() {
        giveHand(state, 2, "7H", "KD");
        arrangeTrick(state, 1, "AS");
        playCards(state, fm, "7H");
        assertEquals(Set.of(Spades), voids(2));
    }

    @Test
    public void discardingWhileAnOpponentWinsWithATrumpShowsOnlyTheLeadSuitVoid() {
        giveHand(state, 2, "7H", "KD");
        arrangeTrick(state, 0, "AS", "10H");           // opponent 1 trumped; player 2's 7H cannot beat 10H
        playCards(state, fm, "KD");
        // the player may still hold lower trumps (and here does)
        assertEquals(Set.of(Spades), voids(2));
    }

    @Test
    public void followingSuitShowsNoVoid() {
        giveHand(state, 2, "7S", "KD");
        arrangeTrick(state, 1, "AS");
        playCards(state, fm, "7S");
        assertEquals(Set.of(), voids(2));
    }

    @Test
    public void leadingShowsNoVoid() {
        giveHand(state, 0, "KD", "8C");
        arrangeTrick(state, 0);
        playCards(state, fm, "KD");
        assertEquals(Set.of(), voids(0));
    }

    @Test
    public void withRememberVoidsOffNothingIsInferred() {
        KlaverjassenParameters params = new KlaverjassenParameters();
        params.setParameterValue("rememberVoids", false);
        state = newState(42, params);
        setTrumps(state, Hearts);
        giveHand(state, 2, "KD", "8C");
        arrangeTrick(state, 1, "AS");                  // the position of the first test
        playCards(state, fm, "KD");
        assertEquals(Set.of(), voids(2));
    }

    @Test
    public void redeterminisationNeverGivesATrumpToAPlayerShownToHoldNone() {
        // player 2 holds only diamonds and clubs; player 1 leads AS and player 2 discards KD
        arrangeHands(state,
                cards("7S", "8S", "9S", "10S", "KH", "AH", "KC", "AC"),
                cards("AS", "KS", "7H", "8H", "9H", "JD", "QD", "AD"),
                cards("7D", "8D", "9D", "10D", "KD", "7C", "8C", "9C"),
                cards("QS", "JS", "10H", "JH", "QH", "10C", "JC", "QC"));
        arrangeTrick(state, 1);
        playCards(state, fm, "AS", "KD");
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Set.of(Spades, Hearts), voids(2));

        // from player 0's view the hidden cards include 7H 8H 9H 10H JH QH; player 2 must never receive one, but the
        // hidden diamonds and clubs (JD QD AD 10C JC QC) can still reach player 2
        int changed = 0;
        for (int i = 0; i < 50; i++) {
            KlaverjassenGameState copy = (KlaverjassenGameState) state.copy(0);
            for (FrenchCard c : copy.getPlayerHand(2).getComponents()) {
                assertNotEquals("player 2 given " + c, Hearts, c.suite);
                assertNotEquals("player 2 given " + c, Spades, c.suite);
            }
            assertEquals(7, copy.getPlayerHand(2).getSize());
            if (!new HashSet<>(cardsOf(copy.getPlayerHand(2))).equals(new HashSet<>(cardsOf(state.getPlayerHand(2)))))
                changed++;
        }
        assertTrue("player 2's hand was never redeterminised", changed > 0);
    }
}
