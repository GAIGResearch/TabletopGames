package core;

import core.actions.AbstractAction;

import java.util.List;

public interface AbstractPlayerDecorator {

    /**
     * This is the core method to be implemented by all player decorators.
     *
     * It takes the list of possible actions and returns a filtered list of actions. This filtered list
     * is then passed to the underlying AbstractPlayer.
     * @param state
     * @param possibleActions
     * @return
     */
    public abstract List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions);

    /**
     * This method needs to apply logic after the decision is made.
     * It provides the actual decision selected by the underlying AbstractPlayer.
     * @param state
     * @param action
     */
    public void recordDecision(AbstractGameState state, AbstractAction action);

}
