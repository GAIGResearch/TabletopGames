package games.seasaltpaper.cards;

import core.components.Card;

import java.util.Objects;


public class SeaSaltPaperCard extends Card {

    protected final CardColor color;
    protected final CardSuite cardSuite;

    protected final CardType cardType;

    protected boolean isPlayed = false;

    public SeaSaltPaperCard(CardColor color, CardSuite cardSuite, CardType cardType) {
        this.color = color;
        this.cardSuite = cardSuite;
        this.cardType = cardType;
        this.componentName = color + " " + cardSuite + " " + cardType;
    }

    @Override
    public String toString() {
        return "{" + color + ", " + cardSuite + ", " + cardType + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SeaSaltPaperCard that = (SeaSaltPaperCard) o;
        return isPlayed == that.isPlayed && color == that.color && cardSuite == that.cardSuite && cardType == that.cardType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), color, cardSuite, cardType, isPlayed);
    }

    @Override
    public SeaSaltPaperCard copy() {
        return new SeaSaltPaperCard(color, cardSuite, cardType);
    }
}
