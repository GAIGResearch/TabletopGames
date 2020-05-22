package games.explodingkittens.actions;

import core.actions.DoNothing;
import core.observations.IPrintable;

public class PassAction extends DoNothing implements IPrintable {

    @Override
    public String toString(){//overriding the toString() method
        return "Player passes";
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}
