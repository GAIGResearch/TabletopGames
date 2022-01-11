package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;

/**
 * The handmaid protects the player from any targeted effects until the next turn.
 */
public class HandmaidAction extends core.actions.DrawCard implements IPrintable {

    public HandmaidAction(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // set the player's protection status
        ((LoveLetterGameState) gs).setProtection(gs.getTurnOrder().getCurrentPlayer(gs), true);
        return super.execute(gs);
    }

    @Override
    public String toString() {
        return "Handmaid - get protection status";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Handmaid (protect)";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(toString());
    }

    @Override
    public AbstractAction copy() {
        return new HandmaidAction(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HandmaidAction)) return false;
        return super.equals(o);
    }
}
