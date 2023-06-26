package games.wonders7.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameState;
import games.wonders7.cards.Wonder7Card;

import java.util.Objects;
import java.util.Set;

public class PlayCard extends DrawCard {

    public String cardName;

    // Player chooses card to play
    public PlayCard(String cardName){
        super();
        this.cardName = cardName;

    }


    @Override
    public boolean execute(AbstractGameState gameState) {
        super.execute(gameState);
        Wonders7GameState wgs = (Wonders7GameState) gameState;

        // Finds the played card
        int index=0; // The index of the card in hand
        for (int i=0; i<wgs.getPlayerHand(wgs.getCurrentPlayer()).getSize(); i++){ // Goes through each card in the playerHand
            if (cardName.equals(wgs.getPlayerHand(wgs.getCurrentPlayer()).get(i).cardName)){ // If cardName is the one searching for (being played)
                index = i;
            }
        }
        Wonder7Card card = wgs.getPlayerHand(wgs.getCurrentPlayer()).get(index); // Card being selected

        // Removes coins paid for card
        if (card.constructionCost.get(Wonders7Constants.Resource.Coin)!=null) {
            int cardValue = card.constructionCost.get(Wonders7Constants.Resource.Coin); // Number of coins the card costs
            int playerValue = wgs.getPlayerResources(wgs.getCurrentPlayer()).get(Wonders7Constants.Resource.Coin); // Number of coins the player owns
            wgs.getPlayerResources(wgs.getCurrentPlayer()).put(Wonders7Constants.Resource.Coin, playerValue - cardValue);// Subtracts coins
        }

        // Gives player resources produced from card
        Set<Wonders7Constants.Resource> keys = card.resourcesProduced.keySet(); // Gets all the resources the card provides
        for (Wonders7Constants.Resource resource: keys){  // Goes through all keys for each resource
            int cardValue = card.resourcesProduced.get(resource); // Number of resource the card provides
            int playerValue = wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource); // Number of resource the player owns
            wgs.getPlayerResources(wgs.getCurrentPlayer()).put(resource, playerValue + cardValue); // Adds the resources provided by the card to the players resource count
        }

        // remove the card from the players hand to the playedDeck
        wgs.getPlayerHand(wgs.getCurrentPlayer()).remove(card);
        wgs.getPlayedCards(wgs.getCurrentPlayer()).add(card);
        return true;
    }

    @Override
    public String toString() {
        return "Played card " + cardName;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayCard)) return false;
        if (!super.equals(o)) return false;
        PlayCard playCard = (PlayCard) o;
        return Objects.equals(cardName, playCard.cardName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardName);
    }

    @Override
    public AbstractAction copy(){return new PlayCard(cardName);}
}
