package games.monopolydeal.actions.boardmanagement;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.MonopolyDealCard;

import java.util.Objects;

/**
 * <p>A simple action for adding a property card onto the player's board</p>
 */
public class AddProperty extends AbstractAction {
    final int player;
    final MonopolyDealCard card;
    public AddProperty(MonopolyDealCard card, int playerId) {
        this.card = card;
        player = playerId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState state = (MonopolyDealGameState) gs;
        state.removeCardFromHand(player, card);
        state.addProperty(player,card);
        state.useAction(1);
        return true;
    }
    @Override
    public AddProperty copy() {
        return new AddProperty(card,player);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddProperty that = (AddProperty) o;
        return player == that.player && Objects.equals(card, that.card);
    }
    @Override
    public int hashCode() {
        return Objects.hash(player, card);
    }
    @Override
    public String toString() { return "Add " + card.toString() +" to properties"; }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
