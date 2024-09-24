package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;

import java.util.Objects;

public class PassSubGamePhase extends AbstractAction {
    public final int playerID;
    public String message;
    public PassSubGamePhase(int playerID){
        this.playerID = playerID;
    }

    public PassSubGamePhase(int playerID, String message){
        this.playerID = playerID;
        this.message = message;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if(playerID == state.getCurrentPlayer()){
            state.increaseSubGamePhase();
            state.setActionsPlayed(0);
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof PassSubGamePhase other){
            if (message != null && other.message != null) {
                return playerID == other.playerID && Objects.equals(message, other.message);
            }
            return playerID == other.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("PassSubGamePhase", playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        if (message != null){
            return gs.getPlayerFaction(playerID).toString() + " " + message;
        }
        return gs.getPlayerFaction(playerID).toString() + " passes Sub-Game-Phase";
    }
}
