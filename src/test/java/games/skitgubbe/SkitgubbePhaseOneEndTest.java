package games.skitgubbe;

import games.skitgubbe.actions.PlayCard;
import org.junit.Test;

import core.components.FrenchCard;

import static games.skitgubbe.SkitgubbeTestUtils.*;
import static org.junit.Assert.*;

/**
 * The end of phase one (the player to act has an empty hand) and the change to phase two. Late positions are
 * arranged with an empty draw deck and small hands. Tests stop at the start of phase two (no phase-two actions).
 */
public class SkitgubbePhaseOneEndTest {

    private final SkitgubbeForwardModel fm = new SkitgubbeForwardModel();

    private void play(SkitgubbeGameState state, String code) {
        fm.next(state, new PlayCard(card(code)));
    }

    private static void assertPhaseOneLocationsEmpty(SkitgubbeGameState state) {
        for (int p = 0; p < state.getNPlayers(); p++) {
            assertEquals("hand of " + p, 0, state.playerHands.get(p).getSize());
            assertEquals("held of " + p, 0, state.heldCards.get(p).getSize());
        }
        assertEquals(0, state.drawDeck.getSize());
        assertEquals(0, state.trumpCard.getSize());
        assertEquals(0, state.trick.getSize());
    }

    @Test
    public void phaseOneEndsWhenTheWinnerWhoShouldLeadHasNoCards() {
        SkitgubbeGameState state = newState(3, 42, SkitgubbeTestUtils.valetParams());
        moveTo(state, state.heldCards.get(2), "QC");  // player 2's only card, held from a bounce
        giveTrumpCard(state, 1, "JD");
        giveHand(state, 0, "KS");
        giveHand(state, 1, "5H", "8C");
        giveHand(state, 2);
        leaveDrawDeck(state, 1);                      // the other 52 - 3 - 1 - 1 = 47 cards are player 1's
        assertEquals(47, state.collectedCards.get(1).getSize());

        play(state, "KS");
        play(state, "5H");                            // King 13 > 5: player 0 wins and should lead, with no cards

        assertEquals(SkitgubbeGameState.Phase.PHASE_TWO, state.getGamePhase());
        assertPhaseOneLocationsEmpty(state);
        // player 0: the trick (KS, 5H) + player 2's held QC = 3
        assertEquals(setOf("KS", "5H", "QC"), asSet(state.collectedCards.get(0)));
        // player 1: 47 + the hand's 8C + the trump card JD = 49
        assertEquals(49, state.collectedCards.get(1).getSize());
        assertTrue(state.collectedCards.get(1).contains(card("8C")));
        assertTrue(state.collectedCards.get(1).contains(card("JD")));
        assertEquals(0, state.collectedCards.get(2).getSize());
        assertEquals(FrenchCard.Suite.Diamonds, state.getTrumpSuit());
        assertEquals(1, state.getTrumpPlayer());
        assertEquals(1, state.getCurrentPlayer());    // the trump player leads phase two
        assertEquals(2, state.getTrickSize());        // players 0 and 1 hold cards
        assertEquals(0, state.getExitScore(0));
        assertEquals(0, state.getExitScore(1));
        assertEquals(2, state.getExitScore(2));       // out at once, scoring trickSize 2
        assertAllCardsPresent(state);
    }

    @Test
    public void phaseOneEndsWhenTheFollowerHasNoCardsAndTheLeadersCardGoesBack() {
        SkitgubbeGameState state = newState(3, 42, SkitgubbeTestUtils.valetParams());
        moveTo(state, state.heldCards.get(0), "10H"); // an earlier bounce between players 2 and 0
        moveTo(state, state.heldCards.get(2), "10S");
        giveTrumpCard(state, 0, "3S");
        giveHand(state, 0, "QD", "2H");
        giveHand(state, 1, "6C");
        giveHand(state, 2);
        leaveDrawDeck(state, 1);                      // the other 52 - 3 - 2 - 1 = 46 cards are player 1's
        assertEquals(46, state.collectedCards.get(1).getSize());
        state.setTurnOwner(1);                        // player 1 leads, player 2 follows

        play(state, "6C");                            // the follower, player 2, has no cards

        assertEquals(SkitgubbeGameState.Phase.PHASE_TWO, state.getGamePhase());
        assertPhaseOneLocationsEmpty(state);
        // player 0: held 10H + hand QD, 2H + trump card 3S = 4
        assertEquals(setOf("10H", "QD", "2H", "3S"), asSet(state.collectedCards.get(0)));
        // player 1: 46 + the incomplete trick's 6C = 47
        assertEquals(47, state.collectedCards.get(1).getSize());
        assertTrue(state.collectedCards.get(1).contains(card("6C")));
        // player 2: held 10S = 1
        assertEquals(setOf("10S"), asSet(state.collectedCards.get(2)));
        assertEquals(FrenchCard.Suite.Spades, state.getTrumpSuit());
        assertEquals(0, state.getCurrentPlayer());    // the trump player
        assertEquals(3, state.getTrickSize());        // all three hold cards
        for (int p = 0; p < 3; p++)
            assertEquals("exit score of " + p, 0, state.getExitScore(p));
        assertAllCardsPresent(state);
    }

    @Test
    public void phaseOneEndsWhenTheLeaderHasNoCardsAfterABounceAndHeldCardsGoBack() {
        SkitgubbeGameState state = newState(4, 42, SkitgubbeTestUtils.valetParams());
        giveTrumpCard(state, 3, "KC");
        giveHand(state, 0);
        giveHand(state, 1);
        giveHand(state, 2, "7S");
        giveHand(state, 3, "7H", "4C");
        leaveDrawDeck(state, 0);                      // the other 52 - 3 - 1 = 48 cards are player 0's
        assertEquals(48, state.collectedCards.get(0).getSize());
        state.setTurnOwner(2);                        // player 2 leads, player 3 follows

        play(state, "7S");
        play(state, "7H");                            // a bounce: player 2 should lead again, with no cards

        assertEquals(SkitgubbeGameState.Phase.PHASE_TWO, state.getGamePhase());
        assertPhaseOneLocationsEmpty(state);
        assertEquals(48, state.collectedCards.get(0).getSize());
        assertEquals(0, state.collectedCards.get(1).getSize());
        assertEquals(setOf("7S"), asSet(state.collectedCards.get(2)));             // held 7S back
        assertEquals(setOf("7H", "4C", "KC"), asSet(state.collectedCards.get(3))); // held + hand + trump card
        assertEquals(FrenchCard.Suite.Clubs, state.getTrumpSuit());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(3, state.getTrickSize());        // players 0, 2 and 3 hold cards
        assertEquals(0, state.getExitScore(0));
        assertEquals(3, state.getExitScore(1));       // out at once, scoring trickSize 3
        assertEquals(0, state.getExitScore(2));
        assertEquals(0, state.getExitScore(3));
        assertAllCardsPresent(state);
    }
}
