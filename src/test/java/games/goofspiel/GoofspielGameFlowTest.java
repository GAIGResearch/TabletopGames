package games.goofspiel;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import core.components.FrenchCard;
import games.goofspiel.GoofspielParameters.TieRule;
import games.goofspiel.actions.Bid;
import org.junit.Test;

import java.util.*;

import static core.CoreConstants.GameResult.*;
import static games.goofspiel.GoofspielTestUtils.*;
import static org.junit.Assert.*;

/**
 * Whole games driven through fm.next: scripted games to the end, bids applied one at a time versus as one
 * SimultaneousAction, and random games (every tie rule, cardsPerSuit, aceHigh) checked against an oracle
 * written from the rules.
 */
public class GoofspielGameFlowTest {

    @Test
    public void tiesInTheLastTwoRoundsLeaveThosePrizesUnwon() {
        Game g = newGame(2, 21);
        GoofspielGameState state = (GoofspielGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        // prize in round r has value r
        arrangePrizes(state, "AD", "2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D", "10D", "JD", "QD", "KD");

        // round 1: 2 < Q, player 1 wins the Ace
        playRound(fm, state, "2C", "QS");
        // rounds 2-10: player 0 bids one more than player 1 (3..J against 2..10), winning prizes 2..10
        for (int r = 2; r <= 10; r++)
            playRound(fm, state, RANKS.get(r) + "C", RANKS.get(r - 1) + "S");
        // round 11: Q > J, player 0 wins the Jack
        playRound(fm, state, "QC", "JS");
        assertEquals(11, state.getRoundCounter());
        // both now hold A and K; round 12: K = K, the Queen of Diamonds is carried over
        playRound(fm, state, "KC", "KS");
        assertTrue("the game ended before the hands were empty", state.isNotTerminal());
        assertEquals(12, state.getRoundCounter());
        assertEquals(cards("QD", "KD"), multiset(state.getPrizesOnOffer()));
        assertEquals(0, state.getPrizeDeck().getSize());
        assertEquals(List.of(0, 1), state.getCurrentSimultaneousPlayers());

        // round 13 (the last): A = A, so the Queen and King go to nobody
        playRound(fm, state, "AC", "AS");
        assertFalse(state.isNotTerminal());
        assertEquals(cards("QD", "KD"), multiset(state.getDiscardedPrizes()));
        assertEquals(0, state.getPrizesOnOffer().getSize());
        assertEquals(0, state.getPrizeDeck().getSize());
        for (int p = 0; p < 2; p++) {
            assertEquals(0, state.getHand(p).getSize());
            assertEquals(0, state.getBid(p).getSize());
            assertEquals(13, state.getPlayedBids(p).getSize());
        }
        // player 0: 2 + 3 + ... + 11 = 65; player 1: Ace = 1
        assertEquals(65.0, state.getGameScore(0), 0.0);
        assertEquals(1.0, state.getGameScore(1), 0.0);
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertAllCardsPresent(state);
    }

    @Test
    public void equalScoresAreADraw() {
        Game g = newGame(2, 22);
        GoofspielGameState state = (GoofspielGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        arrangePrizes(state, "AD", "4D", "2D", "5D", "3D", "6D", "10D", "7D", "JD", "8D", "QD", "9D", "KD");
        // player 0 bids A..K in order; player 1 bids 2,A,4,3,6,5,8,7,10,9,Q,J,K:
        // player 1 wins the odd rounds (prizes 1,2,3,10,11,12 = 39), player 0 the even rounds (4..9 = 39),
        // and the K = K tie in the last round discards the King of Diamonds
        String[] p1 = {"2", "A", "4", "3", "6", "5", "8", "7", "10", "9", "Q", "J", "K"};
        for (int r = 0; r < 13; r++)
            playRound(fm, state, RANKS.get(r) + "C", p1[r] + "S");

        assertFalse(state.isNotTerminal());
        assertEquals(cards("4D", "5D", "6D", "7D", "8D", "9D"), multiset(state.getWonPrizes(0)));
        assertEquals(cards("AD", "2D", "3D", "10D", "JD", "QD"), multiset(state.getWonPrizes(1)));
        assertEquals(cards("KD"), multiset(state.getDiscardedPrizes()));
        assertEquals(39.0, state.getGameScore(0), 0.0);
        assertEquals(39.0, state.getGameScore(1), 0.0);
        assertEquals(DRAW_GAME, state.getPlayerResults()[0]);
        assertEquals(DRAW_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void bidsOneAtATimeAndAsOneSimultaneousActionReachTheSameStates() {
        for (int nPlayers : new int[]{2, 3}) {
            Game g1 = newGame(nPlayers, 31);
            GoofspielGameState joint = (GoofspielGameState) g1.getGameState();
            // A copy, not a second game: Deck equality includes the component IDs, which differ between games
            GoofspielGameState sequential = (GoofspielGameState) joint.copy();
            Random rnd = new Random(31);
            int ties = 0;
            for (int round = 0; round < 13; round++) {
                // choose a random card for each player (the same in both games)
                Map<Integer, AbstractAction> choices = new LinkedHashMap<>();
                int top = -1;
                int nTop = 0;
                for (int p = 0; p < nPlayers; p++) {
                    List<FrenchCard> hand = joint.getHand(p).getComponents();
                    FrenchCard c = hand.get(rnd.nextInt(hand.size()));
                    if (round == 0 && p == 1) {
                        // force a tie for the highest bid in the first round: player 1 matches player 0
                        int target = value(((Bid) choices.get(0)).card);
                        c = hand.stream().filter(h -> value(h) == target).findFirst().orElseThrow();
                    }
                    choices.put(p, new Bid(p, c));
                    if (value(c) > top) { top = value(c); nTop = 1; } else if (value(c) == top) nTop++;
                }
                if (nTop > 1) ties++;
                g1.getForwardModel().next(joint, new SimultaneousAction(choices));
                for (int p = 0; p < nPlayers; p++) {
                    assertEquals(p, sequential.getCurrentPlayer());
                    g1.getForwardModel().next(sequential, choices.get(p));
                }
                // the game content, round and turn agree (the tick and turn counters count actions, so differ)
                assertTrue("round " + round + " with " + nPlayers + " players", joint._equals(sequential));
                assertEquals(sequential.getRoundCounter(), joint.getRoundCounter());
                assertEquals(sequential.getGameStatus(), joint.getGameStatus());
                if (round < 12) {
                    assertEquals(round + 1, joint.getRoundCounter());
                    assertEquals(0, joint.getCurrentPlayer());
                    assertEquals(sequential.getCurrentSimultaneousPlayers(), joint.getCurrentSimultaneousPlayers());
                }
            }
            assertFalse(joint.isNotTerminal());
            assertArrayEquals(sequential.getPlayerResults(), joint.getPlayerResults());
            assertTrue("no tie was exercised with " + nPlayers + " players", ties > 0);
        }
    }

    @Test
    public void randomGamesWithTwoPlayersFollowTheRules() {
        for (long seed = 1; seed <= 10; seed++)
            playRandomGameAgainstOracle(2, seed);
    }

    @Test
    public void randomGamesWithFivePlayersFollowTheRules() {
        for (long seed = 1; seed <= 10; seed++)
            playRandomGameAgainstOracle(5, seed);
    }

    @Test
    public void randomGamesUnderDiscardFollowTheRules() {
        for (int nPlayers : new int[]{2, 5}) {
            int noWinner = 0;
            for (long seed = 1; seed <= 10; seed++)
                noWinner += playRandomGameAgainstOracle(nPlayers, seed, params("tieRule", TieRule.DISCARD))[2];
            assertTrue("no tie was discarded with " + nPlayers + " players", noWinner > 0);
        }
    }

    @Test
    public void randomGamesUnderHighestUniqueFollowTheRules() {
        for (int nPlayers : new int[]{2, 5}) {
            int[] total = new int[4];
            for (long seed = 1; seed <= 10; seed++) {
                int[] counts = playRandomGameAgainstOracle(nPlayers, seed, params("tieRule", TieRule.HIGHEST_UNIQUE));
                for (int i = 0; i < 4; i++) total[i] += counts[i];
            }
            // with 5 players a round with no unique bid needs a triple and a pair, which random play rarely gives;
            // underHighestUniqueWithNoUniqueBid... covers it with 4 players
            if (nPlayers == 2)
                assertTrue("no round without a unique bid with 2 players", total[2] > 0);
            else
                assertTrue("no round won by a unique bid below a tied top with 5 players", total[3] > 0);
        }
    }

    @Test
    public void randomGamesWithFiveCardsPerSuitAndAceHighFollowTheRules() {
        for (TieRule tieRule : TieRule.values())
            for (long seed = 1; seed <= 5; seed++)
                playRandomGameAgainstOracle(3, seed, params("tieRule", tieRule, "cardsPerSuit", 5, "aceHigh", true));
    }

    private static void playRandomGameAgainstOracle(int nPlayers, long seed) {
        int[] counts = playRandomGameAgainstOracle(nPlayers, seed, new GoofspielParameters());
        if (nPlayers == 5) assertTrue("no tie with 5 players, seed " + seed, counts[1] > 0);
    }

    /**
     * Plays a random game through fm.next, always for the player holding the turn, checking at every step that
     * every card is present once, and at every resolution that the outcome is the one the rules give for the
     * parameters' tie rule, cardsPerSuit and aceHigh (read with getParameterValue; the values are the test's own).
     *
     * @return {rounds won, rounds with the highest bid tied, rounds with no winner,
     * rounds won by a lower unique bid below a tied top (HIGHEST_UNIQUE)}
     */
    private static int[] playRandomGameAgainstOracle(int nPlayers, long seed, GoofspielParameters params) {
        TieRule tieRule = (TieRule) params.getParameterValue("tieRule");
        int n = (int) params.getParameterValue("cardsPerSuit");
        boolean aceHigh = (boolean) params.getParameterValue("aceHigh");
        Game g = newGame(nPlayers, seed, params);
        GoofspielGameState state = (GoofspielGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        assertEquals(n, state.getHand(0).getSize());
        Random rnd = new Random(seed);
        int wins = 0, topTies = 0, noWinner = 0, uniqueBelowTie = 0, steps = 0;
        while (state.isNotTerminal() && steps++ < 1000) {
            int player = state.getCurrentPlayer();
            List<Integer> stillToBid = state.getPlayersStillToBid();
            assertTrue("the turn is with player " + player + " who has already bid", stillToBid.contains(player));
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertEquals(state.getHand(player).getSize(), actions.size());
            Bid bid = (Bid) actions.get(rnd.nextInt(actions.size()));
            assertEquals(player, bid.player);

            boolean lastBid = stillToBid.size() == 1;
            // what the oracle needs, taken before the round is resolved
            int[] bidValues = new int[nPlayers];
            List<Map<FrenchCard, Integer>> wonBefore = new ArrayList<>();
            for (int p = 0; p < nPlayers; p++) {
                bidValues[p] = p == player ? value(bid.card, aceHigh) : (state.getBid(p).getSize() == 1 ? value(state.getBid(p).get(0), aceHigh) : -1);
                wonBefore.add(multiset(state.getWonPrizes(p)));
            }
            Map<FrenchCard, Integer> offer = multiset(state.getPrizesOnOffer());
            Map<FrenchCard, Integer> discardedBefore = multiset(state.getDiscardedPrizes());
            boolean lastRound = state.getHand(player).getSize() == 1 && lastBid;
            int roundBefore = state.getRoundCounter();

            fm.next(state, bid);
            assertAllCardsPresent(state);

            if (!lastBid) {
                assertEquals(roundBefore, state.getRoundCounter());
                assertEquals(offer, multiset(state.getPrizesOnOffer()));
                continue;
            }
            // the oracle's winner, from the rules
            int top = Arrays.stream(bidValues).max().getAsInt();
            List<Integer> topBidders = new ArrayList<>();
            for (int p = 0; p < nPlayers; p++) if (bidValues[p] == top) topBidders.add(p);
            if (topBidders.size() > 1) topTies++;
            int winner = topBidders.size() == 1 ? topBidders.get(0) : -1;
            if (tieRule == TieRule.HIGHEST_UNIQUE) {
                // disqualify every value made by more than one player; the highest remaining bid wins
                winner = -1;
                for (int p = 0; p < nPlayers; p++) {
                    int v = bidValues[p];
                    boolean unique = Arrays.stream(bidValues).filter(b -> b == v).count() == 1;
                    if (unique && (winner == -1 || v > bidValues[winner])) winner = p;
                }
                if (winner >= 0 && topBidders.size() > 1) uniqueBelowTie++;
            }

            if (winner >= 0) {
                wins++;
                Map<FrenchCard, Integer> expected = new HashMap<>(wonBefore.get(winner));
                offer.forEach((c, k) -> expected.merge(c, k, Integer::sum));
                assertEquals("winner " + winner + " takes every prize on offer", expected, multiset(state.getWonPrizes(winner)));
                assertEquals(discardedBefore, multiset(state.getDiscardedPrizes()));
                if (!lastRound) assertEquals(1, state.getPrizesOnOffer().getSize());
            } else {
                noWinner++;
                if (lastRound || tieRule != TieRule.CARRY_OVER) {
                    Map<FrenchCard, Integer> expected = new HashMap<>(discardedBefore);
                    offer.forEach((c, k) -> expected.merge(c, k, Integer::sum));
                    assertEquals("no winner under " + tieRule + " discards the prizes on offer, last round " + lastRound,
                            expected, multiset(state.getDiscardedPrizes()));
                    if (!lastRound) {
                        // only the next prize is on offer
                        assertEquals(1, state.getPrizesOnOffer().getSize());
                        assertFalse(offer.containsKey(state.getPrizesOnOffer().get(0)));
                    }
                } else {
                    // carried over: the old prizes plus one new one
                    Map<FrenchCard, Integer> now = multiset(state.getPrizesOnOffer());
                    assertEquals(offer.size() + 1, now.size());
                    assertTrue(now.keySet().containsAll(offer.keySet()));
                    assertEquals(discardedBefore, multiset(state.getDiscardedPrizes()));
                }
            }
            for (int p = 0; p < nPlayers; p++) {
                if (p != winner)
                    assertEquals("player " + p + " won nothing", wonBefore.get(p), multiset(state.getWonPrizes(p)));
                assertEquals(0, state.getBid(p).getSize());
            }
            if (lastRound) {
                assertFalse(state.isNotTerminal());
            } else {
                assertTrue(state.isNotTerminal());
                assertEquals(roundBefore + 1, state.getRoundCounter());
                assertEquals(0, state.getCurrentPlayer());
                assertEquals(nPlayers, state.getPlayersStillToBid().size());
            }
        }
        assertFalse("game did not end within 1000 actions", state.isNotTerminal());
        assertTrue(wins > 0);
        // the game lasts cardsPerSuit rounds
        assertEquals(n, wins + noWinner);

        // end: all n prizes are won or discarded, all n cards of each player played
        int prizes = state.getDiscardedPrizes().getSize();
        for (int p = 0; p < nPlayers; p++) {
            prizes += state.getWonPrizes(p).getSize();
            assertEquals(n, state.getPlayedBids(p).getSize());
            assertEquals(0, state.getHand(p).getSize());
        }
        assertEquals(n, prizes);
        assertEquals(0, state.getPrizesOnOffer().getSize());
        assertEquals(0, state.getPrizeDeck().getSize());

        // results: the highest score wins, a shared highest score draws, everyone else loses
        int[] scores = new int[nPlayers];
        for (int p = 0; p < nPlayers; p++) {
            for (FrenchCard c : state.getWonPrizes(p).getComponents()) scores[p] += value(c, aceHigh);
            assertEquals("score of player " + p + ", seed " + seed, scores[p], state.getGameScore(p), 0.0);
        }
        int best = Arrays.stream(scores).max().getAsInt();
        long nBest = Arrays.stream(scores).filter(s -> s == best).count();
        for (int p = 0; p < nPlayers; p++) {
            GameResult expected = scores[p] < best ? LOSE_GAME : nBest > 1 ? DRAW_GAME : WIN_GAME;
            assertEquals("result of player " + p + ", seed " + seed, expected, state.getPlayerResults()[p]);
        }
        return new int[]{wins, topTies, noWinner, uniqueBelowTie};
    }
}
