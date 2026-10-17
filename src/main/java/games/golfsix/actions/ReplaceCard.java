package games.golfsix.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.golfsix.GolfSixGameState;
import games.golfsix.GolfSixUtils;

/**
 * Put the drawn card face-up at a position in the current player's grid; the card that was there goes face-up on top of the discard pile.
 */
public class ReplaceCard extends AbstractAction {

    public final int position;

    public ReplaceCard(int position) {
        this.position = position;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        GolfSixGameState state = (GolfSixGameState) gs;
        PartialObservableDeck<FrenchCard> grid = state.getGrid(state.getCurrentPlayer());
        state.getDiscardPile().add(grid.get(position));
        grid.setComponent(position, state.getDrawnCardDeck().draw());
        grid.setVisibilityOfComponent(position, GolfSixUtils.visibleToAll(state.getNPlayers()));
        state.setDrawnFromDiscard(false);
        return true;
    }

    @Override
    public ReplaceCard copy() {
        return this;  // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReplaceCard that && position == that.position;
    }

    @Override
    public int hashCode() {
        return position + 582047;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Replace position " + position;
    }
}
