package players.heuristics;

import core.CoreConstants;
import core.components.Counter;
import games.sushigo.SGForwardModel;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Behavioural tests for the Sushi Go! state heuristic.
 *
 * <p>The tests modify the public game-state components directly so that each
 * scoring feature can be tested in isolation. This is preferable to reaching
 * a particular card combination through random play: random simulation would
 * make the expected comparison less precise and would test the forward model
 * as well as the heuristic.</p>
 */
public class SushiGoHeuristicTest {

    private SushiGoHeuristic heuristic;
    private SGGameState state;

    @Before
    public void setUp() {
        heuristic = new SushiGoHeuristic();
        state = new SGGameState(new SGParameters(), 2);
        new SGForwardModel().setup(state);
    }

    @Test
    public void terminalWinLossAndDrawArePlayerRelative() {
        state.setGameStatus(CoreConstants.GameResult.GAME_END);

        state.setPlayerResult(CoreConstants.GameResult.WIN_GAME, 0);
        state.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, 1);
        assertEquals(1.0, heuristic.evaluateState(state, 0), 0.0);
        assertEquals(-1.0, heuristic.evaluateState(state, 1), 0.0);

        state.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, 0);
        state.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, 1);
        assertEquals(0.0, heuristic.evaluateState(state, 0), 0.0);
        assertEquals(0.0, heuristic.evaluateState(state, 1), 0.0);
    }

    @Test
    public void playerAheadInScoreReceivesHigherValue() {
        state.getPlayerScore()[0].setValue(20);
        state.getPlayerScore()[1].setValue(10);
        double ahead = heuristic.evaluateState(state, 0);
        double behind = heuristic.evaluateState(state, 1);
        assertTrue(ahead > behind);
    }

    @Test
    public void incompleteTempuraSetIsRewarded() {
        double empty = heuristic.evaluateState(state, 0);
        setPlayed(0, SGCard.SGCardType.Tempura, 1);
        double incomplete = heuristic.evaluateState(state, 0);
        assertTrue(incomplete > empty);
    }

    @Test
    public void incompleteSashimiSetIsRewarded() {
        double empty = heuristic.evaluateState(state, 0);
        setPlayed(0, SGCard.SGCardType.Sashimi, 1);
        double incomplete = heuristic.evaluateState(state, 0);
        assertTrue(incomplete > empty);
    }

    @Test
    public void completedSetsAreNotRewardedTwice() {
        setPlayed(0, SGCard.SGCardType.Tempura, 2);
        state.getPlayerScore()[0].setValue(5);
        double completedTempura = heuristic.evaluateState(state, 0);
        setPlayed(0, SGCard.SGCardType.Tempura, 4);
        double additionalCompletedTempura = heuristic.evaluateState(state, 0);
        assertEquals(completedTempura, additionalCompletedTempura, 1e-9);

        setPlayed(0, SGCard.SGCardType.Sashimi, 3);
        state.getPlayerScore()[0].setValue(10);
        double completedSashimi = heuristic.evaluateState(state, 0);
        setPlayed(0, SGCard.SGCardType.Sashimi, 6);
        double additionalCompletedSashimi = heuristic.evaluateState(state, 0);
        assertEquals(completedSashimi, additionalCompletedSashimi, 1e-9);
    }

    @Test
    public void dumplingMarginalValueDiminishes() {
        double zero = heuristic.evaluateState(state, 0);
        setPlayed(0, SGCard.SGCardType.Dumpling, 1);
        double one = heuristic.evaluateState(state, 0);
        setPlayed(0, SGCard.SGCardType.Dumpling, 2);
        double two = heuristic.evaluateState(state, 0);

        assertTrue(one - zero > two - one);
    }

    @Test
    public void makiAdvantageIsRewarded() {
        setPlayed(1, SGCard.SGCardType.Maki, 2);
        double behind = heuristic.evaluateState(state, 0);
        setPlayed(0, SGCard.SGCardType.Maki, 3);
        double ahead = heuristic.evaluateState(state, 0);
        assertTrue(ahead > behind);
    }

    @Test
    public void puddingHasMoreInfluenceLaterInGame() throws Exception {
        setPlayedAllGame(0, SGCard.SGCardType.Pudding, 2);
        setRound(0);
        double early = heuristic.evaluateState(state, 0);
        setRound(((SGParameters) state.getGameParameters()).nRounds - 1);
        double late = heuristic.evaluateState(state, 0);
        assertTrue(late > early);
    }

    @Test
    public void valueIsBoundedAndFinite() {
        state.getPlayerScore()[0].setValue(1000);
        state.getPlayerScore()[1].setValue(0);
        setPlayed(0, SGCard.SGCardType.Sashimi, 100);
        setPlayed(0, SGCard.SGCardType.Maki, 100);
        setPlayedAllGame(0, SGCard.SGCardType.Pudding, 100);

        double value = heuristic.evaluateState(state, 0);
        assertTrue(value >= -1.0 && value <= 1.0);
        assertTrue(Double.isFinite(value));
    }

    @Test
    public void ordinaryStateDoesNotProduceNaNOrInfinity() {
        double value = heuristic.evaluateState(state, 0);
        assertTrue(Double.isFinite(value));
    }

    private void setPlayed(int player, SGCard.SGCardType type, int amount) {
        Map<SGCard.SGCardType, Counter>[] cards = state.getPlayedCardTypes();
        cards[player].get(type).setValue(amount);
    }

    private void setPlayedAllGame(int player, SGCard.SGCardType type, int amount) {
        Map<SGCard.SGCardType, Counter>[] cards = state.getPlayedCardTypesAllGame();
        cards[player].get(type).setValue(amount);
    }

    private void setRound(int round) throws Exception {
        Field roundCounter = state.getClass().getSuperclass()
                .getDeclaredField("roundCounter");
        roundCounter.setAccessible(true);
        roundCounter.setInt(state, round);
    }
}
