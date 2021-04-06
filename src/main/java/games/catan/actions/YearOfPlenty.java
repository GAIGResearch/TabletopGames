package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import java.util.List;

import static core.CoreConstants.playerHandHash;
import static games.catan.CatanConstants.resourceDeckHash;

public class YearOfPlenty extends AbstractAction {
    public CatanParameters.Resources resource1;
    public CatanParameters.Resources resource2;
    Card card;

    public YearOfPlenty(CatanParameters.Resources resource1, CatanParameters.Resources resource2, Card card){
        this.resource1 = resource1;
        this.resource2 = resource2;
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        List<Card> playerResourceDeck = ((Deck<Card>)cgs.getComponentActingPlayer(playerHandHash)).getComponents();
        List<Card> commonResourceDeck = ((Deck<Card>)cgs.getComponent(resourceDeckHash)).getComponents();
        Deck<Card> playerDevDeck = (Deck<Card>)cgs.getComponentActingPlayer(CatanConstants.developmentDeckHash);
        Deck<Card> developmentDiscardDeck = (Deck<Card>)cgs.getComponent(CatanConstants.developmentDiscardDeck);
        boolean resource1_done = false;
        boolean resource2_done = false;
        for (int i = 0; i < commonResourceDeck.size(); i++){
            Card card = commonResourceDeck.get(i);
            if( !resource1_done && card.getProperty(CatanConstants.cardType).toString().equals(resource1)){
                playerResourceDeck.add(card);
                commonResourceDeck.remove(i);
                resource1_done = true;
            }
            else if( !resource2_done && card.getProperty(CatanConstants.cardType).toString().equals(resource2)){
                playerResourceDeck.add(card);
                commonResourceDeck.remove(i);
                resource2_done = true;
            }
            if (resource1_done && resource2_done){
                // Knight card gets "discarded" but it remains known in gamestate
                playerDevDeck.remove(this.card);
                developmentDiscardDeck.add(this.card);
                return true;
            }
        }

        return false;
    }

    @Override
    public AbstractAction copy() {
        return new YearOfPlenty(resource1, resource2, card);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof YearOfPlenty){
            YearOfPlenty otherAction = (YearOfPlenty)other;
            return resource1.equals(otherAction.resource1) && resource2.equals(otherAction.resource2) && card.equals(otherAction.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Year of Plenty with resource1 = " + resource1.toString() + " and resource 2 = " + resource2.toString() + " and with card = " + card.toString();
    }
}
