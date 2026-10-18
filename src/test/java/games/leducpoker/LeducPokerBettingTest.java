package games.leducpoker;

import games.leducpoker.actions.Call;
import games.leducpoker.actions.Fold;
import games.leducpoker.actions.Raise;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static games.leducpoker.LeducPokerTestUtils.*;
import static org.junit.Assert.*;

/**
 * Action lists, chip arithmetic and the end of a betting round. Cards are arranged as player 0 QS, player 1 KS,
 * with JH on top of the draw deck (so JH becomes the board card); no test here reaches the showdown.
 */
public class LeducPokerBettingTest {

    LeducPokerParameters params;
    LeducPokerGameState state;
    LeducPokerForwardModel fm;

    static final Call CALL = new Call();
    static final Raise RAISE = new Raise();
    static final Fold FOLD = new Fold();

    @Before
    public void setup() {
        params = new LeducPokerParameters();
        params.setRandomSeed(42);
        newState();
    }

    private void newState() {
        state = new LeducPokerGameState(params, 2);
        fm = new LeducPokerForwardModel();
        fm.setup(state);
        arrange(state, card("QS"), card("KS"), card("JH"));
    }

    // ---- actions offered ----

    @Test
    public void firstActionOfTheHandOffersCallAndRaiseButNotFold() {
        // nothing is owed (1 v 1), so no Fold
        assertEquals(List.of(CALL, RAISE), fm.computeAvailableActions(state));
    }

    @Test
    public void afterACheckTheOpponentIsStillNotOfferedFold() {
        fm.next(state, CALL);
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(List.of(CALL, RAISE), fm.computeAvailableActions(state));
    }

    @Test
    public void facingARaiseThePlayerMayFoldCallOrReRaise() {
        fm.next(state, RAISE);
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(List.of(FOLD, CALL, RAISE), fm.computeAvailableActions(state));
    }

    @Test
    public void afterTwoRaisesInARoundNoFurtherRaiseIsOffered() {
        play(fm, state, RAISE, RAISE);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(FOLD, CALL), fm.computeAvailableActions(state));
    }

    @Test
    public void withOneRaisePerRoundNoReRaiseIsOffered() {
        // pair of facingARaiseThePlayerMayFoldCallOrReRaise, differing only in maxRaisesPerRound
        params.setParameterValue("maxRaisesPerRound", 1);
        newState();
        fm.next(state, RAISE);
        assertEquals(List.of(FOLD, CALL), fm.computeAvailableActions(state));
    }

    @Test
    public void raisesAreAllowedAgainInTheSecondRound() {
        // two raises used up in round 0; the count resets for round 1
        play(fm, state, RAISE, RAISE, CALL);
        assertEquals(1, state.getBettingRound());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(CALL, RAISE), fm.computeAvailableActions(state));
        play(fm, state, CALL, RAISE);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(FOLD, CALL, RAISE), fm.computeAvailableActions(state));
    }

    // ---- chip arithmetic ----

    @Test
    public void raiseAndReRaiseInTheFirstRoundAddTheFirstRoundRaise() {
        fm.next(state, RAISE);
        // player 0: ante 1 + 0 owed + raise 2 = 3
        assertEquals(3, state.getContribution(0));
        assertEquals(1, state.getContribution(1));
        assertEquals(1, state.getRaisesThisRound());
        assertEquals(2, state.amountToCall(1));

        fm.next(state, RAISE);
        // player 1: ante 1 + 2 owed + raise 2 = 5
        assertEquals(5, state.getContribution(1));
        assertEquals(3, state.getContribution(0));
        assertEquals(2, state.getRaisesThisRound());
        assertEquals(2, state.amountToCall(0));

        fm.next(state, CALL);
        // player 0: 3 + 2 owed = 5
        assertEquals(5, state.getContribution(0));
        assertEquals(5, state.getContribution(1));
        assertEquals("5 + 5", 10, state.getPot());
    }

    @Test
    public void checkPutsNothingIn() {
        fm.next(state, CALL);
        assertEquals(1, state.getContribution(0));
        assertEquals(1, state.getContribution(1));
        assertEquals(0, state.getRaisesThisRound());
    }

    @Test
    public void raiseAndReRaiseInTheSecondRoundAddTheSecondRoundRaise() {
        play(fm, state, CALL, CALL);
        assertEquals(1, state.getBettingRound());
        fm.next(state, RAISE);
        // player 0: 1 + 0 owed + raise 4 = 5
        assertEquals(5, state.getContribution(0));
        assertEquals(1, state.getRaisesThisRound());
        fm.next(state, RAISE);
        // player 1: 1 + 4 owed + raise 4 = 9
        assertEquals(9, state.getContribution(1));
        assertEquals(2, state.getRaisesThisRound());
        fm.next(state, CALL);
        // player 0: 5 + 4 owed = 9
        assertEquals(9, state.getContribution(0));
    }

    @Test
    public void raiseAmountsComeFromTheParameters() {
        params.setParameterValue("firstRoundRaise", 1);
        params.setParameterValue("secondRoundRaise", 8);
        newState();
        fm.next(state, RAISE);
        // 1 + raise 1 = 2
        assertEquals(2, state.getContribution(0));
        fm.next(state, CALL);
        // 1 + 1 owed = 2; round 0 over
        assertEquals(2, state.getContribution(1));
        assertEquals(1, state.getBettingRound());
        fm.next(state, RAISE);
        // 2 + raise 8 = 10
        assertEquals(10, state.getContribution(0));
    }

    // ---- end of a betting round ----

    @Test
    public void aSingleCheckDoesNotEndTheRound() {
        fm.next(state, CALL);
        assertEquals(0, state.getBoard().getSize());
        assertEquals(4, state.getDrawDeck().getSize());
        assertEquals(1, state.getActionsThisRound());
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void checkCheckEndsTheFirstRoundAndDealsTheBoardCardFromTheDrawDeck() {
        play(fm, state, CALL, CALL);
        assertEquals(1, state.getBoard().getSize());
        assertEquals("top of the draw deck", card("JH"), state.getBoard().get(0));
        assertEquals(3, state.getDrawDeck().getSize());
        assertFalse(state.getDrawDeck().contains(card("JH")));
        assertEquals(1, state.getBettingRound());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getRaisesThisRound());
        assertEquals(0, state.getActionsThisRound());
        assertEquals(1, state.getContribution(0));
        assertEquals(1, state.getContribution(1));
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void raiseCallEndsTheFirstRound() {
        play(fm, state, RAISE, CALL);
        assertEquals(1, state.getBoard().getSize());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getRaisesThisRound());
        assertEquals(0, state.getActionsThisRound());
        assertEquals(3, state.getContribution(0));
        assertEquals(3, state.getContribution(1));
    }

    @Test
    public void checkRaiseCallEndsTheFirstRoundWithPlayerZeroToActFirst() {
        // player 0 makes the closing call; passing the turn would give player 1, but round 1 starts with player 0
        play(fm, state, CALL, RAISE, CALL);
        assertEquals(1, state.getBoard().getSize());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getRaisesThisRound());
        assertEquals(0, state.getActionsThisRound());
        assertEquals(3, state.getContribution(0));
        assertEquals(3, state.getContribution(1));
    }

    @Test
    public void raiseReRaiseCallEndsTheFirstRound() {
        play(fm, state, RAISE, RAISE, CALL);
        assertEquals(1, state.getBoard().getSize());
        assertEquals(3, state.getDrawDeck().getSize());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getRaisesThisRound());
        assertEquals(0, state.getActionsThisRound());
        assertEquals(5, state.getContribution(0));
        assertEquals(5, state.getContribution(1));
    }

    @Test
    public void aCheckInTheSecondRoundPassesTheTurnWithoutEndingTheHand() {
        play(fm, state, CALL, CALL, CALL);
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getBoard().getSize());
        assertEquals(3, state.getDrawDeck().getSize());
        assertEquals(1, state.getActionsThisRound());
    }
}
