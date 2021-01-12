package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

public class Moat extends DominionAction {
    public Moat(int playerId) {
        super(CardType.MOAT, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.drawCard(player);
        state.drawCard(player);
        return true;
    }

    @Override
    public AbstractAction copy() {
        // state is immutable
        return this;
    }

}
