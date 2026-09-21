package games.whist;

import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.Suite.*;
import static games.whist.WhistTestUtils.*;
import static org.junit.Assert.*;

/**
 * Seeded random games played to the end through fm.next, checked at every step against oracles written from the
 * rules: the legal cards, whose turn it is, who wins each trick, card conservation and the final score.
 */
public class WhistGameFlowTest {

    /**
     * Follow suit if able, else anything.
     */
    private static List<FrenchCard> expectedLegal(List<FrenchCard> hand, List<FrenchCard> trick) {
        if (trick.isEmpty()) return hand;
        FrenchCard.Suite led = trick.get(0).suite;
        List<FrenchCard> follow = hand.stream().filter(c -> c.suite == led).toList();
        return follow.isEmpty() ? hand : follow;
    }

    /**
     * The index in the complete trick of the winning card: the highest trump, else the highest of the suit led.
     */
    private static int winningIndex(List<FrenchCard> trick, FrenchCard.Suite trumps) {
        int best = 0;
        for (int i = 1; i < trick.size(); i++) {
            FrenchCard c = trick.get(i), b = trick.get(best);
            boolean cTrump = c.suite == trumps, bTrump = b.suite == trumps;
            if (cTrump && !bTrump) best = i;
            else if (c.suite == b.suite && c.number > b.number) best = i;
        }
        return best;
    }

    /**
     * The trump suit of deal d in ROTATION mode, from the rules.
     */
    private static FrenchCard.Suite rotationTrumps(int d, boolean noTrumpsInRotation) {
        FrenchCard.Suite[] cycle = noTrumpsInRotation
                ? new FrenchCard.Suite[]{Hearts, Diamonds, Spades, Clubs, null}
                : new FrenchCard.Suite[]{Hearts, Diamonds, Spades, Clubs};
        return cycle[d % cycle.length];
    }

    /**
     * Plays a random game of params.nDeals deals (null params: the defaults, one deal), checking each step and the
     * start and score of each deal.
     */
    private void playRandomGame(long seed, WhistParameters params) {
        Game game = newGame(seed, params);
        WhistGameState state = (WhistGameState) game.getGameState();
        WhistForwardModel fm = (WhistForwardModel) game.getForwardModel();
        WhistParameters p = (WhistParameters) state.getGameParameters();
        int nDeals = p.nDeals;
        Random rnd = new Random(seed);
        int[] expectedPoints = new int[2];
        int[] expectedTricks = new int[4];
        int steps = 0;

        for (int deal = 0; deal < nDeals; deal++) {
            // the start of the deal: dealer (deal + 3) % 4, the player on their left leads
            String d = "deal " + deal + ": ";
            int dealer = (deal + 3) % 4;
            int leader = (dealer + 1) % 4;
            assertTrue(d + "game over early", state.isNotTerminal());
            assertEquals(d + "round counter", deal, state.getRoundCounter());
            assertEquals(d + "dealer", dealer, state.getDealer());
            assertEquals(d + "leader", leader, state.getCurrentPlayer());
            assertEquals(d + "trick leader", leader, state.getCurrentTrick().getLeader());
            assertEquals(d + "discard pile", 0, state.getDiscardPile().getSize());
            for (int pl = 0; pl < 4; pl++) {
                assertEquals(d + "hand size", 13, state.getPlayerHand(pl).getSize());
                assertEquals(d + "tricks", 0, state.getTricksTaken(pl));
                assertTrue(d + "voids", state.getKnownVoids().get(pl).isEmpty());
            }
            assertArrayEquals(d + "points carried over", expectedPoints, state.teamPoints);
            FrenchCard.Suite trumps;
            if (p.trumpMode == WhistParameters.TrumpMode.TURN_UP) {
                assertTrue(d + "trump card with the dealer", state.getPlayerHand(dealer).contains(state.getTrumpCard()));
                trumps = state.getTrumpCard().suite;
                assertEquals(d + "trump suit", trumps, state.getTrumpSuit());
            } else {
                trumps = rotationTrumps(deal, p.noTrumpsInRotation);
                assertNull(d + "trump card", state.getTrumpCard());
                assertEquals(d + "trump suit", trumps, state.getTrumpSuit());
            }

            Arrays.fill(expectedTricks, 0);
            List<FrenchCard> trick = new ArrayList<>();
            for (int step = 0; step < 52; step++) {
                int player = state.getCurrentPlayer();
                assertEquals(d + "current player at step " + step, (leader + trick.size()) % 4, player);
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                List<FrenchCard> hand = new ArrayList<>(state.getPlayerHand(player).getComponents());
                List<AbstractAction> expected = expectedLegal(hand, trick).stream().map(c -> (AbstractAction) new PlayCard(c)).toList();
                assertEquals(d + "legal actions at step " + step, new HashSet<>(expected), new HashSet<>(actions));
                assertEquals("no duplicate actions", expected.size(), actions.size());

                PlayCard chosen = (PlayCard) actions.get(rnd.nextInt(actions.size()));
                trick.add(chosen.card);
                fm.next(state, chosen);
                steps++;
                if (trick.size() == 4) {
                    leader = (leader + winningIndex(trick, trumps)) % 4;
                    expectedTricks[leader]++;
                    trick.clear();
                    if (step < 51)
                        assertArrayEquals(d + "tricks taken", expectedTricks, state.tricksTaken);
                }
                assertAllCardsPresent(state);
            }

            // the side with more tricks scores its tricks - 6; 13 is odd, so there is always such a side
            int team0 = expectedTricks[0] + expectedTricks[2], team1 = expectedTricks[1] + expectedTricks[3];
            assertEquals(13, team0 + team1);
            expectedPoints[team0 > team1 ? 0 : 1] += Math.max(team0, team1) - 6;
            assertArrayEquals(d + "points after the deal", expectedPoints, state.teamPoints);
        }

        // the game ends after the last deal: its 13 tricks of 4 cards each are on the discard pile
        assertFalse("game did not end after " + nDeals + " deals", state.isNotTerminal());
        assertEquals(52 * nDeals, steps);
        // the game ends in the last deal's round (deals are numbered from 0)
        assertEquals("round counter", nDeals - 1, state.getRoundCounter());
        assertEquals(52, state.getDiscardPile().getSize());
        assertEquals(0, state.getCurrentTrick().getSize());
        for (int pl = 0; pl < 4; pl++)
            assertEquals(0, state.getPlayerHand(pl).getSize());
        assertArrayEquals(expectedTricks, state.tricksTaken);

        int difference = expectedPoints[0] - expectedPoints[1];
        for (int pl = 0; pl < 4; pl++) {
            int lead = pl % 2 == 0 ? difference : -difference;
            GameResult r = lead > 0 ? WIN_GAME : lead < 0 ? LOSE_GAME : DRAW_GAME;
            assertEquals("result of player " + pl, r, state.getPlayerResults()[pl]);
            assertEquals(expectedPoints[pl % 2], state.getGameScore(pl), 0.0);
        }
    }

    @Test
    public void randomGamesFollowTheRulesToTheEndOfTheDeal() {
        for (long seed = 1; seed <= 5; seed++)
            playRandomGame(seed, null);
    }

    @Test
    public void randomThreeDealGamesWithTurnedUpTrumpsCarryPointsToTheEnd() {
        for (long seed = 1; seed <= 3; seed++)
            playRandomGame(seed, params(3, WhistParameters.TrumpMode.TURN_UP, false));
    }

    @Test
    public void randomRotationGamesFollowTheTrumpCycle() {
        playRandomGame(4, params(5, WhistParameters.TrumpMode.ROTATION, false));
        playRandomGame(5, params(6, WhistParameters.TrumpMode.ROTATION, true));
    }
}
