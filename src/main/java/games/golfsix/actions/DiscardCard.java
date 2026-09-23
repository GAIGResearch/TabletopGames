package games.golfsix.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.golfsix.GolfSixGameState;

/**
 * Put a card drawn from the draw deck face-up on top of the discard pile.
 */
public class DiscardCard extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        GolfSixGameState state = (GolfSixGameState) gs;
        state.getDiscardPile().add(state.getDrawnCardDeck().draw());
        return true;
    }

    @Override
    public DiscardCard copy() {
        return this;  // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DiscardCard;
    }

    @Override
    public int hashCode() {
        return 582061;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Discard drawn card";
    }
}
