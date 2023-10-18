package games.wonders7.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import core.components.Deck;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameParameters;
import games.wonders7.Wonders7GameState;
import games.wonders7.cards.Wonder7Card;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static games.wonders7.Wonders7Constants.Resource.Coin;

public class PlayCard extends DrawCard {

    public final String cardName;
    public final int player;
    public final boolean free;

    // Player chooses card to play
    public PlayCard(int player, String cardName, boolean free){
        super();
        this.cardName = cardName;
        this.player = player;
        this.free = free;
    }


    @Override
    public boolean execute(AbstractGameState gameState) {
        Wonders7GameState wgs = (Wonders7GameState) gameState;

        Deck<Wonder7Card> playerHand = wgs.getPlayerHand(player);
        HashMap<Wonders7Constants.Resource, Integer> playerResources = wgs.getPlayerResources(player);

        // Finds the played card
        Wonder7Card card = null;
        for (Wonder7Card cardSearch: wgs.getPlayerHand(player).getComponents()){ // Goes through each card in the playerHand
            if (cardName.equals(cardSearch.cardName)){ // If cardName is the one searching for (being played)
                card = cardSearch;
                break;
            }
        }

        if (card == null) {
            throw new AssertionError("Card not found in player hand");
        }

        cardId = card.getComponentID();

        // Removes coins paid for card
        if (!free && card.constructionCost.get(Coin) != null) {
            int cardValue = card.getNCost(Coin); // Number of coins the card costs
            int playerCoins = playerResources.get(Coin); // Number of coins the player owns
            playerResources.put(Coin, playerCoins - cardValue);// Subtracts coins
        }

        // Collects the resources player may not have
        Set<Wonders7Constants.Resource> key = card.constructionCost.keySet();
        HashMap<Wonders7Constants.Resource, Integer> neededResources = new HashMap<>();
        int coinCost = 0;
        int nCostNeighbourResource = ((Wonders7GameParameters)wgs.getGameParameters()).nCostNeighbourResource;  // TODO may vary if yellow cards played
        for (Wonders7Constants.Resource resource : key) { // Goes through every resource the player needs
            if ((playerResources.get(resource)) < card.constructionCost.get(resource)) { // If the player does not have resource count, added to needed resources
                neededResources.put(resource, card.getNCost(resource)-playerResources.get(resource));
                coinCost += nCostNeighbourResource * neededResources.get(resource); // For each unit of the resource needed
            }
        }
        if (!neededResources.isEmpty()) {
            HashMap<Wonders7Constants.Resource, Integer> neighbourLResources = wgs.getPlayerResources((wgs.getNPlayers()+player-1)%wgs.getNPlayers()); // Resources available to the neighbour on left
            HashMap<Wonders7Constants.Resource, Integer> neighbourRResources = wgs.getPlayerResources((player+1)%wgs.getNPlayers()); // Resources available to the neighbour on right

            // Calculates combined resources of neighbour and player
            Set<Wonders7Constants.Resource> extendedChoice = new HashSet<>();
            for (Wonders7Constants.Resource resource : neededResources.keySet()) { // Goes through every resource provided by the neighbour
                boolean leftHasResource = neighbourLResources.get(resource) != null && neighbourLResources.get(resource) > 0;
                boolean rightHasResource = neighbourRResources.get(resource) != null && neighbourRResources.get(resource) > 0;
                if (leftHasResource && rightHasResource) {
                    // Both neighbours have this resource, may have to choose which neighbour to buy from.
                    // But, check if we don't need to buy all from both neighbours.
                    int combined = wgs.getPlayerResources(player).get(resource)
                            + neighbourLResources.get(resource)
                            + neighbourRResources.get(resource);
                    if (combined == card.constructionCost.get(resource)) {
                        // No choice, we need to buy all from both neighbours.
                        int neighbourLCoins = neighbourLResources.get(Coin); // Neighbour's coin count
                        neighbourLResources.put(Coin, neighbourLCoins + nCostNeighbourResource * neighbourLResources.get(resource)); // Neighbour receives coins from player
                        int neighbourRCoins = neighbourRResources.get(Coin); // Neighbour's coin count
                        neighbourRResources.put(Coin, neighbourRCoins + nCostNeighbourResource * neighbourRResources.get(resource)); // Neighbour receives coins from player

                        playerResources.put(Coin, playerResources.get(Coin) - coinCost); // Player pays coins to neighbour
                        continue;
                    }
                    extendedChoice.add(resource);
                } else {
                    // Only one neighbour has this resource, we need to buy all from them.
                    if (leftHasResource) {
                        int neighbourCoins = neighbourLResources.get(Coin);
                        neighbourLResources.put(Coin, neighbourCoins + coinCost); // Neighbour receives coins from player
                    } else {
                        int neighbourCoins = neighbourRResources.get(Coin);
                        neighbourRResources.put(Coin, neighbourCoins + coinCost); // Neighbour receives coins from player
                    }
                    playerResources.put(Coin, playerResources.get(Coin) - coinCost); // Player pays coins to neighbour
                }
            }
            if (!extendedChoice.isEmpty()) {
                // The player has a choice as to how to distribute payment
//                wgs.setActionInProgress(this);  // TODO set this up for distribution of resources to neighbours
                return false;
            }

            // Gives player to the right their money and removes coins from current player
            int currentPlayerCoins = playerResources.get(Wonders7Constants.Resource.Coin); // Current players coin count
            int neighbourCoins = wgs.getPlayerResources((player + 1) % wgs.getNPlayers()).get(Wonders7Constants.Resource.Coin); // Neighbour's coin count
            playerResources.put(Wonders7Constants.Resource.Coin, currentPlayerCoins - coinCost); // Player pays coins to neighbour
            wgs.getPlayerResources((player + 1) % wgs.getNPlayers()).put(Wonders7Constants.Resource.Coin, neighbourCoins + coinCost); // Neighbour receives coins for player
        }

        // Gives player resources produced from card
        Set<Wonders7Constants.Resource> keys = card.resourcesProduced.keySet(); // Gets all the resources the card provides
        for (Wonders7Constants.Resource resource: keys){  // Goes through all keys for each resource
            int cardValue = card.getNProduced(resource); // Number of resource the card provides
            int playerValue = playerResources.get(resource); // Number of resource the player owns
            playerResources.put(resource, playerValue + cardValue); // Adds the resources provided by the card to the players resource count
        }

        // remove the card from the players hand to the playedDeck
        playerHand.remove(card);
        wgs.getPlayedCards(player).add(card);
        return true;
    }

    @Override
    public String toString() {
        return "Player " + player + " played card " + cardName + (free ? " (free)" : "");
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
        return player == playCard.player && free == playCard.free && Objects.equals(cardName, playCard.cardName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardName, player, free);
    }

    @Override
    public PlayCard copy(){ return this; }
}
