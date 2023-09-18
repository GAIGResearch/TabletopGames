package core.interfaces;

import core.AbstractGameState;
import core.actions.AbstractAction;

public interface ISubGoalIndependent extends ISubGoal {
    default boolean isSubGoal(AbstractGameState previousState, AbstractAction action) {
        return isSubGoal();
    }

    // Independent subgoal predicate
    boolean isSubGoal();
}
