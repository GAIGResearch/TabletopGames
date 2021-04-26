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

public class DiceMonasteryLinearHeuristic003Year extends TunableParameters implements IStateHeuristic {

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
                retValue = -0.0004;
                retValue += state.monksIn(CHAPEL, playerId).size() * 0.0047;
                retValue += state.monksIn(GATEHOUSE, playerId).stream().mapToDouble(Monk::getPiety).sum() * 0.0006;
                retValue += allMonks.stream().mapToInt(Monk::getPiety).sum() * 0.0042;
                retValue += allStores.getOrDefault(PROTO_MEAD_2, 0) * 0.0021;
                retValue += allStores.getOrDefault(GRAIN, 0) * -0.0005;
                retValue += allStores.getOrDefault(BREAD, 0) * 0.0011;
                retValue += allStores.getOrDefault(HONEY, 0) * -0.0001;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 5).count() * 0.0083;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 6).count() * 0.0276;
                retValue += state.monksIn(RETIRED, playerId).size() * 0.0109;
                retValue += state.getVictoryPoints(playerId) * 0.0100;
                break;
            case 2:
                retValue = 0.1355;
                retValue += state.monksIn(CHAPEL, playerId).size() * 0.0013;
                retValue += allStores.entrySet().stream().filter(entry -> entry.getKey().isPigment).count() * 0.0018;
                retValue += allStores.getOrDefault(PROTO_MEAD_2, 0) * 0.0011;
                retValue += allStores.getOrDefault(WAX, 0) * 0.0016;
                retValue += allStores.getOrDefault(GRAIN, 0) * 0.0005;
                retValue += allStores.getOrDefault(BREAD, 0) * 0.0049;
                retValue += allStores.getOrDefault(HONEY, 0) * 0.0031;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 5).count() * 0.0013;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 6).count() * 0.0131;
                retValue += state.monksIn(GRAVEYARD, playerId).size() * 0.0269;
                retValue += state.getVictoryPoints(playerId) * 0.0067;
                break;
            case 3:
                retValue = 0.2412;
                retValue += allStores.getOrDefault(BEER, 0) * 0.0044;
                retValue += allStores.getOrDefault(PROTO_BEER_2, 0) * 0.0145;
                retValue += allStores.getOrDefault(MEAD, 0) * 0.0071;
                retValue += allStores.getOrDefault(PROTO_MEAD_2, 0) * 0.0110;
                retValue += allStores.getOrDefault(WAX, 0) * 0.0007;
                retValue += allStores.getOrDefault(GRAIN, 0) * 0.0003;
                retValue += allStores.getOrDefault(BREAD, 0) * 0.0031;
                retValue += allStores.getOrDefault(HONEY, 0) * 0.0091;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 1).count() * -0.0055;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 2).count() * -0.0057;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 5).count() * 0.0050;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 6).count() * 0.0198;
                retValue += allMonks.size() * -0.0285;
                retValue += state.monksIn(GRAVEYARD, playerId).size() * 0.0089;
                retValue += state.getVictoryPoints(playerId) * 0.0092;
        }

        return retValue;
    }

    @Override
    protected DiceMonasteryLinearHeuristic003Year _copy() {
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof DiceMonasteryLinearHeuristic003Year;
    }

    @Override
    public DiceMonasteryLinearHeuristic003Year instantiate() {
        return this;
    }

    @Override
    public void _reset() {

    }
}
