package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import java.util.Arrays;
import java.util.Objects;

/* Takes in a list of cards to be discarded from the player's hand*/
public class DiscardCards extends AbstractAction {
    public final CatanParameters.Resource[] cardsToDiscard;
    public final int playerID;

    public DiscardCards(CatanParameters.Resource[] cardsToDiscard, int playerID){
        this.cardsToDiscard = cardsToDiscard;
        this.playerID = playerID;
    }

    public CatanParameters.Resource[] getToBeDiscarded() {
        return cardsToDiscard;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        for (CatanParameters.Resource cardType: cardsToDiscard){
            cgs.getPlayerResources(playerID).get(cardType).decrement();
            cgs.getResourcePool().get(cardType).increment();
        }
        return true;
    }

    @Override
    public DiscardCards copy() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof DiscardCards){
            DiscardCards otherAction = (DiscardCards)other;
            return Arrays.equals(otherAction.cardsToDiscard, cardsToDiscard) && playerID == otherAction.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int returnVal = Objects.hash(playerID);
        return returnVal + 13 * Arrays.hashCode(cardsToDiscard);

    }

    @Override
    public String toString() {
        return "Discard Cards: " + Arrays.toString(cardsToDiscard);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
