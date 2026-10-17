package games.scarto;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.TarotCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static core.components.TarotCard.*;
import static games.scarto.ScartoTestUtils.*;
import static org.junit.Assert.*;

/**
 * The several-deals variant (ScartoParameters.nDeals = 3): the end of each deal, the banked scores, the final scores
 * and a whole random three-deal game.
 */
public class ScartoSeveralDealsTest {

    ScartoForwardModel fm = new ScartoForwardModel();

    /**
     * Arranges the last trick of a deal, led by player 0: player 0 holds cup King, player 1 cup Queen, player 2
     * trump 2; player 0 has already won the Angel and coin King, player 2 nothing, and every other card except the
     * scarto (sword 2, 3, 4) is in player 1's cardsWon (78 - 3 - 2 - 3 = 70 cards). Player 0 is also marked void in
     * swords, so a test can see the voids cleared.
     * <p>
     * Card points of the pack: 51. Of these, cup King 4 + cup Queen 3 + Angel 4 + coin King 4 = 15 are outside
     * player 1's pile (trump 2 and swords 2-4 score 0), so player 1's 70 cards hold 36 card points.
     * Trick points are (cards + 1) / 3, rounded down.
     */
    void arrangeLastTrick(ScartoGameState state) {
        giveHand(state, 0, cup(KING));
        giveHand(state, 1, cup(QUEEN));
        giveHand(state, 2, trump(2));
        setCardsWon(state, 0, trump(ANGEL), coin(KING));
        setCardsWon(state, 2);
        arrangeTrick(state, 0);
        setScarto(state, state.cardsWon.get(1), sword(2), sword(3), sword(4));
        state.knownVoids.get(0).add(TarotCard.Suit.Swords);
        assertEquals(70, state.getCardsWon(1).getSize());
        assertAllCardsPresent(state);
    }

    /** Plays the arranged last trick: cup King (0), cup Queen (1), trump 2 (2) - player 2's trump takes it. */
    void playLastTrick(ScartoGameState state) {
        play(state, fm, 0, cup(KING));
        play(state, fm, 1, cup(QUEEN));
        play(state, fm, 2, trump(2));
    }

    // The arranged deal's scores, by who deals (the scarto, 3 cards of 0 points, goes to the dealer):
    //   p0: Angel + coin King = 8, plus (2 + 1) / 3 = 1 -> 9; as dealer 5 cards: 8 + (5 + 1) / 3 = 2 -> 10
    //   p1: 36 + (70 + 1) / 3 = 23 -> 59;               as dealer 73 cards: 36 + (73 + 1) / 3 = 24 -> 60
    //   p2: cup King 4 + cup Queen 3 = 7, plus (3 + 1) / 3 = 1 -> 8; as dealer 6 cards: 7 + (6 + 1) / 3 = 2 -> 9
    // Deal 1 (dealer 2): 9, 59, 9. Deal 2 (dealer 0): 10, 59, 8. Deal 3 (dealer 1): 9, 60, 8. Each deal sums to 77.

    /** A three-deal state (seed as given) whose first deal has just ended through the arranged last trick. */
    ScartoGameState afterFirstDeal(long seed) {
        ScartoGameState state = newState(seed, fm, threeDeals());
        arrangeLastTrick(state);
        playLastTrick(state);
        assertTrue("the game ended after the first of 3 deals", state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        return state;
    }

    // ---- the end of a deal that is not the last ----

    @Test
    public void afterTheFirstOfThreeDealsTheNextPlayerDealsAfresh() {
        ScartoGameState state = newState(21, fm, threeDeals());
        assertEquals(2, state.getDealer());
        arrangeLastTrick(state);
        playLastTrick(state);

        assertTrue(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_ONGOING, state.getGameStatus());
        assertEquals(1, state.getRoundCounter());
        // round 1: dealer (1 + 3 - 1) % 3 = 0; player 1, after the dealer, leads an empty trick
        assertEquals(0, state.getDealer());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getCurrentTrick().getLeader());
        assertEquals(0, state.getCurrentTrick().getSize());
        for (int p = 0; p < 3; p++) {
            assertEquals("hand " + p, 25, state.getPlayerHand(p).getSize());
            assertEquals("cardsWon " + p, 0, state.getCardsWon(p).getSize());
            assertTrue("voids " + p, state.getKnownVoids().get(p).isEmpty());
        }
        assertEquals(3, state.getScarto().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void theFirstDealsScoresAreBanked() {
        ScartoGameState state = afterFirstDeal(23);
        // deal 1 (dealer 2): 9, 59, 9
        assertEquals(9, state.getBankedScore(0));
        assertEquals(59, state.getBankedScore(1));
        assertEquals(9, state.getBankedScore(2));
        // the new deal has no cards won yet: non-dealers score just their banked points ((0 + 1) / 3 = 0 for
        // an empty pile); the new dealer (0) also has the new scarto
        assertEquals(59, state.getGameScore(1), 1e-9);
        assertEquals(9, state.getGameScore(2), 1e-9);
        ScartoParameters params = (ScartoParameters) state.getGameParameters();
        assertEquals(9 + params.pilePoints(state.getScarto().getComponents()), state.getGameScore(0), 1e-9);
    }

    @Test
    public void theNextDealIsShuffled() {
        // two runs that differ only in their random number generator: a copy has its own
        ScartoGameState state = newState(25, fm, threeDeals());
        arrangeLastTrick(state);
        play(state, fm, 0, cup(KING));
        play(state, fm, 1, cup(QUEEN));
        ScartoGameState other = (ScartoGameState) state.copy();
        assertEquals(state, other);
        play(state, fm, 2, trump(2));
        play(other, fm, 2, trump(2));
        assertEquals(1, state.getRoundCounter());
        assertEquals(1, other.getRoundCounter());
        assertEquals(25, state.getPlayerHand(1).getSize());
        assertEquals(25, other.getPlayerHand(1).getSize());
        // 78 cards shuffled: the same hands by chance are negligible
        assertNotEquals(setOf(state.getPlayerHand(1)), setOf(other.getPlayerHand(1)));
    }

    @Test
    public void afterTheSecondDealPlayerOneDealsAndPlayerTwoLeads() {
        ScartoGameState state = afterFirstDeal(27);
        arrangeLastTrick(state);
        playLastTrick(state);

        assertTrue(state.isNotTerminal());
        assertEquals(2, state.getRoundCounter());
        // round 2: dealer (2 + 3 - 1) % 3 = 1; player 2 leads
        assertEquals(1, state.getDealer());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(2, state.getCurrentTrick().getLeader());
        assertEquals(0, state.getCurrentTrick().getSize());
        for (int p = 0; p < 3; p++) {
            assertEquals(25, state.getPlayerHand(p).getSize());
            assertEquals(0, state.getCardsWon(p).getSize());
        }
        // deal 1 (9, 59, 9) + deal 2 with dealer 0 (10, 59, 8)
        assertEquals(19, state.getBankedScore(0));
        assertEquals(118, state.getBankedScore(1));
        assertEquals(17, state.getBankedScore(2));
        assertAllCardsPresent(state);
    }

    @Test
    public void theGameEndsAfterTheThirdDealWithTheSummedScores() {
        ScartoGameState state = afterFirstDeal(29);
        arrangeLastTrick(state);
        playLastTrick(state);
        assertEquals(2, state.getRoundCounter());
        arrangeLastTrick(state);
        playLastTrick(state);

        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        // the last deal's cards stay in the piles (p1 70 + p0 2 + p2 3) and the scarto
        assertEquals(70, state.getCardsWon(1).getSize());
        assertEquals(2, state.getCardsWon(0).getSize());
        assertEquals(3, state.getCardsWon(2).getSize());
        assertEquals(3, state.getScarto().getSize());
        // banked (19, 118, 17) + deal 3 with dealer 1 (9, 60, 8) = 28, 178, 25; total 231 = 3 * 77
        assertEquals(28, state.getGameScore(0), 1e-9);
        assertEquals(178, state.getGameScore(1), 1e-9);
        assertEquals(25, state.getGameScore(2), 1e-9);
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertEquals(WIN_GAME.value, state.getHeuristicScore(1), 1e-9);
        assertEquals(LOSE_GAME.value, state.getHeuristicScore(0), 1e-9);
    }

    @Test
    public void theHeuristicInTheSecondDealIsOutOfTheTwoDealsPoints() {
        ScartoGameState state = afterFirstDeal(31);
        // mid deal 2: player 1 has won the coin King (4 points, (1 + 1) / 3 = 0 trick points)
        setCardsWon(state, 1, coin(KING));
        // p1: 59 banked + 4 = 63, out of 77 * 2 = 154 points in deals 1 and 2; p2: 9 banked + 0
        assertEquals(63.0 / 154, state.getHeuristicScore(1), 1e-9);
        assertEquals(9.0 / 154, state.getHeuristicScore(2), 1e-9);
    }

    @Test
    public void aCopyInTheSecondDealIsEqualAndItsBankedScoresAreIndependent() {
        ScartoGameState state = afterFirstDeal(33);
        List<AbstractAction> available = fm.computeAvailableActions(state);
        fm.next(state, available.get(0));
        ScartoGameState copy = (ScartoGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        for (int p = 0; p < 3; p++)
            assertEquals(state.getBankedScore(p), copy.getBankedScore(p));
        copy.bankedScores[1] += 10;
        assertEquals(59, state.getBankedScore(1));
        assertNotEquals(state, copy);
    }

    // ---- a whole random three-deal game ----

    @Test
    public void aRandomThreeDealGameLetsEachPlayerDealOnceAndScoresTwoHundredAndThirtyOne() {
        for (long seed = 1; seed <= 3; seed++) {
            Game game = newGame(seed, threeDeals());
            ScartoGameState state = (ScartoGameState) game.getGameState();
            AbstractForwardModel gfm = game.getForwardModel();
            Random rnd = new Random(seed);
            List<Integer> dealers = new ArrayList<>(List.of(state.getDealer()));
            int round = 0;
            int actions = 0;
            while (state.isNotTerminal()) {
                assertTrue("seed " + seed + ": more than 225 actions", actions < 225);
                List<AbstractAction> available = gfm.computeAvailableActions(state);
                assertFalse("seed " + seed + ": no actions at action " + actions, available.isEmpty());
                gfm.next(state, available.get(rnd.nextInt(available.size())));
                actions++;
                assertAllCardsPresent(state);
                if (state.getRoundCounter() != round) {
                    round = state.getRoundCounter();
                    // a new deal after 75 actions a deal
                    assertEquals("seed " + seed, 75 * round, actions);
                    dealers.add(state.getDealer());
                    assertEquals((state.getDealer() + 1) % 3, state.getCurrentPlayer());
                    int banked = 0;
                    for (int p = 0; p < 3; p++) {
                        assertEquals(25, state.getPlayerHand(p).getSize());
                        assertEquals(0, state.getCardsWon(p).getSize());
                        banked += state.getBankedScore(p);
                    }
                    assertEquals("seed " + seed + " banked after deal " + round, 77 * round, banked);
                }
            }
            // 3 deals of 25 tricks of 3 cards
            assertEquals(225, actions);
            assertEquals(2, state.getRoundCounter());
            // each player deals once: 2, then 0, then 1
            assertEquals(List.of(2, 0, 1), dealers);
            double total = 0, best = -1;
            for (int p = 0; p < 3; p++) {
                total += state.getGameScore(p);
                best = Math.max(best, state.getGameScore(p));
            }
            assertEquals("seed " + seed, 3 * 77, total, 1e-9);
            int nBest = 0;
            for (int p = 0; p < 3; p++)
                if (state.getGameScore(p) == best) nBest++;
            for (int p = 0; p < 3; p++) {
                CoreConstants.GameResult expected = state.getGameScore(p) < best ? LOSE_GAME
                        : nBest == 1 ? WIN_GAME : DRAW_GAME;
                assertEquals("seed " + seed + " player " + p, expected, state.getPlayerResults()[p]);
            }
        }
    }
}
