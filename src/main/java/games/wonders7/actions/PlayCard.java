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
    public PlayCard(int player, String cardName, boolean free) {
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
        for (Wonder7Card cardSearch : wgs.getPlayerHand(player).getComponents()) { // Goes through each card in the playerHand
            if (cardName.equals(cardSearch.cardName)) { // If cardName is the one searching for (being played)
                card = cardSearch;
                break;
            }
        }

        if (card == null) {
            throw new AssertionError("Card not found in player hand");
        }

        cardId = card.getComponentID();

        // Removes coins paid for card
        if (!free) {
            // TODO may vary if yellow (commercial) cards played - however these are not yet implemented
            // TODO So all resources cost the same

            // Collects the resources player may not have
            Set<Wonders7Constants.Resource> key = card.constructionCost.keySet();
            int nCostNeighbourResource = ((Wonders7GameParameters) wgs.getGameParameters()).nCostNeighbourResource;
            for (Wonders7Constants.Resource resource : key) { // Goes through every resource the player needs
                if (resource == Coin) {
                    int cardValue = card.getNCost(Coin); // Number of coins the card costs
                    int playerCoins = playerResources.get(Coin); // Number of coins the player owns
                    if (playerCoins < cardValue) {
                        throw new AssertionError("We cannot afford this card so should not be here");
                    }
                    playerResources.put(Coin, playerCoins - cardValue);// Subtracts coins
                } else if ((playerResources.get(resource)) < card.constructionCost.get(resource)) { // If the player does not have resource count, added to needed resources
                    int amountToBuy = card.getNCost(resource) - playerResources.get(resource);
                    int coinCost = nCostNeighbourResource * amountToBuy;
                    if (coinCost > playerResources.get(Coin)) {
                        throw new AssertionError("We cannot afford this card so should not be here");
                    }

                    HashMap<Wonders7Constants.Resource, Integer> neighbourLResources = wgs.getPlayerResources((wgs.getNPlayers() + player - 1) % wgs.getNPlayers()); // Resources available to the neighbour on left
                    HashMap<Wonders7Constants.Resource, Integer> neighbourRResources = wgs.getPlayerResources((player + 1) % wgs.getNPlayers()); // Resources available to the neighbour on right

                    int combined = neighbourLResources.get(resource) + neighbourRResources.get(resource);
                    if (combined < amountToBuy) {
                        throw new AssertionError("We cannot buy the resources for this card so should not be here");
                    }
                    // we buy preferentially from one of the players
                    int randomNumber = wgs.getRnd().nextInt(2); // Randomly chooses which neighbour to buy from
                    HashMap<Wonders7Constants.Resource, Integer> firstPreference = (randomNumber == 0) ? neighbourLResources : neighbourRResources;
                    HashMap<Wonders7Constants.Resource, Integer> secondPreference = (randomNumber == 0) ? neighbourRResources : neighbourLResources;

                    int amountFromFirstPreference = Math.min(firstPreference.get(resource), amountToBuy);
                    if (amountFromFirstPreference > 0) {
                        firstPreference.put(Coin, firstPreference.get(Coin) + nCostNeighbourResource * amountFromFirstPreference); // Neighbour receives coins from player
                        playerResources.put(Coin, playerResources.get(Coin) - amountFromFirstPreference * nCostNeighbourResource); // Player pays coins to neighbour
                    }
                    amountToBuy -= amountFromFirstPreference;
                    if (amountToBuy > 0) {
                        secondPreference.put(Coin, secondPreference.get(Coin) + nCostNeighbourResource * amountToBuy); // Neighbour receives coins from player
                        playerResources.put(Coin, playerResources.get(Coin) - amountToBuy * nCostNeighbourResource); // Player pays coins to neighbour
                    }
                }
            }
        }

        // Gives player resources produced from card
        Set<Wonders7Constants.Resource> keys = card.resourcesProduced.keySet(); // Gets all the resources the card provides
        for (
                Wonders7Constants.Resource resource : keys) {  // Goes through all keys for each resource
            int cardValue = card.getNProduced(resource); // Number of resource the card provides
            int playerValue = playerResources.get(resource); // Number of resource the player owns
            playerResources.put(resource, playerValue + cardValue); // Adds the resources provided by the card to the players resource count
        }

        // remove the card from the players hand to the playedDeck
        boolean cardFound = playerHand.remove(card);
        if (!cardFound) {
            throw new AssertionError("Card not found in player hand");
        }
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
    public PlayCard copy() {
        return this;
    }
}
