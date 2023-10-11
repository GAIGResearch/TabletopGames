package games.monopolydeal.actions.boardmanagement;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.SetType;

import java.util.Objects;

/**
 * <p>A simple action for adding a house or hotel to a specified property set.</p>
 */
public class AddBuilding extends AbstractAction {
    final int player;
    final MonopolyDealCard card;
    final SetType setType;
    public AddBuilding(MonopolyDealCard card, int playerId, SetType setType) {
        this.card = card;
        this.setType = setType;
        player = playerId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState state = (MonopolyDealGameState) gs;
        state.removeCardFromHand(player, card);
        // For debugging
        if(setType!=SetType.UNDEFINED){
            int i=0;
        }
        state.addPropertyToSet(player,card,setType);
        state.useAction(1);
        return true;
    }
    @Override
    public AddBuilding copy() {
        return new AddBuilding(card,player,setType);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddBuilding that = (AddBuilding) o;
        return player == that.player && Objects.equals(card, that.card) && setType == that.setType;
    }
    @Override
    public int hashCode() {
        return Objects.hash(player, card, setType);
    }
    @Override
    public String toString() {
        return "Add " + card.toString() + " to " + setType;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
