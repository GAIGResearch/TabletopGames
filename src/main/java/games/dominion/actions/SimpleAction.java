package games.dominion.actions;

import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

public class SimpleAction extends DominionAction {

    public SimpleAction(CardType type, int playerId) {
        super(type, playerId);
    }
    public SimpleAction(CardType type, int playerId, boolean dummy) {
        super(type, playerId, dummy);
    }

    @Override
    public SimpleAction copy() {
        //no  mutable state
        return this;
    }

    @Override
    boolean _execute(DominionGameState state) {
        return true;
    }
}
