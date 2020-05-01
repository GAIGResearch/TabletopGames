package games.uno.cards;

import core.AbstractGameState;
import games.uno.UnoGameState;
import turnorder.TurnOrder;


public class UnoSkipCard extends UnoCard {

    public UnoSkipCard(UnoCardColor color, UnoCardType type){
        super(color, type);
    }

    @Override
    public boolean isPlayable(UnoGameState gameState) {
        return false;
    }

    public static class SkipCardEffect extends CardEffect{
        public SkipCardEffect(){};

        @Override
        public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
            turnOrder.endPlayerTurn(gs);
            return true;
        }
    }
}
