package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

public class Woodcutter extends DominionAction{

    public Woodcutter(int playerId) {
        super(CardType.WOODCUTTER, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.changeBuys(1);
        state.spend(-2);
        return true;
    }

    @Override
    public AbstractAction copy() {
        // immutable object
        return this;
    }
}
