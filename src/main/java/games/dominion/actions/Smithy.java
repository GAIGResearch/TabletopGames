package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

public class Smithy extends DominionAction {

    public Smithy(int playerId) {
        super(CardType.SMITHY, playerId);
    }

    @Override
    boolean _execute(DominionGameState state) {
        for (int i = 0; i < 3; i++) {
            state.drawCard(state.getCurrentPlayer());
        }
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
        // no state
    }
}
