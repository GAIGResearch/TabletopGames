package core.actions;

import core.AbstractGameState;
import core.components.Card;

public interface IAction {

    /**
     * Executes this action, applying its effect to the given game state.
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    boolean execute(AbstractGameState gs);

    /**
     * Returns the card used to play this action. Null if no card was needed (as default).
     * @return - Card, to be discarded.
     */
    Card getCard();
}
