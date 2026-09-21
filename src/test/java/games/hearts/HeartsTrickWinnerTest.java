package games.hearts;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Whole random games of Hearts (3, 4 and 5 players) checked against a test-side oracle for who takes each trick.
 * The oracle records who played each card from getCurrentPlayer() before the action and works out the winner from
 * the rules itself (highest card of the suit led, no trumps; Ace = 14). It reads nothing of the trick from the state,
 * so it survives the migration onto games.tricktaking.Trick - only {@link #cardPlayed} needs to change then.
 */
public class HeartsTrickWinnerTest {

    private static final int STEP_CAP = 5000;

    @Test
    public void trickWinnersFollowTheRulesInRandomGamesWith3Players() {
        for (long seed = 1; seed <= 3; seed++) playRandomGame(3, seed);
    }

    @Test
    public void trickWinnersFollowTheRulesInRandomGamesWith4Players() {
        for (long seed = 1; seed <= 3; seed++) playRandomGame(4, seed);
    }

    @Test
    public void trickWinnersFollowTheRulesInRandomGamesWith5Players() {
        for (long seed = 1; seed <= 3; seed++) playRandomGame(5, seed);
    }

    /** The card a trick-play action plays, or null for any other action (passing). The only place tied to the action class. */
    private static FrenchCard cardPlayed(AbstractAction action) {
        return action instanceof PlayCard ? ((PlayCard) action).card : null;
    }

    private void playRandomGame(int nPlayers, long seed) {
        HeartsParameters params = new HeartsParameters();
        params.setRandomSeed(seed);
        HeartsGameState state = new HeartsGameState(params, nPlayers);
        HeartsForwardModel fm = new HeartsForwardModel();
        fm.setup(state);
        Random rnd = new Random(seed);
        // 2C is in the pack for 3, 4 and 5 players (only 2D and 2S are removed)
        FrenchCard startingCard = new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 2);

        List<Integer> players = new ArrayList<>();
        List<FrenchCard> cards = new ArrayList<>();
        int round = -1;
        boolean firstPlayOfRound = false;
        int tricksChecked = 0, roundsSeen = 0;

        for (int step = 0; step < STEP_CAP && state.isNotTerminal(); step++) {
            String where = nPlayers + " players, seed " + seed + ", round " + state.getRoundCounter() + ", step " + step;
            if (state.getRoundCounter() != round) {
                round = state.getRoundCounter();
                roundsSeen++;
                firstPlayOfRound = true;
            }
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse(where + ": no actions", actions.isEmpty());
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            FrenchCard card = cardPlayed(action);
            if (card == null) {
                // passing: played randomly, no oracle
                fm.next(state, action);
                continue;
            }
            int player = state.getCurrentPlayer();
            if (firstPlayOfRound) {
                // after passing, the holder of the starting card leads, and must lead it
                assertEquals(where + ": first card of the round", startingCard, card);
                firstPlayOfRound = false;
            }
            players.add(player);
            cards.add(card);
            int[] before = state.playerTricksTaken.clone();

            fm.next(state, action);

            if (cards.size() < nPlayers) continue;

            int winner = players.get(winningIndex(cards));
            boolean roundEnded = state.getRoundCounter() != round || !state.isNotTerminal();
            if (state.isNotTerminal() && roundEnded) {
                // playerTricksTaken is reset for the new round, so the last trick's winner cannot be seen in it
                assertArrayEquals(where + ": counters reset for the new round", new int[nPlayers], state.playerTricksTaken);
            } else {
                for (int p = 0; p < nPlayers; p++) {
                    int expected = before[p] + (p == winner ? 1 : 0);
                    assertEquals(where + ": tricks of player " + p + " after trick " + cards + " played by " + players
                            + " (winner " + winner + ")", expected, state.playerTricksTaken[p]);
                }
                if (!roundEnded) {
                    assertEquals(where + ": trick winner leads next", winner, state.getCurrentPlayer());
                }
                tricksChecked++;
            }
            players.clear();
            cards.clear();
        }
        assertFalse(nPlayers + " players, seed " + seed + ": game did not end within " + STEP_CAP + " steps",
                state.isNotTerminal());
        assertTrue(nPlayers + " players, seed " + seed + ": only " + roundsSeen + " rounds", roundsSeen >= 2);
        assertTrue(nPlayers + " players, seed " + seed + ": only " + tricksChecked + " tricks checked", tricksChecked >= 10);
    }

    /** Index (in play order) of the winning card: the highest card of the suit led. */
    private static int winningIndex(List<FrenchCard> cards) {
        FrenchCard.Suite led = cards.get(0).suite;
        int best = 0;
        for (int i = 1; i < cards.size(); i++) {
            FrenchCard c = cards.get(i);
            if (c.suite == led && c.number > cards.get(best).number) best = i;
        }
        return best;
    }
}
