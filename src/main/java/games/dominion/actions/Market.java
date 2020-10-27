package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

public class Market extends DominionAction {
    public Market(int playerId) {
        super(CardType.MARKET, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.changeBuys(1);
        state.spend(-1);
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
