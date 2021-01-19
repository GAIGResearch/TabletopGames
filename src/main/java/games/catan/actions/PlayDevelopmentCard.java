package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;

public class PlayDevelopmentCard extends AbstractAction {
    Card card;

    public PlayDevelopmentCard(Card card){
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        // todo
        // Dev card can be either:
        //        KNIGHT_CARD,
        //        PROGRESS_CARD,
        //        VICTORY_POINT_CARD
        return false;
    }

    @Override
    public AbstractAction copy() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return null;
    }
}
