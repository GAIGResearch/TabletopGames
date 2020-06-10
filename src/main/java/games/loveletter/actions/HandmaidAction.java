package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;

/**
 * The handmaid protects the player from any targeted effects until the next turn.
 */
public class HandmaidAction extends DrawCard implements IPrintable {

    public HandmaidAction(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // set the player's protection status
        ((LoveLetterGameState)gs).setProtection(gs.getTurnOrder().getCurrentPlayer(gs), true);
        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Handmaid - get protection status";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(toString());
    }

    @Override
    public AbstractAction copy() {
        return new HandmaidAction(deckFrom, deckTo, fromIndex);
    }
}
