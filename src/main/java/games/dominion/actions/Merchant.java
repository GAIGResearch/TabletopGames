package games.dominion.actions;

import core.actions.AbstractAction;
import core.components.Deck;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

public class Merchant extends DominionAction {
    public Merchant(int playerId) {
        super(CardType.MERCHANT, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.drawCard(player);
        state.changeActions(1);
        return true;
    }

    @Override
    public AbstractAction copy() {
        // immutable class
        return this;
    }
}
