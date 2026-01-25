package games.root.actions.choosers;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.Objects;

public class ChooseNumber extends AbstractAction {
    public final int playerID;
    public final int number;

    public ChooseNumber(int playerID, int number){
        this.playerID = playerID;
        this.number = number;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        //this action serves purely as a placeholder for the amount of warriors to be moved in the next action -> to reduce the action space
        return gs.getCurrentPlayer() == playerID;
    }

    @Override
    public ChooseNumber copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof ChooseNumber other){
            return playerID == other.playerID && number == other.number;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("ChooseAmount",playerID, number);
    }

    @Override
    public String toString() {
        return "Player " + playerID + " chooses " + number;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
