package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.components.Monk;

import java.util.List;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;
import static java.util.stream.Collectors.toList;

public class ChooseMonk extends AbstractAction {

    public final int piety;
    public final ActionArea destination;

    public ChooseMonk(int piety, ActionArea destination) {
        this.piety = piety;
        this.destination = destination;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        List<Monk> matchingMonks = availableMonks(state);
        if (!matchingMonks.isEmpty()) {
            state.moveMonk(matchingMonks.get(0).getComponentID(), ActionArea.DORMITORY, destination);
        } else {
            throw new AssertionError(String.format("No monk found in Dormitory for player %d with piety of %d", state.getCurrentPlayer(), piety));
        }
        return true;
    }

    private List<Monk> availableMonks(DiceMonasteryGameState state) {
        return state.monksIn(null, state.getCurrentPlayer()).stream()
                .filter(m -> m.getPiety() == piety
                        && state.getMonkLocation(m.getComponentID()) == ActionArea.DORMITORY)
                .collect(toList());
    }

    @Override
    public AbstractAction copy() {
        // no mutable state
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChooseMonk) {
            ChooseMonk other = (ChooseMonk) obj;
            return piety == other.piety && destination == other.destination;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 101 + piety * 1487 + destination.ordinal() * -6373;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Move piety-%d monk to %s", piety, destination);
    }

}
