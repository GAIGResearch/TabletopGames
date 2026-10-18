package games.pitch;

import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.pitch.PitchTestUtils.*;
import static org.junit.Assert.*;

/**
 * Known voids: which plays record them, rememberVoids, clearing them at a new deal, and their use in copy(p).
 */
public class PitchVoidsTest {

    PitchForwardModel fm;
    PitchGameState state;
    PitchParameters params;

    @Before
    public void setup() {
        fm = new PitchForwardModel();
        state = newState(61, fm);
        params = (PitchParameters) state.getGameParameters();
        assertTrue(params.rememberVoids);
        for (int p = 0; p < 4; p++)
            assertEquals(Set.of(), state.getKnownVoids().get(p));
    }

    private void assertVoids(Set<?> v0, Set<?> v1, Set<?> v2, Set<?> v3) {
        PitchTestUtils.assertVoids(state, v0, v1, v2, v3);
    }

    /** Trumps Hearts; player 1 has led 5C; player 2 (no clubs, no hearts) is to act. */
    private void clubLedToAPlayerWithNoClubsOrHearts() {
        giveHand(state, 2, "2D", "3S");
        startPlay(state, 1, 2, Hearts, 1);
        arrangeTrick(state, 1, "5C");
    }

    /** Trumps Hearts; player 1 has led 5H; player 2 (no hearts) is to act. */
    private void trumpLedToAPlayerWithNoTrumps() {
        giveHand(state, 2, "2D", "3S");
        startPlay(state, 1, 2, Hearts, 1);
        arrangeTrick(state, 1, "5H");
    }

    // ---------------------------------------------------------------- recording voids

    @Test
    public void discardingOnANonTrumpLeadShowsAVoidInTheSuitLed() {
        clubLedToAPlayerWithNoClubsOrHearts();
        play(state, fm, 2, "2D");
        // clubs led (not trumps), 2D is neither a club nor a heart: player 2 could not follow -> void in Clubs
        assertVoids(Set.of(), Set.of(), Set.of(Clubs), Set.of());
    }

    @Test
    public void notFollowingATrumpLeadShowsAVoidInTrumps() {
        trumpLedToAPlayerWithNoTrumps();
        play(state, fm, 2, "2D");
        // hearts (trumps) led, 2D is not a trump -> void in Hearts
        assertVoids(Set.of(), Set.of(), Set.of(Hearts), Set.of());
    }

    @Test
    public void trumpingANonTrumpLeadRevealsNothing() {
        // player 2 holds a club but trumps with 2H - allowed, so it says nothing about clubs
        giveHand(state, 2, "6C", "2H");
        startPlay(state, 1, 2, Hearts, 1);
        arrangeTrick(state, 1, "5C");
        play(state, fm, 2, "2H");
        assertVoids(Set.of(), Set.of(), Set.of(), Set.of());
    }

    @Test
    public void followingSuitRevealsNothing() {
        // clubs led, then trumps led: following suit each time records nothing
        giveHand(state, 2, "6C", "2D");
        giveHand(state, 3, "7H", "2S");
        startPlay(state, 1, 2, Hearts, 1);
        arrangeTrick(state, 1, "5C");
        play(state, fm, 2, "6C");
        arrangeTrick(state, 2, "4H");
        play(state, fm, 3, "7H");
        assertVoids(Set.of(), Set.of(), Set.of(), Set.of());
    }

    @Test
    public void leadingRevealsNothing() {
        // the pitcher's first lead sets trumps (Diamonds) and reveals nothing
        giveHand(state, 1, "2D", "3C");
        startPlay(state, 1, 2, null, 1);
        play(state, fm, 1, "2D");
        assertEquals(Diamonds, state.getTrumpSuit());
        assertVoids(Set.of(), Set.of(), Set.of(), Set.of());

        // a later lead of a non-trump into an empty trick reveals nothing either
        arrangeTrick(state, 1);
        assertEquals(1, state.getCurrentPlayer());
        play(state, fm, 1, "3C");
        assertVoids(Set.of(), Set.of(), Set.of(), Set.of());
    }

    @Test
    public void voidsAccumulateForAPlayer() {
        // player 2 shows a void in Clubs, then (trumps led) a void in Hearts
        giveHand(state, 2, "2D", "3D");
        startPlay(state, 1, 2, Hearts, 1);
        arrangeTrick(state, 1, "5C");
        play(state, fm, 2, "2D");
        arrangeTrick(state, 1, "5H");
        play(state, fm, 2, "3D");
        assertVoids(Set.of(), Set.of(), EnumSet.of(Clubs, Hearts), Set.of());
    }

    // ---------------------------------------------------------------- rememberVoids false

    @Test
    public void withoutRememberVoidsADiscardOnANonTrumpLeadRecordsNothing() {
        params.setParameterValue("rememberVoids", false);
        clubLedToAPlayerWithNoClubsOrHearts();
        play(state, fm, 2, "2D");
        assertVoids(Set.of(), Set.of(), Set.of(), Set.of());
    }

    @Test
    public void withoutRememberVoidsNotFollowingATrumpLeadRecordsNothing() {
        params.setParameterValue("rememberVoids", false);
        trumpLedToAPlayerWithNoTrumps();
        play(state, fm, 2, "2D");
        assertVoids(Set.of(), Set.of(), Set.of(), Set.of());
    }

    // ---------------------------------------------------------------- a new deal

    @Test
    public void voidsAreClearedAtANewDeal() {
        params.setParameterValue("targetScore", 21);
        // last trick, trumps Hearts: 0 leads 10S; 1 discards 10D (void in Spades); 2 trumps 2H; 3 follows 5S
        arrangeLastTrick(state, 0, new int[]{3, 0, 0, 0}, 0,
                new String[]{"AH", "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C",
                        "2D", "3D", "4D", "5D", "6D", "7D", "8D"},
                new String[]{"3H", "9D", "2S", "3S"},
                "10S", "10D", "2H", "5S");
        play(state, fm, 0, "10S");
        play(state, fm, 1, "10D");
        play(state, fm, 2, "2H");
        assertVoids(Set.of(), Set.of(Spades), Set.of(), Set.of());

        // team 0 (bid 3) scores at most 4 < 21: the next deal starts in the same fm.next, and voids are forgotten
        play(state, fm, 3, "5S");
        assertEquals(1, state.getRoundCounter());
        assertEquals(PitchGameState.Phase.BIDDING, state.getGamePhase());
        assertVoids(Set.of(), Set.of(), Set.of(), Set.of());
    }

    // ---------------------------------------------------------------- copy(p) respects voids

    /**
     * Mid-deal, trumps Hearts, seen by player 0. Player 1 is known void in Hearts and player 2 in Clubs (their real
     * hands agree). Player 3 has no known voids.
     * Hidden from player 0: hands 1 (5), 2 (4), 3 (4) and the undealt deck - 41 cards, with 10 Hearts
     * (all but 4H in hand 0, QH in the trick, 3H won) and 7 Clubs (3C 4C 9C 10C JC QC AC).
     * Without the constraint, a copy gives player 1 no heart with chance about (31/41)^5 = 0.25 and player 2 no
     * club about (34/41)^4 = 0.47, so 50 copies all obeying the voids by luck is about 1e-30 and 1e-16.
     * Player 3 gets at least one heart in a copy with chance about 1 - (31/41)^4 = 0.67, never in 50 about 1e-24.
     */
    @Test
    public void aPlayersCopyDealsNoCardOfAKnownVoidSuit() {
        setTeamTricks(state, 0, "3H", "2C", "KC", "9D");
        giveHand(state, 0, "4H", "5C", "6C", "7C", "8C");
        giveHand(state, 1, "2D", "3D", "4D", "5D", "6D");
        giveHand(state, 2, "5H", "2S", "3S", "4S");
        giveHand(state, 3, "6H", "5S", "6S", "7S");
        startPlay(state, 1, 3, Hearts, 0);
        arrangeTrick(state, 2, "QH", "JS");
        state.knownVoids.get(1).add(Hearts);
        state.knownVoids.get(2).add(Clubs);
        assertAllCardsPresent(state);

        int player3GotAHeart = 0;
        for (int i = 0; i < 50; i++) {
            PitchGameState copy = (PitchGameState) state.copy(0);
            assertAllCardsPresent(copy);
            assertEquals(Set.of(Hearts), copy.getKnownVoids().get(1));
            assertEquals(Set.of(Clubs), copy.getKnownVoids().get(2));
            for (FrenchCard c : copy.getPlayerHand(1).getComponents())
                assertNotEquals("player 1 is void in Hearts but was dealt " + c, Hearts, c.suite);
            for (FrenchCard c : copy.getPlayerHand(2).getComponents())
                assertNotEquals("player 2 is void in Clubs but was dealt " + c, Clubs, c.suite);
            if (copy.getPlayerHand(3).getComponents().stream().anyMatch(c -> c.suite == Hearts))
                player3GotAHeart++;
        }
        assertTrue("player 3 (no voids) never received a heart in 50 copies", player3GotAHeart > 0);
    }

    @Test
    public void aFullCopyKeepsTheVoidsIndependently() {
        state.knownVoids.get(1).add(Hearts);
        PitchGameState copy = (PitchGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(Set.of(Hearts), copy.getKnownVoids().get(1));
        copy.knownVoids.get(1).add(Clubs);
        assertNotEquals(state, copy);
        assertEquals(Set.of(Hearts), state.getKnownVoids().get(1));
    }
}
