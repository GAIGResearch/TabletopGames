package games.explodingkittens.actions;

import core.actions.DoNothing;
import core.observations.IPrintable;

public class PassAction extends DoNothing implements IsNope, IPrintable {

    @Override
    public String toString(){//overriding the toString() method
        return "Player passes";
    }

    @Override
    public boolean isNope() {
        return false;
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}
