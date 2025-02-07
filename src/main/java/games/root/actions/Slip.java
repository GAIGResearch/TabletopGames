package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class Slip extends AbstractAction {
    public final int playerID;
    public final int fromID;
    public final int toID;
    public final boolean passSubGamePhase;

    public Slip(int playerID, int fromID, int toID, boolean passSubGamePhase){
        this.playerID = playerID;
        this.fromID = fromID;
        this.toID = toID;
        this.passSubGamePhase = passSubGamePhase;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            if(currentState.getGameMap().getNodeByID(fromID).getWarrior(RootParameters.Factions.Vagabond) == 1){
                currentState.getGameMap().getNodeByID(fromID).removeWarrior(RootParameters.Factions.Vagabond);
                currentState.getGameMap().getNodeByID(toID).addWarrior(RootParameters.Factions.Vagabond);
                currentState.increaseActionsPlayed();
                if(passSubGamePhase){
                    currentState.increaseSubGamePhase();
                }
            }
        }
        return false;
    }

    @Override
    public Slip copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Slip slip = (Slip) o;
        return playerID == slip.playerID && fromID == slip.fromID && toID == slip.toID && passSubGamePhase == slip.passSubGamePhase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, fromID, toID, passSubGamePhase);
    }

    @Override
    public String toString() {
        return "p" + playerID + " slips from " + fromID + " to " + toID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootBoardNodeWithRootEdges from = gs.getGameMap().getNodeByID(fromID);
        RootBoardNodeWithRootEdges to = gs.getGameMap().getNodeByID(toID);
        return gs.getPlayerFaction(playerID).toString()  + " slips from " + from.identifier + " to " + to.identifier;
    }
}
