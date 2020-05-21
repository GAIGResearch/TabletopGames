package games.explodingkittens.cards;

import core.components.Card;

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
