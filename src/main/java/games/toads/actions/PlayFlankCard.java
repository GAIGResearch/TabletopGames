package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.toads.components.ToadCard;
import games.toads.ToadGameState;

public class PlayFlankCard extends AbstractAction {

    public final ToadCard card;

    public PlayFlankCard(ToadCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ToadGameState state = (ToadGameState) gs;
        int currentPlayer = state.getCurrentPlayer();
        state.playFlankCard(currentPlayer, card);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayFlankCard && ((PlayFlankCard) obj).card.equals(card);
    }

    @Override
    public int hashCode() {
        return card.hashCode() + 91;
    }

    @Override public String toString() {
        return "Play " + card.getComponentName() + " to the flank";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
