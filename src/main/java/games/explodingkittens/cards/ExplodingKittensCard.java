package games.explodingkittens.cards;

import core.components.Card;

public class ExplodingKittensCard extends Card {
    public enum CardType {
        EXPLODING_KITTEN(false),
        DEFUSE (false),
        NOPE (true),
        ATTACK (true),
        SKIP (true),
        FAVOR (true),
        SHUFFLE (true),
        SEETHEFUTURE (true),
        TACOCAT (false),
        MELONCAT (false),
        FURRYCAT (false),
        BEARDCAT (false),
        RAINBOWCAT (false);

        public boolean nopeable;

        CardType(boolean nope) {
            this.nopeable = nope;
        }
    }

    public CardType cardType;

    public ExplodingKittensCard(CardType cardType) {
        super(cardType.toString());
        this.cardType = cardType;
    }

    public ExplodingKittensCard(CardType cardType, int ID) {
        super(cardType.toString(), ID);
        this.cardType = cardType;
    }

    @Override
    public Card copy() {
        return this; // immutable
    }

    @Override
    public String toString() {
        return cardType.name();
    }
}
