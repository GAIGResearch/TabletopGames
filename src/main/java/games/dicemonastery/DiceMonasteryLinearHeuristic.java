package games.dicemonastery;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

import java.util.List;
import java.util.Map;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;

public class DiceMonasteryLinearHeuristic implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;

        double retValue = 0.0;

        Map<DiceMonasteryConstants.Resource, Integer> allStores =state.getStores(playerId, r -> true);
        List<Monk> allMonks = state.monksIn(null, playerId);
        retValue += state.monksIn(MEADOW, playerId).size() * 0.007;
        retValue += state.monksIn(KITCHEN, playerId).size() * 0.010;
        retValue += state.monksIn(WORKSHOP, playerId).size() * -0.003;
        retValue += state.monksIn(GATEHOUSE, playerId).size() * -0.001;
        retValue += state.monksIn(LIBRARY, playerId).size() * -0.026;
        retValue += state.monksIn(CHAPEL, playerId).size() * 0.005;
        retValue += state.monksIn(MEADOW, playerId).stream().mapToDouble(Monk::getPiety).sum() * -0.002;
        retValue += state.monksIn(KITCHEN, playerId).stream().mapToDouble(Monk::getPiety).sum() * -0.003;
        retValue += state.monksIn(GATEHOUSE, playerId).stream().mapToDouble(Monk::getPiety).sum() * 0.002;
        retValue += state.monksIn(LIBRARY, playerId).stream().mapToDouble(Monk::getPiety).sum() * 0.005;
        retValue += allStores.getOrDefault(SHILLINGS, 0) * 0.0002;
        retValue += allStores.getOrDefault(BEER, 0) * 0.018;
        retValue += allStores.getOrDefault(PROTO_BEER_1, 0) * 0.008;
        retValue += allStores.getOrDefault(PROTO_BEER_2, 0) * 0.022;
        retValue += allStores.getOrDefault(MEAD, 0) * 0.021;
        retValue += allStores.getOrDefault(PROTO_MEAD_1, 0) * 0.021;
        retValue += allStores.getOrDefault(PROTO_MEAD_2, 0) * 0.029;
        retValue += allStores.getOrDefault(GRAIN, 0) * 0.003;
        retValue += allStores.getOrDefault(BREAD, 0) * 0.006;
        retValue += allStores.getOrDefault(HONEY, 0) * 0.016;
        retValue += allStores.getOrDefault(BERRIES, 0) * 0.004;
        retValue += allStores.getOrDefault(CALF_SKIN, 0) * -0.012;
        retValue += allStores.getOrDefault(VELLUM, 0) * 0.011;
        retValue += state.getResource(playerId, GRAIN, MEADOW) * -0.006;
        retValue += state.getResource(playerId, SKEP, MEADOW) * -0.002;
        retValue += allStores.getOrDefault(PRAYER, 0) * 0.0006;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 1).count() * -0.011;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 2).count() * -0.011;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 3).count() * -0.010;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 4).count() * -0.007;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 5).count() * 0.011;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 6).count() * 0.038;
        retValue += state.monksIn(RETIRED, playerId).size() * 0.031;
        retValue += state.monksIn(PILGRIMAGE, playerId).size() * 0.029;
        retValue += state.monksIn(GRAVEYARD, playerId).size() * 0.044;
        retValue += state.getVictoryPoints(playerId) * 0.012;
        return retValue;

    }
}
