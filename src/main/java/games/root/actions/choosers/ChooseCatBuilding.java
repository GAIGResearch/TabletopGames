package games.root.actions.choosers;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;

import java.util.Objects;

public class ChooseCatBuilding extends AbstractAction {
    public final int playerID;
    public final RootParameters.BuildingType bt;
    public final int cost;

    public ChooseCatBuilding(int playerID, RootParameters.BuildingType bt, int cost){
        this.playerID = playerID;
        this.bt = bt;
        this.cost = cost;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        return currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat;
    }

    @Override
    public ChooseCatBuilding copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof ChooseCatBuilding cc){
            return playerID == cc.playerID && bt == cc.bt && cost == cc.cost;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("ChooseCatBuilding", playerID, bt, cost);
    }

    @Override
    public String toString() {
        return "p" + playerID + " chooses " + bt.toString();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " chooses " + bt.toString();
    }
}
