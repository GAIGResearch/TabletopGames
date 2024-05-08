package games.dicemonastery.heuristics;

import core.AbstractGameState;
import games.dicemonastery.DiceMonasteryGameState;

import java.util.function.Function;

public class YearSeasonBucket implements Function<AbstractGameState, String> {
    @Override
    public String apply(AbstractGameState gameState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
        return String.format("%s-%d", state.getSeason(), state.getYear());
    }
}
