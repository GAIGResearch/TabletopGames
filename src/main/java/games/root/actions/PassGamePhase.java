package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;

import java.util.Objects;

public class PassGamePhase extends AbstractAction {
    public final int playerID;

    public PassGamePhase(int playerID){
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(playerID == currentState.getCurrentPlayer()){
            if(currentState.getGamePhase().equals(RootGameState.RootGamePhase.Birdsong)){
                currentState.setGamePhase(RootGameState.RootGamePhase.Daylight);
                currentState.setPlayerSubGamePhase(0);
                currentState.setActionsPlayed(0);
                return true;
            }else if(currentState.getGamePhase().equals(RootGameState.RootGamePhase.Daylight)){
                currentState.setGamePhase(RootGameState.RootGamePhase.Evening);
                currentState.setPlayerSubGamePhase(0);
                currentState.setActionsPlayed(0);
                return true;
            }
        }
        return false;
    }

    @Override
    public PassGamePhase copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PassGamePhase that = (PassGamePhase) o;
        return playerID == that.playerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID);
    }

    @Override
    public String toString() {
        return "p" + playerID + " passed";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootGameState.RootGamePhase phase = (RootGameState.RootGamePhase) gs.getGamePhase();
        return gs.getPlayerFaction(playerID).toString() + " passed " + phase.toString();
    }
}
