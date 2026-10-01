package games.golfsix;

import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.golfsix.actions.DiscardCard;
import games.golfsix.actions.DrawCard;
import games.golfsix.actions.ReplaceCard;
import games.golfsix.actions.TurnUp;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static games.golfsix.GolfSixTestUtils.*;
import static org.junit.Assert.*;

/**
 * The variants in real games from the factory, driven by fm.next: a scripted walk-through for finalTurns, for
 * nDeals 2 and for both, and seeded random games played to the end with each.
 */
public class GolfSixVariantFlowTest {

    /**
     * The scripted 2-player deal of GolfSixGameFlowTest.aWholeDealIsPlayedAndScored, up to player 0 turning up their
     * last card: player 0 draws 2S, 7C, 8C, KS into positions 2-5; player 1 draws and discards 9S, 10S, JS.
     * Player 0 ends on 7/7 pair + 8/8 pair + 2/K = 0 + 0 + (-2 + 0) = -2. The next draw-deck card is nextDraw.
     */
    private void playFirstDealUntilPlayerZeroFinishes(GolfSixForwardModel fm, GolfSixGameState state, String nextDraw) {
        setGrid(state, 0, "7H", "8H", "9H", "10H", "JH", "QH");
        setGrid(state, 1, "AH", "4C", "6D", "3H", "QD", "6S");
        stackDrawDeck(state, "2S", "9S", "7C", "10S", "8C", "JS", "KS", nextDraw);
        setTopDiscard(state, "5C");

        step(fm, state, new TurnUp(0));
        step(fm, state, new TurnUp(1));
        step(fm, state, new TurnUp(0));
        step(fm, state, new TurnUp(1));
        for (int pos = 2; pos < 6; pos++) {
            assertEquals(0, state.getCurrentPlayer());
            step(fm, state, new DrawCard(false));
            if (pos == 5) break;
            step(fm, state, new ReplaceCard(pos));
            assertEquals(1, state.getCurrentPlayer());
            step(fm, state, new DrawCard(false));
            step(fm, state, new DiscardCard());
        }
        assertEquals("the KS is drawn; ReplaceCard(5) is next", card("KS"), state.getDrawnCard());
    }

    @Test
    public void withFinalTurnsTheOtherPlayerHasOneMoreTurnBeforeTheDealIsScored() {
        Game game = newGame(2, 5, variant(true, 1));
        GolfSixGameState state = (GolfSixGameState) game.getGameState();
        GolfSixForwardModel fm = (GolfSixForwardModel) game.getForwardModel();
        playFirstDealUntilPlayerZeroFinishes(fm, state, "4H");
        step(fm, state, new ReplaceCard(5));

        assertTrue("player 1 has a final turn", state.isNotTerminal());
        assertEquals(0, state.getFinisher());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals("UUUUUU", faceUpPattern(state, 0));
        assertEquals("UUDDDD", faceUpPattern(state, 1));

        // player 1 draws the 4H and puts it on the face-down QD at position 4
        step(fm, state, new DrawCard(false));
        step(fm, state, new ReplaceCard(4));

        assertEquals(GAME_END, state.getGameStatus());
        assertEquals(cards("AH", "4C", "6D", "3H", "4H", "6S"), state.getGrid(1).getComponents());
        assertEquals(card("QD"), state.getDiscardPile().peek());
        assertEquals("UUUUUU", faceUpPattern(state, 1));
        assertEquals(-2, state.getScore(0));
        // player 1: A/3 + 4/4 pair + 6/6 pair = (1 + 3) + 0 + 0 = 4
        assertEquals(4, state.getScore(1));
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    /**
     * Arranges the second deal (player 1 plays first) and plays it: player 1 draws 2D, 7S, 8S, KD into positions
     * 2-5 and turns up their last card; player 0 draws and discards 9C, 10C, JC in between.
     * Player 1 ends on 7/7 pair + 8/8 pair + 2/K = 0 + 0 + (-2 + 0) = -2. The next draw-deck card is nextDraw.
     */
    private void playSecondDealUntilPlayerOneFinishes(GolfSixForwardModel fm, GolfSixGameState state, String nextDraw) {
        setGrid(state, 1, "7D", "8D", "9D", "10D", "JD", "QD");
        setGrid(state, 0, "AS", "4H", "6C", "3D", "QC", "6H");
        stackDrawDeck(state, "2D", "9C", "7S", "10C", "8S", "JC", "KD", nextDraw);
        setTopDiscard(state, "5S");

        assertEquals(allTurnUps(), actions(fm, state));
        step(fm, state, new TurnUp(0));
        step(fm, state, new TurnUp(1));
        assertEquals(0, state.getCurrentPlayer());
        step(fm, state, new TurnUp(0));
        step(fm, state, new TurnUp(1));
        for (int pos = 2; pos < 6; pos++) {
            assertEquals(1, state.getCurrentPlayer());
            step(fm, state, new DrawCard(false));
            step(fm, state, new ReplaceCard(pos));
            if (pos == 5) break;
            assertEquals(0, state.getCurrentPlayer());
            step(fm, state, new DrawCard(false));
            step(fm, state, new DiscardCard());
        }
        assertEquals(cards("7D", "8D", "2D", "7S", "8S", "KD"), state.getGrid(1).getComponents());
    }

    @Test
    public void twoDealsArePlayedAndTheTotalsDecideTheResult() {
        Game game = newGame(2, 5, variant(false, 2));
        GolfSixGameState state = (GolfSixGameState) game.getGameState();
        GolfSixForwardModel fm = (GolfSixForwardModel) game.getForwardModel();
        playFirstDealUntilPlayerZeroFinishes(fm, state, "4H");
        step(fm, state, new ReplaceCard(5));

        assertTrue("the second deal starts", state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getDealer());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(-2, state.getScore(0));
        // player 1: A/3 + 4/Q + 6/6 pair = (1 + 3) + (4 + 10) + 0 = 18
        assertEquals(18, state.getScore(1));
        assertEquals("DDDDDD", faceUpPattern(state, 0));
        assertEquals("DDDDDD", faceUpPattern(state, 1));
        assertEquals(52 - 12 - 1, state.getDrawDeck().getSize());

        playSecondDealUntilPlayerOneFinishes(fm, state, "4D");

        assertEquals(GAME_END, state.getGameStatus());
        // player 0 in deal 2: A/3 + 4/Q + 6/6 pair = (1 + 3) + (4 + 10) + 0 = 18
        // totals: player 0 -2 + 18 = 16, player 1 18 + (-2) = 16: a tie on the lowest total
        assertEquals(16, state.getScore(0));
        assertEquals(16, state.getScore(1));
        assertArrayEquals(new GameResult[]{DRAW_GAME, DRAW_GAME}, state.getPlayerResults());
    }

    @Test
    public void finalTurnsInEachOfTwoDeals() {
        Game game = newGame(2, 5, variant(true, 2));
        GolfSixGameState state = (GolfSixGameState) game.getGameState();
        GolfSixForwardModel fm = (GolfSixForwardModel) game.getForwardModel();
        playFirstDealUntilPlayerZeroFinishes(fm, state, "4H");
        step(fm, state, new ReplaceCard(5));
        assertEquals(0, state.getFinisher());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals("still the first deal", 0, state.getRoundCounter());
        step(fm, state, new DrawCard(false));
        step(fm, state, new ReplaceCard(4));

        // first deal: player 0 -2; player 1 A/3 + 4/4 pair + 6/6 pair = 4 (see the finalTurns walk-through)
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertEquals(-1, state.getFinisher());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(-2, state.getScore(0));
        assertEquals(4, state.getScore(1));

        playSecondDealUntilPlayerOneFinishes(fm, state, "KC");
        assertTrue("player 0 has a final turn", state.isNotTerminal());
        assertEquals(1, state.getFinisher());
        assertEquals(0, state.getCurrentPlayer());
        // player 0 draws the KC and puts it on the face-down QC at position 4
        step(fm, state, new DrawCard(false));
        step(fm, state, new ReplaceCard(4));

        assertEquals(GAME_END, state.getGameStatus());
        // player 0 in deal 2: A/3 + 4/K + 6/6 pair = (1 + 3) + (4 + 0) + 0 = 8; total -2 + 8 = 6
        // player 1: 4 + (-2) = 2
        assertEquals(6, state.getScore(0));
        assertEquals(2, state.getScore(1));
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void seededRandomGamesPlayToTheEndWithEachVariant() {
        int games = 0;
        boolean[] finalTurns = {true, false, true};
        int[] nDeals = {1, 3, 3};
        for (int v = 0; v < 3; v++)
            for (int nPlayers = 2; nPlayers <= 4; nPlayers++)
                for (long seed = 1; seed <= 3; seed++) {
                    playRandomGame(nPlayers, seed, finalTurns[v], nDeals[v]);
                    games++;
                }
        assertEquals(27, games);
    }

    /**
     * Plays random actions to the end, checking at the end of each deal that each score went up by the oracle's score
     * of that deal's final grid, that the next deal starts correctly, and (with finalTurns) that each player other
     * than the finisher had exactly one turn after the finish (at most one if the safeguard ended the deal).
     */
    private void playRandomGame(int nPlayers, long seed, boolean finalTurns, int nDeals) {
        String label = nPlayers + " players, seed " + seed + ", finalTurns " + finalTurns + ", nDeals " + nDeals + ": ";
        Game game = newGame(nPlayers, seed, variant(finalTurns, nDeals));
        GolfSixGameState state = (GolfSixGameState) game.getGameState();
        GolfSixForwardModel fm = (GolfSixForwardModel) game.getForwardModel();
        int maxTurns = nPlayers * 50;
        Random rnd = new Random(seed);
        int[] totals = new int[nPlayers];
        int[] turnsAfterFinish = new int[nPlayers];
        int deals = 0, steps = 0, cap = 3000 * nDeals;
        while (state.isNotTerminal() && steps++ < cap) {
            List<AbstractAction> available = fm.computeAvailableActions(state);
            assertFalse(label + "no actions available", available.isEmpty());
            AbstractAction action = available.get(rnd.nextInt(available.size()));
            int actor = state.getCurrentPlayer();
            int round = state.getRoundCounter();
            int finisherBefore = state.getFinisher();
            int turnBefore = state.getTurnCounter();
            assertTrue(label + "turn counter " + turnBefore, turnBefore < maxTurns);
            // the grids as they will be when the deal is scored, if this action ends it
            List<List<FrenchCard>> grids = gridSnapshot(state);
            if (action instanceof ReplaceCard r)
                grids.get(actor).set(r.position, state.getDrawnCard());
            boolean endsTurn = action instanceof ReplaceCard || action instanceof DiscardCard
                    || (action instanceof TurnUp && state.faceUpCount(actor) + 1 >= 2);

            fm.next(state, action);
            assertAllCardsPresent(state);

            if (finisherBefore >= 0 && endsTurn) {
                assertNotEquals(label + "the finisher has another turn", finisherBefore, actor);
                turnsAfterFinish[actor]++;
            }
            boolean dealEnded = !state.isNotTerminal() || state.getRoundCounter() != round;
            if (!dealEnded) {
                if (!finalTurns)
                    assertEquals(label + "no finisher without finalTurns", -1, state.getFinisher());
                else if (finisherBefore < 0 && state.getFinisher() >= 0) {
                    assertEquals(label + "finisher", actor, state.getFinisher());
                    assertEquals("UUUUUU", faceUpPattern(state, actor));
                }
                continue;
            }

            deals++;
            for (int p = 0; p < nPlayers; p++) {
                totals[p] += oracleGridScore(grids.get(p));
                assertEquals(label + "total of player " + p + " after deal " + deals, totals[p], state.getScore(p));
            }
            boolean bySafeguard = endsTurn && turnBefore + 1 >= maxTurns;
            if (finalTurns && !bySafeguard)
                assertTrue(label + "with finalTurns only the final turns or the safeguard end a deal",
                        finisherBefore >= 0);
            if (finalTurns && finisherBefore >= 0) {
                for (int p = 0; p < nPlayers; p++) {
                    int expected = p == finisherBefore ? 0 : 1;
                    if (bySafeguard) assertTrue(label + "final turns of " + p, turnsAfterFinish[p] <= expected);
                    else assertEquals(label + "final turns of " + p, expected, turnsAfterFinish[p]);
                }
            }
            turnsAfterFinish = new int[nPlayers];
            if (state.isNotTerminal()) {
                assertEquals(label + "round counter", deals, state.getRoundCounter());
                // the player on the new dealer's left: (dealer + 1) % n = roundCounter % n
                assertEquals(label + "first player of the deal", deals % nPlayers, state.getCurrentPlayer());
                assertEquals(label + "finisher reset", -1, state.getFinisher());
                assertEquals(label + "turn counter reset", 0, state.getTurnCounter());
                for (int p = 0; p < nPlayers; p++)
                    assertEquals(label + "redealt face-down", "DDDDDD", faceUpPattern(state, p));
            }
        }
        assertFalse(label + "game did not end within " + cap + " actions", state.isNotTerminal());
        assertEquals(label + "deals played", nDeals, deals);

        int best = Integer.MAX_VALUE;
        for (int t : totals) best = Math.min(best, t);
        int nBest = 0;
        for (int t : totals) if (t == best) nBest++;
        for (int p = 0; p < nPlayers; p++) {
            GameResult result = totals[p] != best ? LOSE_GAME : nBest == 1 ? WIN_GAME : DRAW_GAME;
            assertEquals(label + "result of player " + p, result, state.getPlayerResults()[p]);
        }
    }
}
