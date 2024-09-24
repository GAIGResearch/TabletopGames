package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.Item;

import java.util.Objects;

public class VagabondRepair extends AbstractAction {
    public final int playerID;
    public Item item;
    public final boolean hammerPlayed;
    public VagabondRepair(int playerID, Item item, boolean hammerPlayed){
        this.playerID = playerID;
        this.item = item;
        this.hammerPlayed = hammerPlayed;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            switch (item.itemType){
                case coin:
                    for (Item itemToRepair: currentState.getCoins()){
                        if (itemToRepair.equals(item) && itemToRepair.damaged){
                            itemToRepair.damaged = false;
                            break;
                        }
                    }
                    break;
                case tea:
                    for (Item itemToRepair: currentState.getTeas()){
                        if (itemToRepair.equals(item) && itemToRepair.damaged){
                            itemToRepair.damaged = false;
                            break;
                        }
                    }
                    break;
                case bag:
                    for (Item itemToRepair: currentState.getBags()){
                        if (itemToRepair.equals(item) && itemToRepair.damaged){
                            itemToRepair.damaged = false;
                            break;
                        }
                    }
                    break;
                default:
                    for (Item itemToRepair: currentState.getSachel()){
                        if (itemToRepair.equals(item) && itemToRepair.damaged){
                            itemToRepair.damaged = false;
                            break;
                        }
                    }
                    break;
            }
            if(hammerPlayed) {
                for (Item hammer : currentState.getSachel()) {
                    if (hammer.refreshed && !hammer.damaged && hammer.itemType == Item.ItemType.hammer) {
                        hammer.refreshed = false;
                        currentState.increaseActionsPlayed();
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new VagabondRepair(playerID, item, hammerPlayed);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof VagabondRepair vr){
            return playerID == vr.playerID && item.getComponentID() == vr.item.getComponentID() && hammerPlayed == vr.hammerPlayed;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("VagabondRepair", playerID, item.hashCode(), hammerPlayed);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " repairs " + item.itemType.toString();
    }
}
