package games.loveletter.actions;

import core.AbstractGameState;
import core.interfaces.IPrintable;

import java.util.Objects;

/**
 * The Countess needs to be discarded in case the player also hold a King or a Prince card.
 * Despite its high value, the Countess has no other effect.
 */
public class DiscardCard extends PlayCard implements IPrintable {

    public DiscardCard(int fromIndex, int playerId) {
        super(fromIndex, playerId);
    }

    @Override
    public String toString(){
        return "Discard card - can't execute effect (" + playerID + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Discard card " + getCard(gameState).toString() + " - can't execute effect (" + playerID + ")";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this);
    }

    @Override
    public DiscardCard copy() {
        return new DiscardCard(fromIndex, playerID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiscardCard)) return false;
        if (!super.equals(o)) return false;
        DiscardCard that = (DiscardCard) o;
        return playerID == that.playerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerID);
    }
}
