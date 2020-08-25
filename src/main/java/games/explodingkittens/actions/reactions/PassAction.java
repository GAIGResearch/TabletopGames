package games.explodingkittens.actions.reactions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IPrintable;

public class PassAction extends DoNothing implements IPrintable {

    @Override
    public String toString(){//overriding the toString() method
        return "Player passes";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "No NOPE";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this.toString());
    }

    @Override
    public AbstractAction copy() {
        return new PassAction();
    }
}
