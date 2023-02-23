package games.loveletter.actions;

import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

/**
 * The handmaid protects the player from any targeted effects until the next turn.
 */
public class HandmaidAction extends PlayCard implements IPrintable {

    public HandmaidAction(int playerID) {
        super(LoveLetterCard.CardType.Handmaid, playerID, -1, null, null);
    }

    @Override
    protected boolean _execute(LoveLetterGameState gs) {
        // set the player's protection status
        gs.setProtection(playerID, true);
        return true;
    }

    @Override
    public String toString() {
        return "Handmaid (" + playerID + " is protected until their next turn)";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this);
    }

    @Override
    public HandmaidAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HandmaidAction && super.equals(o);
    }
}
