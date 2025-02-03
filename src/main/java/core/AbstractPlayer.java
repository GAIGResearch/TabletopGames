package core;

import core.actions.AbstractAction;
import core.interfaces.IPlayerDecorator;
import evaluation.metrics.Event;
import players.PlayerParameters;

import java.util.*;

public abstract class AbstractPlayer {

    // ID of this player, assigned by the game - this is intentionally package-private. Do not change it!
    int playerID;
    String name;
    protected Random rnd = new Random(System.currentTimeMillis());
    // Forward model for the game
    private AbstractForwardModel forwardModel;
    public PlayerParameters parameters;
    protected List<IPlayerDecorator> decorators;

    public AbstractPlayer(PlayerParameters params, String name) {
        this.parameters = params != null ? params : new PlayerParameters();
        this.name = name;
        // We may have one Decorator defined in the Parameters
        // others can then be added by calling addDecorator()
        decorators = new ArrayList<>();
        if (parameters.decorator != null) {
            decorators.add(parameters.decorator);
        }
    }

    /* Final methods */

    /**
     * Retrieves this player's ID, as set by the game.
     *
     * @return - int, player ID
     */
    public final int getPlayerID() {
        return playerID;
    }

    public final void setName(String name) {
        this.name = name;
    }

    /**
     * First of all this applies any decorators to the list of possible actions.
     * Then we choose one (delegating to the _getAction() implemented by the AbstractPlayer subclass)
     * Then we apply any decorators to the chosen action.
     */
    public final AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> observedActions) {
        for (IPlayerDecorator decorator : decorators) {
            observedActions = decorator.actionFilter(gameState, observedActions);
        }
        AbstractAction action = switch (observedActions.size()) {
            case 0 -> throw new AssertionError("No actions available for player " + this);
            case 1 -> observedActions.get(0);
            default -> {
                // we then use our Random for any random choices
                gameState.rnd = this.rnd;
                yield _getAction(gameState, observedActions);
            }
        };
        for (IPlayerDecorator decorator : decorators) {
            decorator.recordDecision(gameState, action);
        }
        return action;


    }

    /**
     * Sets the forward model for the current environment.
     * This is used by Game, and also when an AbstractPlayer is a component of another agent
     * (for example in MCTSPlayer, where the heuristic rollout policy could itself be an AbstractPlayer
     * that needs a copy of the forward model)
     * <p>
     * Anything that has a reference to an AbstractPlayer can therefore pass a ForwardModel.
     * In competition mode this it is not currently possible for opponents to get a link to your
     * object...and this should be avoided.
     *
     * @param model
     */
    public void setForwardModel(AbstractForwardModel model) {
        // TODO: We currently have no way of specifying a Decorator to only apply
        // TODO: to the 'top-level' of getAction(), without also being used in the search algorithm.
        // This could be useful if we want to take random actions at the top level (but not in search)
        this.forwardModel = model;
        for (IPlayerDecorator decorator : decorators) {
            model.addPlayerDecorator(decorator);
        }
    }
    /**
     * Retrieves the forward model for current game being played.
     *
     * @return - ForwardModel
     */
    public final AbstractForwardModel getForwardModel() {
        return forwardModel;
    }

    public final void addDecorator(IPlayerDecorator decorator) {
        decorators.add(decorator);
    }

    public final void clearDecorators() {
        decorators.clear();
    }

    public final void removeDecorator(IPlayerDecorator decorator) {
        decorators.remove(decorator);
    }

    @Override
    public String toString() {
        if (name != null) return name;
        return this.getClass().getSimpleName();
    }

    /* Methods that should be implemented in subclass */

    /**
     * Generate a valid action to play in the game. Valid actions can be found by accessing
     * AbstractGameState.getActions()
     *
     * @param gameState observation of the current game state
     */
    public abstract AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions);

    /* Methods that can be implemented in subclass */

    /**
     * Initialize agent given an observation of the initial game state.
     * e.g. load weights, initialize neural network
     *
     * @param gameState observation of the initial game state
     */
    public void initializePlayer(AbstractGameState gameState) {
    }

    /**
     * Finalize agent given an observation of the final game state.
     * e.g. store variables after training, modify weights, etc.
     *
     * @param gameState observation of the final game state
     */
    public void finalizePlayer(AbstractGameState gameState) {
    }

    /**
     * Receive an updated game state for which it is not required to respond with an action.
     *
     * @param gameState observation of the current game state
     */
    public void registerUpdatedObservation(AbstractGameState gameState) {
    }


    public void onEvent(Event event) {
    }

    public abstract AbstractPlayer copy();

    // override this to provide information on the last decision taken
    public Map<AbstractAction, Map<String, Object>> getDecisionStats() {
        return Collections.emptyMap();
    }

    public PlayerParameters getParameters() {
        return parameters;
    }

    public Random getRnd() {
        return rnd;
    }
}
