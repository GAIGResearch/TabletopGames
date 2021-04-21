package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanGameState;

import java.util.ArrayList;

import static core.CoreConstants.playerHandHash;
import static games.catan.CatanConstants.resourceDeckHash;

/* Takes in a list of cards to be discarded from the player's hand*/
public class DiscardCards extends AbstractAction {
    //TODO HASH,Equals,Copy,State
    ArrayList<Card> toBeDiscarded;
    int playerID;

    public DiscardCards(ArrayList<Card> toBeDiscarded, int playerID){
        this.toBeDiscarded = toBeDiscarded;
        this.playerID = playerID;
    }

    public ArrayList<Card> getToBeDiscarded() {
        return toBeDiscarded;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        Deck<Card> playerResourceDeck = (Deck<Card>)cgs.getComponentActingPlayer(playerHandHash);
        Deck<Card> commonResourceDeck = (Deck<Card>)cgs.getComponent(resourceDeckHash);

        for (Card card: toBeDiscarded){
            playerResourceDeck.remove(card);
            commonResourceDeck.add(card);
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
        return new DiscardCards((ArrayList<Card>) toBeDiscarded.clone(), playerID);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof DiscardCards){
            DiscardCards otherAction = (DiscardCards)other;
            return toBeDiscarded.equals(otherAction.toBeDiscarded) && playerID == otherAction.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "DiscardCards cards= " + toBeDiscarded.toString();
    }
}
