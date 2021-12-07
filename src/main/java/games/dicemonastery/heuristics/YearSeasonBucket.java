package games.dicemonastery.heuristics;

import core.AbstractGameState;
import games.dicemonastery.DiceMonasteryTurnOrder;

import java.util.function.Function;

public class YearSeasonBucket implements Function<AbstractGameState, String> {
    @Override
    public String apply(AbstractGameState gameState) {
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) gameState.getTurnOrder();
        return String.format("%s-%d", turnOrder.getSeason(), turnOrder.getYear());
    }
}
