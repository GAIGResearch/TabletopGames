package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static core.CoreConstants.playerHandHash;
import static games.catan.CatanConstants.resourceDeckHash;

/* Takes in a list of cards to be discarded from the player's hand*/
public class DiscardCards extends AbstractAction {
    public final CatanParameters.Resources[] cardsToDiscard;
    public final int playerID;

    public DiscardCards(CatanParameters.Resources[] cardsToDiscard, int playerID){
        this.cardsToDiscard = cardsToDiscard;
        this.playerID = playerID;
    }

    public CatanParameters.Resources[] getToBeDiscarded() {
        return cardsToDiscard;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        Deck<Card> playerResourceDeck = (Deck<Card>)cgs.getComponentActingPlayer(playerHandHash);
        Deck<Card> commonResourceDeck = (Deck<Card>)cgs.getComponent(resourceDeckHash);

        for (CatanParameters.Resources cardType: cardsToDiscard){

            Optional<Card> resource = playerResourceDeck.stream()
                    .filter(card -> card.getProperty(CatanConstants.cardType).toString().equals(cardType.toString()))
                    .findFirst();
            if(resource.isPresent()){
                Card resourceCard = resource.get();
                playerResourceDeck.remove(resourceCard);
                commonResourceDeck.add(resourceCard);
            } else {
                throw new AssertionError(String.format("Player cannot dispose of a card they do not possess: %s ",cardType.toString()));
            }
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
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
        return "DiscardCards cards= " + Arrays.toString(cardsToDiscard);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
