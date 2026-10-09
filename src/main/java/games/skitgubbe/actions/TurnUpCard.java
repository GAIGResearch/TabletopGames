package games.skitgubbe.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.skitgubbe.SkitgubbeGameState;

/**
 * Phase one: instead of a card from hand, the current player turns up the top card of the draw deck and plays it to
 * the trick.
 */
public class TurnUpCard extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        SkitgubbeGameState state = (SkitgubbeGameState) gs;
        state.getTrick().add(state.getDrawDeck().draw());
        return true;
    }

    @Override
    public TurnUpCard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TurnUpCard;
    }

    @Override
    public int hashCode() {
        return 518831;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Turn up the top card of the draw deck";
    }
}
