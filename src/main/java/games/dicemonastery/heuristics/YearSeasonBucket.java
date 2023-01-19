package games.dicemonastery.heuristics;

import core.AbstractGameState;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.DiceMonasteryTurnOrder;

import java.util.function.Function;

public class YearSeasonBucket implements Function<AbstractGameState, String> {
    @Override
    public String apply(AbstractGameState gameState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        return String.format("%s-%d", turnOrder.getSeason(), turnOrder.getYear());
    }
}
