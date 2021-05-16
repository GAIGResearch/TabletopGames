package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.Monk;

import java.util.Objects;
import java.util.Optional;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;

public class PromoteMonk extends AbstractAction {

    public final int pietyLevelToPromote;
    public final ActionArea location;

    public PromoteMonk(int piety, ActionArea where) {
        pietyLevelToPromote = piety;
        location = where;
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
            return pietyLevelToPromote == other.pietyLevelToPromote && location == other.location;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pietyLevelToPromote, location) + 97;
    }
    // +97 is to avoid hashcode clashes with ChooseMonk

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Promote Monk of piety %d in %s", pietyLevelToPromote, location);
    }
}
