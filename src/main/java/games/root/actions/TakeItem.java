package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.Item;

import java.util.Objects;

public class TakeItem extends AbstractAction {
    public final int playerID;
    public final int targetID;
    public final Item.ItemType item;

    public TakeItem(int playerID, int targetID, Item.ItemType item){
        this.playerID = playerID;
        this.targetID = targetID;
        this.item = item;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            for (Item craftedItem: currentState.getPlayerCraftedItems(targetID)){
                if (craftedItem.itemType.equals(item)){
                    switch (item){
                        case tea:
                            currentState.getTeas().add(craftedItem);
                            break;
                        case coin:
                            currentState.getCoins().add(craftedItem);
                            break;
                        case bag:
                            currentState.getBags().add(craftedItem);
                            break;
                        default:
                            currentState.getSatchel().add(craftedItem);
                            break;
                    }
                    currentState.getPlayerCraftedItems(targetID).remove(craftedItem);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public TakeItem copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TakeItem takeItem = (TakeItem) o;
        return playerID == takeItem.playerID && targetID == takeItem.targetID && item == takeItem.item;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, targetID, item);
    }

    @Override
    public String toString() {
        return "p" + playerID + " takes " + item.toString() + " from p" + targetID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " takes " + item.toString() + " from " + gs.getPlayerFaction(targetID).toString();
    }
}
