package updated_core.actions;

import core.GameState;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

public interface IAction {
    boolean Execute(AbstractGameState gs, TurnOrder turnOrder);
}
