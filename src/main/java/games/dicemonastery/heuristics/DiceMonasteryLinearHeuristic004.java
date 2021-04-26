package games.dicemonastery.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;
import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.Monk;

import java.util.List;
import java.util.Map;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.GRAVEYARD;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;

public class DiceMonasteryLinearHeuristic004 extends TunableParameters implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;

        if (!gs.isNotTerminal()) {
            // Game Over
            return gs.getGameScore(playerId) / 100.0;
        }

        double retValue = 0.2447;

        Map<DiceMonasteryConstants.Resource, Integer> allStores = state.getStores(playerId, r -> true);
        List<Monk> allMonks = state.monksIn(null, playerId);
        retValue += allStores.getOrDefault(BEER, 0) * 0.0044;
        retValue += allStores.getOrDefault(PROTO_BEER_2, 0) * 0.0026;
        retValue += allStores.getOrDefault(MEAD, 0) * 0.0013;
        retValue += allStores.getOrDefault(WAX, 0) * 0.0051;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 1).count() * -0.0072;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 2).count() * -0.0023;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 5).count() * 0.0117;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 6).count() * 0.0257;
        retValue += allMonks.size() * -0.0247;
        retValue += state.monksIn(GRAVEYARD, playerId).size() * 0.0140;
        retValue += state.getVictoryPoints(playerId) * 0.0085;
        return retValue;

    }

    @Override
    protected DiceMonasteryLinearHeuristic004 _copy() {
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof DiceMonasteryLinearHeuristic004;
    }

    @Override
    public DiceMonasteryLinearHeuristic004 instantiate() {
        return this;
    }

    @Override
    public void _reset() {

    }
}
