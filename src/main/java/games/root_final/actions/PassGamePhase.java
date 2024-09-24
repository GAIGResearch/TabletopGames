package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;

import java.util.Objects;

public class PassGamePhase extends AbstractAction {

    protected int playerID;

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
    public AbstractAction copy() {
        return new PassGamePhase(playerID);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(obj instanceof PassGamePhase){
            PassGamePhase passGamePhase = (PassGamePhase) obj;
            return passGamePhase.playerID == playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, "PassGamePhase");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootGameState.RootGamePhase phase = (RootGameState.RootGamePhase) gs.getGamePhase();
        return gs.getPlayerFaction(playerID).toString() + " passed " + phase.toString();
    }
}
