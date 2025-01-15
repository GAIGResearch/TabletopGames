package games.loveletter.cards;

import core.components.Card;

public class LoveLetterCard extends Card {

    public final CardType cardType;

    public LoveLetterCard(CardType cardType) {
        super(cardType.toString());
        this.cardType = cardType;
    }

    public LoveLetterCard(CardType cardType, int componentID) {
        super(cardType.toString(), componentID);
        this.cardType = cardType;
    }

    public String toString(){
        return cardType.toString();
    }

    @Override
    public LoveLetterCard copy() {
        return new LoveLetterCard(cardType, componentID);
    }
}
