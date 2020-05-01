package games.pandemic.engine.conditions;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import games.pandemic.Constants;
import games.pandemic.PandemicGameState;

@SuppressWarnings("unchecked")
public class PlayerHandOverCapacity extends ConditionNode {
    @Override
    public boolean test(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        int activePlayer = pgs.getActingPlayerID();
        Deck<Card> playerDeck = (Deck<Card>) pgs.getComponent(Constants.playerHandHash, activePlayer);
        return playerDeck != null && playerDeck.isOverCapacity();
    }
}
