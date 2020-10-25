package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionGameState;

public class Laboratory extends DominionAction {
    @Override
    boolean _execute(DominionGameState state) {
        state.drawCard(state.getCurrentPlayer());
        state.drawCard(state.getCurrentPlayer());
        state.changeActions(1);
        return true;
    }

    /**
     * Create a copy of this action, with all of its variables.
     * NO REFERENCES TO COMPONENTS TO BE KEPT IN ACTIONS, PRIMITIVE TYPES ONLY.
     *
     * @return - new AbstractAction object with the same properties.
     */
    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Laboratory);
    }

    @Override
    public int hashCode() {
        return 43935;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "LABORATORY";
    }
}
