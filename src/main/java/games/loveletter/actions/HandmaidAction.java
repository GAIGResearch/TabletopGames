package games.loveletter.actions;

import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.CardType;

/**
 * The handmaid protects the player from any targeted effects until the next turn.
 */
public class HandmaidAction extends PlayCard implements IPrintable {

    public HandmaidAction(int cardIdx, int playerID) {
        super(CardType.Handmaid, cardIdx, playerID, -1, null, null, true, true);
    }

    @Override
    protected boolean _execute(LoveLetterGameState gs) {
        // set the player's protection status
        gs.setProtection(playerID, true);
        return true;
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
