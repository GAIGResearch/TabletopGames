package pandemic.engine.conditions;

import components.Deck;
import core.GameState;
import pandemic.Constants;

public class PlayerHandOverCapacity extends ConditionNode {
    @Override
    public boolean test(GameState gs) {
        int activePlayer = gs.getActingPlayer().a;
        Deck playerDeck = (Deck) gs.getAreas().get(activePlayer).getComponent(Constants.playerHandHash);
        return playerDeck != null && playerDeck.isOverCapacity();
    }
}
