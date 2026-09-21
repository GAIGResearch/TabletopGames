package games.spades;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.spades.actions.Bid;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Whole random games of Spades checked against a test-side oracle for who takes each trick. The oracle records who
 * played each card from getCurrentPlayer() before the action, and works out the winner from the rules itself
 * (highest spade if any spade was played, else highest card of the suit led; Ace = 14). It never reads the
 * trick's leader or playerOf, so it catches a Trick led by the wrong player.
 */
public class SpadesTrickWinnerTest {

    private static final int N_PLAYERS = 4;
    private static final int STEP_CAP = 30 * (4 + 52) + 100; // maxRounds 30; 4 bids + 52 cards per round

    @Test
    public void trickWinnersFollowTheRulesInRandomGames() {
        for (long seed = 1; seed <= 5; seed++) {
            playRandomGame(seed);
        }
    }

    private void playRandomGame(long seed) {
        SpadesParameters params = new SpadesParameters();
        params.setRandomSeed(seed);
        SpadesGameState state = new SpadesGameState(params, N_PLAYERS);
        SpadesForwardModel fm = new SpadesForwardModel();
        fm.setup(state);
        Random rnd = new Random(seed);

        List<Integer> players = new ArrayList<>();
        List<FrenchCard> cards = new ArrayList<>();
        int round = -1;
        boolean firstPlayOfRound = false;
        int tricksChecked = 0, roundsSeen = 0;

        for (int step = 0; step < STEP_CAP && state.isNotTerminal(); step++) {
            String where = "seed " + seed + ", round " + state.getRoundCounter() + ", step " + step;
            if (state.getRoundCounter() != round) {
                // new round: the round's first player (rotating clockwise, round r starts with player r % 4) bids first
                round = state.getRoundCounter();
                roundsSeen++;
                firstPlayOfRound = true;
                assertEquals(where, SpadesGameState.Phase.BIDDING, state.getGamePhase());
                assertEquals(where + ": first bidder", round % N_PLAYERS, state.getCurrentPlayer());
            }
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse(where + ": no actions", actions.isEmpty());
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));

            if (action instanceof Bid) {
                // bidding: played randomly, no oracle
                fm.next(state, action);
                continue;
            }
            assertTrue(where + ": unexpected action " + action, action instanceof PlayCard);
            int player = state.getCurrentPlayer();
            if (firstPlayOfRound) {
                // the first player of the round, who bid first, also leads the first trick
                assertEquals(where + ": first leader of the round", round % N_PLAYERS, player);
                firstPlayOfRound = false;
            }
            players.add(player);
            cards.add(((PlayCard) action).card);
            int[] before = tricksTaken(state);

            fm.next(state, action);

            if (cards.size() < N_PLAYERS) continue;

            int winner = players.get(winningIndex(cards));
            boolean roundEnded = state.getRoundCounter() != round || !state.isNotTerminal();
            if (state.isNotTerminal() && roundEnded) {
                // counters are reset for the new round, so the last trick's winner cannot be seen in them
                assertArrayEquals(where + ": counters reset for the new round", new int[N_PLAYERS], tricksTaken(state));
            } else {
                int[] after = tricksTaken(state);
                for (int p = 0; p < N_PLAYERS; p++) {
                    int expected = before[p] + (p == winner ? 1 : 0);
                    assertEquals(where + ": tricks of player " + p + " after trick " + cards + " played by " + players
                            + " (winner " + winner + ")", expected, after[p]);
                }
                if (!roundEnded) {
                    assertEquals(where + ": trick winner leads next", winner, state.getCurrentPlayer());
                }
                tricksChecked++;
            }
            players.clear();
            cards.clear();
        }
        assertFalse("seed " + seed + ": game did not end within " + STEP_CAP + " steps", state.isNotTerminal());
        // several rounds, so the round-start leader moves away from player 0 (where a leader-0 bug hides)
        assertTrue("seed " + seed + ": only " + roundsSeen + " rounds", roundsSeen >= 3);
        assertTrue("seed " + seed + ": only " + tricksChecked + " tricks checked", tricksChecked >= 24);
    }

    /** Index (in play order) of the winning card: the highest spade if any, else the highest of the suit led. */
    private static int winningIndex(List<FrenchCard> cards) {
        boolean anySpade = cards.stream().anyMatch(c -> c.suite == FrenchCard.Suite.Spades);
        FrenchCard.Suite winningSuit = anySpade ? FrenchCard.Suite.Spades : cards.get(0).suite;
        int best = -1;
        for (int i = 0; i < cards.size(); i++) {
            FrenchCard c = cards.get(i);
            if (c.suite == winningSuit && (best < 0 || c.number > cards.get(best).number)) best = i;
        }
        return best;
    }

    private static int[] tricksTaken(SpadesGameState state) {
        int[] t = new int[N_PLAYERS];
        for (int p = 0; p < N_PLAYERS; p++) t[p] = state.getTricksTaken(p);
        return t;
    }
}
