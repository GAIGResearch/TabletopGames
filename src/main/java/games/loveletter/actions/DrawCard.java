package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

/**
 * At the beginning of each round the player draws a card and loses its protection status.
 */
public class DrawCard extends AbstractAction implements IPrintable {

    @Override
    public boolean execute(AbstractGameState gs) {
        // Player is no longer protected
        LoveLetterGameState state = (LoveLetterGameState) gs;
        state.setProtection(gs.getCurrentPlayer(), false);
        LoveLetterCard cardDrawn = state.getDrawPile().draw();
        state.getPlayerHandCards().get(state.getCurrentPlayer()).add(cardDrawn);
        return true;
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
        System.out.println(this);
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DrawCard;
    }

    @Override
    public int hashCode() {
        return 433904;
    }

}
