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

public class DiceMonasteryLinearHeuristic005Year extends TunableParameters implements IStateHeuristic {

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
                retValue = 0.2326;
                retValue += state.monksIn(GATEHOUSE, playerId).size() * -0.0027;
                retValue += state.monksIn(CHAPEL, playerId).size() * 0.0002;
                retValue += allStores.getOrDefault(PROTO_BEER_2, 0) * 0.0032;
                retValue += allStores.getOrDefault(PROTO_MEAD_1, 0) * -0.0004;
                retValue += allStores.getOrDefault(PROTO_MEAD_2, 0) * 0.0035;
                retValue += allStores.getOrDefault(BREAD, 0) * 0.0019;
                retValue += allStores.getOrDefault(BERRIES, 0) * 0.0006;
                retValue += allStores.getOrDefault(WAX, 0) * 0.0028;
                retValue += allStores.getOrDefault(SKEP, 0) * -0.0047;
                retValue += state.getResource(playerId, GRAIN, MEADOW) * 0.0023;
                retValue += state.getResource(playerId, SKEP, MEADOW) * 0.0011;
                retValue += allStores.getOrDefault(PRAYER, 0) * 0.0007;
                retValue += allStores.keySet().stream().filter(r -> r.isInk).count() * -0.0054;
                retValue += allStores.getOrDefault(PALE_BLUE_PIGMENT, 0) * -0.0008;
                retValue += allStores.getOrDefault(PALE_GREEN_PIGMENT, 0) * -0.0011;
                retValue += allStores.getOrDefault(VIVID_GREEN_PIGMENT, 0) * 0.0018;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 1).count() * -0.0014;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 3).count() * 0.0029;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 6).count() * -0.0071;
                retValue += allMonks.size() * -0.0053;
                retValue += state.monksIn(RETIRED, playerId).size() * -0.0325;
                retValue += state.monksIn(PILGRIMAGE, playerId).size() * -0.0080;
                retValue += state.getVictoryPoints(playerId) * 0.0026;
                retValue += state.getTreasures(playerId).stream().mapToInt(t -> t.vp).sum() * -0.0017;
                break;
            case 2:
                retValue = 0.1409;
                retValue += allStores.getOrDefault(PROTO_BEER_2, 0) * 0.0107;
                retValue += allStores.getOrDefault(PROTO_MEAD_2, 0) * 0.0064;
                retValue += allStores.getOrDefault(WAX, 0) * 0.0036;
                retValue += allStores.getOrDefault(HONEY, 0) * 0.0036;
                retValue += allStores.getOrDefault(SKEP, 0) * -0.0019;
                retValue += state.getResource(playerId, SKEP, MEADOW) * -0.0038;
                retValue += allStores.getOrDefault(PRAYER, 0) * -0.0028;
                retValue += allStores.keySet().stream().filter(r -> r.isInk).count() * -0.0031;
                retValue += allStores.getOrDefault(PALE_GREEN_PIGMENT, 0) * -0.0014;
                retValue += allStores.getOrDefault(PALE_BLUE_INK, 0) * -0.0028;
                retValue += allStores.getOrDefault(PALE_RED_INK, 0) * -0.0046;
                retValue += allStores.getOrDefault(PALE_GREEN_INK, 0) * -0.0058;
                retValue += allStores.getOrDefault(VIVID_RED_INK, 0) * 0.0114;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 3).count() * -0.0018;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 4).count() * -0.0015;
                retValue += allMonks.stream().filter(m -> m.getPiety() == 6).count() * 0.0040;
                retValue += state.monksIn(PILGRIMAGE, playerId).size() * 0.0007;
                retValue += allMonks.size() * -0.0004;
                retValue += state.getVictoryPoints(playerId) * 0.0048;
                break;
            case 3:
                retValue = 0.0611;
                retValue += allStores.getOrDefault(BEER, 0) * 0.0070;
                retValue += allStores.getOrDefault(PROTO_BEER_2, 0) * 0.0015;
                retValue += allStores.getOrDefault(MEAD, 0) * 0.0110;
                retValue += allStores.getOrDefault(PROTO_MEAD_2, 0) * 0.0114;
                retValue += allStores.getOrDefault(WAX, 0) * 0.0005;
                retValue += allStores.getOrDefault(BREAD, 0) * 0.0021;
                retValue += state.monksIn(PILGRIMAGE, playerId).size() * 0.0017;
                retValue += state.getVictoryPoints(playerId) * 0.0080;
        }

        return retValue;
    }

    @Override
    protected DiceMonasteryLinearHeuristic005Year _copy() {
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof DiceMonasteryLinearHeuristic005Year;
    }

    @Override
    public DiceMonasteryLinearHeuristic005Year instantiate() {
        return this;
    }

    @Override
    public void _reset() {

    }
}
