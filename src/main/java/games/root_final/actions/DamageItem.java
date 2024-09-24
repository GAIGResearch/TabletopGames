package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.Item;

import java.util.Objects;

public class DamageItem extends AbstractAction {
    public final int playerID;
    public Item item;

    public DamageItem(int playerID, Item item){
        this.playerID = playerID;
        this.item = item;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            for(Item owned_item: currentState.getSachel()){
                if (owned_item.equals(item) && !owned_item.damaged){
                    owned_item.damaged = true;
                    return true;
                }
            }
            for (Item ownedTea: currentState.getTeas()){
                if (ownedTea.equals(item) && !ownedTea.damaged){
                    ownedTea.damaged = true;
                    return true;
                }
            }
            for (Item ownedCoin: currentState.getCoins()){
                if (ownedCoin.equals(item) && !ownedCoin.damaged){
                    ownedCoin.damaged = true;
                    return true;
                }
            }
            for (Item ownedBag: currentState.getBags()){
                if (ownedBag.equals(item) && !ownedBag.damaged){
                    ownedBag.damaged = true;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new DamageItem(playerID, item);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof DamageItem){
            DamageItem other = (DamageItem) obj;
            return playerID == other.playerID && item.getComponentID() == other.item.getComponentID();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("DamageItem", playerID, item);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " chooses to damage " + item.itemType.toString();
    }
}
