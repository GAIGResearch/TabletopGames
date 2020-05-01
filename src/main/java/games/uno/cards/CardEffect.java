package updated_core.games.uno.cards;

import updated_core.actions.IAction;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

public abstract class CardEffect implements IAction {
    public abstract boolean Execute(AbstractGameState gs, TurnOrder turnOrder);
}
