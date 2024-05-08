package games.dicemonastery;

import core.Game;
import core.components.Component;
import games.GameType;
import games.dicemonastery.DiceMonasteryConstants.ActionArea;
import games.dicemonastery.*;
import games.dicemonastery.actions.PlaceMonk;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.Arrays;
import java.util.Map;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.CHAPEL;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.DORMITORY;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;

public class vOneEightTests {

    DiceMonasteryForwardModel fm = new DiceMonasteryForwardModel();
    Game game = GameType.DiceMonastery.createGameInstance(4, new DiceMonasteryParams(3));
    RandomPlayer rnd = new RandomPlayer();

    @Test
    public void bonusTokenDistribution() {
        for (int np = 2; np <= 4; np++) {
            Game game = GameType.DiceMonastery.createGameInstance(np, new DiceMonasteryParams(3));
            DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
            DiceMonasteryParams params = (DiceMonasteryParams) state.getGameParameters();
            for (ActionArea area : ActionArea.values()) {
                int expectedTokens = area.dieMinimum == 0 ? 0 : params.BONUS_TOKENS_PER_PLAYER[np];
                assertEquals(expectedTokens, state.availableBonusTokens(area).size());
            }
        }
    }

    @Test
    public void bonusTokensRefreshedCorrectlyWithHousekeeping() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        fm.next(state, new PlaceMonk(0, CHAPEL)); // ensure we have one monk at least in the CHAPEL, so that we can stop before Housekeeping
        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, -1).size() > 0);
        // Once the Dormitory is empty, all Monks have been place, so we can calculate
        // how many Bonus Tokens should be taken as rewards
        Map<ActionArea, Integer> playersPerArea = Arrays.stream(ActionArea.values())
                .filter(a -> a.dieMinimum > 0)
                .collect(toMap(a -> a, a -> (int) state.monksIn(a, -1).stream().mapToInt(Component::getOwnerId).distinct().count()));

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.getCurrentArea() != CHAPEL);  // stop when we are on the Chapel

        // check that some bonus tokens have been removed as rewards (1 or 2)
        for (ActionArea area : ActionArea.values()) {
            if (area == CHAPEL || area.dieMinimum == 0) continue;
           System.out.printf("%s has %d players and %s tokens%n", area, playersPerArea.getOrDefault(area, 0), state.availableBonusTokens(area).size());
            int expectedTokens = 2 - playersPerArea.getOrDefault(area, 0);
            if (expectedTokens < 0) expectedTokens = 0;
            assertEquals(expectedTokens, state.availableBonusTokens(area).size());
        }

        do {
            fm.next(state, rnd._getAction(state, fm.computeAvailableActions(state)));
        } while (state.getSeason() != DiceMonasteryConstants.Season.SUMMER);  // stop when we have done HouseKeeping

        for (ActionArea area : ActionArea.values()) {
            if (area == CHAPEL || area.dieMinimum == 0) continue;
            assertEquals(2, state.availableBonusTokens(area).size());
        }
    }

    @Test
    public void gameScoreCorrectForBeerAndMead() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        assertEquals(0.0, state.getGameScore(0), 0.001);
        assertEquals(0.0, state.getGameScore(1), 0.001);
        assertEquals(0.0, state.getGameScore(2), 0.001);

        state.addResource(2, DiceMonasteryConstants.Resource.BEER, 5);
        state.addResource(1, DiceMonasteryConstants.Resource.MEAD, 2);
        state.addResource(0, DiceMonasteryConstants.Resource.BEER, 2);
        state.addResource(1, DiceMonasteryConstants.Resource.BEER, 1);

        state.addVP(2, 0);
        state.addVP(1, 2);

        assertEquals(3.0, state.getGameScore(0), 0.001);
        assertEquals(2.0, state.getGameScore(1), 0.001);
        assertEquals(3.0, state.getGameScore(2), 0.001);

    }
}
