package games.monopolydeal.actions.boardmanagement;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;

import java.util.Objects;

/**
 * <p>A simple action for adding money onto a player's bank</p>
 */
public class AddMoney extends AbstractAction {
    final int player;
    public final CardType cardType;

    public AddMoney(CardType cardType, int playerId) {
        this.cardType = cardType;
        player = playerId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState state = (MonopolyDealGameState) gs;
        state.removeCardFromHand(player, cardType);
        state.addMoney(player,cardType);
        state.useAction(1);
        return true;
    }
    @Override
    public AddMoney copy() {
        return this;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddMoney addMoney = (AddMoney) o;
        return player == addMoney.player && Objects.equals(cardType, addMoney.cardType);
    }
    @Override
    public int hashCode() {
        return Objects.hash(player, cardType);
    }
    @Override
    public String toString() {
        return "Add " + cardType.toString() + " to Bank";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
