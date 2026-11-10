package games.perudo;

import core.AbstractForwardModel;
import core.Game;
import games.perudo.actions.Bid;
import games.perudo.actions.Dudo;
import org.junit.Test;

import java.util.Arrays;

import static core.CoreConstants.GameResult.*;
import static games.perudo.PerudoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Scripted games driven by fm.next on factory games: turn order, the round after a Dudo, elimination and game end.
 */
public class PerudoRoundFlowTest {

    PerudoGameState state;
    AbstractForwardModel fm;

    private void start(int nPlayers, long seed) {
        Game g = newGame(nPlayers, seed);
        state = (PerudoGameState) g.getGameState();
        fm = g.getForwardModel();
    }

    /** The 3-player position of PerudoDudoTest: 7 dice count towards fours (4 fours + 3 Pacos), 15 dice in all. */
    private void arrangeThreePlayers() {
        setAllDice(state,
                faces(1, 0, 0, 2, 1, 1),
                faces(0, 2, 0, 1, 1, 1),
                faces(2, 0, 1, 1, 0, 1));
    }

    @Test
    public void turnPassesInSeatOrderSkippingAnEliminatedPlayer() {
        start(3, 21);
        eliminate(state, 1, 0);
        fm.next(state, new Bid(2, 3));
        assertEquals(0, state.getBidder());
        assertEquals(2, state.getCurrentPlayer());   // player 1 is out
        fm.next(state, new Bid(3, 3));
        assertEquals(2, state.getBidder());
        assertEquals(0, state.getCurrentPlayer());   // wraps to player 0
    }

    @Test
    public void callerWhoLosesADieStartsTheNextRound() {
        start(3, 21);
        arrangeThreePlayers();
        fm.next(state, new Bid(5, 4));   // 7 >= 5: a correct bid
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Dudo());      // player 1 calls wrongly and loses a die

        assertEquals(5, state.getNDice(0));
        assertEquals(4, state.getNDice(1));
        assertEquals(5, state.getNDice(2));
        assertEquals(15 - 1, state.getTotalDice());
        assertFalse(state.hasBid());
        assertEquals(-1, state.getBidder());
        assertEquals(-1, state.getNextRoundStarter());
        assertEquals(0 + 1, state.getRoundCounter());
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());

        // the new round plays on from the starter
        fm.next(state, new Bid(1, 2));
        assertEquals(1, state.getBidder());
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void bidderWhoLosesADieStartsTheNextRound() {
        start(3, 21);
        arrangeThreePlayers();
        fm.next(state, new Bid(8, 4));   // 7 < 8: a wrong bid
        fm.next(state, new Dudo());      // player 1 calls rightly; player 0 loses a die

        assertEquals(4, state.getNDice(0));
        assertEquals(5, state.getNDice(1));
        assertEquals(5, state.getNDice(2));
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(state.hasBid());
    }

    @Test
    public void everyoneStillInRerollsTheirDiceForTheNextRound() {
        // Same position and actions in a state and its copy, which differ only in their random number generator.
        // Without a re-roll both keep the arranged dice (less the lost die) and come out the same. With one, all
        // 14 dice repeat with chance below 0.016^3 (the likeliest set of counts for 5 dice is 5!/6^5 = 0.016),
        // so every one of the 5 seeds should differ.
        for (long seed = 1; seed <= 5; seed++) {
            start(3, seed);
            arrangeThreePlayers();
            PerudoGameState other = (PerudoGameState) state.copy();
            assertEquals(state, other);
            for (PerudoGameState s : new PerudoGameState[]{state, other}) {
                fm.next(s, new Bid(5, 4));
                fm.next(s, new Dudo());   // player 1 loses a die
                assertEquals(5, s.getNDice(0));
                assertEquals(4, s.getNDice(1));
                assertEquals(5, s.getNDice(2));
            }
            boolean differ = false;
            for (int p = 0; p < 3; p++)
                if (!Arrays.equals(state.getDice(p), other.getDice(p))) differ = true;
            assertTrue("seed " + seed + ": the dice were not re-rolled for the new round", differ);
        }
    }

    @Test
    public void playerLosingTheirLastDieIsOutAndTheNextPlayerStillInStarts() {
        start(4, 21);
        eliminate(state, 2, 0);
        // player 0: five sixes; player 1: one six; player 3: five twos. Sixes: 5 + 1 = 6, no Pacos
        setAllDice(state, faces(0, 0, 0, 0, 0, 5), faces(0, 0, 0, 0, 0, 1),
                faces(0, 0, 0, 0, 0, 0), faces(0, 5, 0, 0, 0, 0));
        fm.next(state, new Bid(6, 6));   // 6 >= 6: correct
        fm.next(state, new Dudo());      // player 1 loses their last die

        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(0, state.getEliminationRound(1));   // the round it happened in
        assertEquals(0, state.getNDice(1));
        assertEquals(5 + 5, state.getTotalDice());
        assertTrue(state.isNotTerminal());
        assertEquals(GAME_ONGOING, state.getPlayerResults()[0]);
        assertEquals(GAME_ONGOING, state.getPlayerResults()[3]);
        // next after player 1 is player 2, also out, so player 3 starts round 1
        assertEquals(1, state.getRoundCounter());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(-1, state.getNextRoundStarter());

        fm.next(state, new Bid(1, 2));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Bid(2, 2));
        assertEquals(3, state.getCurrentPlayer());       // players 1 and 2 are skipped
        assertEquals(0, state.getNDice(1));              // still no dice after the re-roll
    }

    @Test
    public void twoPlayerGameEndsWhenTheCallerLosesTheLastDie() {
        start(2, 21);
        setAllDice(state, faces(0, 0, 0, 0, 0, 5), faces(0, 0, 0, 0, 0, 1));
        fm.next(state, new Bid(6, 6));   // sixes 5 + 1 = 6 >= 6: correct
        fm.next(state, new Dudo());      // player 1 loses their only die

        assertFalse(state.isNotTerminal());
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(0, state.getEliminationRound(1));
        assertEquals(-1, state.getEliminationRound(0));
    }

    @Test
    public void twoPlayerGameEndsWhenTheBidderLosesTheLastDie() {
        start(2, 21);
        setAllDice(state, faces(0, 0, 0, 0, 0, 5), faces(0, 1, 0, 0, 0, 0));
        fm.next(state, new Bid(1, 2));
        fm.next(state, new Bid(2, 2));   // player 1: twos 0 + 1 = 1, no Pacos: 1 < 2, wrong
        fm.next(state, new Dudo());      // player 0 calls rightly; player 1 loses their only die

        assertFalse(state.isNotTerminal());
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(5, state.getNDice(0));
    }

    @Test
    public void threePlayersAreRankedByHowLateTheyWereEliminated() {
        // player 1 goes out in round 0 and player 2 in round 1: player 2 ranks above player 1, against seat order
        start(3, 21);
        setAllDice(state, faces(0, 0, 0, 0, 0, 5), faces(0, 0, 0, 0, 0, 1), faces(0, 1, 0, 0, 0, 0));
        fm.next(state, new Bid(6, 6));   // sixes 5 + 1 + 0 = 6 >= 6: correct
        fm.next(state, new Dudo());      // player 1 loses their only die
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(0, state.getEliminationRound(1));
        assertEquals(2, state.getCurrentPlayer());       // next still in after player 1

        // round 1: fix the re-rolled dice
        state.setDice(0, faces(0, 0, 0, 0, 0, 5));
        state.setDice(2, faces(0, 1, 0, 0, 0, 0));
        fm.next(state, new Bid(2, 2));   // twos 1, no Pacos: 1 < 2, wrong
        assertEquals(0, state.getCurrentPlayer());       // player 1 skipped
        fm.next(state, new Dudo());      // player 2 loses their only die

        assertFalse(state.isNotTerminal());
        assertEquals(1, state.getEliminationRound(2));
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(2));
        assertEquals(3, state.getOrdinalPosition(1));
    }
}
