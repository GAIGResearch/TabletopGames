package updated_core.games.explodingkittens.actions;

import updated_core.actions.IAction;
import updated_core.actions.IPrintable;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

public class PassAction implements IAction, IsNope, IPrintable {

    private final int playerID;
    public PassAction(int playerID){
        this.playerID = playerID;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
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
