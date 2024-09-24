package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.Item;

import java.util.Objects;

public class VagabondRepairAllItems extends AbstractAction {

    public final int playerID;

    public VagabondRepairAllItems(int playerID){
        this.playerID = playerID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            for (Item bag: currentState.getBags()){
                bag.damaged = false;
            }
            for (Item tea: currentState.getTeas()){
                tea.damaged = false;
            }
            for (Item coin: currentState.getCoins()){
                coin.damaged = false;
            }
            for (Item item: currentState.getSachel()){
                item.damaged = false;
            }
            currentState.increaseSubGamePhase();
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof VagabondRepairAllItems vr){
            return playerID == vr.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("VagabondRepairAllItems", playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " repairs all items";
    }
}
