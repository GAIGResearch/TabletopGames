package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.Objects;

public class ChooseAmount extends AbstractAction {
    public final int playerID;
    public final int amount;

    public ChooseAmount(int playerID, int amount){
        this.playerID = playerID;
        this.amount = amount;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        //this action serves purely as a placeholder for the amount of warriors to be moved in the next action -> to reduce the action space
        if(gs.getCurrentPlayer() == playerID){
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof ChooseAmount other){
            return playerID == other.playerID && amount == other.amount;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("ChooseAmount",playerID, amount);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Player " + playerID + " chooses to move " + amount + " units";
    }
}
