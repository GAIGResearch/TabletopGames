package games.uno.cards;

import core.AbstractGameState;
import core.components.Card;
import games.uno.UnoGameState;


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
        public boolean execute(AbstractGameState gs) {
            gs.getTurnOrder().endPlayerTurn(gs);
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
