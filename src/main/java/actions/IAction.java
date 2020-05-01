package actions;

import core.AbstractGameState;
import turnorder.TurnOrder;

public interface IAction {
    boolean Execute(AbstractGameState gs, TurnOrder turnOrder);
}
