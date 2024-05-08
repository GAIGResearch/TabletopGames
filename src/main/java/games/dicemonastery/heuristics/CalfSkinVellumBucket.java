package games.dicemonastery.heuristics;

import core.AbstractGameState;
import games.dicemonastery.DiceMonasteryGameState;

import java.util.function.Function;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.Resource.CALF_SKIN;
import static games.dicemonastery.DiceMonasteryConstants.Resource.VELLUM;

public class CalfSkinVellumBucket implements Function<AbstractGameState, String> {
    @Override
    public String apply(AbstractGameState gameState) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
        int player = state.getCurrentPlayer();
        return String.format("CS%d-V%d-%s-%d",
                state.getResource(player, CALF_SKIN, STOREROOM),
                state.getResource(player, VELLUM, STOREROOM),
                state.getSeason(),
                state.getYear());
    }
}
