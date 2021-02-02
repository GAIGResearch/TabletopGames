package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.CoreConstants.VERBOSE;
import static core.CoreConstants.playerHandHash;
import static games.catan.CatanConstants.resourceDeckHash;

/* Takes in a list of cards to be discarded from the player's hand*/
public class DiscardCards extends AbstractAction {
    ArrayList<Card> tobeDiscarded;
    int playerID;

    public DiscardCards(ArrayList<Card> tobeDiscarded, int playerID){
        this.tobeDiscarded = tobeDiscarded;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        Deck<Card> playerResourceDeck = (Deck<Card>)cgs.getComponentActingPlayer(playerHandHash);
        Deck<Card> commonResourceDeck = (Deck<Card>)cgs.getComponent(resourceDeckHash);

        for (Card card: tobeDiscarded){
            playerResourceDeck.remove(card);
            commonResourceDeck.add(card);
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
        return new DiscardCards((ArrayList<Card>)tobeDiscarded.clone(), playerID);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof DiscardCards){
            DiscardCards otherAction = (DiscardCards)other;
            return tobeDiscarded.equals(otherAction.tobeDiscarded) && playerID == otherAction.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "DiscardCards cards= " + tobeDiscarded.toString();
    }
}
