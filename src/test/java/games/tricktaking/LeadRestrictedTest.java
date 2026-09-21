package games.tricktaking;

import org.junit.Test;

import static core.components.FrenchCard.Suite.Hearts;
import static core.components.FrenchCard.Suite.Spades;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.assertEquals;

/**
 * PlayRule.leadRestricted(suit): the suit may not be led unless the hand holds nothing else; otherwise FOLLOW_SUIT.
 * (Spades before spades are broken, Hearts before hearts are broken.)
 */
public class LeadRestrictedTest {

    @Test
    public void theRestrictedSuitMayNotBeLedWhileTheHandHoldsAnythingElse() {
        // leading, spades restricted: every non-spade, in hand order
        assertEquals(cards("2H", "3D", "7C"),
                PlayRule.leadRestricted(Spades).legalPlays(cards("KS", "2H", "3D", "9S", "7C"), trick(0)));
    }

    @Test
    public void aHandOfOnlyTheRestrictedSuitMayLeadAnyOfIt() {
        assertEquals(cards("KS", "2S", "9S"),
                PlayRule.leadRestricted(Spades).legalPlays(cards("KS", "2S", "9S"), trick(0)));
    }

    @Test
    public void theRestrictedSuitIsWhicheverSuitIsGiven() {
        // hearts restricted: the spade and club may be led, the hearts may not
        assertEquals(cards("KS", "5C"),
                PlayRule.leadRestricted(Hearts).legalPlays(cards("2H", "KS", "9H", "5C"), trick(0)));
    }

    @Test
    public void aPlayerHoldingTheLeadSuitMustFollowIt() {
        // hearts led, spades restricted: only the hearts
        assertEquals(cards("2H", "9H"),
                PlayRule.leadRestricted(Spades).legalPlays(cards("KS", "2H", "3D", "9H"), trick(0, "QH")));
    }

    @Test
    public void whenTheRestrictedSuitIsLedAPlayerHoldingItMustFollowWithIt() {
        // spades led (by a player holding only spades): the follower must play a spade
        assertEquals(cards("5S", "9S"),
                PlayRule.leadRestricted(Spades).legalPlays(cards("2H", "5S", "3D", "9S"), trick(0, "QS")));
    }

    @Test
    public void aPlayerVoidInTheLeadSuitMayPlayAnythingIncludingTheRestrictedSuit() {
        // clubs led, no clubs: the spade may be discarded (restriction applies only to leading)
        assertEquals(cards("KS", "2H", "3D"),
                PlayRule.leadRestricted(Spades).legalPlays(cards("KS", "2H", "3D"), trick(0, "QC")));
    }
}
