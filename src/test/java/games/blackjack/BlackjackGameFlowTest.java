package games.blackjack;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import games.blackjack.actions.Bet;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Insurance;
import games.blackjack.actions.Stand;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Betting;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * Integration: real games from the factory, driven only by fm.next.
 */
public class BlackjackGameFlowTest {

    @Test
    public void threePlayersBetPlayAndAreSettledInTurn() {
        Game game = newGame(3, 11);
        BlackjackGameState state = (BlackjackGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        // player 0: 10 + 5, hits the 3 -> 18; player 1: 9 + 8 = 17; player 2: 10 + 4, hits the K -> bust.
        // Dealer 7 + 10 = hard 17, stands.
        stackDeal(state, new String[]{"10H 5C", "9S 8D", "10C 4H"}, "7S", "10D", "3D KS");

        assertEquals(Betting, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Bet(2));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Bet(4));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Bet(6));
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(setOf("10C 4H"), setOf(state.getPlayerHand(2, 0)));
        assertAllCardsPresent(state);

        fm.next(state, new Hit());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(setOf("10H 5C 3D"), setOf(state.getPlayerHand(0, 0)));
        fm.next(state, new Stand());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Stand());
        assertEquals(2, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
        fm.next(state, new Hit());

        // 18 beats 17 (2 bet: 8 + 4); 17 pushes (4 bet: 6 + 4); bust loses (6 bet: 4)
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("7S 10D"), setOf(state.getDealerHand()));
        assertArrayEquals(new int[]{12, 10, 4}, state.chips);
        assertArrayEquals(new GameResult[]{WIN_GAME, DRAW_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void aSinglePlayerCanLoseToTheBank() {
        Game game = newGame(1, 5);
        BlackjackGameState state = (BlackjackGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        // player 10 + 6, hits the 2 -> 18; dealer 5 + 10 = 15 draws the 4 -> 19
        stackDeal(state, new String[]{"10H 6C"}, "5S", "10D", "2C 4H 9S");

        fm.next(state, new Bet(10));
        assertEquals(Play, state.getGamePhase());
        fm.next(state, new Hit());
        fm.next(state, new Stand());

        assertFalse(state.isNotTerminal());
        assertEquals(setOf("5S 10D 4H"), setOf(state.getDealerHand()));
        assertEquals(0, state.getChips(0));
        assertArrayEquals(new GameResult[]{LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void randomGamesEndAfterOneHandWithEveryCardAccountedFor() {
        int[] outcomes = new int[3];    // wins, draws, losses over all players and games
        for (int nPlayers : new int[]{1, 2, 3, 5, 7}) {
            for (long seed = 0; seed < 10; seed++) {
                Game game = newGame(nPlayers, seed);
                BlackjackGameState state = (BlackjackGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);
                int[] insuranceBought = new int[nPlayers];     // half the bet, paid when insurance is bought
                int steps = 0;
                while (state.isNotTerminal() && steps++ < 200) {
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    assertFalse("no actions for player " + state.getCurrentPlayer(), actions.isEmpty());
                    AbstractAction action = actions.get(rnd.nextInt(actions.size()));
                    if (action instanceof Insurance ins && ins.buy)
                        insuranceBought[state.getCurrentPlayer()] = state.getBet(state.getCurrentPlayer(), 0) / 2;
                    fm.next(state, action);
                    assertAllCardsPresent(state);
                    // while the hand is on, only a lost insurance stake has left the player (a won one ends the game)
                    if (state.isNotTerminal())
                        for (int p = 0; p < nPlayers; p++)
                            assertEquals("chips + bet + insurance bought during the hand", 10,
                                    state.getChips(p) + state.getBet(p, 0) + insuranceBought[p]);
                }
                assertFalse("game did not end within 200 actions", state.isNotTerminal());
                assertTrue("every player bets", steps >= nPlayers);

                assertEquals(0, state.getHoleCard().getSize());
                boolean anyStanding = false;
                for (int p = 0; p < nPlayers; p++) {
                    assertEquals(0, state.getBet(p, 0));
                    anyStanding |= BlackjackGameState.handValue(state.getPlayerHand(p, 0).getComponents()) <= 21;
                    int chips = state.getChips(p);
                    GameResult expected = chips > 10 ? WIN_GAME : chips == 10 ? DRAW_GAME : LOSE_GAME;
                    assertEquals(expected, state.getPlayerResults()[p]);
                    outcomes[expected == WIN_GAME ? 0 : expected == DRAW_GAME ? 1 : 2]++;
                }
                if (anyStanding)
                    assertTrue("dealer stops at 17 or more",
                            BlackjackGameState.handValue(state.getDealerHand().getComponents()) >= 17);
                else
                    assertEquals("no draw when every hand is bust", 2, state.getDealerHand().getSize());
            }
        }
        assertTrue("no player won", outcomes[0] > 0);
        assertTrue("no player pushed", outcomes[1] > 0);
        assertTrue("no player lost", outcomes[2] > 0);
    }
}
