package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.toads.ToadConstants;
import games.toads.ToadGameState;


public class ShowCards extends AbstractAction {

    public final ToadConstants.ToadCardType cardNotRevealed;

    public ShowCards() {
        // this will show all cards in hand
        cardNotRevealed = null;
    }

    public ShowCards(ToadConstants.ToadCardType card) {
        cardNotRevealed = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ToadGameState state = (ToadGameState) gs;
        state.seeOpponentsHand(1 - state.getCurrentPlayer(), cardNotRevealed);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ShowCards && ((ShowCards) obj).cardNotRevealed == cardNotRevealed;
    }

    @Override
    public int hashCode() {
        return 43424 - (cardNotRevealed == null ? 0 : cardNotRevealed.ordinal()) * 255;
    }

    @Override
    public String toString() {
        return "Show all cards " + (cardNotRevealed == null ? "" : "except " + cardNotRevealed);
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
