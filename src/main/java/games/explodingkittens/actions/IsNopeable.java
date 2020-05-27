package games.explodingkittens.actions;

import core.AbstractGameState;
import core.turnorders.TurnOrder;

public interface IsNopeable {
    boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder);
}
