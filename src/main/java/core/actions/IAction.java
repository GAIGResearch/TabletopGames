package core.actions;

import core.AbstractGameState;
import core.components.Card;

public interface IAction {
    boolean execute(AbstractGameState gs);
    Card getCard();  // Returns null if no card needed to execute this action.
}
