package games.golfsix.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.golfsix.GolfSixGameState;
import games.golfsix.GolfSixUtils;

/**
 * Before play starts, turn a face-down card of the current player's grid face-up.
 */
public class TurnUp extends AbstractAction {

    public final int position;

    public TurnUp(int position) {
        this.position = position;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        GolfSixGameState state = (GolfSixGameState) gs;
        PartialObservableDeck<FrenchCard> grid = state.getGrid(state.getCurrentPlayer());
        grid.setVisibilityOfComponent(position, GolfSixUtils.visibleToAll(state.getNPlayers()));
        return true;
    }

    @Override
    public TurnUp copy() {
        return this;  // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TurnUp that && position == that.position;
    }

    @Override
    public int hashCode() {
        return position + 582031;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Turn up position " + position;
    }
}
