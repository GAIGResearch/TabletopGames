package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.components.Monk;

import java.util.Optional;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;

public class PromoteMonk extends AbstractAction {

    public final int pietyLevelToPromote;
    public final ActionArea location;
    public final boolean useAllAP;

    public PromoteMonk(int piety, ActionArea where) {
        pietyLevelToPromote = piety;
        location = where;
        useAllAP = false;
    }

    public PromoteMonk(int piety, ActionArea where, boolean useAP) {
        pietyLevelToPromote = piety;
        location = where;
        useAllAP = useAP;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        //  choose one of the monks to increase in piety
        Optional<Monk> optMonk = state.monksIn(location, state.getCurrentPlayer()).stream()
                .filter(m -> m.getPiety() == pietyLevelToPromote)
                .findFirst();
        if (optMonk.isPresent()) {
            optMonk.get().promote(state);
        } else {
            throw new AssertionError(String.format("No monk with piety level %d for player %d in %s", pietyLevelToPromote, state.getCurrentPlayer(), location));
        }
        if (useAllAP)
            state.addActionPoints(-state.getAPLeft());
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
            return pietyLevelToPromote == other.pietyLevelToPromote && location == other.location && other.useAllAP == useAllAP;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // we deliberately skip the location in the hashcode
        return 97 + pietyLevelToPromote * 1487 + (useAllAP ? 1 : 0);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Promote Monk of piety %d %s", pietyLevelToPromote, useAllAP ? "(Chapel)" : "");
    }
}
