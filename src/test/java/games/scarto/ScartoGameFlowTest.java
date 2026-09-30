package games.scarto;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.TarotCard;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static core.components.TarotCard.*;
import static games.scarto.ScartoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Playing cards through the forward model: turn order within a trick, trick completion (who takes which cards,
 * who leads next), the end of the deal and game, and whole random deals (conservation, scores, results).
 */
public class ScartoGameFlowTest {

    ScartoForwardModel fm = new ScartoForwardModel();

    // ---- within a trick ----

    @Test
    public void afterTheLeadTheNextPlayerPlays() {
        ScartoGameState state = newState(3, fm);
        giveHand(state, 0, cup(3), trump(4));
        giveHand(state, 1, cup(5), sword(2));
        play(state, fm, 0, cup(3));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(List.of(cup(3)), state.getCurrentTrick().getComponents());
        play(state, fm, 1, cup(5));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(List.of(cup(3), cup(5)), state.getCurrentTrick().getComponents());
        assertEquals(0, state.getCardsWon(0).getSize() + state.getCardsWon(1).getSize());
    }

    // ---- trick completion ----

    @Test
    public void theWinnerTakesAllThreeCardsAndLeadsTheNextTrick() {
        ScartoGameState state = newState(5, fm);
        giveHand(state, 0, cup(5), sword(2));
        // led by 1: cup 3 (p1), cup King (p2); player 0 to act and must follow cups
        arrangeTrick(state, 1, cup(3), cup(KING));
        play(state, fm, 0, cup(5));
        // cup King is the highest cup -> player 2 takes cup 3, cup King, cup 5
        assertEquals(Set.of(cup(3), cup(KING), cup(5)), setOf(state.getCardsWon(2)));
        assertEquals(0, state.getCardsWon(0).getSize());
        assertEquals(0, state.getCardsWon(1).getSize());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(2, state.getCurrentTrick().getLeader());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(List.of(sword(2)), state.getPlayerHand(0).getComponents());
        assertAllCardsPresent(state);
    }

    @Test
    public void theFoolGoesBackToItsPlayerAndTheWinnerTakesTheOtherTwo() {
        ScartoGameState state = newState(7, fm);
        giveHand(state, 2, cup(9), trump(PAGAT));
        // led by 0: cup 2 (p0), Fool (p1); player 2 must follow cups with the 9
        arrangeTrick(state, 0, cup(2), fool());
        play(state, fm, 2, cup(9));
        // round suit: cup 2 beats cup 9 -> player 0 takes cup 2 and cup 9; the Fool goes to player 1
        assertEquals(Set.of(cup(2), cup(9)), setOf(state.getCardsWon(0)));
        assertEquals(Set.of(fool()), setOf(state.getCardsWon(1)));
        assertEquals(0, state.getCardsWon(2).getSize());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(0, state.getCurrentTrick().getLeader());
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void aTrumpWinsAFoolLedTrick() {
        ScartoGameState state = newState(9, fm);
        giveHand(state, 0, trump(PAGAT), cup(3));
        // led by 1: Fool (p1), baton 4 (p2); player 0 has no batons and must trump
        arrangeTrick(state, 1, fool(), baton(4));
        play(state, fm, 0, trump(PAGAT));
        // the Pagat is the only trump -> player 0 takes baton 4 and the Pagat; the Fool goes back to player 1
        assertEquals(Set.of(baton(4), trump(PAGAT)), setOf(state.getCardsWon(0)));
        assertEquals(Set.of(fool()), setOf(state.getCardsWon(1)));
        assertEquals(0, state.getCardsWon(2).getSize());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getLeader());
        assertAllCardsPresent(state);
    }

    // ---- end of the deal ----

    @Test
    public void theLastTrickEndsTheGameAndTheHighestScoreWins() {
        ScartoGameState state = newState(11, fm);
        // one card left in each hand; player 0 leads the last trick. Every other card (72) goes to player 1's
        // cardsWon, except the scarto: sword 2, 3, 4 (0 points)
        giveHand(state, 0, cup(KING));
        giveHand(state, 1, cup(QUEEN));
        giveHand(state, 2, trump(2));
        setScarto(state, state.cardsWon.get(1), sword(2), sword(3), sword(4));
        arrangeTrick(state, 0);
        assertEquals(72, state.getCardsWon(1).getSize());
        assertAllCardsPresent(state);

        play(state, fm, 0, cup(KING));
        play(state, fm, 1, cup(QUEEN));
        assertTrue(state.isNotTerminal());
        play(state, fm, 2, trump(2));

        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        // p0: nothing -> 0
        // p1: 72 cards = the pack less cup King (4), cup Queen (3), trump 2, sword 2, 3, 4 (0):
        //     51 - 7 = 44 card points, plus (72 + 1) / 3 = 24 -> 68
        // p2 (dealer): cup King 4 + cup Queen 3 + trump 2 + the scarto (0) = 7, 6 cards: + (6 + 1) / 3 = 2 -> 9
        // total 0 + 68 + 9 = 77
        assertEquals(0, state.getGameScore(0), 1e-9);
        assertEquals(68, state.getGameScore(1), 1e-9);
        assertEquals(9, state.getGameScore(2), 1e-9);
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    // ---- whole random deals ----

    /**
     * Plays a factory game with random legal actions to the end, checking conservation after every action and
     * trick bookkeeping after every third. Returns the state at the end.
     */
    ScartoGameState playRandomDeal(long seed) {
        Game game = newGame(seed);
        ScartoGameState state = (ScartoGameState) game.getGameState();
        AbstractForwardModel gfm = game.getForwardModel();
        Random rnd = new Random(seed);
        int actions = 0;
        while (state.isNotTerminal()) {
            assertTrue("more than 75 actions in one deal", actions < 75);
            List<AbstractAction> available = gfm.computeAvailableActions(state);
            assertFalse("no actions for player " + state.getCurrentPlayer() + " at action " + actions,
                    available.isEmpty());
            gfm.next(state, available.get(rnd.nextInt(available.size())));
            actions++;
            assertAllCardsPresent(state);
            if (actions % 3 == 0 && state.isNotTerminal()) {
                // a completed trick has been cleared into the cardsWon piles
                assertEquals("trick after " + actions + " actions", 0, state.getCurrentTrick().getSize());
                int won = 0;
                for (int p = 0; p < 3; p++)
                    won += state.getCardsWon(p).getSize();
                assertEquals("cards won after " + actions / 3 + " tricks", actions, won);
            }
        }
        // 25 tricks of 3 cards
        assertEquals(75, actions);
        return state;
    }

    @Test
    public void aRandomDealLastsTwentyFiveTricksAndConservesTheCards() {
        for (long seed = 1; seed <= 5; seed++) {
            ScartoGameState state = playRandomDeal(seed);
            assertEquals(GAME_END, state.getGameStatus());
            for (int p = 0; p < 3; p++)
                assertEquals(0, state.getPlayerHand(p).getSize());
            assertEquals(3, state.getScarto().getSize());
        }
    }

    @Test
    public void theScoresOfARandomDealAddUpToSeventySeven() {
        for (long seed = 1; seed <= 5; seed++) {
            ScartoGameState state = playRandomDeal(seed);
            double total = 0;
            for (int p = 0; p < 3; p++)
                total += state.getGameScore(p);
            // 51 card points + 78 / 3 = 26 trick points
            assertEquals("seed " + seed, 77, total, 1e-9);
        }
    }

    @Test
    public void theResultsOfARandomDealFollowTheScores() {
        for (long seed = 1; seed <= 5; seed++) {
            ScartoGameState state = playRandomDeal(seed);
            double best = Math.max(state.getGameScore(0), Math.max(state.getGameScore(1), state.getGameScore(2)));
            int nBest = 0;
            for (int p = 0; p < 3; p++)
                if (state.getGameScore(p) == best) nBest++;
            assertTrue("someone scored", best > 0);
            for (int p = 0; p < 3; p++) {
                CoreConstants.GameResult expected = state.getGameScore(p) < best ? LOSE_GAME
                        : nBest == 1 ? WIN_GAME : DRAW_GAME;
                assertEquals("seed " + seed + " player " + p, expected, state.getPlayerResults()[p]);
                assertEquals(expected.value, state.getHeuristicScore(p), 1e-9);
            }
        }
    }

    @Test
    public void theHeuristicMidDealIsBetweenZeroAndOne() {
        Game game = newGame(13);
        ScartoGameState state = (ScartoGameState) game.getGameState();
        Random rnd = new Random(13);
        // play 10 tricks
        for (int i = 0; i < 30; i++) {
            List<AbstractAction> available = fm.computeAvailableActions(state);
            assertFalse("no actions at action " + i, available.isEmpty());
            fm.next(state, available.get(rnd.nextInt(available.size())));
        }
        assertTrue(state.isNotTerminal());
        double total = 0;
        for (int p = 0; p < 3; p++) {
            double h = state.getHeuristicScore(p);
            assertTrue("heuristic " + h, h >= 0 && h <= 1);
            assertEquals(state.getGameScore(p) / 77, h, 1e-9);
            total += state.getGameScore(p);
        }
        // 30 cards have been won: some pile has at least 10 cards, worth at least (10 + 1) / 3 = 3 trick points
        assertTrue("no points after 10 tricks", total >= 3);
    }
}
