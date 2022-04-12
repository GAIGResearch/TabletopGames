package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;

/**
 * The Countess needs to be discarded in case the player also hold a King or a Prince card.
 * Despite its high value, the Countess has no other effect.
 */
public class CountessAction extends core.actions.DrawCard implements IPrintable {

    public CountessAction(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public String toString(){
        return "Countess - needs to be discarded if the player also holds King or Prince";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Countess (auto discard with King/Prince)";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(toString());
    }

    @Override
    public AbstractAction copy() {
        return new CountessAction(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CountessAction)) return false;
        return super.equals(o);
    }
}
