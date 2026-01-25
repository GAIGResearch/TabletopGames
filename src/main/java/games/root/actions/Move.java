package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;

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
    public Move copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return from == move.from && to == move.to && amount == move.amount && playerID == move.playerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, amount, playerID);
    }

    @Override
    public String toString() {
        return "p" + playerID + " moves " + amount + " units from " + from + " to " + to;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " moves " + amount + " units from " + gs.getGameMap().getNodeByID(from).identifier + " to " + gs.getGameMap().getNodeByID(to).identifier;
    }
}
