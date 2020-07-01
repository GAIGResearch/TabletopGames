package games.loveletter.cards;

import core.components.Card;

public class LoveLetterCard extends Card {

    // each card consists of a type and a value. the card type defines the actions available to the player
    public enum CardType {
        Princess(8),
        Countess(7),
        King(6),
        Prince(5),
        Handmaid(4),
        Baron(3),
        Priest(2),
        Guard(1);

        private final int value;
        CardType(int value){
            this.value = value;
        }

        public int getValue(){ return value;}

        public static int getMaxCardValue() { return 8; }
    }

    public final CardType cardType;

    public LoveLetterCard(CardType cardType) {
        super(cardType.toString());
        this.cardType = cardType;
    }

    public String toString(){
        return cardType.toString();
    }

    @Override
    public Card copy() {
        return new LoveLetterCard(cardType);
    }
}
