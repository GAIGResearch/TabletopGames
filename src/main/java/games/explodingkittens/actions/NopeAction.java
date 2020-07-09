package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.interfaces.IPrintable;
import core.turnorders.TurnOrder;

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
    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.execute(gs);
    }
}
