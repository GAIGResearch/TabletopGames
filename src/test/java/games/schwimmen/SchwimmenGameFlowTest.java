package games.schwimmen;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import games.schwimmen.actions.ChooseHand;
import games.schwimmen.actions.Close;
import games.schwimmen.actions.ExchangeAll;
import games.schwimmen.actions.Pass;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static games.schwimmen.SchwimmenTestUtils.*;
import static org.junit.Assert.*;

/**
 * Whole games from the factory, driven by fm.next: turn order round the dealer, the end of the deal when the draw
 * deck runs out, the maxCircuitsPerDeal safeguard, and random games (conservation, termination, results).
 */
public class SchwimmenGameFlowTest {

    @Test
    public void theDealerChoosesThenPlayGoesClockwiseFromTheDealersLeft() {
        Game game = newGame(4, ordinarySeed(4, 0));
        SchwimmenGameState state = (SchwimmenGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();

        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Set.of(new ChooseHand(false), new ChooseHand(true)),
                new HashSet<>(fm.computeAvailableActions(state)));
        fm.next(state, new ChooseHand(false));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(3, state.getTable().getSize());

        takeTurn(state, fm, harmlessExchange(state, fm));
        assertEquals(1, state.getCurrentPlayer());
        takeTurn(state, fm, new Pass());
        assertEquals(2, state.getCurrentPlayer());
        takeTurn(state, fm, new Pass());
        // the dealer's choice was not a turn: the dealer now has a normal one
        assertEquals(3, state.getCurrentPlayer());
        List<AbstractAction> dealerActions = fm.computeAvailableActions(state);
        assertEquals(11, dealerActions.size());
        assertTrue(dealerActions.contains(new ExchangeAll()));
        assertFalse(dealerActions.contains(new ChooseHand(false)));
        takeTurn(state, fm, new Pass());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(3, state.getConsecutivePasses());
        assertEquals(0, state.getDiscardPile().getSize());

        // player 0 completes 4 passes since the exchange: the table is replaced
        takeTurn(state, fm, new Pass());
        assertEquals(3, state.getDiscardPile().getSize());
        assertEquals(32 - 4 * 3 - 3 - 3, state.getDrawDeck().getSize());
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void withFivePlayersTheFifthRoundOfPassesEndsTheGame() {
        Game game = newGame(5, ordinarySeed(5, 0));
        SchwimmenGameState state = (SchwimmenGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        fm.next(state, new ChooseHand(false));
        // 32 - 5 x 3 dealt - 3 on the table = 14
        assertEquals(14, state.getDrawDeck().getSize());

        int[] drawAfterRound = {14 - 3, 14 - 6, 14 - 9, 14 - 12};   // 11, 8, 5, 2
        for (int round = 0; round < 4; round++) {
            for (int p = 0; p < 5; p++)
                takeTurn(state, fm, new Pass());
            assertTrue(state.isNotTerminal());
            assertEquals(drawAfterRound[round], state.getDrawDeck().getSize());
            assertEquals(3 * (round + 1), state.getDiscardPile().getSize());
        }
        for (int p = 0; p < 4; p++)
            takeTurn(state, fm, new Pass());
        assertTrue("24 passes: the game goes on", state.isNotTerminal());
        takeTurn(state, fm, new Pass());
        assertFalse("the 25th pass: 2 cards cannot replace the table", state.isNotTerminal());
        assertEquals(2, state.getDrawDeck().getSize());
        assertAllCardsPresent(state);
    }

    /** 3 players; the dealer keeps; then 'turns' single exchanges (no passes). Returns the state. */
    private SchwimmenGameState exchangeTurns(int maxCircuits, int turns) {
        SchwimmenParameters params = SchwimmenTestUtils.valetParams();
        params.setParameterValue("maxCircuitsPerDeal", maxCircuits);
        Game game = newGame(3, ordinarySeed(3, 0), params);
        SchwimmenGameState state = (SchwimmenGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        fm.next(state, new ChooseHand(false));
        for (int t = 0; t < turns; t++) {
            assertTrue("ended early, before turn " + (t + 1), state.isNotTerminal());
            takeTurn(state, fm, harmlessExchange(state, fm));
        }
        return state;
    }

    @Test
    public void theGameEndsOnceEveryPlayerHasHadMaxCircuitsTurns() {
        // maxCircuitsPerDeal 2, 3 players: 2 x 3 = 6 turns; the dealer's choice is not one of them
        SchwimmenGameState state = exchangeTurns(2, 5);
        assertTrue("5 turns: the dealer has had only one", state.isNotTerminal());
        state = exchangeTurns(2, 6);
        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
    }

    @Test
    public void aHigherMaxCircuitsLetsTheGameGoOn() {
        // maxCircuitsPerDeal 3: 3 x 3 = 9 turns
        SchwimmenGameState state = exchangeTurns(3, 8);
        assertTrue(state.isNotTerminal());
        state = exchangeTurns(3, 9);
        assertFalse(state.isNotTerminal());
    }

    @Test
    public void theSafeguardCountsTurnsNotCloseDecisions() {
        // maxCircuitsPerDeal 2, 3 players: 6 turns, each an exchange then Close(false)
        SchwimmenParameters params = SchwimmenTestUtils.valetParams();
        params.setParameterValue("maxCircuitsPerDeal", 2);
        Game game = newGame(3, ordinarySeed(3, 0), params);
        SchwimmenGameState state = (SchwimmenGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        fm.next(state, new ChooseHand(false));
        for (int t = 1; t <= 6; t++) {
            int player = (t - 1) % 3;
            assertEquals(player, state.getCurrentPlayer());
            fm.next(state, harmlessExchange(state, fm));
            // 5 turns plus the 6th exchange: the 6th turn is not over until its close decision
            assertTrue("ended before turn " + t + "'s close decision", state.isNotTerminal());
            assertEquals(player, state.getCurrentPlayer());
            assertEquals(CLOSE_CHOICES, new HashSet<>(fm.computeAvailableActions(state)));
            fm.next(state, new Close(false));
            assertEquals("turn " + t, t < 6, state.isNotTerminal());
        }
        assertEquals(-1, state.getCloser());
    }

    @Test
    public void aCloseInTheFirstRoundGivesEachOtherPlayerOneLastTurn() {
        Game game = newGame(4, ordinarySeed(4, 0));
        SchwimmenGameState state = (SchwimmenGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        fm.next(state, new ChooseHand(false));

        // player 0 exchanges and does not close
        fm.next(state, harmlessExchange(state, fm));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(CLOSE_CHOICES, new HashSet<>(fm.computeAvailableActions(state)));
        fm.next(state, new Close(false));
        // player 1 passes and closes
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Pass());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Close(true));
        assertEquals(1, state.getCloser());
        assertEquals(0, state.getConsecutivePasses());

        // players 2, 3 (the dealer) and 0 each have one turn with no close decision: n - 1 = 3 turns
        int[] lastTurns = {2, 3, 0};
        for (int i = 0; i < 3; i++) {
            assertTrue(state.isNotTerminal());
            assertEquals(lastTurns[i], state.getCurrentPlayer());
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertEquals(11, actions.size());
            assertFalse(actions.contains(new Close(true)));
            fm.next(state, i == 1 ? new Pass() : harmlessExchange(state, fm));
        }
        assertFalse("play would return to player 1, the closer", state.isNotTerminal());
        assertEquals(0, state.getDiscardPile().getSize());
        assertAllCardsPresent(state);
        for (int p = 0; p < 4; p++)
            assertNotEquals(CoreConstants.GameResult.GAME_ONGOING, state.getPlayerResults()[p]);
    }

    @Test
    public void randomGamesConserveTheCardsAndEndWithTheBestHandWinning() {
        int games = 0, closedGames = 0;
        for (int n : new int[]{2, 5, 8}) {
            for (long seed = 1; seed <= 3; seed++) {
                Game game = newGame(n, seed);
                SchwimmenGameState state = (SchwimmenGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);
                int steps = 0;
                int turnsAfterClose = -1;   // -1 until someone closes
                // Schnauz or Feuer in a player's hand ends the deal at once (after the deal too)
                assertEquals("a dealt Schnauz / Feuer ends the game in setup", !anyPlayerSpecial(state), state.isNotTerminal());
                while (state.isNotTerminal() && steps++ < 2000) {
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    assertFalse("no actions for player " + state.getCurrentPlayer(), actions.isEmpty());
                    if (turnsAfterClose >= 0)
                        assertFalse("close offered after a close", actions.contains(new Close(true)));
                    AbstractAction chosen = actions.get(rnd.nextInt(actions.size()));
                    fm.next(state, chosen);
                    if (chosen.equals(new Close(true)))
                        turnsAfterClose = 0;
                    else if (turnsAfterClose >= 0)
                        turnsAfterClose++;
                    assertAllCardsPresent(state);
                    for (int p = 0; p < n; p++)
                        assertEquals(3, state.getPlayerHand(p).getSize());
                    if (anyPlayerSpecial(state))
                        assertFalse("the game went on with Schnauz / Feuer after " + chosen, state.isNotTerminal());
                }
                assertFalse("game did not end within 2000 actions", state.isNotTerminal());
                // after a close at most n - 1 turns (fewer only if the safeguard or a Schnauz / Feuer ends it first)
                assertTrue("turns after the close: " + turnsAfterClose, turnsAfterClose <= n - 1);
                if (turnsAfterClose == n - 1) closedGames++;
                games++;

                // scores by the full hand values; results by value, then the tie-breaks
                for (int p = 0; p < n; p++) {
                    List<core.components.FrenchCard> hand = state.getPlayerHand(p).getComponents();
                    assertEquals(hand.toString(), oracleHandValue(hand), state.getGameScore(p), 1e-9);
                    assertEquals("player " + p + " of " + n + ", seed " + seed,
                            oracleResult(state, p), state.getPlayerResults()[p]);
                }
            }
        }
        assertEquals(9, games);
        assertTrue("no random game ended by a close and its last round", closedGames > 0);
    }
}
