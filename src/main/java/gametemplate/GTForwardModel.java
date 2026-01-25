package gametemplate;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import gametemplate.actions.GTAction;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>The forward model contains all the game rules and logic. It is mainly responsible for declaring rules for:</p>
 * <ol>
 *     <li>Game setup</li>
 *     <li>Actions available to players in a given game state</li>
 *     <li>Game events or rules applied after a player's action</li>
 *     <li>Game end</li>
 * </ol>
 */
public class GTForwardModel extends StandardForwardModel {

    /**
     * Initializes all variables in the given game state. Performs initial game setup according to game rules, e.g.:
     * <ul>
     *     <li>Sets up decks of cards and shuffles them</li>
     *     <li>Gives player cards</li>
     *     <li>Places tokens on boards</li>
     *     <li>...</li>
     * </ul>
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        // TODO: perform initialization of variables and game setup
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        // TODO: create action classes for the current player in the given game state and add them to the list. Below just an example that does nothing, remove.
        actions.add(new GTAction());
        return actions;
    }

    /**
     * This is a method hook for any game-specific functionality that should run before an Action is executed
     * by the forward model
     *
     * @param currentState - the current game state
     * @param actionChosen - the action chosen by the current player, not yet applied to the game state
     */
    protected void _beforeAction(AbstractGameState currentState, AbstractAction actionChosen) {
        // override if needed
        // TODO: implement any game-specific functionality that should run before an Action is executed
        // TODO: (This is actually quite rare, and if not needed then remove this method)
    }

    /**
     * This is a method hook for any game-specific functionality that should run after an Action is executed
     * by the forward model
     *
     * @param currentState the current game state
     * @param actionTaken  the action taken by the current player, already applied to the game state
     */
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        // TODO: implement any game-specific functionality that should run after an Action is executed
        // TODO: Unlike _beforeAction, this is almost always implemented
        // TODO: This generally does things like checking for end of turn or round or game (and then doing the
        // TODO: appropriate actions).
    }


}
