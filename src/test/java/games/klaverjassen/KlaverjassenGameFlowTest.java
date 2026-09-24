package games.klaverjassen;

import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.klaverjassen.KlaverjassenParameters.PartnerTrumpRule;
import games.klaverjassen.actions.ChooseTrump;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.Suite.*;
import static games.klaverjassen.KlaverjassenTestUtils.*;
import static org.junit.Assert.*;

/**
 * Seeded random games (one hand, and several hands) played to the end through fm.next, checked at every step
 * against oracles written from the rules (KlaverjassenTestUtils): turns, trick winners, legal plays under each
 * partnerTrumpRule value, card points, roem, the nat of each hand, the redeal, card conservation and the result.
 */
public class KlaverjassenGameFlowTest {

    /**
     * Counts over all the hands played, so the guards can check the interesting cases occurred.
     */
    private int nats, hands, rulesDiffer, restrictedWhileVoid, tricksWithRoem, natsOfTeamOne, madeByTeamOne;

    /**
     * Plays a random game of nHands hands with the given partnerTrumpRule and tieIsFailure, checking every step.
     */
    private void playRandomGame(long seed, PartnerTrumpRule rule, int nHands, boolean tieIsFailure) {
        KlaverjassenParameters params = new KlaverjassenParameters();
        params.setParameterValue("partnerTrumpRule", rule);
        params.setParameterValue("nHands", nHands);
        params.setParameterValue("tieIsFailure", tieIsFailure);
        Game game = newGame(seed, params);
        KlaverjassenGameState state = (KlaverjassenGameState) game.getGameState();
        KlaverjassenForwardModel fm = (KlaverjassenForwardModel) game.getForwardModel();
        Random rnd = new Random(seed);
        int[] expectedScores = new int[2];
        int steps = 0;

        for (int h = 0; h < nHands; h++) {
            String g = rule + " seed " + seed + " hand " + (h + 1) + " of " + nHands + ": ";
            boolean lastHand = h == nHands - 1;
            // hand h (from 0) is dealt by player (h + 3) % 4, so its trump chooser, who leads, is player h % 4
            int chooser = h % 4;

            // a freshly dealt hand: the chooser is to choose trumps; nothing of an earlier hand remains but the scores
            assertTrue(g + "game over before the hand", state.isNotTerminal());
            assertEquals(g + "round", h, state.getRoundCounter());
            assertEquals(g + "trump chooser", chooser, state.getTrumpChooser());
            assertEquals(g + "chooser to play", chooser, state.getCurrentPlayer());
            assertNull(g + "trumps", state.getTrumpSuit());
            assertEquals(g + "trick", 0, state.getCurrentTrick().getSize());
            assertEquals(g + "trick leader", chooser, state.getCurrentTrick().getLeader());
            assertArrayEquals(g + "hand points", new int[2], state.handPoints);
            assertArrayEquals(g + "hand roem", new int[2], state.handRoem);
            assertArrayEquals(g + "tricks won", new int[2], state.tricksWon);
            for (int p = 0; p < 4; p++) {
                assertEquals(g + "cards of player " + p, 8, state.getPlayerHand(p).getSize());
                assertEquals(g + "voids of player " + p, Set.of(), state.knownVoids.get(p));
            }
            assertEquals(g + "discard pile", 0, state.getDiscardPile().getSize());
            assertAllCardsPresent(state);
            assertArrayEquals(g + "team scores carried over", expectedScores, state.teamScores);

            // the trump choice: the chooser chooses from the four suits and then leads
            List<AbstractAction> choices = fm.computeAvailableActions(state);
            assertEquals(g + "trump choices", Set.of(new ChooseTrump(Hearts), new ChooseTrump(Diamonds),
                    new ChooseTrump(Clubs), new ChooseTrump(Spades)), new HashSet<>(choices));
            assertEquals(4, choices.size());
            ChooseTrump choice = (ChooseTrump) choices.get(rnd.nextInt(choices.size()));
            fm.next(state, choice);
            FrenchCard.Suite trumps = choice.suit;
            assertEquals(g + "trumps", trumps, state.getTrumpSuit());
            steps++;

            int leader = chooser;
            int[] expectedPoints = new int[2];
            int[] expectedTricks = new int[2];
            int[] expectedRoem = new int[2];
            List<FrenchCard> trick = new ArrayList<>();
            for (int step = 0; step < 32; step++) {
                String s = g + "step " + step + ": ";
                int player = state.getCurrentPlayer();
                assertTrue(s + "game over early", state.isNotTerminal());
                assertEquals(s + "current player", (leader + trick.size()) % 4, player);
                assertEquals(s + "trick leader", leader, state.getCurrentTrick().getLeader());

                List<FrenchCard> hand = new ArrayList<>(state.getPlayerHand(player).getComponents());
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                assertFalse(s + "no actions", actions.isEmpty());
                assertEquals(s + "duplicate actions", actions.size(), new HashSet<>(actions).size());
                List<FrenchCard> offered = new ArrayList<>();
                for (AbstractAction a : actions) {
                    assertTrue(s + "not a PlayCard: " + a, a instanceof PlayCard);
                    offered.add(((PlayCard) a).card);
                }
                assertTrue(s + "offered a card not in hand", hand.containsAll(offered));
                List<FrenchCard> expected = expectedLegalPlays(hand, trick, leader, trumps, rule);
                assertEquals(s + "legal plays for hand " + hand + " to trick " + trick,
                        new HashSet<>(expected), new HashSet<>(offered));
                PartnerTrumpRule other = rule == PartnerTrumpRule.DISCARD ? PartnerTrumpRule.NO_UNDERTRUMP
                        : PartnerTrumpRule.DISCARD;
                if (!expected.equals(expectedLegalPlays(hand, trick, leader, trumps, other)))
                    rulesDiffer++;
                if (!trick.isEmpty() && hand.stream().noneMatch(c -> c.suite == trick.get(0).suite)
                        && expected.size() < hand.size())
                    restrictedWhileVoid++;

                PlayCard chosen = (PlayCard) actions.get(rnd.nextInt(actions.size()));
                trick.add(chosen.card);
                fm.next(state, chosen);
                steps++;
                if (trick.size() == 4) {
                    int winner = (leader + expectedWinningIndex(trick, trumps)) % 4;
                    int points = 0;
                    for (FrenchCard c : trick)
                        points += expectedCardPoints(c, trumps);
                    if (step == 31)
                        points += 10;                    // last trick bonus
                    expectedPoints[winner % 2] += points;
                    expectedTricks[winner % 2]++;
                    String trickCards = trick.toString();
                    int roem = expectedRoem(trick, trumps);
                    expectedRoem[winner % 2] += roem;
                    if (roem > 0) tricksWithRoem++;
                    if (step == 31)
                        for (int t = 0; t < 2; t++)
                            if (expectedTricks[t] == 8)
                                expectedRoem[t] += 100;      // pit, added when the hand is scored
                    leader = winner;
                    trick.clear();
                    // after the last trick of a hand that is not the last, these fields belong to the next hand
                    // (checked at its start); this hand is then checked through the team scores
                    if (step < 31 || lastHand) {
                        assertArrayEquals(s + "hand points", expectedPoints, state.handPoints);
                        assertArrayEquals(s + "tricks won", expectedTricks, state.tricksWon);
                        assertArrayEquals(s + "roem after trick " + trickCards, expectedRoem, state.handRoem);
                        assertEquals(s + "cards on the discard pile", step + 1, state.getDiscardPile().getSize());
                    }
                    if (step < 31)
                        assertEquals(s + "winner leads", winner, state.getCurrentPlayer());
                }
                assertAllCardsPresent(state);
            }
            assertEquals(g + "card points in a hand", 162, expectedPoints[0] + expectedPoints[1]);

            // the nat: the trump team (the chooser's) fails with a lower total (card points + roem) than the
            // opponents, or an equal one with tieIsFailure; the opponents then score both totals
            int trumpTeam = chooser % 2, opponents = 1 - trumpTeam;
            int trumpTotal = expectedPoints[trumpTeam] + expectedRoem[trumpTeam];
            int opponentsTotal = expectedPoints[opponents] + expectedRoem[opponents];
            boolean nat = trumpTotal < opponentsTotal || (tieIsFailure && trumpTotal == opponentsTotal);
            if (nat) {
                expectedScores[opponents] += trumpTotal + opponentsTotal;
            } else {
                expectedScores[trumpTeam] += trumpTotal;
                expectedScores[opponents] += opponentsTotal;
            }
            assertArrayEquals(g + "team scores after the hand", expectedScores, state.teamScores);
            // the game ends after the last hand, and only then
            assertEquals(g + "game over", lastHand, !state.isNotTerminal());
            hands++;
            if (nat) nats++;
            if (trumpTeam == 1) {
                if (nat) natsOfTeamOne++;
                else madeByTeamOne++;
            }
        }
        assertEquals(33 * nHands, steps);

        int difference = expectedScores[0] - expectedScores[1];
        for (int p = 0; p < 4; p++) {
            int lead = p % 2 == 0 ? difference : -difference;
            GameResult r = lead > 0 ? WIN_GAME : lead < 0 ? LOSE_GAME : DRAW_GAME;
            assertEquals(rule + " seed " + seed + ": result of player " + p, r, state.getPlayerResults()[p]);
            assertEquals(expectedScores[p % 2], state.getGameScore(p), 0.0);
        }
    }

    @Test
    public void randomGamesFollowTheRulesToTheEndOfTheHand() {
        for (PartnerTrumpRule rule : PartnerTrumpRule.values())
            for (long seed = 1; seed <= 40; seed++)
                playRandomGame(seed, rule, 1, false);
        // roem was scored in some tricks
        assertTrue("no trick ever had roem", tricksWithRoem > 0);
        // both branches of the nat rule are exercised
        assertTrue("the trump team was never nat", nats > 0);
        assertTrue("the trump team was always nat", nats < hands);
        // the oracle's Amsterdam-specific branches were reached: a void player restricted (to trumping, overtrumping,
        // or not undertrumping) and a position where the two partnerTrumpRule values differ
        assertTrue("no void player was ever restricted", restrictedWhileVoid > 0);
        assertTrue("the partner trump rules never differed", rulesDiffer > 0);
    }

    @Test
    public void threeHandsAreChosenByPlayersZeroOneAndTwoWithScoresAccumulating() {
        // choosers 0, 1, 2, the redeals, the scores after each hand and the end after the third hand (and not
        // before) are asserted in playRandomGame
        for (long seed = 1; seed <= 5; seed++)
            playRandomGame(seed, PartnerTrumpRule.DISCARD, 3, false);
        assertEquals(15, hands);
    }

    @Test
    public void randomMultiHandGamesFollowTheRules() {
        // five hands: the choosers go 0, 1, 2, 3 and back to 0; tieIsFailure alternates with the seed
        for (PartnerTrumpRule rule : PartnerTrumpRule.values())
            for (long seed = 1; seed <= 10; seed++)
                playRandomGame(seed, rule, 5, seed % 2 == 0);
        assertEquals(100, hands);
        // team 1 was the trump team (hands 2 and 4) both failing and making it
        assertTrue("team 1 was never nat as the trump team", natsOfTeamOne > 0);
        assertTrue("team 1 never made it as the trump team", madeByTeamOne > 0);
    }
}
