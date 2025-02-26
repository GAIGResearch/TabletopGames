package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.Item;

import java.util.Objects;

public class VagabondRepair extends AbstractAction {
    public final int playerID;
    public final Item.ItemType item;
    public final boolean hammerPlayed;

    public VagabondRepair(int playerID, Item.ItemType item, boolean hammerPlayed){
        this.playerID = playerID;
        this.item = item;
        this.hammerPlayed = hammerPlayed;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            switch (item){
                case coin:
                    for (Item itemToRepair: currentState.getCoins()){
                        if (itemToRepair.itemType.equals(item) && itemToRepair.damaged){
                            itemToRepair.damaged = false;
                            break;
                        }
                    }
                    break;
                case tea:
                    for (Item itemToRepair: currentState.getTeas()){
                        if (itemToRepair.itemType.equals(item) && itemToRepair.damaged){
                            itemToRepair.damaged = false;
                            break;
                        }
                    }
                    break;
                case bag:
                    for (Item itemToRepair: currentState.getBags()){
                        if (itemToRepair.itemType.equals(item) && itemToRepair.damaged){
                            itemToRepair.damaged = false;
                            break;
                        }
                    }
                    break;
                default:
                    for (Item itemToRepair: currentState.getSatchel()){
                        if (itemToRepair.itemType.equals(item) && itemToRepair.damaged){
                            itemToRepair.damaged = false;
                            break;
                        }
                    }
                    break;
            }
            if(hammerPlayed) {
                for (Item hammer : currentState.getSatchel()) {
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
    public VagabondRepair copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VagabondRepair that = (VagabondRepair) o;
        return playerID == that.playerID && hammerPlayed == that.hammerPlayed && item == that.item;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, item, hammerPlayed);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " repairs " + item.toString();
    }
}
