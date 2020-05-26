package games.loveletter.actions;

import core.AbstractGameState;
import core.observations.IPrintable;
import games.loveletter.LoveLetterGameState;

/**
 * At the beginning of each round the player draws a card and loses its protection status.
 */
public class DrawCard extends core.actions.DrawCard implements IPrintable {

    public DrawCard(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((LoveLetterGameState)gs).setProtection(gs.getTurnOrder().getCurrentPlayer(gs), false);
        return super.execute(gs);
    }

    @Override
    public String toString() {
        return "Draw a card and remove protection status.";
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
}
