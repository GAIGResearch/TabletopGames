package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;


public class Move extends AbstractAction {

    public final int from;
    public final int to;
    public final int amount;
    public final int playerID;
    public Move(int from, int to, int amount, int playerIdx){
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.playerID = playerIdx;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(currentState.getCurrentPlayer() == playerID){
            if(currentState.getGameMap().getNodeByID(from).getWarrior(currentState.getPlayerFaction(playerID)) >= amount){
                for(int i = 0; i < amount; i++){
                    currentState.getGameMap().getNodeByID(from).removeWarrior(currentState.getPlayerFaction(playerID));
                    currentState.getGameMap().getNodeByID(to).addWarrior(currentState.getPlayerFaction(playerID));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new Move(from,to,amount,playerID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if(obj instanceof Move)
        {
            Move other = (Move) obj;
            return from == other.from && to == other.to && amount == other.amount && playerID == other.playerID;

        }else return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID,to,from,amount);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " moves " + amount + " units from " + gs.getGameMap().getNodeByID(from).identifier + " to " + gs.getGameMap().getNodeByID(to).identifier;
    }
}
