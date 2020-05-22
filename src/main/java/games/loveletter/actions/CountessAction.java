package games.loveletter.actions;

import core.observations.IPrintable;

public class CountessAction extends DrawCard implements IPrintable {

    public CountessAction(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public String toString(){
        return "Countess - needs to be discarded if the player also holds King or Prince";
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
}
