package games.loveletter.actions;

import core.observations.IPrintable;

/**
 * The Countess needs to be discarded in case the player also hold a King or a Prince card.
 * Despite its high value, the Countess has no other effect.
 */
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
