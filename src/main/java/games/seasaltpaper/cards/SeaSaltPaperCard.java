package games.seasaltpaper.cards;

import core.components.Card;

import java.util.Arrays;
import java.util.Objects;


public class SeaSaltPaperCard extends Card {

    // TODO add collectorBonus/duoBonus/Multiplier here directly

    protected final CardColor color;
    protected final CardSuite cardSuite;

    protected final CardType cardType;

//    protected final int[] collectorBonus;
//    protected final int duoBonus;
//    protected final int multiplierBonus;


    public SeaSaltPaperCard(CardColor color, CardSuite cardSuite, CardType cardType) {
        this.color = color;
        this.cardSuite = cardSuite;
        this.cardType = cardType;
        this.componentName = color + " " + cardSuite + " " + cardType;
//        collectorBonus = new int[]{};
//        duoBonus = 0;
//        multiplierBonus = 0;
    }

    private SeaSaltPaperCard(CardColor color, CardSuite cardSuite, CardType cardType, String componentName, int componentID) {
        super(componentName, componentID);
        this.color = color;
        this.cardSuite = cardSuite;
        this.cardType = cardType;
    }

    // bonus is either duoBonus or multiplierBonus
//    public SeaSaltPaperCard(CardColor color, CardSuite cardSuite, CardType cardType, int bonus) {
//        this.color = color;
//        this.cardSuite = cardSuite;
//        this.cardType = cardType;
//        this.componentName = color + " " + cardSuite + " " + cardType;
//        collectorBonus = null;
//
//        if (cardType == CardType.DUO) {
//            duoBonus = bonus;
//            multiplierBonus = 0;
//        }
//        else {
//            duoBonus = 0;
//            multiplierBonus = bonus;
//        }
//    }

//    public SeaSaltPaperCard(CardColor color, CardSuite cardSuite, CardType cardType, int[] collectorBonus) {
//        this.color = color;
//        this.cardSuite = cardSuite;
//        this.cardType = cardType;
//        this.componentName = color + " " + cardSuite + " " + cardType;
//        this.collectorBonus = collectorBonus;
//        duoBonus = 0;
//        multiplierBonus = 0;
//    }

    @Override
    public String toString() {
        return "{" + color + ", " + cardSuite + ", " + cardType + "}";
    }

    public CardSuite getCardSuite() { return cardSuite;}

    public CardColor getCardColor() { return color;}

    public CardType getCardType() { return cardType;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SeaSaltPaperCard that = (SeaSaltPaperCard) o;
        return color == that.color && cardSuite == that.cardSuite && cardType == that.cardType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), color, cardSuite, cardType);
    }

    @Override
    public SeaSaltPaperCard copy() {
        SeaSaltPaperCard copy =  new SeaSaltPaperCard(color, cardSuite, cardType, componentName, componentID);
        copyComponentTo(copy);
        return copy;
//        return this;
    }

//    public int getDuoBonus() {
//        return duoBonus;
//    }
//
//    public int[] getCollectorBonus() {
//        return collectorBonus;
//    }
//
//    public int getMultiplierBonus(){
//        return multiplierBonus;
//    }
}
