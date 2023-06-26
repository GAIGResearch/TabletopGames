package games.wonders7.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameState;
import games.wonders7.cards.Wonder7Card;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public class BuyResourceL extends DrawCard {

    public String cardName;

    // Player chooses card to play
    public BuyResourceL(String cardName){
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

        // Collects the resources player does not have
        Set<Wonders7Constants.resources> key = card.constructionCost.keySet();
        HashMap<Wonders7Constants.resources, Integer> neededResources = new HashMap<>();
        for (Wonders7Constants.resources resource : key) { // Goes through every resource the player needs
            if ((wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource)) < card.constructionCost.get(resource)) { // If the player does not have resource count, added to needed resources
                neededResources.put(resource, card.constructionCost.get(resource)-wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource));
            }
        }
        // Calculates the cost of resources
        int coinCost=0;
        key = neededResources.keySet();
        for (Wonders7Constants.resources resource : key)
            coinCost += 2*neededResources.get(resource); // For each unit of the resource needed

        // Gives player to the left their money and removes coins from current player
        int currentPlayerCoins = wgs.getPlayerResources(wgs.getCurrentPlayer()).get(Wonders7Constants.resources.coin); // Current players coin count
        int neighbourCoins = wgs.getPlayerResources(Math.floorMod(wgs.getCurrentPlayer()-1, wgs.getNPlayers())).get(Wonders7Constants.resources.coin); // Neighbour's coin count
        wgs.getPlayerResources(wgs.getCurrentPlayer()).put(Wonders7Constants.resources.coin, currentPlayerCoins - coinCost); // Adds the resources provided by the card to the players resource count
        wgs.getPlayerResources(Math.floorMod(wgs.getCurrentPlayer()-1, wgs.getNPlayers())).put(Wonders7Constants.resources.coin, neighbourCoins + coinCost); // Neighbour receives coins for player

        // Gives player resources produced from card
        key = card.resourcesProduced.keySet(); // Gets all the resources the card provides
        for (Wonders7Constants.resources resource: key){  // Goes through all keys for each resource
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
        return "Buy resources for card " + cardName + " from left-hand neighbour " ;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wonder7Card)) return false;
        if (!super.equals(o)) return false;
        BuyResourceL buyResourceL = (BuyResourceL) o;
        return Objects.equals(cardName, buyResourceL.cardName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardName);
    }

    @Override
    public AbstractAction copy(){return new BuyResourceL(cardName);}
}
