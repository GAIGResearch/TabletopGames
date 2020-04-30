package updated_core.games.uno.cards;

import updated_core.games.uno.UnoGameState;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.AlternatingTurnOrder;
import updated_core.turn_order.TurnOrder;

public class UnoReverseCard extends UnoCard {

    public UnoReverseCard(UnoCardColor color, UnoCardType type) {
        super(color, type);
    }

    @Override
    public boolean isPlayable(UnoGameState gameState) {
        return false;
    }

    public static class ReverseCardEffect extends CardEffect{
        public ReverseCardEffect(){};

        @Override
        public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
            ((AlternatingTurnOrder)turnOrder).reverse();
            return true;
        }
    }
}
