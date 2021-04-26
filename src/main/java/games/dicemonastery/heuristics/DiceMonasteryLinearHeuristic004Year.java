package games.dicemonastery.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;
import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.DiceMonasteryTurnOrder;
import games.dicemonastery.Monk;

import java.util.List;
import java.util.Map;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;

public class DiceMonasteryLinearHeuristic004Year extends TunableParameters implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;

        if (!gs.isNotTerminal()) {
            // Game Over
            return gs.getGameScore(playerId) / 100.0;
        }

        double retValue = 0.0;
        Map<DiceMonasteryConstants.Resource, Integer> allStores = state.getStores(playerId, r -> true);
        List<Monk> allMonks = state.monksIn(null, playerId);

        int year = ((DiceMonasteryTurnOrder) state.getTurnOrder()).getYear();
        switch (year) {
            case 1:
                retValue = -0.0123;
                retValue += state.monksIn(CHAPEL, playerId).size() * 0.0050;
                retValue += state.monksIn(GATEHOUSE, playerId).stream().mapToDouble(Monk::getPiety).sum() * 0.0004;
                retValue += allMonks.stream().mapToInt(Monk::getPiety).sum() * 0.0063;
                retValue += allStores.getOrDefault(PROTO_MEAD_2, 0) * 0.0044;
                retValue += allStores.getOrDefault(BREAD, 0) * 0.0009;
                retValue += allStores.getOrDefault(WAX, 0) * 0.0061;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 6).count() * 0.0200;
                retValue += state.monksIn(RETIRED, playerId).size() * 0.0084;
                retValue += state.monksIn(PILGRIMAGE, playerId).size() * 0.0017;
                retValue += state.getVictoryPoints(playerId) * 0.0104;
                retValue += allMonks.size() * -0.0040;
                break;
            case 2:
                retValue = 0.1412;
                retValue += state.monksIn(CHAPEL, playerId).size() * 0.0008;
                retValue += allMonks.stream().mapToInt(Monk::getPiety).sum() * 0.0010;
                retValue += allStores.getOrDefault(WAX, 0) * 0.0054;
                retValue += allStores.getOrDefault(BREAD, 0) * 0.0054;
                retValue += state.monksIn(GRAVEYARD, playerId).size() * 0.0424;
                retValue += state.getVictoryPoints(playerId) * 0.0065;
                break;
            case 3:
                retValue = 0.2582;
                retValue += allStores.getOrDefault(BEER, 0) * 0.0013;
                retValue += allStores.getOrDefault(PROTO_BEER_2, 0) * 0.0076;
                retValue += allStores.getOrDefault(WAX, 0) * 0.0031;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 1).count() * -0.0165;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 2).count() * -0.0091;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 5).count() * 0.0041;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 6).count() * 0.0134;
                retValue += allMonks.size() * -0.0150;
                retValue += state.monksIn(GRAVEYARD, playerId).size() * 0.0211;
                retValue += state.getVictoryPoints(playerId) * 0.0080;
        }

        return retValue;
    }

    @Override
    protected DiceMonasteryLinearHeuristic004Year _copy() {
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof DiceMonasteryLinearHeuristic004Year;
    }

    @Override
    public DiceMonasteryLinearHeuristic004Year instantiate() {
        return this;
    }

    @Override
    public void _reset() {

    }
}
