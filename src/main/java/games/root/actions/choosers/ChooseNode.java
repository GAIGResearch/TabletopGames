package games.root.actions.choosers;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;

import java.util.Objects;

public class ChooseNode extends AbstractAction {
    public final int playerID;
    public final int nodeID;
    public boolean birdPlayed=false;

    public ChooseNode(int playerID, int nodeID){
        this.playerID= playerID;
        this.nodeID = nodeID;
    }
    public ChooseNode(int playerID, int nodeID, boolean birdPlayed){
        this.playerID = playerID;
        this.nodeID = nodeID;
        this.birdPlayed = birdPlayed;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        return gs.getCurrentPlayer() == playerID;
    }

    @Override
    public ChooseNode copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if (obj instanceof ChooseNode other){
            return playerID == other.playerID && nodeID == other.nodeID && birdPlayed == other.birdPlayed;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("ChooseNode", playerID, nodeID);
    }

    @Override
    public String toString() {
        return "p" + playerID + " chooses node " + nodeID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        if (gs.getPlayerFaction(playerID) == RootParameters.Factions.EyrieDynasties){
            return gs.getPlayerFaction(playerID).toString() + " chooses " + gs.getGameMap().getNodeByID(nodeID).identifier + " as a bird action:" + birdPlayed;
        }
        return gs.getPlayerFaction(playerID).toString() + " chooses " + gs.getGameMap().getNodeByID(nodeID).identifier;
    }
}
