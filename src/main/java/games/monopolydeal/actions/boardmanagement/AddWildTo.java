package games.monopolydeal.actions.boardmanagement;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.PropertySet;

import java.util.Objects;

/**
 * <p>A simple action for adding a wild card to a specific set</p>
 */
public class AddWildTo extends AbstractAction {

    final int player;
    final PropertySet pSet;

    public AddWildTo(PropertySet pSet, int playerID){
        this.player = playerID;
        this.pSet = pSet;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState state = (MonopolyDealGameState) gs;
        state.removeCardFromHand(player, CardType.MulticolorWild);
        state.addPropertyToSet(player,CardType.MulticolorWild,pSet.getSetType());
        state.useAction(1);
        return true;
    }
    @Override
    public AddWildTo copy() {
        return new AddWildTo(pSet,player);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddWildTo addWildTo = (AddWildTo) o;
        return player == addWildTo.player && Objects.equals(pSet, addWildTo.pSet);
    }
    @Override
    public int hashCode() {
        return Objects.hash(player, pSet);
    }
    @Override
    public String toString() {
        return "Add MulticolorWild to "+ pSet.toString();
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
