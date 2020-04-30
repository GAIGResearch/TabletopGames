package updated_core.games.uno.cards;

import updated_core.games.uno.UnoGameState;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;


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
