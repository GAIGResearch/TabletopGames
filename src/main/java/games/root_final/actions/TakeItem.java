package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.Item;

import java.util.Objects;

public class TakeItem extends AbstractAction {
    public final int playerID;
    public final int targetID;
    public Item item;

    public TakeItem(int playerID, int targetID, Item item){
        this.playerID = playerID;
        this.targetID = targetID;
        this.item = item;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            for (Item craftedItem: currentState.getPlayerCraftedItems(targetID)){
                if (craftedItem.equals(item)){
                    switch (item.itemType){
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
                            currentState.getSachel().add(craftedItem);
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
    public AbstractAction copy() {
        return new TakeItem(playerID, targetID, item);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof TakeItem ti){
            return playerID == ti.playerID && targetID == ti.targetID && item.equals(ti.item);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("TakeItem", playerID, targetID, item.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " takes " + item.itemType.toString() + " from " + gs.getPlayerFaction(targetID).toString();
    }
}
