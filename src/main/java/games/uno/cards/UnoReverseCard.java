package games.uno.cards;

import core.AbstractGameState;
import games.uno.UnoGameState;
import turnorder.AlternatingTurnOrder;
import turnorder.TurnOrder;

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
