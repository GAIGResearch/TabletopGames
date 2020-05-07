package games.uno.cards;

import core.actions.IAction;
import core.AbstractGameState;

public abstract class CardEffect implements IAction {
    public abstract boolean execute(AbstractGameState gs);
}
