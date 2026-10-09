package games.rummy.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.rummy.RummyGameState;

import java.util.Objects;

public class Discard extends AbstractAction {

    public final FrenchCard card;

    public Discard(FrenchCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RummyGameState state = (RummyGameState) gs;
        state.getPlayerHand(state.getCurrentPlayer()).remove(card);
        state.getDiscardPile().add(card);
        return true;
    }

    @Override
    public Discard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Discard other && Objects.equals(other.card, card);
    }

    @Override
    public int hashCode() {
        return 610369 + 31 * Objects.hashCode(card);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Discard " + card;
    }
}
