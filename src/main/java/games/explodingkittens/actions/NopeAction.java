package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.interfaces.IPrintable;

public class NopeAction extends DrawCard implements IPrintable, IsNopeable {

    public NopeAction(int deckFrom, int deckTo, int index) {
        super(deckFrom, deckTo, index);
    }

    @Override
    public String toString(){//overriding the toString() method
        return "Player nopes the previous action";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "NOPE";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this.toString());
    }

    @Override
    public AbstractAction copy() {
        return new NopeAction(deckFrom, deckTo, fromIndex);
    }

    @Override
    public void nopedExecute(AbstractGameState gs) {
        super.execute(gs);
    }

    @Override
    public void actionPlayed(AbstractGameState gs) {
        // Doesn't need to do anything, this card is played automatically.
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NopeAction)) return false;
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 792;
    }
}
