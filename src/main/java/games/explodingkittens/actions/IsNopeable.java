package games.explodingkittens.actions;

import core.AbstractGameState;
import core.turnorder.TurnOrder;

public interface IsNopeable {
    boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder);
}
