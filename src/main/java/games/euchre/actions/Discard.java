package games.euchre.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.euchre.EuchreGameState;

/**
 * The dealer, having taken the up-card, puts a card from their hand face down into the kitty.
 */
public class Discard extends AbstractAction {

    public final FrenchCard card;

    public Discard(FrenchCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        EuchreGameState state = (EuchreGameState) gs;
        state.getPlayerHand(state.getCurrentPlayer()).remove(card);
        state.getKitty().add(card);
        state.setDealerDiscard(card);
        return true;
    }

    @Override
    public Discard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Discard that && card.equals(that.card);
    }

    @Override
    public int hashCode() {
        return card.hashCode() + 562309;
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
