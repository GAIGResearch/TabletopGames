package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;

/**
 * The Countess needs to be discarded in case the player also hold a King or a Prince card.
 * Despite its high value, the Countess has no other effect.
 */
public class CountessAction extends PlayCard implements IPrintable {
    final LoveLetterCard.CardType cardTypeForce;

    public CountessAction(int fromIndex, int playerID, LoveLetterCard.CardType cardType) {
        super(fromIndex, playerID);
        this.cardTypeForce = cardType;
    }

    @Override
    public String toString(){
        if (cardTypeForce == null) return "Countess (no effect)";
        return "Countess (auto discard with " + cardTypeForce + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this);
    }

    @Override
    public AbstractAction copy() {
        return new CountessAction(fromIndex, playerID, cardTypeForce);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CountessAction)) return false;
        if (!super.equals(o)) return false;
        CountessAction that = (CountessAction) o;
        return playerID == that.playerID && cardTypeForce == that.cardTypeForce;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerID, cardTypeForce);
    }
}
