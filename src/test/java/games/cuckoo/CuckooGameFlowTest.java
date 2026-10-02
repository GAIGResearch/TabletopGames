package games.cuckoo;

import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.cuckoo.actions.KeepCard;
import games.cuckoo.actions.SwapCard;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

import static core.CoreConstants.GameResult.*;
import static games.cuckoo.CuckooTestUtils.*;
import static org.junit.Assert.*;

/**
 * Real games from the factory, driven by fm.next: scripted rounds, and seeded random games played to the end.
 */
public class CuckooGameFlowTest {

    private static void assertCards(CuckooGameState state, String... codes) {
        for (int p = 0; p < codes.length; p++)
            assertEquals("card of player " + p, codes[p] == null ? null : card(codes[p]), state.getPlayerCard(p));
    }

    @Test
    public void oneRoundGoesLeftFromTheDealerAndEndsAfterTheDealerDecides() {
        // knowledge rows: one string per observer, '1' where they know that holder's card
        Game game = newGame(4, 3);
        CuckooGameState state = (CuckooGameState) game.getGameState();
        CuckooForwardModel fm = (CuckooForwardModel) game.getForwardModel();
        // player 3 deals; player 0 decides first
        dealCards(state, card("6H"), card("KS"), card("3D"), card("9C"));
        putOnTopOfDrawDeck(state, card("2C"));
        Set<AbstractAction> both = Set.of(new KeepCard(), new SwapCard());

        assertEquals(0, state.getCurrentPlayer());
        assertEquals(both, new HashSet<>(fm.computeAvailableActions(state)));
        assertKnowledge(state, "1000", "0100", "0010", "0001");
        fm.next(state, new SwapCard());
        // player 1 holds a King and refuses: everyone has now seen it
        assertCards(state, "6H", "KS", "3D", "9C");
        assertEquals(1, state.getCurrentPlayer());
        assertKnowledge(state, "1100", "0100", "0110", "0101");

        fm.next(state, new SwapCard());
        // player 1 gives the King to player 2 for the 3D
        assertCards(state, "6H", "3D", "KS", "9C");
        assertEquals(2, state.getCurrentPlayer());
        // columns 1 and 2 swap for every observer (1010 0010 0110 0011), then P1 and P2 know each other's cards:
        // everyone still knows where the King is, and only P1 and P2 know P1's new 3D
        assertKnowledge(state, "1010", "0110", "0110", "0011");

        fm.next(state, new KeepCard());
        assertCards(state, "6H", "3D", "KS", "9C");
        assertEquals(3, state.getCurrentPlayer());
        assertKnowledge(state, "1010", "0110", "0110", "0011");
        assertEquals(both, new HashSet<>(fm.computeAvailableActions(state)));
        assertEquals("the round is still going", 0, state.getRoundCounter());

        fm.next(state, new SwapCard());
        // the dealer cuts the 2C for the 9C: 2C is lowest of 6, 3, K, 2, so the dealer loses a life
        assertArrayEquals(new int[]{3, 3, 3, 2}, state.lives);
        assertEquals(1, state.getRoundCounter());
        assertEquals("new dealer", 0, state.getDealer());
        assertEquals("first to decide", 1, state.getCurrentPlayer());
        assertOneCardPerPlayerInGame(state);
        assertAllCardsPresent(state);
        assertKnowledge(state, "1000", "0100", "0010", "0001");
    }

    @Test
    public void aPlayerGoesOutInALaterRoundAndIsSkippedFromThen() {
        CuckooParameters params = new CuckooParameters();
        params.setParameterValue("nLives", 2);
        Game game = newGame(4, 5, params);
        CuckooGameState state = (CuckooGameState) game.getGameState();
        CuckooForwardModel fm = (CuckooForwardModel) game.getForwardModel();

        // Round 0: dealer 3, order 0 1 2 3; everyone keeps and player 1's 2S is lowest
        dealCards(state, card("5H"), card("2S"), card("KD"), card("9C"));
        for (int p : new int[]{0, 1, 2, 3}) {
            assertEquals(p, state.getCurrentPlayer());
            fm.next(state, new KeepCard());
        }
        assertArrayEquals(new int[]{2, 1, 2, 2}, state.lives);

        // Round 1: dealer 0, order 1 2 3 0; player 1's 2D is lowest again and they go out
        assertEquals(0, state.getDealer());
        dealCards(state, card("9H"), card("2D"), card("KC"), card("8S"));
        for (int p : new int[]{1, 2, 3, 0}) {
            assertEquals(p, state.getCurrentPlayer());
            fm.next(state, new KeepCard());
        }
        assertArrayEquals(new int[]{2, 0, 2, 2}, state.lives);
        assertEquals("went out in round 1, the round just played", 1, state.roundEliminated[1]);
        assertEquals(2, state.getRoundCounter());
        assertNull(state.getPlayerCard(1));

        // Round 2: the deal passes from player 0 over player 1 to player 2, and player 3 decides first
        assertEquals(2, state.getDealer());
        dealCards(state, card("4H"), null, card("JD"), card("10S"));
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, new KeepCard());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new SwapCard());
        // player 0's neighbour is player 2 (player 1 is out)
        assertCards(state, "JD", null, "4H", "10S");
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new KeepCard());
        // the dealer, player 2, is left with the 4H, the lowest of J, 4, 10
        assertArrayEquals(new int[]{2, 0, 1, 2}, state.lives);
        assertArrayEquals(new int[]{-1, 1, -1, -1}, state.roundEliminated);
        assertEquals(3, state.getRoundCounter());
        assertEquals(3, state.getDealer());
        assertEquals(0, state.getCurrentPlayer());
        assertOneCardPerPlayerInGame(state);
        assertAllCardsPresent(state);
    }

    private static boolean[][] ownCardsOnly(int nPlayers) {
        boolean[][] knows = new boolean[nPlayers][nPlayers];
        for (int p = 0; p < nPlayers; p++) knows[p][p] = true;
        return knows;
    }

    /**
     * The state's knowledge equals the oracle's for every pair of players in the game, and each player's
     * redeterminised copy leaves every card they know where it is.
     */
    private static void assertKnowledgeMatches(String where, boolean[][] knows, CuckooGameState state) {
        int n = state.getNPlayers();
        for (int o = 0; o < n; o++) {
            if (!state.isInGame(o)) continue;
            for (int h = 0; h < n; h++)
                if (state.isInGame(h))
                    assertEquals(where + ": does " + o + " know the card of " + h, knows[o][h], state.knowsCard(o, h));
            CuckooGameState copy = (CuckooGameState) state.copy(o);
            for (int h = 0; h < n; h++)
                if (state.isInGame(h) && knows[o][h])
                    assertEquals(where + ": " + o + "'s copy keeps the known card of " + h,
                            state.getPlayerCard(h), copy.getPlayerCard(h));
        }
    }

    /**
     * Plays a seeded random game to the end, checking every step against the rules, then the results.
     * Returns true if it ended with joint winners.
     */
    private boolean playRandomGame(int nPlayers, int nLives, long seed) {
        CuckooParameters params = new CuckooParameters();
        params.setParameterValue("nLives", nLives);
        Game game = newGame(nPlayers, seed, params);
        CuckooGameState state = (CuckooGameState) game.getGameState();
        CuckooForwardModel fm = (CuckooForwardModel) game.getForwardModel();
        Random rnd = new Random(seed);
        String where = nPlayers + " players, " + nLives + " lives, seed " + seed;

        // someone loses a life every round, so there are at most nPlayers * nLives rounds of at most nPlayers decisions
        int cap = nPlayers * nLives * nPlayers + 1;
        int steps = 0;
        int decisionsThisRound = 0;
        int inGameAtRoundStart = nPlayers;
        // oracle for what each player knows, kept by the rules independently of CuckooKnowledge
        boolean[][] knows = ownCardsOnly(nPlayers);
        while (state.isNotTerminal() && steps++ < cap) {
            int[] livesBefore = state.lives.clone();
            int[] outBefore = state.roundEliminated.clone();
            int dealer = state.getDealer(), player = state.getCurrentPlayer(), round = state.getRoundCounter();
            assertTrue(where + ": the player to decide is in the game", livesBefore[player] > 0);
            assertTrue(where + ": the dealer is in the game", livesBefore[dealer] > 0);

            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertEquals(where, Set.of(new KeepCard(), new SwapCard()), new HashSet<>(actions));
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            int neighbour = nextWithLives(livesBefore, player);
            boolean neighbourHasKing = state.getPlayerCard(neighbour).type == FrenchCard.FrenchCardType.King;
            fm.next(state, action);
            if (player != dealer && action instanceof SwapCard) {
                if (neighbourHasKing) {
                    for (int o = 0; o < nPlayers; o++) knows[o][neighbour] = true;
                } else {
                    for (int o = 0; o < nPlayers; o++) {
                        boolean k = knows[o][player];
                        knows[o][player] = knows[o][neighbour];
                        knows[o][neighbour] = k;
                    }
                    knows[player][neighbour] = knows[player][player] = true;
                    knows[neighbour][player] = knows[neighbour][neighbour] = true;
                }
            }
            decisionsThisRound++;
            assertAllCardsPresent(state);
            CuckooGameState copy = (CuckooGameState) state.copy();
            assertEquals(where + ": full copy", state, copy);
            assertEquals(where + ": full copy hash", state.hashCode(), copy.hashCode());

            if (player != dealer) {
                assertKnowledgeMatches(where, knows, state);
                // mid-round: nothing but the cards changes, and the turn passes left
                assertArrayEquals(where, livesBefore, state.lives);
                assertEquals(where, round, state.getRoundCounter());
                assertEquals(where, dealer, state.getDealer());
                assertEquals(where + ": next player", nextWithLives(livesBefore, player), state.getCurrentPlayer());
                continue;
            }

            // the dealer decided: the round is over
            assertEquals(where + ": one decision per player in the game", inGameAtRoundStart, decisionsThisRound);
            int lost = 0, left = 0;
            for (int p = 0; p < nPlayers; p++) {
                int loss = livesBefore[p] - state.lives[p];
                assertTrue(where + ": player " + p + " loses 0 or 1 lives", loss == 0 || loss == 1);
                assertTrue(where + ": only players in the game lose lives", loss == 0 || livesBefore[p] > 0);
                lost += loss;
                if (state.lives[p] > 0) left++;
                int expectedOut = (loss == 1 && state.lives[p] == 0) ? round : outBefore[p];
                assertEquals(where + ": round player " + p + " went out", expectedOut, state.roundEliminated[p]);
            }
            assertTrue(where + ": somebody loses a life every round", lost >= 1);
            assertEquals(where + ": game over exactly when at most one player is left", left <= 1, !state.isNotTerminal());
            if (state.isNotTerminal()) {
                assertEquals(where, round + 1, state.getRoundCounter());
                int newDealer = nextWithLives(state.lives, dealer);
                assertEquals(where + ": new dealer", newDealer, state.getDealer());
                assertEquals(where + ": first player", nextWithLives(state.lives, newDealer), state.getCurrentPlayer());
                assertOneCardPerPlayerInGame(state);
                // new deal: each player knows only their own card
                knows = ownCardsOnly(nPlayers);
                assertKnowledgeMatches(where, knows, state);
            }
            decisionsThisRound = 0;
            inGameAtRoundStart = left;
        }
        assertFalse(where + ": game did not end within " + cap + " decisions", state.isNotTerminal());

        // Game.terminate() calls endGame again; the results are then checked against the rules
        fm.endGame(state);
        int survivor = -1, lastRound = -1;
        for (int p = 0; p < nPlayers; p++) {
            if (state.lives[p] > 0) survivor = p;
            lastRound = Math.max(lastRound, state.roundEliminated[p]);
        }
        for (int p = 0; p < nPlayers; p++) {
            boolean wins = survivor >= 0 ? p == survivor : state.roundEliminated[p] == lastRound;
            GameResult expected = wins ? WIN_GAME : LOSE_GAME;
            assertEquals(where + ": result of player " + p, expected, state.getPlayerResults()[p]);
        }
        return survivor < 0;
    }

    @Test
    public void seededRandomGamesPlayToTheEndWithConsistentResults() {
        int games = 0, joint = 0;
        for (int nPlayers : new int[]{4, 10})
            for (int nLives : new int[]{1, 3})
                for (long seed = 0; seed < 5; seed++) {
                    if (playRandomGame(nPlayers, nLives, seed)) joint++;
                    games++;
                }
        assertEquals(20, games);
        assertTrue("some games end with a single survivor", joint < games);
    }

    @Test(timeout = 30000)  // Game.run has no step cap of its own
    public void gameRunEndsWithOneWinnerOrJointWinnersAndEveryoneElseLosing() {
        // the full framework loop, including Game.terminate() calling endGame a second time
        Game game = newGame(6, 21);
        game.run();
        CuckooGameState state = (CuckooGameState) game.getGameState();
        assertFalse(state.isNotTerminal());
        int survivor = -1, lastRound = -1;
        for (int p = 0; p < 6; p++) {
            if (state.lives[p] > 0) {
                assertEquals("at most one player is left", -1, survivor);
                survivor = p;
            }
            lastRound = Math.max(lastRound, state.roundEliminated[p]);
        }
        for (int p = 0; p < 6; p++) {
            boolean wins = survivor >= 0 ? p == survivor : state.roundEliminated[p] == lastRound;
            assertEquals("result of player " + p, wins ? WIN_GAME : LOSE_GAME, state.getPlayerResults()[p]);
        }
    }
}
