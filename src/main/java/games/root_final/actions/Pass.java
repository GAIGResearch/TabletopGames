package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;

import java.util.Objects;

public class Pass extends AbstractAction {
    public final int playerID;

    public String message;
    public Pass(int playerID){
        this.playerID = playerID;
    }
    public Pass(int playerID, String message){
        this.playerID = playerID;
        this.message = message;
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
        return new Pass(playerID, message);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof Pass){
            Pass other = (Pass) obj;
            if (message == null || other.message == null){
                return playerID == other.playerID && message == other.message;
            }
            return playerID == other.playerID && message.equals(other.message);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Pass", playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        if (message != null){
            return gs.getPlayerFaction(playerID).toString() + " " + message;
        }
        return gs.getPlayerFaction(playerID).toString() + " passes";
    }
}
