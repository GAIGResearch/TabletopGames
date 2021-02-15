package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;

/**
 * At the beginning of each round the player draws a card and loses its protection status.
 */
public class DrawCard extends core.actions.DrawCard implements IPrintable {

    public DrawCard(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean _execute(AbstractGameState gs) {
        ((LoveLetterGameState)gs).setProtection(gs.getTurnOrder().getCurrentPlayer(gs), false);
        return super._execute(gs);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Draw card";
    }

    @Override
    public String toString() {
        return "Draw a card and remove protection status.";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(toString());
    }

    @Override
    public AbstractAction copy() {
        return new DrawCard(deckFrom, deckTo, fromIndex);
    }
}
