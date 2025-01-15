package games.seasaltpaper.cards;

import core.components.Card;
import games.GameType;

import java.util.Arrays;
import java.util.Objects;


public class SeaSaltPaperCard extends Card {

    protected final CardColor color;
    protected final CardSuite cardSuite;

    protected final CardType cardType;

    // Individual visibility (overwrite PartialObservableDeck visibility)
    // Mostly for AI agents
    protected boolean[] visibility = new boolean[GameType.SeaSaltPaper.getMaxPlayers()];


    public SeaSaltPaperCard(CardColor color, CardSuite cardSuite, CardType cardType) {
        this.color = color;
        this.cardSuite = cardSuite;
        this.cardType = cardType;
        this.componentName = color + " " + cardSuite + " " + cardType;
    }

    private SeaSaltPaperCard(CardColor color, CardSuite cardSuite, CardType cardType, String componentName, int componentID) {
        super(componentName, componentID);
        this.color = color;
        this.cardSuite = cardSuite;
        this.cardType = cardType;
    }

    public void setVisible(int playerId, boolean visible) {
        visibility[playerId] = visible;
    }

    public void setVisible(boolean visible) {
        Arrays.fill(visibility, visible);
    }

    public boolean isVisible(int playerId) {
        return visibility[playerId];
    }

    public void copyVisibility(SeaSaltPaperCard c) {
        visibility = c.visibility.clone();
    }

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
        return color == that.color && cardSuite == that.cardSuite && cardType == that.cardType && Arrays.equals(visibility, that.visibility);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), color, cardSuite, cardType);
    }

    @Override
    public SeaSaltPaperCard copy() {
        SeaSaltPaperCard c =  new SeaSaltPaperCard(color, cardSuite, cardType, componentName, componentID);
        copyComponentTo(c);
        return c;
//        return this;
    }
}
