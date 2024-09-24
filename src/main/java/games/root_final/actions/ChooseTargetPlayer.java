package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;

import java.util.Objects;

public class ChooseTargetPlayer extends AbstractAction {
    public final int playerID;
    public final int targetID;

    public ChooseTargetPlayer(int playerID, int targetID){
        this.playerID = playerID;
        this.targetID = targetID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        if (gs.getCurrentPlayer() == playerID){
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
        if (obj instanceof ChooseTargetPlayer ct){
            return playerID == ct.playerID && targetID == ct.targetID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("ChooseTargetPlayer", playerID, targetID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " chooses " + gs.getPlayerFaction(targetID).toString() ;
    }
}
