package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class Slip extends AbstractAction {
    public final int playerID;
    public final boolean passSubGamePhase;
    public RootBoardNodeWithRootEdges from;
    public RootBoardNodeWithRootEdges to;

    public Slip(int playerID, RootBoardNodeWithRootEdges from, RootBoardNodeWithRootEdges to, boolean passSubGamePhase){
        this.playerID = playerID;
        this.from = from;
        this.to = to;
        this.passSubGamePhase = passSubGamePhase;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            if(currentState.getGameMap().getNodeByID(from.getComponentID()).getWarrior(RootParameters.Factions.Vagabond) == 1){
                currentState.getGameMap().getNodeByID(from.getComponentID()).removeWarrior(RootParameters.Factions.Vagabond);
                currentState.getGameMap().getNodeByID(to.getComponentID()).addWarrior(RootParameters.Factions.Vagabond);
                currentState.increaseActionsPlayed();
                if(passSubGamePhase){
                    currentState.increaseSubGamePhase();
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new Slip(playerID, from, to, passSubGamePhase);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof Slip){
            Slip other = (Slip) obj;
            return playerID == other.playerID && from.getComponentID() == other.from.getComponentID() && to.getComponentID() == other.to.getComponentID() && passSubGamePhase == other.passSubGamePhase;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Slip", playerID, from.hashCode(), to.hashCode(), passSubGamePhase);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " slips from " + from.identifier + " to " + to.identifier;
    }
}
