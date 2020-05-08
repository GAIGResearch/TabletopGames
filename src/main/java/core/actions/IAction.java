package core.actions;

import core.AbstractGameState;

public interface IAction {
    boolean execute(AbstractGameState gs);
}
