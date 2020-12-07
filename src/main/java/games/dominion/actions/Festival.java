package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

public class Festival extends DominionAction {

    public Festival(int playerId) {
        super(CardType.FESTIVAL, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.changeActions(2);
        state.changeBuys(1);
        state.changeAdditionalSpend(2);
        return true;
    }

    @Override
    public AbstractAction copy() {
        // immutable
        return this;
    }
}
