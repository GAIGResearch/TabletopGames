package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;

import java.util.Objects;

public class WoodlandSetup extends AbstractAction {
    public final int playerID;
    public final boolean increaseSubGamePhase;

    public WoodlandSetup(int playerID, boolean increaseSubGamePhase){
        this.playerID = playerID;
        this.increaseSubGamePhase = increaseSubGamePhase;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance){
            currentState.increaseActionsPlayed();
            if (increaseSubGamePhase){
                currentState.increaseSubGamePhase();
            }
            for(int i = 0; i < 3; i++){  // todo param
                currentState.getSupporters().add(currentState.getDrawPile().draw());
            }
        }
        return false;
    }

    @Override
    public WoodlandSetup copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WoodlandSetup that = (WoodlandSetup) o;
        return playerID == that.playerID && increaseSubGamePhase == that.increaseSubGamePhase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, increaseSubGamePhase);
    }

    @Override
    public String toString() {
        return "p" + playerID + " draws 3 supporters";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " draws 3 supporters";
    }
}
