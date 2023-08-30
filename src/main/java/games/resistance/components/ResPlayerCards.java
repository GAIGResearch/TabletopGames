package games.resistance.components;

import core.components.Card;
import core.components.Component;


import java.util.Objects;

public class ResPlayerCards extends Card {
    public enum CardType {
        SPY,
        RESISTANCE,
        LEADER,
        Yes,
        No,
    }

    public CardType cardType;

    public ResPlayerCards(CardType cardType) {
        super(cardType.toString());
        this.cardType = cardType;
    }

    protected ResPlayerCards(CardType cardType, int ID) {
        super(cardType.toString(), ID);
        this.cardType = cardType;
    }

    @Override
    public ResPlayerCards copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResPlayerCards)) return false;
        if (!super.equals(o)) return false;
        ResPlayerCards resCard = (ResPlayerCards) o;
        return cardType == resCard.cardType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardType);
    }

    public String toString() {
        return cardType.name();
    }
}
