package games.president.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.president.PresidentGameState;
import games.president.PresidentUtils;

/**
 * The President gives a card back to the Scum of the last deal.
 */
public class GiveCard extends AbstractAction {

    public final FrenchCard card;

    public GiveCard(FrenchCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PresidentGameState state = (PresidentGameState) gs;
        state.getPlayerHand(state.getCurrentPlayer()).remove(card);
        state.getPlayerHand(state.getScum()).add(card);
        return true;
    }

    @Override
    public GiveCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GiveCard other && other.card.equals(card);
    }

    @Override
    public int hashCode() {
        return card.hashCode() + 610339;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Give " + PresidentUtils.rankName(card.number) + " of " + card.suite;
    }
}
