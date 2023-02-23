package games.loveletter.actions;

import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

/**
 * The Countess needs to be discarded in case the player also hold a King or a Prince card.
 * Despite its high value, the Countess has no other effect.
 */
public class DiscardCard extends PlayCard implements IPrintable {

    public DiscardCard(LoveLetterCard.CardType cardType, int playerID) {
        super(cardType, playerID, -1, null, null);
    }

    @Override
    public String toString(){
        return "Discard card - no effect (" + playerID + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Discard card " + cardType.name() + " - no effect (" + playerID + ")";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this);
    }

    @Override
    public DiscardCard copy() {
        return this;
    }

    @Override
    protected boolean _execute(LoveLetterGameState llgs) {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DiscardCard && super.equals(o);
    }
}
