package games.uno.cards;

import core.AbstractGameState;
import core.components.Card;
import core.components.Component;
import games.uno.UnoGameState;
import core.turnorder.AlternatingTurnOrder;

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
        public boolean execute(AbstractGameState gs) {
            ((AlternatingTurnOrder)gs.getTurnOrder()).reverse();
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return null;
        }


    }
}
