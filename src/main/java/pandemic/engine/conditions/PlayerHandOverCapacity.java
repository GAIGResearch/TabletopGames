package pandemic.engine.conditions;

import components.Deck;
import core.GameState;
import pandemic.Constants;
import pandemic.engine.Node;

public class PlayerHandOverCapacity extends ConditionNode {
    public PlayerHandOverCapacity(Node yes, Node no) {
        super(yes, no);
    }

    @Override
    public boolean test(GameState gs) {
        int activePlayer = gs.getActingPlayer().a;
        Deck playerDeck = (Deck) gs.getAreas().get(activePlayer).getComponent(Constants.playerHandHash);
        return playerDeck != null && playerDeck.isOverCapacity();
    }
}
