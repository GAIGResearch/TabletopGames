package games.monopolydeal.actions.boardmanagement;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.SetType;

import java.util.Objects;

/**
 * <p>A simple action for adding a house or hotel to a specified property set.</p>
 */
public class AddBuilding extends AbstractAction {
    final int player;
    final CardType cardType;
    final SetType setType;

    public AddBuilding(CardType cardType, int playerId, SetType setType) {
        this.cardType = cardType;
        this.setType = setType;
        player = playerId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState state = (MonopolyDealGameState) gs;
        state.removeCardFromHand(player, cardType);
        // For debugging
        if(setType!=SetType.UNDEFINED){
            int i=0;
        }
        state.addPropertyToSet(player,cardType,setType);
        state.useAction(1);
        return true;
    }
    @Override
    public AddBuilding copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AddBuilding that = (AddBuilding) o;
        return player == that.player && cardType == that.cardType && setType == that.setType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, cardType, setType);
    }

    @Override
    public String toString() {
        return "Add " + cardType.toString() + " to " + setType;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
