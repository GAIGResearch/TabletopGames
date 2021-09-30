package games.dicemonastery.heuristics;

import core.AbstractGameState;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.DiceMonasteryTurnOrder;

import java.util.function.Function;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.Resource.CALF_SKIN;
import static games.dicemonastery.DiceMonasteryConstants.Resource.VELLUM;

public class CalfSkinMonksBucket implements Function<AbstractGameState, String> {
    @Override
    public String apply(AbstractGameState gameState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        int player = state.getCurrentPlayer();
        return String.format("CS%d-V%d-M%d-%s-%d",
                state.getResource(player, CALF_SKIN, STOREROOM),
                state.getResource(player, VELLUM, STOREROOM),
                state.monksIn(null, player).stream().filter(m -> m.getPiety() == 1).count(),
                turnOrder.getSeason(),
                turnOrder.getYear());
    }
}
