package games.monopolydeal.actions.boardmanagement;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.PropertySet;
import games.monopolydeal.cards.SetType;

import java.util.Objects;

/**
 * <p>A simple action for adding a wild card to a specific set</p>
 */
public class AddWildTo extends AbstractAction {

    final int player;
    final int pSetId;
    final SetType pSetType;

    public AddWildTo(PropertySet pSet, int playerID){
        this.player = playerID;
        this.pSetId = pSet.getComponentID();
        this.pSetType = pSet.getSetType();
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyDealGameState state = (MonopolyDealGameState) gs;
        state.removeCardFromHand(player, CardType.MulticolorWild);
        PropertySet pSet = (PropertySet) state.getComponentById(pSetId);
        state.addPropertyToSet(player,CardType.MulticolorWild,pSet.getSetType());
        state.useAction(1);
        return true;
    }
    @Override
    public AddWildTo copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AddWildTo addWildTo = (AddWildTo) o;
        return player == addWildTo.player && pSetId == addWildTo.pSetId && pSetType == addWildTo.pSetType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, pSetId, pSetType);
    }

    @Override
    public String toString() {
        return "Add MulticolorWild to "+ pSetType;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
