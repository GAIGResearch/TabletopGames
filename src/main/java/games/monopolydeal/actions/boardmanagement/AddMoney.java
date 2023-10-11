package games.monopolydeal.actions.boardmanagement;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.MonopolyDealCard;

import java.util.Objects;

/**
 * <p>A simple action for adding money onto a player's bank</p>
 */
public class AddMoney extends AbstractAction {
    final int player;
    final MonopolyDealCard card;
    public AddMoney(MonopolyDealCard card, int playerId) {
        this.card = card;
        player = playerId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState state = (MonopolyDealGameState) gs;
        state.removeCardFromHand(player, card);
        state.addMoney(player,card);
        state.useAction(1);
        return true;
    }
    @Override
    public AddMoney copy() {
        return new AddMoney(card,player);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddMoney addMoney = (AddMoney) o;
        return player == addMoney.player && Objects.equals(card, addMoney.card);
    }
    @Override
    public int hashCode() {
        return Objects.hash(player, card);
    }
    @Override
    public String toString() {
        return "Add " + card.toString() + " to Bank";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
