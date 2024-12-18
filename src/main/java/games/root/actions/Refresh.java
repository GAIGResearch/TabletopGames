package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.Item;

import java.util.Objects;

public class Refresh extends AbstractAction {
    public final int playerID;
    public final Item.ItemType item;
    public final int itemID;

    public Refresh(int playerID, Item.ItemType item, int itemID){
        this.playerID = playerID;
        this.item = item;
        this.itemID = itemID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if(playerID == state.getCurrentPlayer() && state.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            state.increaseActionsPlayed();
            for (Item item: state.getSatchel()){
                if (item.getComponentID() == this.itemID){
                    item.refreshed = true;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Refresh copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Refresh refresh = (Refresh) o;
        return playerID == refresh.playerID && itemID == refresh.itemID && item == refresh.item;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, item, itemID);
    }

    @Override
    public String toString() {
        return "p" + playerID + " refreshes " + item.toString();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " refreshes " + item.toString();
    }
}
