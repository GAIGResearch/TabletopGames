package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;

import java.util.Objects;

public class PassSubGamePhase extends AbstractAction {
    public final int playerID;
    public final String message;

    public PassSubGamePhase(int playerID){
        this.playerID = playerID;
        this.message = "";
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
    public PassSubGamePhase copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PassSubGamePhase that = (PassSubGamePhase) o;
        return playerID == that.playerID && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, message);
    }

    @Override
    public String toString() {
        return "p" + playerID + " passes sub-game-phase: " + message;
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
