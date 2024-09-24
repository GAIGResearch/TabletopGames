package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;

import java.util.Objects;

public class WoodlandSetup extends AbstractAction {
    public final int playerID;
    public final boolean increaseSubGamePlase;

    public WoodlandSetup(int playerID, boolean increaseSubGamePhase){
        this.playerID = playerID;
        this.increaseSubGamePlase = increaseSubGamePhase;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance){
            currentState.increaseActionsPlayed();
            if (increaseSubGamePlase){
                currentState.increaseSubGamePhase();
            }
            for(int i = 0; i < 3; i++){
                currentState.getSupporters().add(currentState.getDrawPile().draw());
            }
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
        if(obj instanceof WoodlandSetup){
            WoodlandSetup other = (WoodlandSetup) obj;
            return playerID == other.playerID && increaseSubGamePlase == other.increaseSubGamePlase;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, "Draw Supporters");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " Draws 3 supporters";
    }
}
