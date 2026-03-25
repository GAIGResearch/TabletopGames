package core.interfaces;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.List;

public interface IPlayerDecorator {

    /**
     * This is the core method to be implemented by all player decorators.
     *
     * It takes the list of possible actions and returns a filtered list of actions. This filtered list
     * is then passed to the underlying AbstractPlayer.
     * @param state
     * @param possibleActions
     * @return
     */
    List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions);

    /**
     * This method needs to apply logic after the decision is made.
     * It provides the actual decision selected by the underlying AbstractPlayer.
     * @param state
     * @param action
     */
    default void recordDecision(AbstractGameState state, AbstractAction action) {
        // do nothing as default....override if needed
    }

    /**
     * this is a marker to indicate if the Decorator only applies to the decision player.
     * The default is to use the same decorator for all players (e.g., when constructing the MCTS Search tree)
     * But we may also want to see the impact of, "We cannot use X; but we will plan for other players to do so".
     */
    default boolean decisionPlayerOnly() {
        return false;
    }

    default void reset() {
        // do nothing by default, but can be overridden if the decorator needs to reset internal state at the start of a new game/move
    }

    /**
     * Override this method to return a copy of the Decorator *iff* it stores state
     */
    default IPlayerDecorator copy() {
        return this;  // default assumption is stateless
    }

}
