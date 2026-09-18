package games.blackjack;

import core.CoreConstants.GameResult;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * Settlement against the dealer (win 1:1, push, loss) and the result of each player against the bank:
 * WIN_GAME with more chips than startingChips, DRAW_GAME with the same, LOSE_GAME with fewer, independently.
 * No hand here wins with 21, which is paid differently (see BlackjackPayoutTest).
 */
public class BlackjackSettlementTest {

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
        state = new BlackjackGameState(params, 1);
        fm = new BlackjackForwardModel();
        fm.setup(state);
    }

    private void newState(int nPlayers) {
        state = new BlackjackGameState(params, nPlayers);
        fm.setup(state);
    }

    private void assertSettled(int[] chips, GameResult... results) {
        assertFalse("game over after one hand", state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        assertArrayEquals(chips, state.chips);
        for (int p = 0; p < state.getNPlayers(); p++)
            assertEquals("bet of player " + p, 0, state.getBet(p, 0));
        assertArrayEquals(results, state.getPlayerResults());
    }

    @Test
    public void aHigherTotalWinsEvenMoney() {
        // 19 beats 18: bet 4 returned plus 4 won, 6 + 8 = 14
        arrangePlay(state, new int[]{4}, new String[]{"10C 9C"}, "8S", "10D", "");
        fm.next(state, new Stand());
        assertSettled(new int[]{14}, WIN_GAME);
    }

    @Test
    public void aLowerTotalLosesTheBetAndTheResultSurvivesASecondEndGame() {
        // 17 v 19: the only player loses against the bank (the default, ordinal, endGame would call it a win).
        // Game.terminate() calls endGame once more after the game is over
        arrangePlay(state, new int[]{4}, new String[]{"10C 7C"}, "9S", "10D", "");
        fm.next(state, new Stand());
        fm.endGame(state);
        assertSettled(new int[]{6}, LOSE_GAME);
    }

    @Test
    public void anEqualTotalIsAPush() {
        // 18 v 18: the bet of 4 is returned, 6 + 4 = 10
        arrangePlay(state, new int[]{4}, new String[]{"10C 8C"}, "8S", "10D", "");
        fm.next(state, new Stand());
        assertSettled(new int[]{10}, DRAW_GAME);
    }

    @Test
    public void aDealerBustPaysEveryHandThatIsNotBust() {
        // dealer 6 + 10 = 16, draws K: 26 bust. Player 0 on 12 and player 1 on 18 win (2 bet -> 8 + 4 = 12 each);
        // player 2 went bust (6 bet) and loses although the dealer also busts
        newState(3);
        arrangePlay(state, new int[]{2, 2, 6}, new String[]{"10H 2C", "9S 9D", "7C 7D"}, "6S", "10D", "8H KC");
        fm.next(state, new Stand());
        fm.next(state, new Stand());
        fm.next(state, new Hit());      // 14 + 8 = 22
        assertEquals(setOf("6S 10D KC"), setOf(state.getDealerHand()));
        assertSettled(new int[]{12, 12, 4}, WIN_GAME, WIN_GAME, LOSE_GAME);
    }

    @Test
    public void eachPlayerIsSettledIndependentlyAgainstTheDealer() {
        // dealer 8 + 10 = 18. Player 0: 19 wins (bet 6: 4 + 12 = 16); player 1: 18 pushes (bet 4: 6 + 4 = 10);
        // player 2: 17 loses (bet 10: 0)
        newState(3);
        arrangePlay(state, new int[]{6, 4, 10}, new String[]{"10H 9C", "9S 9D", "7C QD"}, "8S", "10D", "");
        fm.next(state, new Stand());
        fm.next(state, new Stand());
        fm.next(state, new Stand());
        assertSettled(new int[]{16, 10, 0}, WIN_GAME, DRAW_GAME, LOSE_GAME);
    }

    @Test
    public void allPlayersBustLoseEveryBet() {
        newState(2);
        arrangePlay(state, new int[]{2, 8}, new String[]{"10H 6C", "9S 8D"}, "3S", "10D", "KH QC");
        fm.next(state, new Hit());
        fm.next(state, new Hit());
        assertSettled(new int[]{8, 2}, LOSE_GAME, LOSE_GAME);
    }
}
