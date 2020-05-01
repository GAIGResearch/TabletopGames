package games.uno.cards;

import actions.IAction;
import core.AbstractGameState;
import turnorder.TurnOrder;

public abstract class CardEffect implements IAction {
    public abstract boolean Execute(AbstractGameState gs, TurnOrder turnOrder);
}
