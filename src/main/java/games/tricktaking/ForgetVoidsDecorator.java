package games.tricktaking;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPlayerDecorator;

import java.util.List;

/**
 * <p>Makes an agent in a trick-taking game forget the suits other players are known to be void in, from having
 * failed to follow suit. Before the agent decides, its observation forgets them and is redeterminised without them,
 * so that the agent's search, and every perspective copy it makes, neither records nor uses voids. The real game
 * state, and other agents, are not affected. It changes no actions.</p>
 *
 * <p>This differs from a game's {@code rememberVoids} parameter, which applies to every player.</p>
 *
 * <p>The game state must implement {@link IForgettableVoids} (so far: Whist). The decorator does nothing in a game
 * with no known voids, and fails in a trick-taking game that does not yet support forgetting them, rather than
 * silently leaving the agent with full knowledge.</p>
 *
 * <p>In a player's JSON:</p>
 * <pre>
 * "actionRestriction" : { "class" : "games.tricktaking.ForgetVoidsDecorator" }
 * </pre>
 */
public class ForgetVoidsDecorator implements IPlayerDecorator {

    @Override
    public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
        // within a search the voids are already forgotten, so this does nothing
        if (state instanceof IForgettableVoids<?> forgettable)
            forgettable.forgetVoids(state.getCurrentPlayer());
        else if (state instanceof ITrickTakingState<?, ?>)
            throw new IllegalStateException(state.getGameType() + " does not support forgetting known voids:"
                    + " its state needs to implement IForgettableVoids");
        return possibleActions;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ForgetVoidsDecorator;
    }

    @Override
    public int hashCode() {
        return 8317;
    }

    @Override
    public String toString() {
        return "ForgetVoids";
    }
}
