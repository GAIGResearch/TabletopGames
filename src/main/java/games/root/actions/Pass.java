package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;

import java.util.Objects;

public class Pass extends AbstractAction {
    public final int playerID;
    public final String message;

    public Pass(int playerID){
        this.playerID = playerID;
        this.message = "";
    }
    public Pass(int playerID, String message){
        this.playerID = playerID;
        this.message = message;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        return gs.getCurrentPlayer() == playerID;
    }

    @Override
    public Pass copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pass pass = (Pass) o;
        return playerID == pass.playerID && Objects.equals(message, pass.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, message);
    }

    @Override
    public String toString() {
        return "p" + playerID + " passes: " + message;
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
