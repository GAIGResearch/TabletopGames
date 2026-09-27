package games.skitgubbe;

import core.CoreConstants;
import core.actions.AbstractAction;
import games.skitgubbe.actions.PickUp;
import games.skitgubbe.actions.PlayCard;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.skitgubbe.SkitgubbeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Phase two, arranged with arrangePhaseTwo (3 players A = 0, B = 1, C = 2 unless stated; trumps Hearts).
 * The discard pile starts with the rest of the pack, so tests assert on its change.
 */
public class SkitgubbePhaseTwoTest {

    SkitgubbeForwardModel fm = new SkitgubbeForwardModel();

    private SkitgubbeGameState newThree() {
        return newState(3, 42, new SkitgubbeParameters());
    }

    private void play(SkitgubbeGameState state, String code) {
        fm.next(state, new PlayCard(card(code)));
    }

    private void pickUp(SkitgubbeGameState state) {
        fm.next(state, new PickUp());
    }

    private static void assertActions(List<AbstractAction> actual, AbstractAction... expected) {
        assertEquals(Set.of(expected), new HashSet<>(actual));
        assertEquals(expected.length, actual.size());
    }

    private static PlayCard pc(String code) {
        return new PlayCard(card(code));
    }

    // ---------------- actions ----------------

    @Test
    public void onAnEmptyTrickThePlayerMayLeadAnyCollectedCardAndMayNotPickUp() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S 9S 2H KD", "3C", "4C");
        assertActions(fm.computeAvailableActions(state), pc("5S"), pc("9S"), pc("2H"), pc("KD"));
    }

    @Test
    public void onANonEmptyTrickThePlayerMayPlayOnlyBeatingCardsOrPickUp() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 1, "3C", "5S 9S 2H KD", "4C");
        moveTo(state, state.trick, "7S");
        // on 7S: 9S (higher Spade) and 2H (a trump) beat; 5S is lower, KD is a third suit
        assertActions(fm.computeAvailableActions(state), pc("9S"), pc("2H"), new PickUp());
    }

    @Test
    public void onATrumpOnlyAHigherTrumpIsOfferedWithPickUp() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 1, "3C", "3H 8H AS", "4C");
        moveTo(state, state.trick, "5H");
        // 8H > 5H beats; 3H is a lower trump; AS is not a trump
        assertActions(fm.computeAvailableActions(state), pc("8H"), new PickUp());
    }

    @Test
    public void aPlayerWhoCannotBeatTheTopCardMayOnlyPickUp() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 1, "3C", "2S KD", "4C");
        moveTo(state, state.trick, "7S");
        assertActions(fm.computeAvailableActions(state), new PickUp());
    }

    @Test
    public void onlyTheTopCardOfTheTrickCountsForBeating() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 2, "3C", "4C", "8S 10S");
        moveTo(state, state.trick, "5S", "9S");   // 9S on top of 5S
        // 10S beats 9S; 8S beats only the bottom card 5S, so is not offered
        assertActions(fm.computeAvailableActions(state), pc("10S"), new PickUp());
    }

    // ---------------- effects of one action ----------------

    @Test
    public void pickUpTakesOnlyTheTopCardOfTheTrickIntoTheCollectedCards() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 2, "3C", "4C", "2S 5D");
        moveTo(state, state.trick, "5S", "9S");   // 9S on top
        pickUp(state);
        assertEquals(List.of(card("5S")), state.trick.getComponents());
        assertEquals(setOf("2S", "5D", "9S"), asSet(state.collectedCards.get(2)));
        assertEquals(1, state.phaseTwoActions);
        assertAllCardsPresent(state);
    }

    @Test
    public void aPlayedCardLeavesTheCollectedCardsForTheTopOfTheTrickAndTheNextHolderActs() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S 2C", "9S 3C", "JS 4C");
        play(state, "5S");
        assertEquals(List.of(card("5S")), state.trick.getComponents());
        assertEquals(setOf("2C"), asSet(state.collectedCards.get(0)));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.phaseTwoActions);
    }

    @Test
    public void everyPhaseTwoActionCountsTowardsPhaseTwoActions() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S 2C", "9S 3C", "JS 4C");
        play(state, "5S");
        play(state, "9S");
        pickUp(state);
        assertEquals(3, state.phaseTwoActions);   // play + play + pick up
    }

    // ---------------- trick completion and turn order ----------------

    @Test
    public void aTrickOfTrickSizeCardsIsDiscardedAndThePlayerAfterTheCompleterActs() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S 2C", "9S 3C", "JS 4C");
        int discardBefore = state.discardPile.getSize();
        play(state, "5S");     // A leads
        play(state, "9S");     // B beats
        assertEquals(2, state.trick.getSize());   // 2 < trickSize 3: not complete
        assertEquals(2, state.getCurrentPlayer());
        play(state, "JS");     // C beats: 3 cards = trickSize 3, complete

        assertEquals(0, state.trick.getSize());
        assertEquals(discardBefore + 3, state.discardPile.getSize());
        assertTrue(state.discardPile.getComponents().containsAll(cards("5S", "9S", "JS")));
        assertEquals(0, state.getCurrentPlayer());   // the next holder after C (2) is (2 + 1) % 3 = A
        assertEquals(3, state.trickSize);            // A, B, C all still hold one card each
        assertEquals(3, state.phaseTwoActions);
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void pagatExampleOneAPickUpLeavesTheLeaderToBeatHisOwnCardAndTheTrickCompletesLater() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S 7S 2C", "6S 10S 3C", "4C 5C");
        int discardBefore = state.discardPile.getSize();
        play(state, "5S");     // A leads
        play(state, "6S");     // B beats
        pickUp(state);         // C picks up B's 6S
        assertEquals(List.of(card("5S")), state.trick.getComponents());
        assertEquals(setOf("4C", "5C", "6S"), asSet(state.collectedCards.get(2)));
        assertEquals(0, state.getCurrentPlayer());   // the next holder after C is A
        // A must beat his own 5S (7S does; 2C cannot) or pick it up
        assertActions(fm.computeAvailableActions(state), pc("7S"), new PickUp());

        play(state, "7S");     // A beats: trick 5S, 7S (2 < 3)
        assertEquals(1, state.getCurrentPlayer());
        play(state, "10S");    // B beats: 3 cards = trickSize 3, complete
        assertEquals(0, state.trick.getSize());
        assertEquals(discardBefore + 3, state.discardPile.getSize());
        assertTrue(state.discardPile.getComponents().containsAll(cards("5S", "7S", "10S")));
        assertEquals(2, state.getCurrentPlayer());   // the next holder after B is C (default, not the completer)
        assertEquals(3, state.trickSize);            // A {2C}, B {3C}, C {4C 5C 6S}
        assertEquals(5, state.phaseTwoActions);
        assertAllCardsPresent(state);
    }

    @Test
    public void pagatExampleTwoPickingUpTheLastCardEmptiesTheTrickAndTheNextPlayerLeads() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S 2C", "6S 3C", "4C");
        int discardBefore = state.discardPile.getSize();
        play(state, "5S");     // A leads
        play(state, "6S");     // B beats
        pickUp(state);         // C picks up 6S
        pickUp(state);         // A picks up his own 5S: the trick is empty
        assertEquals(0, state.trick.getSize());
        assertEquals(setOf("2C", "5S"), asSet(state.collectedCards.get(0)));
        assertEquals(setOf("4C", "6S"), asSet(state.collectedCards.get(2)));
        assertEquals(discardBefore, state.discardPile.getSize());   // nothing discarded
        assertEquals(1, state.getCurrentPlayer());   // the next holder after A is B, who leads
        assertEquals(3, state.trickSize);
        // a new trick: B may lead either card and may not pick up
        assertActions(fm.computeAvailableActions(state), pc("3C"));
        assertEquals(4, state.phaseTwoActions);
        assertAllCardsPresent(state);
    }

    // ---------------- exits ----------------

    @Test
    public void pagatExampleThreeAPlayerOutIsSkippedButTheTrickStillNeedsTrickSizeCards() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S", "6S 8S 3C", "9S 4C");
        int discardBefore = state.discardPile.getSize();
        play(state, "5S");     // A leads his last card: out with exitScore = trickSize 3
        assertEquals(3, state.getExitScore(0));
        assertEquals(3, state.trickSize);            // fixed for the trick although only 2 now hold cards
        assertEquals(1, state.getCurrentPlayer());
        play(state, "6S");     // B beats
        pickUp(state);         // C picks up 6S
        assertEquals(1, state.getCurrentPlayer());   // after C: A holds nothing and is skipped -> B
        play(state, "8S");     // B beats A's 5S: trick 5S, 8S (2 < 3)
        assertEquals(2, state.trick.getSize());
        assertEquals(2, state.getCurrentPlayer());
        play(state, "9S");     // C beats: 3 cards = trickSize 3, complete

        assertEquals(0, state.trick.getSize());
        assertEquals(discardBefore + 3, state.discardPile.getSize());
        assertTrue(state.discardPile.getComponents().containsAll(cards("5S", "8S", "9S")));
        assertEquals(2, state.trickSize);            // new trick: B {3C}, C {4C 6S}
        assertEquals(1, state.getCurrentPlayer());   // after C: A is out, so B
        assertTrue(state.isNotTerminal());
        assertEquals(3, state.getExitScore(0));
        assertEquals(0, state.getExitScore(1));
        assertEquals(0, state.getExitScore(2));
        assertAllCardsPresent(state);
    }

    @Test
    public void aFollowerPlayingTheirLastCardScoresTheTrickSize() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 0, "5S 2C 6D", "9S", "JS 4C");
        play(state, "5S");
        play(state, "9S");     // B's last card: exitScore = trickSize 3
        assertEquals(3, state.getExitScore(1));
        assertEquals(0, state.getExitScore(0));
        assertEquals(2, state.getCurrentPlayer());
        play(state, "JS");     // C completes (3 cards)
        assertEquals(0, state.getCurrentPlayer());   // after C: A holds 2C, 6D
        play(state, "2C");     // new trick of trickSize 2 (A, C hold)
        assertEquals(2, state.trickSize);
        assertEquals(2, state.getCurrentPlayer());   // after A: B is out, so C
    }

    // ---------------- game end ----------------

    @Test
    public void theGameEndsWhenOnlyOnePlayerHoldsCards() {
        SkitgubbeGameState state = newThree();
        // A went out earlier in a trick of 3; B and C hold cards, a trick of 2 with B to lead
        arrangePhaseTwo(state, "H", 1, "", "5S", "7S 2C");
        state.exitScores[0] = 3;
        play(state, "5S");     // B's last card: exitScore = trickSize 2; only C holds cards, so the game is over
        assertEquals(2, state.getExitScore(1));
        // RECYCLE checks the end after every turn: C does not answer the unfinished trick
        assertFalse(state.isNotTerminal());
        assertEquals(1, state.trick.getSize());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(3.0, state.getGameScore(0), 0.0);
        assertEquals(2.0, state.getGameScore(1), 0.0);
        assertEquals(0.0, state.getGameScore(2), 0.0);   // still holding 7S 2C
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(1));
        assertEquals(3, state.getOrdinalPosition(2));
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void theLastHolderLosesEvenWhenTheirLastCardCouldBeatTheTrick() {
        SkitgubbeGameState state = newThree();
        arrangePhaseTwo(state, "H", 1, "", "5S", "7S");
        state.exitScores[0] = 3;
        play(state, "5S");     // B out: exitScore = trickSize 2; C (7S, which would beat 5S) is the only holder

        assertFalse(state.isNotTerminal());
        assertEquals(List.of(card("7S")), state.collectedCards.get(2).getComponents());
        assertEquals(3, state.getExitScore(0));
        assertEquals(2, state.getExitScore(1));
        assertEquals(0, state.getExitScore(2));
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(1));
        assertEquals(3, state.getOrdinalPosition(2));
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    private SkitgubbeGameState capped(int maxPhaseTwoActions) {
        SkitgubbeParameters params = new SkitgubbeParameters();
        params.setParameterValue("maxPhaseTwoActions", maxPhaseTwoActions);
        SkitgubbeGameState state = newState(4, 42, params);
        // D (3) went out earlier with exitScore 4; A, B, C hold two cards each
        arrangePhaseTwo(state, "H", 0, "5S 2C", "9S 3C", "JS 4C", "");
        state.exitScores[3] = 4;
        play(state, "5S");
        play(state, "9S");
        assertTrue("2 actions < cap " + maxPhaseTwoActions, state.isNotTerminal());
        play(state, "JS");     // 3 cards = trickSize 3: complete, trick empty, 3 phase-two actions
        return state;
    }

    @Test
    public void theGameEndsOnceMaxPhaseTwoActionsAreTakenWithHoldersScoringZero() {
        SkitgubbeGameState state = capped(3);
        // 3 actions >= cap 3: game over although A, B, C still hold cards
        assertFalse(state.isNotTerminal());
        assertEquals(3, state.phaseTwoActions);
        for (int p = 0; p < 3; p++)
            assertEquals("score of holder " + p, 0.0, state.getGameScore(p), 0.0);
        assertEquals(4.0, state.getGameScore(3), 0.0);
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, LOSE_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void theGameContinuesWhileFewerThanMaxPhaseTwoActionsAreTaken() {
        SkitgubbeGameState state = capped(4);
        // 3 actions < cap 4 and three players hold cards
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getCurrentPlayer());   // after C: D is out, so A
    }
}
