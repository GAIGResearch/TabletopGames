package games.golfsix.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.golfsix.GolfSixGameState;

import java.util.Arrays;

/**
 * Draw the top card of the draw deck, or of the discard pile.
 */
public class DrawCard extends AbstractAction {

    public final boolean fromDiscard;

    public DrawCard(boolean fromDiscard) {
        this.fromDiscard = fromDiscard;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        GolfSixGameState state = (GolfSixGameState) gs;
        FrenchCard card = (fromDiscard ? state.getDiscardPile() : state.getDrawDeck()).draw();
        boolean[] visibility = new boolean[state.getNPlayers()];
        if (fromDiscard)
            Arrays.fill(visibility, true);
        else
            visibility[state.getCurrentPlayer()] = true;
        state.getDrawnCardDeck().add(card, visibility);
        state.setDrawnFromDiscard(fromDiscard);
        return true;
    }

    @Override
    public DrawCard copy() {
        return this;  // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DrawCard that && fromDiscard == that.fromDiscard;
    }

    @Override
    public int hashCode() {
        return fromDiscard ? 582073 : 582079;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return fromDiscard ? "Draw from discard pile" : "Draw from draw deck";
    }
}
