package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.Item;

import java.util.Objects;

public class Refresh extends AbstractAction {
    public final int playerID;
    public Item item;

    public Refresh(int playerID, Item item){
        this.playerID = playerID;
        this.item = item;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if(playerID == state.getCurrentPlayer() && state.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            state.increaseActionsPlayed();
            for (Item item: state.getSachel()){
                if (item.getComponentID() == this.item.getComponentID()){
                    item.refreshed = true;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new Refresh(playerID, item);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof Refresh){
            Refresh other = (Refresh) obj;
            return playerID==other.playerID && item.getComponentID() == other.item.getComponentID();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Refresh", playerID, item.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " refreshes " + item.itemType.toString();
    }
}
