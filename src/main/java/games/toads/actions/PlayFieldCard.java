package games.toads.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.toads.components.ToadCard;
import games.toads.ToadGameState;

public class PlayFieldCard extends AbstractAction {

    public final ToadCard card;

    public PlayFieldCard(ToadCard card) {
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ToadGameState state = (ToadGameState) gs;
        int currentPlayer = state.getCurrentPlayer();
        state.playFieldCard(currentPlayer, card);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayFieldCard && ((PlayFieldCard) obj).card.equals(card);
    }

    @Override
    public int hashCode() {
        return card.hashCode() + 91;
    }

    @Override public String toString() {
        return "Play " + card.getComponentName() + " to the field";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
