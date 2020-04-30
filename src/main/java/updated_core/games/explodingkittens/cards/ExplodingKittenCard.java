package updated_core.games.explodingkittens.cards;

import components.Card;
//import explodingkittens.ExplodingKittensCardTypeProperty;

public class ExplodingKittenCard extends Card {
    public enum CardType {
        EXPLODING_KITTEN,
        DEFUSE,
        NOPE,
        ATTACK,
        SKIP,
        FAVOR,
        SHUFFLE,
        SEETHEFUTURE,
        TACOCAT,
        MELONCAT,
        FURRYCAT,
        BEARDCAT,
        RAINBOWCAT,
    }

    public CardType cardType;

    public ExplodingKittenCard(CardType cardType) {
        this.cardType = cardType;
    }


}
