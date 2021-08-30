package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.components.Monk;

import static java.util.stream.Collectors.joining;

public class PromoteAllMonks extends AbstractAction {

    public final DiceMonasteryConstants.ActionArea location;

    public PromoteAllMonks(DiceMonasteryConstants.ActionArea where) {
        location = where;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        // we use all available AP
        state.useAP(state.getAPLeft());
        int player = state.getCurrentPlayer();

        for (Monk monk : state.monksIn(location, player)) {
            monk.promote(state);
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        // immutable
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PromoteAllMonks) {
            return ((PromoteAllMonks) obj).location == location;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return location.ordinal() + 3301;
    }

    @Override
    public String toString() {
        return "Promote all monks in " + location;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return String.format("Promote all monks in %s : %s", location,
                ((DiceMonasteryGameState) gameState).monksIn(location, gameState.getCurrentPlayer()).stream()
                        .map(m -> String.valueOf(m.getPiety())).collect(joining(", ")));
    }
}
