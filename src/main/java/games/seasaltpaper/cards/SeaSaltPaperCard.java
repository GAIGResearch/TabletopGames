package games.seasaltpaper.cards;

import core.components.Card;


public class SeaSaltPaperCard extends Card {

    protected final CardColor color;
    protected final SuiteType suiteType;

    protected final CardType cardType;

    protected boolean isPlayed = false;

    public SeaSaltPaperCard(CardColor color, SuiteType suiteType, CardType cardType) {
        this.color = color;
        this.suiteType = suiteType;
        this.cardType = cardType;
    }

    @Override
    public String toString() {
        return "{" + color + ", " + suiteType + ", " + cardType + "}";
    }

}
