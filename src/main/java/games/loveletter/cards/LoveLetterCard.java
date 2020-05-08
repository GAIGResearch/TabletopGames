package games.loveletter.cards;

import core.components.Card;

public class LoveLetterCard extends Card {
    public enum CardType {
        Princess(8),
        Countess(7),
        King(6),
        Prince(5),
        Handmaid(4),
        Baron(3),
        Priest(2),
        Guard(1);

        private int value;
        CardType(int value){
            this.value = value;
        }

        public int getValue(){ return value;}
    }

    public CardType cardType;

    public LoveLetterCard(CardType cardType) {
        this.cardType = cardType;
    }

    public String toString(){
        return cardType.toString();
    }

}
