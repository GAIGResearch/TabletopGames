package games.explodingkittens.actions;

import core.actions.IAction;
import core.AbstractGameState;
import core.observations.IPrintable;

public class PassAction implements IAction, IsNope, IPrintable {

    private final int playerID;
    public PassAction(int playerID){
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return false;
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player %d passes", playerID);
    }

    @Override
    public boolean isNope() {
        return false;
    }

    @Override
    public void PrintToConsole() {
        System.out.println(this.toString());
    }
}
