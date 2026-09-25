package games.president;

import games.president.actions.Pass;
import games.president.actions.PlayCards;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static games.president.PresidentTestUtils.*;
import static org.junit.Assert.*;

/**
 * Turn order within a trick, clearing the play pile, who leads the next trick, and going out with its scores.
 */
public class PresidentTrickTest {

    PresidentForwardModel fm;

    @Before
    public void setup() {
        fm = new PresidentForwardModel();
    }

    @Test
    public void aPlayerWhoPassedMayPlayLaterInTheSameTrick() {
        PresidentGameState state = newState(4, 31, fm);
        giveHand(state, 0, "5H", "9S", "KC");
        giveHand(state, 1, "8D", "3C", "JC");
        giveHand(state, 2, "7S", "4D");
        giveHand(state, 3, "6C", "3D");

        play(state, fm, 0, 5, 1);
        pass(state, fm, 1);
        play(state, fm, 2, 7, 1);
        pass(state, fm, 3);
        pass(state, fm, 0);
        // passes in a row since the 7: players 3 and 0 = 2, fewer than the 3 other holders - the trick goes on
        assertEquals(2, state.getPassesInRow());
        assertEquals(List.of(7, 5), numbers(state.getPlayPile()));
        // player 1 passed on the 5 but may now beat the 7 with the 8 or the Jack
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new Pass(), new PlayCards(8, 1), new PlayCards(11, 1)), actionSet(state, fm));

        play(state, fm, 1, 8, 1);
        assertEquals(1, state.getLastPlayer());
        assertEquals(0, state.getPassesInRow());
        assertEquals(List.of(8, 7, 5), numbers(state.getPlayPile()));
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void thePileIsClearedWhenAllOtherPlayersHavePassedAndTheLastPlayerLeads() {
        PresidentGameState state = newState(4, 32, fm);
        giveHand(state, 0, "5H", "9S");
        giveHand(state, 1, "3C", "4C");
        giveHand(state, 2, "3D", "4D");
        giveHand(state, 3, "3S", "4S");
        int discards = state.getDiscardPile().getSize();

        play(state, fm, 0, 5, 1);
        pass(state, fm, 1);
        pass(state, fm, 2);
        // 2 passes, 3 other holders: not yet cleared
        assertEquals(List.of(5), numbers(state.getPlayPile()));
        assertEquals(2, state.getPassesInRow());
        assertEquals(3, state.getCurrentPlayer());

        pass(state, fm, 3);
        // 3 passes = the 3 other holders: the 5 goes to the discard pile and player 0 leads again
        assertEquals(0, state.getPlayPile().getSize());
        assertEquals(discards + 1, state.getDiscardPile().getSize());
        assertTrue(state.getDiscardPile().contains(card("5H")));
        assertEquals(0, state.getSetSize());
        assertEquals(-1, state.getLastPlayer());
        assertEquals(0, state.getPassesInRow());
        assertEquals(0, state.getCurrentPlayer());
        // leading: no Pass
        assertEquals(Set.of(new PlayCards(9, 1)), actionSet(state, fm));
        assertAllCardsPresent(state);
    }

    @Test
    public void playersWhoAreOutAreSkippedAndNotCountedForTheClearing() {
        // 5 players, player 2 is already out (the President); player 1 leads
        PresidentGameState state = newState(5, 33, fm);
        arrangeOut(state, 2);
        state.playerScores[2] = 2;
        giveHand(state, 1, "5H", "9S");
        giveHand(state, 0, "3C", "4C");
        giveHand(state, 3, "3D", "4D");
        giveHand(state, 4, "3S", "4S");
        state.setTurnOwner(1);

        play(state, fm, 1, 5, 1);
        assertEquals(3, state.getCurrentPlayer());   // 2 is skipped
        pass(state, fm, 3);
        pass(state, fm, 4);
        // 2 passes, 3 other holders (0, 3, 4): not yet cleared
        assertEquals(List.of(5), numbers(state.getPlayPile()));
        assertEquals(0, state.getCurrentPlayer());
        pass(state, fm, 0);
        // 3 passes = the other holders 0, 3 and 4 (player 2 does not count): cleared, player 1 leads
        assertEquals(0, state.getPlayPile().getSize());
        assertEquals(0, state.getSetSize());
        assertEquals(-1, state.getLastPlayer());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new PlayCards(9, 1)), actionSet(state, fm));
        assertAllCardsPresent(state);
    }

    @Test
    public void whenTheLastPlayerWentOutTheNextHolderClockwiseLeads() {
        // 5 players, player 2 already out; player 0 leads a 5 and player 1 goes out on it with their last card
        PresidentGameState state = newState(5, 34, fm);
        arrangeOut(state, 2);
        state.playerScores[2] = 2;
        giveHand(state, 0, "5H", "3C");
        giveHand(state, 1, "9S");
        giveHand(state, 3, "3D", "4D");
        giveHand(state, 4, "3S", "4S");

        play(state, fm, 0, 5, 1);
        play(state, fm, 1, 9, 1);
        // player 1 is the second out: vicePresidentPoints = 1
        assertEquals(List.of(2, 1), state.getFinishingOrder());
        assertEquals(1, state.getPlayerScore(1));
        assertEquals(2, state.getPlayerScore(2));
        assertEquals(0, state.getPlayerScore(0));
        assertTrue(state.isNotTerminal());
        assertEquals(3, state.getCurrentPlayer());   // 2 is out

        pass(state, fm, 3);
        pass(state, fm, 4);
        assertEquals(List.of(9, 5), numbers(state.getPlayPile()));
        pass(state, fm, 0);
        // 3 passes = holders other than player 1 (0, 3, 4): cleared. Player 1 is out, player 2 too, so 3 leads
        assertEquals(0, state.getPlayPile().getSize());
        assertEquals(-1, state.getLastPlayer());
        assertEquals(0, state.getSetSize());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Set.of(new PlayCards(3, 1), new PlayCards(4, 1)), actionSet(state, fm));
        assertAllCardsPresent(state);
    }

    /** 4 players: player 0 leads their whole hand (a pair of 7s), then player 1 goes out with a pair of 9s. */
    private PresidentGameState twoPlayersGoOut(PresidentParameters params) {
        PresidentGameState state = newState(params, 4, 35, fm);
        giveHand(state, 0, "7H", "7D");
        giveHand(state, 1, "9S", "9D");
        giveHand(state, 2, "3C", "4C");
        giveHand(state, 3, "3D", "4D");

        play(state, fm, 0, 7, 2);
        assertEquals(List.of(0), state.getFinishingOrder());
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getLastPlayer());
        assertEquals(2, state.getSetSize());
        play(state, fm, 1, 9, 2);
        assertEquals(List.of(0, 1), state.getFinishingOrder());
        // players 2 and 3 still hold cards
        assertTrue(state.isNotTerminal());
        assertEquals(2, state.getCurrentPlayer());
        assertAllCardsPresent(state);
        return state;
    }

    @Test
    public void theFirstOutScoresPresidentPointsAndTheSecondVicePresidentPoints() {
        PresidentGameState state = twoPlayersGoOut(new PresidentParameters());
        // defaults: presidentPoints 2, vicePresidentPoints 1
        assertEquals(2, state.getPlayerScore(0));
        assertEquals(1, state.getPlayerScore(1));
        assertEquals(0, state.getPlayerScore(2));
        assertEquals(0, state.getPlayerScore(3));
    }

    @Test
    public void thePointsForGoingOutComeFromTheParameters() {
        PresidentParameters params = new PresidentParameters();
        params.setParameterValue("presidentPoints", 5);
        params.setParameterValue("vicePresidentPoints", 3);
        PresidentGameState state = twoPlayersGoOut(params);
        assertEquals(5, state.getPlayerScore(0));
        assertEquals(3, state.getPlayerScore(1));
        assertEquals(0, state.getPlayerScore(2));
        assertEquals(0, state.getPlayerScore(3));
    }

    @Test
    public void theThirdOutScoresNothing() {
        // 5 players: 2 and 1 are out (2 and 1 points); player 0 led a 5 and player 3 plays their last card
        PresidentGameState state = newState(5, 36, fm);
        arrangeOut(state, 2, 1);
        state.playerScores[2] = 2;
        state.playerScores[1] = 1;
        giveHand(state, 3, "6C");
        giveHand(state, 4, "3S", "4S");
        giveHand(state, 0, "3C", "4C");
        arrangeTrick(state, 0, 1, 0, 3, "5H");

        play(state, fm, 3, 6, 1);
        assertEquals(List.of(2, 1, 3), state.getFinishingOrder());
        assertEquals(0, state.getPlayerScore(3));
        assertEquals(1, state.getPlayerScore(1));
        assertEquals(2, state.getPlayerScore(2));
        // players 4 and 0 still hold cards
        assertTrue(state.isNotTerminal());
        assertEquals(4, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }
}
