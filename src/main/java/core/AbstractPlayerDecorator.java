package core;

import core.actions.AbstractAction;

import java.util.List;

public abstract class AbstractPlayerDecorator extends AbstractPlayer {

    protected AbstractPlayer player;

    public AbstractPlayerDecorator(AbstractPlayer player) {
        this.player = player;
    }

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
     * This method may optionally be overridden id the decorator needs to apply logic after the decision is made.
     * It provides the actual decision selected by the underlying AbstractPlayer.
     * @param state
     * @param action
     */
    public void recordDecision(AbstractGameState state, AbstractAction action) {
        // do nothing
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        return player._getAction(gameState, actionFilter(gameState, possibleActions));
    }

}
