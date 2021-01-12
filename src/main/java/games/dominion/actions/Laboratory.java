package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

public class Laboratory extends DominionAction {

    public Laboratory(int playerId) {
        super(CardType.LABORATORY, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        state.drawCard(state.getCurrentPlayer());
        state.drawCard(state.getCurrentPlayer());
        state.changeActions(1);
        return true;
    }

    /**
     * Create a copy of this action, with all of its variables.
     * NO REFERENCES TO COMPONENTS TO BE KEPT IN ACTIONS, PRIMITIVE TYPES ONLY.
     *
     * @return - new AbstractAction object with the same properties.
     */
    @Override
    public AbstractAction copy() {
        return this;
    }

}
