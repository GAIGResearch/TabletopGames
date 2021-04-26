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

public class DiceMonasteryLinearHeuristic003 extends TunableParameters implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;

        if (!gs.isNotTerminal()) {
            // Game Over
            return gs.getGameScore(playerId) / 100.0;
        }

        double retValue = 0.2350;

        Map<DiceMonasteryConstants.Resource, Integer> allStores = state.getStores(playerId, r -> true);
        List<Monk> allMonks = state.monksIn(null, playerId);
        retValue += allStores.getOrDefault(BEER, 0) * 0.0020;
        retValue += allStores.getOrDefault(PROTO_BEER_2, 0) * 0.0054;
        retValue += allStores.getOrDefault(MEAD, 0) * 0.0035;
        retValue += allStores.getOrDefault(PROTO_MEAD_2, 0) * 0.0034;
        retValue += allStores.getOrDefault(WAX, 0) * 0.0019;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 1).count() * -0.0036;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 2).count() * -0.0006;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 5).count() * 0.0083;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 6).count() * 0.0260;
        retValue += allMonks.size() * -0.0254;
        retValue += state.monksIn(GRAVEYARD, playerId).size() * 0.0100;
        retValue += state.getVictoryPoints(playerId) * 0.0085;
        return retValue;

    }

    @Override
    protected DiceMonasteryLinearHeuristic003 _copy() {
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof DiceMonasteryLinearHeuristic003;
    }

    @Override
    public DiceMonasteryLinearHeuristic003 instantiate() {
        return this;
    }

    @Override
    public void _reset() {

    }
}
