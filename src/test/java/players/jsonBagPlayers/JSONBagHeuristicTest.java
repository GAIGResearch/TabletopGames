package players.jsonBagPlayers;

import core.AbstractGameState;
import core.CoreConstants;
import games.GameType;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JSONBagHeuristic}, the prototype-distance value function behind
 * JSON-Bag OSLA: the value of a state for player i is derived from the Jensen-Shannon distance
 * between the trajectory JSON-Bag so far and the prototype for player i.
 */
public class JSONBagHeuristicTest {

    private AbstractGameState gameState;
    private Map<String, Integer> stateBag;
    private Map<String, Integer> otherBag;

    @Before
    public void setup() {
        gameState = GameType.DotsAndBoxes.createGameInstance(2, 42L).getGameState();
        stateBag = Tokenizer.tokenize(gameState);
        otherBag = new HashMap<>();
        otherBag.put("some.other.token", 1);
    }

    private JSONBagHeuristic heuristicWith(Map<String, Integer> playerZeroPrototype,
                                           Map<String, Integer> playerOnePrototype) {
        return new JSONBagHeuristic(Arrays.asList(playerZeroPrototype, playerOnePrototype));
    }

    @Test
    public void valueIsHigherForThePrototypeMatchingTheCurrentState() {
        JSONBagHeuristic heuristic = heuristicWith(stateBag, otherBag);

        assertEquals(1.0, heuristic.evaluateState(gameState, 0), 1e-9);
        assertEquals(0.0, heuristic.evaluateState(gameState, 1), 1e-9);
    }

    @Test
    public void valuesAreNormalisedAcrossPlayers() {
        JSONBagHeuristic heuristic = heuristicWith(stateBag, otherBag);
        double zero = heuristic.evaluateState(gameState, 0);
        double one = heuristic.evaluateState(gameState, 1);
        assertEquals(1.0, zero + one, 1e-9);
    }

    @Test
    public void winningPlayerScoresOne() {
        JSONBagHeuristic heuristic = heuristicWith(stateBag, otherBag);
        gameState.setPlayerResult(CoreConstants.GameResult.WIN_GAME, 1);
        assertEquals(1.0, heuristic.evaluateState(gameState, 1), 1e-9);
    }

    @Test
    public void losingPlayerScoresZero() {
        JSONBagHeuristic heuristic = heuristicWith(stateBag, otherBag);
        gameState.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, 0);
        assertEquals(0.0, heuristic.evaluateState(gameState, 0), 1e-9);
    }

    @Test(expected = RuntimeException.class)
    public void throwsWhenPrototypeCountDoesNotMatchPlayerCount() {
        JSONBagHeuristic heuristic = new JSONBagHeuristic(Collections.singletonList(stateBag));
        heuristic.evaluateState(gameState, 0);
    }

    @Test
    public void resetCurrentBagStartsFromTheGivenState() {
        JSONBagHeuristic heuristic = heuristicWith(stateBag, otherBag);
        heuristic.currentJSONBag.put("stale.token", 5);

        heuristic.resetCurrentBag(gameState);

        assertEquals(stateBag, heuristic.currentJSONBag);
    }

    @Test
    public void updateJSONBagAccumulatesTheTrajectory() {
        JSONBagHeuristic heuristic = heuristicWith(stateBag, otherBag);

        heuristic.resetCurrentBag(gameState);
        heuristic.updateJSONBag(gameState);

        stateBag.forEach((token, count) ->
                assertEquals("token " + token, 2 * count, heuristic.currentJSONBag.get(token).intValue()));
    }

    @Test
    public void filterSetRestrictsWhichTokensAreRecorded() {
        String keptToken = stateBag.keySet().iterator().next();
        JSONBagHeuristic heuristic = new JSONBagHeuristic(Arrays.asList(stateBag, otherBag),
                Collections.singletonList(keptToken));

        heuristic.resetCurrentBag(gameState);

        assertEquals(Collections.singleton(keptToken), heuristic.currentJSONBag.keySet());
    }

    @Test
    public void copyIsIndependentOfTheOriginalBag() {
        JSONBagHeuristic heuristic = heuristicWith(stateBag, otherBag);
        heuristic.resetCurrentBag(gameState);

        JSONBagHeuristic copy = heuristic.copy();
        heuristic.updateJSONBag(gameState);

        assertEquals(stateBag, copy.currentJSONBag);
        assertNotEquals(heuristic.currentJSONBag, copy.currentJSONBag);
    }

    @Test
    public void minValueIsZero() {
        assertEquals(0.0, heuristicWith(stateBag, otherBag).minValue(), 0.0);
    }
}
