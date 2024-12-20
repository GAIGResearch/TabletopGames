package games.monopolydeal.actions.boardmanagement;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;

import java.util.Objects;

/**
 * <p>A simple action for adding a property card onto the player's board</p>
 */
public class AddProperty extends AbstractAction {
    final int player;
    final CardType cardType;

    public AddProperty(CardType cardType, int playerId) {
        this.cardType = cardType;
        player = playerId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState state = (MonopolyDealGameState) gs;
        state.removeCardFromHand(player, cardType);
        state.addProperty(player,cardType);
        state.useAction(1);
        return true;
    }
    @Override
    public AddProperty copy() {
        return this;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddProperty that = (AddProperty) o;
        return player == that.player && Objects.equals(cardType, that.cardType);
    }
    @Override
    public int hashCode() {
        return Objects.hash(player, cardType);
    }
    @Override
    public String toString() { return "Add " + cardType.toString() +" to properties"; }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
