package games.scopa;

import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.TarotCard;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static core.components.TarotCard.*;
import static core.components.TarotCard.Suit.*;
import static games.scopa.ScopaTestUtils.*;
import static org.junit.Assert.*;

/**
 * Games of several deals, with ScopaParameters.targetScore > 0: when the totals (banked + deal score) end the game,
 * when the deal is banked instead, and who deals the next deal. Each test arranges the last play of a deal as in ScopaGameEndTest's worked example, which scores
 * P0 4 and P1 3 for the deal, and sets state.bankedScores directly to put the totals where the test needs them.
 * targetScore 0 (one deal, the game ends) is covered by ScopaGameEndTest, which uses the default parameters.
 */
public class ScopaMultiDealTest {

    /**
     * The worked example's last play (see ScopaGameEndTest): P1 is to play the 5 of Swords capturing the 5 of Batons,
     * the last card of the deal. Deal score afterwards:
     * primiera P0 Coins 7 21 + Swords 6 18 + Batons 6 18 + Cups 6 18 = 75, P1 Coins 6 18 + 7s 21 * 3 = 81 -> P1;
     * P0 scopa 1 + cards (22 > 18) 1 + Coins (6 > 4) 1 + 7 of Coins 1 = 4; P1 scopas 2 + primiera 1 = 3.
     */
    private static void arrangeWorkedExample(ScopaGameState state, int banked0, int banked1) {
        arrangeLastPlay(state, 1, of(), of(sword(5)), of(baton(5)),
                cat(suit(Coins, 7, 1, 2, KNAVE, CAVALIER, KING), suit(Swords, 1, 2, 3, 4, 6),
                        suit(Batons, 1, 2, 3, 4, 6), suit(Cups, 1, 2, 3, 4, 5, 6)),
                cat(suit(Coins, 3, 4, 5, 6), suit(Swords, 7, KNAVE, CAVALIER, KING),
                        suit(Batons, 7, KNAVE, CAVALIER, KING), suit(Cups, 7, KNAVE, CAVALIER, KING)));
        state.scopas[0] = 1;
        state.scopas[1] = 2;
        state.lastCapturer = 0;
        state.bankedScores[0] = banked0;
        state.bankedScores[1] = banked1;
    }

    private static void playWorkedExampleLastPlay(ScopaGameState state, ScopaForwardModel fm) {
        play(state, fm, 1, capture(sword(5), baton(5)));
    }

    /** The state is at the start of a fresh deal in play: 4 / 3 / 3 / 30, empty piles, no scopas, no capturer. */
    private static void assertFreshDeal(ScopaGameState state) {
        assertEquals(CoreConstants.GameResult.GAME_ONGOING, state.getGameStatus());
        assertAllCardsPresent(state);
        assertEquals("table", 4, state.getTable().getSize());
        assertEquals("hand 0", 3, state.getPlayerHand(0).getSize());
        assertEquals("hand 1", 3, state.getPlayerHand(1).getSize());
        // 40 - 4 - 3 - 3 = 30
        assertEquals("draw deck", 30, state.getDrawDeck().getSize());
        assertEquals("pile 0", 0, state.getCapturedCards(0).getSize());
        assertEquals("pile 1", 0, state.getCapturedCards(1).getSize());
        assertEquals("scopas 0", 0, state.getScopas(0));
        assertEquals("scopas 1", 0, state.getScopas(1));
        assertEquals("last capturer", -1, state.getLastCapturer());
    }

    @Test
    public void aDealLeavingBothBelowTheTargetIsBankedAndPlayerZeroDealsTheNext() {
        Game game = newGame(61, params(11, false));
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        arrangeWorkedExample(state, 0, 0);

        playWorkedExampleLastPlay(state, fm);

        // totals 0 + 4 = 4 and 0 + 3 = 3: nobody at 11 -> banked, and a new deal
        assertEquals(4, state.getBankedScore(0));
        assertEquals(3, state.getBankedScore(1));
        assertFreshDeal(state);
        assertEquals("round", 1, state.getRoundCounter());
        // deal 2: player 0 deals, so player 1 plays first
        assertEquals("dealer", 0, state.getDealer());
        assertEquals("to act", 1, state.getCurrentPlayer());
        // game score = banked + a deal score of 0 on empty piles (no cards, coins, settebello, primiera or scopas)
        assertEquals(4.0, state.getGameScore(0), 0.0);
        assertEquals(3.0, state.getGameScore(1), 0.0);
    }

    @Test
    public void inTheSecondDealPlayerOneIsDealtFirstAndLeadsEachHand() {
        Game game = newGame(62, params(11, false));
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        arrangeWorkedExample(state, 0, 0);
        playWorkedExampleLastPlay(state, fm);
        assertEquals(0, state.getDealer());

        // re-deal deal 2 from a known pack: the player after the dealer (player 1) gets pack[4..6], player 0 pack[7..9]
        Deck<TarotCard> pack = stackedPack();
        List<TarotCard> order = List.copyOf(pack.getComponents());
        fm.deal(state, pack);
        assertEquals(setOf(order.get(0), order.get(1), order.get(2), order.get(3)), setOf(state.getTable()));
        assertEquals(setOf(order.get(4), order.get(5), order.get(6)), setOf(state.getPlayerHand(1)));
        assertEquals(setOf(order.get(7), order.get(8), order.get(9)), setOf(state.getPlayerHand(0)));

        // play the first hand (6 plays) at random: player 1 leads the next hand as well
        Random rnd = new Random(62);
        for (int i = 0; i < 6; i++) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            int expected = i % 2 == 0 ? 1 : 0;  // 1, 0, 1, 0, 1, 0
            assertEquals("to act at play " + i, expected, state.getCurrentPlayer());
            fm.next(state, actions.get(rnd.nextInt(actions.size())));
        }
        // 30 - 6 = 24 left after the new hands
        assertEquals(24, state.getDrawDeck().getSize());
        assertEquals("player 1 leads the second hand", 1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void aTotalAtTheTargetThatIsStrictlyHighestEndsTheGame() {
        Game game = newGame(63, params(11, false));
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        arrangeWorkedExample(state, 7, 5);

        playWorkedExampleLastPlay(state, fm);

        // totals 7 + 4 = 11, exactly the target, and 5 + 3 = 8 -> P0 wins
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(11.0, state.getGameScore(0), 0.0);
        assertEquals(8.0, state.getGameScore(1), 0.0);
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertAllCardsPresent(state);
    }

    @Test
    public void theSameTotalsBelowATargetOf21AreBankedAndPlayContinues() {
        // the previous test with targetScore 21 instead of 11
        Game game = newGame(64, params(21, false));
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        arrangeWorkedExample(state, 7, 5);

        playWorkedExampleLastPlay(state, fm);

        // totals 7 + 4 = 11 and 5 + 3 = 8, both below 21 -> banked, new deal
        assertEquals(11, state.getBankedScore(0));
        assertEquals(8, state.getBankedScore(1));
        assertFreshDeal(state);
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void theHigherTotalAtTheTargetWinsEvenIfItScoredLessThisDeal() {
        Game game = newGame(65, params(11, false));
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        arrangeWorkedExample(state, 5, 9);

        playWorkedExampleLastPlay(state, fm);

        // totals 5 + 4 = 9 and 9 + 3 = 12 >= 11 -> P1 wins
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(9.0, state.getGameScore(0), 0.0);
        assertEquals(12.0, state.getGameScore(1), 0.0);
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(WIN_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void equalTotalsAtTheTargetAreBankedAndAnotherDealIsPlayed() {
        Game game = newGame(66, params(11, false));
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        arrangeWorkedExample(state, 7, 8);

        playWorkedExampleLastPlay(state, fm);

        // totals 7 + 4 = 11 and 8 + 3 = 11: both at the target but tied -> no winner yet, banked, new deal
        assertEquals(11, state.getBankedScore(0));
        assertEquals(11, state.getBankedScore(1));
        assertFreshDeal(state);
        assertEquals(0, state.getDealer());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void bothTotalsAtTheTargetTheHigherWins() {
        Game game = newGame(67, params(11, false));
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        arrangeWorkedExample(state, 9, 9);

        playWorkedExampleLastPlay(state, fm);

        // totals 9 + 4 = 13 and 9 + 3 = 12, both >= 11 -> P0, the higher, wins
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(13.0, state.getGameScore(0), 0.0);
        assertEquals(12.0, state.getGameScore(1), 0.0);
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void atMaxRoundsTheGameEndsOnTheBankedTotalsWithoutCountingTheDealTwice() {
        ScopaParameters params = params(11, false);
        params.setMaxRounds(1);
        Game game = newGame(68, params);
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        arrangeWorkedExample(state, 0, 0);

        playWorkedExampleLastPlay(state, fm);

        // totals 4 and 3, below 11 -> banked (4, 3) and the round ends; round 1 = maxRounds ends the game
        // game score = banked 4 + 0 on cleared piles (not 4 + 4 = 8), and 3 (not 6) -> P0 wins
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(4, state.getBankedScore(0));
        assertEquals(3, state.getBankedScore(1));
        assertEquals(4.0, state.getGameScore(0), 0.0);
        assertEquals(3.0, state.getGameScore(1), 0.0);
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertAllCardsPresent(state);
    }

    // ---- full random games ----

    /** Plays a whole game at random, checking each step, each new deal and the result. */
    private static void playRandomGame(long seed, int target, boolean redealOnKings) {
        Game game = newGame(seed, params(target, redealOnKings));
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        Random rnd = new Random(seed);
        int round = 0, deals = 1, steps = 0;
        int[] banked = {0, 0};
        while (state.isNotTerminal()) {
            assertTrue("step cap", ++steps <= 36 * 200);
            // the player after the dealer leads every hand: deal r + 1 has roundCounter r, dealer (r + 1) % 2 and
            // leader r % 2
            if (state.getPlayerHand(0).getSize() == 3 && state.getPlayerHand(1).getSize() == 3)
                assertEquals("leader of a hand in round " + round, round % 2, state.getCurrentPlayer());
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, actions.get(rnd.nextInt(actions.size())));
            assertAllCardsPresent(state);
            if (state.getRoundCounter() != round) {
                assertEquals("one round at a time", round + 1, state.getRoundCounter());
                round++;
                deals++;
                if (!state.isNotTerminal())
                    break;  // maxRounds (not set here)
                assertFreshDeal(state);
                assertEquals("dealer", (round + 1) % 2, state.getDealer());
                if (redealOnKings)
                    assertTrue("fewer than 3 Kings on the table",
                            state.getTable().getComponents().stream().filter(c -> c.number == KING).count() < 3);
                int b0 = state.getBankedScore(0), b1 = state.getBankedScore(1);
                assertTrue("banked scores do not fall", b0 >= banked[0] && b1 >= banked[1]);
                // the deal was continued, so no banked total is at the target and strictly highest
                assertFalse("totals " + b0 + "-" + b1 + " should have ended the game",
                        Math.max(b0, b1) >= target && b0 != b1);
                banked[0] = b0;
                banked[1] = b1;
            }
        }
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertTrue("more than one deal to reach " + target, deals > 1);
        // the winner's total is at the target and strictly highest
        double s0 = state.getGameScore(0), s1 = state.getGameScore(1);
        int winner = s0 > s1 ? 0 : 1;
        assertTrue("scores " + s0 + "-" + s1 + ": the winner reaches " + target, Math.max(s0, s1) >= target);
        assertNotEquals("the winner is strictly highest", s0, s1, 0.0);
        assertEquals(WIN_GAME, state.getPlayerResults()[winner]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1 - winner]);
        // the game score is the banked total plus the last deal's score
        assertEquals(state.getBankedScore(winner) + state.getDealScore(winner), state.getGameScore(winner), 0.0);
    }

    @Test
    public void randomGamesToEleven() {
        for (long seed = 71; seed <= 73; seed++)
            playRandomGame(seed, 11, false);
    }

    @Test
    public void randomGamesToTwentyOneWithRedealOnKings() {
        for (long seed = 74; seed <= 76; seed++)
            playRandomGame(seed, 21, true);
    }
}
