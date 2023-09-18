package core.interfaces;

import core.AbstractGameState;
import core.actions.AbstractAction;

public interface ISubGoal {
    // Previous game state and action that led to current state; can be used to find differences more easily
    boolean isSubGoal(AbstractGameState previousState, AbstractAction action);
}
