package games.dicemonastery.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;
import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.Monk;

import java.util.List;
import java.util.Map;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;

public class DiceMonasteryLinearHeuristic005_45 extends TunableParameters implements IStateHeuristic  {

    // From data gathered using 10s with Lin004 and a rollout of 100
    // AIC model with all data for 50+ visits
    // Regularisation at lambda = 1.2e-4

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;

        if (!gs.isNotTerminal()) {
            // Game Over
            return gs.getGameScore(playerId) / 100.0;
        }

        double retValue = 0.2027;

        Map<DiceMonasteryConstants.Resource, Integer> allStores =state.getStores(playerId, r -> true);
        List<Monk> allMonks = state.monksIn(null, playerId);
        retValue += state.monksIn(KITCHEN, playerId).size() * -0.0003;
        retValue += state.monksIn(WORKSHOP, playerId).size() * -0.0001;
        retValue += state.monksIn(GATEHOUSE, playerId).size() * -0.0011;
      //  retValue += state.monksIn(LIBRARY, playerId).size() * 0.0133;
        retValue += state.monksIn(CHAPEL, playerId).size() * -0.0004;
        retValue += state.monksIn(MEADOW, playerId).stream().mapToDouble(Monk::getPiety).sum() * -0.00006;
      //  retValue += state.monksIn(KITCHEN, playerId).stream().mapToDouble(Monk::getPiety).sum() * -0.00056;
        retValue += state.monksIn(WORKSHOP, playerId).stream().mapToDouble(Monk::getPiety).sum() * -0.00049;
   //     retValue += state.monksIn(GATEHOUSE, playerId).stream().mapToDouble(Monk::getPiety).sum() * 0.00036;
        retValue += state.monksIn(LIBRARY, playerId).stream().mapToDouble(Monk::getPiety).sum() * -0.00063;
        retValue += allStores.getOrDefault(SHILLINGS, 0) * 0.0014;
        retValue += allStores.getOrDefault(BEER, 0) * 0.0103;
        retValue += allStores.getOrDefault(PROTO_BEER_1, 0) * 0.0018;
        retValue += allStores.getOrDefault(PROTO_BEER_2, 0) * 0.0069;
        retValue += allStores.getOrDefault(MEAD, 0) * 0.0083;
        retValue += allStores.getOrDefault(PROTO_MEAD_1, 0) * 0.0075;
        retValue += allStores.getOrDefault(PROTO_MEAD_2, 0) * 0.0083;
        retValue += allStores.getOrDefault(GRAIN, 0) * 0.0010;
        retValue += allStores.getOrDefault(BREAD, 0) * 0.0050;
        retValue += allStores.getOrDefault(HONEY, 0) * 0.0068;
        retValue += allStores.getOrDefault(BERRIES, 0) * 0.0007;
        retValue += allStores.getOrDefault(WAX, 0) * 0.0017;
        retValue += allStores.getOrDefault(CANDLE, 0) * -0.0001;
        retValue += allStores.getOrDefault(CALF_SKIN, 0) * -0.0110;
        retValue += allStores.getOrDefault(VELLUM, 0) * -0.0119;
        retValue += allStores.getOrDefault(SKEP, 0) * -0.0014;
        retValue += state.getResource(playerId, GRAIN, MEADOW) * -0.0027;
        retValue += state.getResource(playerId, SKEP, MEADOW) * -0.0042;
        retValue += allStores.getOrDefault(PRAYER, 0) * -0.0012;
        retValue += allStores.getOrDefault(BERRIES, 0) * 0.0007;
//        retValue += allStores.keySet().stream().filter(r -> r.isPigment).count() * 0.0004;
        retValue += allStores.keySet().stream().filter(r -> r.isInk).count() * -0.0055;
//        retValue += allStores.getOrDefault(PALE_BLUE_PIGMENT, 0) * -0.0001;
        retValue += allStores.getOrDefault(PALE_RED_PIGMENT, 0) * -0.0025;
        retValue += allStores.getOrDefault(PALE_GREEN_PIGMENT, 0) * -0.0016;
        retValue += allStores.getOrDefault(PALE_BLUE_INK, 0) * 0.0031;
        retValue += allStores.getOrDefault(PALE_RED_INK, 0) * 0.0005;
//        retValue += allStores.getOrDefault(PALE_GREEN_INK, 0) * 0.0013;
        retValue += allStores.getOrDefault(VIVID_BLUE_PIGMENT, 0) * 0.0135;
        retValue += allStores.getOrDefault(VIVID_RED_PIGMENT, 0) * -0.0035;
        retValue += allStores.getOrDefault(VIVID_GREEN_PIGMENT, 0) * -0.0033;
        retValue += allStores.getOrDefault(VIVID_BLUE_INK, 0) * 0.0219;
        retValue += allStores.getOrDefault(VIVID_RED_INK, 0) * 0.0051;
        retValue += allStores.getOrDefault(VIVID_GREEN_INK, 0) * 0.0038;
        retValue += allStores.getOrDefault(VIVID_PURPLE_PIGMENT, 0) * 0.0052;
        retValue += allStores.getOrDefault(VIVID_PURPLE_INK, 0) * 0.0305;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 1).count() * 0.0050;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 2).count() * 0.0042;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 3).count() * -0.0073;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 4).count() * -0.0047;
        retValue += allMonks.stream().filter(m -> m.getPiety() == 5).count() * -0.0004;
        retValue += allMonks.stream().mapToInt(Monk::getPiety).sum() * -0.00301;
        retValue += state.monksIn(RETIRED, playerId).size() * -0.0197;
        retValue += state.monksIn(PILGRIMAGE, playerId).size() * 0.0088;
        retValue += state.monksIn(GRAVEYARD, playerId).size() * -0.0221;
        retValue += state.getTreasures(playerId).stream().mapToInt(t -> t.vp).sum() * -0.0005;
        retValue += state.getVictoryPoints(playerId) * 0.0071;
        return retValue;

    }

    @Override
    protected DiceMonasteryLinearHeuristic005_45 _copy() {
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof DiceMonasteryLinearHeuristic005_45;
    }

    @Override
    public DiceMonasteryLinearHeuristic005_45 instantiate() {
        return this;
    }

    @Override
    public void _reset() {

    }
}
