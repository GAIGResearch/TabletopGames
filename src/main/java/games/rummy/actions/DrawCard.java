package games.rummy.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.rummy.RummyGameState;

import java.util.Arrays;

/**
 * Draws the top card of the draw deck, or of the discard pile.
 */
public class DrawCard extends AbstractAction {

    public final boolean fromDiscardPile;

    public DrawCard(boolean fromDiscardPile) {
        this.fromDiscardPile = fromDiscardPile;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RummyGameState state = (RummyGameState) gs;
        Deck<FrenchCard> source = fromDiscardPile ? state.getDiscardPile() : state.getDrawDeck();
        FrenchCard card = source.draw();
        if (fromDiscardPile) {
            // everyone saw the card taken
            boolean[] allPlayers = new boolean[state.getNPlayers()];
            Arrays.fill(allPlayers, true);
            state.getPlayerHand(state.getCurrentPlayer()).add(card, allPlayers);
        } else {
            state.getPlayerHand(state.getCurrentPlayer()).add(card);
        }
        state.setTakenCard(fromDiscardPile ? card : null);
        state.setGamePhase(RummyGameState.Phase.PLAY);
        return true;
    }

    @Override
    public DrawCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DrawCard other && other.fromDiscardPile == fromDiscardPile;
    }

    @Override
    public int hashCode() {
        return fromDiscardPile ? 610357 : 610363;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return fromDiscardPile ? "Take the discard" : "Draw from the draw deck";
    }
}
