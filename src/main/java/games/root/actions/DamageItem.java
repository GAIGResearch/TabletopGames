package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.Item;

import java.util.Objects;

public class DamageItem extends AbstractAction {
    public final int playerID;
    public Item.ItemType item;

    public DamageItem(int playerID, Item.ItemType item){
        this.playerID = playerID;
        this.item = item;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            for(Item owned_item: currentState.getSatchel()){
                if (owned_item.itemType == item && !owned_item.damaged){
                    owned_item.damaged = true;
                    return true;
                }
            }
            for (Item ownedTea: currentState.getTeas()){
                if (ownedTea.itemType.equals(item) && !ownedTea.damaged){
                    ownedTea.damaged = true;
                    return true;
                }
            }
            for (Item ownedCoin: currentState.getCoins()){
                if (ownedCoin.itemType.equals(item) && !ownedCoin.damaged){
                    ownedCoin.damaged = true;
                    return true;
                }
            }
            for (Item ownedBag: currentState.getBags()){
                if (ownedBag.itemType.equals(item) && !ownedBag.damaged){
                    ownedBag.damaged = true;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public DamageItem copy() {
        return new DamageItem(playerID, item);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof DamageItem other){
            return playerID == other.playerID && item == other.item;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("DamageItem", playerID, item);
    }

    @Override
    public String toString() {
        return "p" + playerID + " chooses to damage " + item.name();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " chooses to damage " + item.toString();
    }
}
