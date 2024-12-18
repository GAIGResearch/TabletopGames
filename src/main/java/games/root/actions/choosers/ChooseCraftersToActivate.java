package games.root.actions.choosers;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;

import java.util.Objects;

public class ChooseCraftersToActivate extends AbstractAction {
    public final int playerID;
    public final RootParameters.ClearingTypes activate;
    public final RootParameters.ClearingTypes actual;

    public ChooseCraftersToActivate(int playerID, RootParameters.ClearingTypes activate, RootParameters.ClearingTypes actual){
        this.playerID = playerID;
        this.activate = activate;
        this.actual = actual;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        return gs.getCurrentPlayer() == playerID;
    }

    @Override
    public ChooseCraftersToActivate copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof ChooseCraftersToActivate cca){
            return playerID == cca.playerID && activate == cca.activate && actual == cca.actual;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("ActivateCrafters", playerID, activate, actual);
    }

    @Override
    public String toString() {
         return "p" + playerID + " activates " + activate.toString() + " to satisfy " + actual.toString() + " requirement";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " activates " + activate.toString() + " to satisfy " + actual.toString() + " requirement";
    }
}
