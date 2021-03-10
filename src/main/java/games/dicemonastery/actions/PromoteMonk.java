package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.DiceMonasteryTurnOrder;
import games.dicemonastery.Monk;

import java.util.Optional;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;

public class PromoteMonk extends AbstractAction {

    public final int pietyLevelToPromote;
    public final ActionArea location;
    public final boolean areaReward;

    public PromoteMonk(int piety, ActionArea where, boolean reward) {
        pietyLevelToPromote = piety;
        location = where;
        areaReward = reward;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        // we use all available AP, and choose one of the monks to increase in piety
        state.useAP(state.getAPLeft());

        if (areaReward) {
            DiceMonasteryTurnOrder dto = (DiceMonasteryTurnOrder) state.getTurnOrder();
            dto.setRewardTaken(state.getCurrentPlayer());
        }
        Optional<Monk> optMonk = state.monksIn(location, state.getCurrentPlayer()).stream()
                .filter(m -> m.getPiety() == pietyLevelToPromote)
                .findFirst();
        if (optMonk.isPresent()) {
            optMonk.get().promote(state);
        } else {
            throw new AssertionError(String.format("No monk with piety level %d for player %d in %s", pietyLevelToPromote, state.getCurrentPlayer(), location));
        }
        return true;
    }

    @Override
    public PromoteMonk copy() {
        // no mutable state
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PromoteMonk) {
            PromoteMonk other = (PromoteMonk) obj;
            return pietyLevelToPromote == other.pietyLevelToPromote;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return pietyLevelToPromote;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Promote Monk of piety " + pietyLevelToPromote;
    }
}
