package games.explodingkittens.actions;

import core.actions.DrawCard;
import core.observations.IPrintable;

public class NopeAction extends DrawCard implements IsNope, IPrintable {

    public NopeAction(int deckFrom, int deckTo, int index) {
        super(deckFrom, deckTo, index);
    }

    @Override
    public String toString(){//overriding the toString() method
        return "Player nopes the previous action";
    }

    @Override
    public boolean isNope() {
        return true;
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}
