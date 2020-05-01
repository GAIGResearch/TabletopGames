package updated_core.games.explodingkittens.actions;

import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

public interface IsNopeable {
    boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder);
}
