package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.components.MonopolyCard;

import java.util.Objects;

/**
 * The current player uses a Get Out of Jail Free card they hold: they leave Jail, the card goes to the bottom of the
 * pile it came from, and they then roll as normal.
 */
public class UseJailCard extends AbstractAction {

    public final MonopolyCard card;

    public UseJailCard(MonopolyCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        int player = state.getCurrentPlayer();
        state.returnJailCard(player, card);
        state.leaveJail(player);
        return true;
    }

    @Override
    public UseJailCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UseJailCard u && u.card.equals(card);
    }

    @Override
    public int hashCode() {
        return Objects.hash(card) + 640227;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use Get Out of Jail Free (" + (card.pile == MonopolyCard.Pile.CHANCE ? "Chance" : "Community Chest") + ")";
    }

    @Override
    public String toString() {
        return "UseJailCard " + card.pile;
    }
}
