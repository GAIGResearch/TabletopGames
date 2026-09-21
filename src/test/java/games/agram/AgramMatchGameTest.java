package games.agram;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.agram.AgramTestUtils.*;
import static org.junit.Assert.*;

/**
 * Multi-deal matches driven with fm.next.
 */
public class AgramMatchGameTest {

    private static Game newMatch(int nPlayers, long seed, int nDeals) {
        AgramParameters params = new AgramParameters();
        params.setParameterValue("nDeals", nDeals);
        return newGame(nPlayers, seed, params);
    }

    private static void assertDealsWon(String label, AgramGameState state, int... expected) {
        for (int p = 0; p < expected.length; p++) {
            assertEquals(label + ": deals won, player " + p, expected[p], state.getDealsWon(p));
            assertEquals(label + ": score, player " + p, expected[p], state.getGameScore(p), 0.0);
        }
    }

    private static void assertNewDealLedBy(String label, AgramGameState state, int leader) {
        assertTrue(label + ": the match goes on", state.isNotTerminal());
        assertEquals(label + ": current player", leader, state.getCurrentPlayer());
        assertEquals(label + ": trick leader", leader, state.getCurrentTrick().getLeader());
        for (int p = 0; p < state.getNPlayers(); p++)
            assertEquals(label + ": hand size, player " + p, 6, state.getPlayerHands().get(p).getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void aThreeDealMatchIsWonByThePlayerWithMostDealsAndEachWinnerDealsTheNext() {
        Game game = newMatch(3, 5, 3);
        AgramGameState state = (AgramGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();

        // deal 1, led by player 0: player 1's Ace of Hearts wins
        assertEquals(0, state.getCurrentPlayer());
        playOneTrickDeal(state, fm, "4H", "AH", "5C");
        assertDealsWon("after deal 1", state, 0, 1, 0);
        assertNewDealLedBy("deal 2", state, 2);

        // deal 2, led by player 2 (then 0, 1): player 0's Nine of Diamonds wins
        playOneTrickDeal(state, fm, "3D", "9D", "6D");
        assertDealsWon("after deal 2", state, 1, 1, 0);
        assertNewDealLedBy("deal 3", state, 1);

        // deal 3, led by player 1 (then 2, 0): player 1's Ten of Spades wins, and the third deal ends the match
        playOneTrickDeal(state, fm, "10S", "3S", "5S");
        assertFalse(state.isNotTerminal());
        assertDealsWon("at the end", state, 1, 2, 0);
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(WIN_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
        assertEquals("the last deal's tricks stay on the discard pile", 3, state.getDiscardPile().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void playersSharingTheMostDealsWonDrawAndTheRestLose() {
        Game game = newMatch(3, 7, 5);
        AgramGameState state = (AgramGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();

        // deal winners 0, 0, 1, 1, 2: deals won 2, 2, 1
        assertEquals(0, state.getCurrentPlayer());
        playOneTrickDeal(state, fm, "AH", "3H", "4H");          // players 0, 1, 2: player 0 wins
        assertNewDealLedBy("deal 2", state, 1);
        playOneTrickDeal(state, fm, "3C", "4C", "AC");          // players 1, 2, 0: player 0 wins
        assertNewDealLedBy("deal 3", state, 1);
        playOneTrickDeal(state, fm, "AD", "3D", "4D");          // players 1, 2, 0: player 1 wins
        assertNewDealLedBy("deal 4", state, 2);
        playOneTrickDeal(state, fm, "3S", "4S", "10S");         // players 2, 0, 1: player 1 wins
        assertNewDealLedBy("deal 5", state, 2);
        assertDealsWon("after deal 4", state, 2, 2, 0);
        playOneTrickDeal(state, fm, "9H", "5H", "6H");          // players 2, 0, 1: player 2 wins

        assertFalse(state.isNotTerminal());
        assertDealsWon("at the end", state, 2, 2, 1);
        assertEquals(DRAW_GAME, state.getPlayerResults()[0]);
        assertEquals(DRAW_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
    }

    @Test
    public void randomThreeDealMatchesForTwoToFivePlayersRedealEveryDealAndRankByDealsWon() {
        int redeals = 0, drawnMatches = 0, wonMatches = 0;
        for (int nPlayers = 2; nPlayers <= 5; nPlayers++) {
            for (long seed = 1; seed <= 5; seed++) {
                Game game = newMatch(nPlayers, seed, 3);
                AgramGameState state = (AgramGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);
                String label = nPlayers + " players, seed " + seed;
                int cardsPerDeal = 6 * nPlayers;

                int[] dealsWon = new int[nPlayers];     // tallied here from the rules
                List<FrenchCard> trickCards = new ArrayList<>();
                List<Integer> trickPlayers = new ArrayList<>();
                int cardsThisDeal = 0, deals = 0, steps = 0;
                assertEquals(label + ": player 0 leads the first deal", 0, state.getCurrentPlayer());
                while (state.isNotTerminal() && steps++ < 200) {
                    int player = state.getCurrentPlayer();
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    assertFalse(label + ": no actions offered", actions.isEmpty());
                    PlayCard chosen = (PlayCard) actions.get(rnd.nextInt(actions.size()));
                    fm.next(state, chosen);
                    trickCards.add(chosen.card);
                    trickPlayers.add(player);
                    cardsThisDeal++;
                    String at = label + ", step " + steps;
                    assertAllCardsPresent(state);

                    if (trickCards.size() == nPlayers) {
                        int winner = expectedTrickWinner(trickCards, trickPlayers);
                        trickCards.clear();
                        trickPlayers.clear();
                        if (cardsThisDeal == cardsPerDeal) {
                            // the last trick of a deal: its winner wins the deal
                            dealsWon[winner]++;
                            deals++;
                            cardsThisDeal = 0;
                            if (deals < 3) {
                                // the winner deals the next deal, so the player after them leads it
                                assertNewDealLedBy(at + ", deal " + (deals + 1), state, (winner + 1) % nPlayers);
                                assertEquals(at, deals, state.getRoundCounter());
                                assertEquals(at, 35 - cardsPerDeal, state.getDrawDeck().getSize());
                                assertEquals(at, 0, state.getCurrentTrick().getSize());
                                assertEquals(at, 0, state.getDiscardPile().getSize());
                                for (int p = 0; p < nPlayers; p++)
                                    assertEquals(at + ": known voids cleared, player " + p, Set.of(),
                                            state.getKnownVoids().get(p));
                                redeals++;
                            }
                        }
                    }
                    assertDealsWon(at, state, dealsWon);
                }
                assertFalse(label + ": match did not end within 200 actions", state.isNotTerminal());
                assertEquals(label + ": 3 deals of 6 tricks", 3 * cardsPerDeal, steps);
                assertEquals(label, 3, deals);

                // ranked by deals won: a unique top scorer wins; players sharing the top score draw; the rest lose
                int top = 0;
                for (int d : dealsWon) top = Math.max(top, d);
                int nTop = 0;
                for (int d : dealsWon) if (d == top) nTop++;
                for (int p = 0; p < nPlayers; p++) {
                    CoreConstants.GameResult expected = dealsWon[p] < top ? LOSE_GAME : nTop > 1 ? DRAW_GAME : WIN_GAME;
                    assertEquals(label + ", player " + p + " with deals won " + dealsWon[p], expected,
                            state.getPlayerResults()[p]);
                }
                if (nTop > 1) drawnMatches++;
                else wonMatches++;
            }
        }
        assertTrue("no new deal was checked", redeals > 0);
        assertTrue("no match ended in a draw", drawnMatches > 0);
        assertTrue("no match had a single winner", wonMatches > 0);
    }
}
