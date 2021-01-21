package games.catan.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanParameters.Resources;

import java.util.ArrayList;
import java.util.List;

/* Player may trade any 4 resources of the same type of 1 resource of choice with the bank
* This action also includes the Harbor trades using the exchangeRate*/
public class DefaultTrade extends AbstractAction {
    Resources resourceOffer;
    Resources resourceToGet;
    int exchangeRate;

    public DefaultTrade(Resources resourceOffer, Resources resourceToGet, int exchangeRate){
        this.resourceOffer = resourceOffer;
        this.resourceToGet = resourceToGet;
        this.exchangeRate = exchangeRate;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        CatanParameters params = (CatanParameters) cgs.getGameParameters();
        Deck<Card> playerResources = (Deck<Card>)cgs.getComponentActingPlayer(CoreConstants.playerHandHash);
        Deck<Card> resourceDeck = (Deck<Card>)cgs.getComponent(CatanConstants.resourceDeckHash);
        List<Card> cardsToReturn = new ArrayList<>();
        for (Card card: playerResources.getComponents()){
            if (card.getProperty(CatanConstants.cardType).toString().equals(resourceOffer.toString())){
                cardsToReturn.add(card);
            }
        }
        if (cardsToReturn.size() < exchangeRate) return false;
        for (int i = 0; i < exchangeRate; i++){
            Card card = cardsToReturn.get(i);
            playerResources.remove(card);
            resourceDeck.add(card);
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new DefaultTrade(resourceOffer, resourceToGet, exchangeRate);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof DefaultTrade){
            DefaultTrade otherAction = (DefaultTrade)other;
            return resourceOffer == otherAction.resourceOffer && resourceToGet == otherAction.resourceToGet && exchangeRate == otherAction.exchangeRate;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Trade getting 1" + resourceToGet + " in exchange of " +  exchangeRate + " " + resourceOffer;
    }
}
