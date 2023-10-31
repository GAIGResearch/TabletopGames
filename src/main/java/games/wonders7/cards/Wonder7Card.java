package games.wonders7.cards;

import core.components.Card;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameParameters;
import games.wonders7.Wonders7GameState;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static games.wonders7.Wonders7Constants.Resource.Coin;

public class Wonder7Card extends Card {

    public enum Type {
        RawMaterials,
        ManufacturedGoods,
        CivilianStructures,
        ScientificStructures,
        CommercialStructures,
        MilitaryStructures,
        Guilds
    }

    public final Type type;  // Different type of cards, brown cards, grey cards...)
    public final String cardName; // Name of card
    public final Map<Wonders7Constants.Resource, Long> constructionCost; // The resources required to construct structure
    public final Map<Wonders7Constants.Resource, Long> resourcesProduced; // Resources the card creates
    //public final HashMap<Wonder7Card, Integer> prerequisite; // THE STRUCTURES REQUIRED TO BUILD CARD FOR FREE
    public final String prerequisiteCard;

    // A normal card with construction cost, produces resources
    public Wonder7Card(String name, Type type,
                       Map<Wonders7Constants.Resource, Long> constructionCost,
                       Map<Wonders7Constants.Resource, Long> resourcesProduced) {
        super(name);
        this.cardName = name;
        this.type = type;
        this.constructionCost = constructionCost;
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = "";
    }

    // Card has prerequisite cards
    public Wonder7Card(String name, Type type,
                       Map<Wonders7Constants.Resource, Long> constructionCost,
                       Map<Wonders7Constants.Resource, Long> resourcesProduced, String prerequisiteCard) {
        super(name);
        this.cardName = name;
        this.type = type;
        this.constructionCost = constructionCost;
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = prerequisiteCard;
    }

    // A free card (no construction cost)
    public Wonder7Card(String name, Type type, Map<Wonders7Constants.Resource, Long> resourcesProduced) {
        super(name);
        this.cardName = name;
        this.type = type;
        this.constructionCost = new HashMap<>(); // Card costs nothing
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = "";
    }

    protected Wonder7Card(String name, Type type,
                          Map<Wonders7Constants.Resource, Long> constructionCost,
                          Map<Wonders7Constants.Resource, Long> resourcesProduced, String prerequisiteCard, int componentID) {
        super(name, componentID);
        this.cardName = name;
        this.type = type;
        this.constructionCost = constructionCost;
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = prerequisiteCard;
    }

    public int getNProduced(Wonders7Constants.Resource resource) {
        return resourcesProduced.get(resource).intValue();
    }

    public int getNCost(Wonders7Constants.Resource resource) {
        return constructionCost.get(resource).intValue();
    }

    @Override
    public String toString() {
        String cost = mapToStr(constructionCost);
        String makes = mapToStr(resourcesProduced);
        return "{" + cardName +
                "(" + type + ")" +
                (!cost.equals("") ? ":cost=" + cost : ",free") +
                (!makes.equals("") ? ",makes=" + makes : "") + "}  ";
    }

    private String mapToStr(Map<Wonders7Constants.Resource, Long> m) {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<Wonders7Constants.Resource, Long> e : m.entrySet()) {
            if (e.getValue() > 0) s.append(e.getValue()).append(" ").append(e.getKey()).append(",");
        }
        s.append("]");
        if (s.toString().equals("]")) return "";
        return s.toString().replace(",]", "");
    }

    public boolean isFree(int player, Wonders7GameState wgs) {
        // Checks if the player has prerequisite cards and can play for free
        for (Wonder7Card card : wgs.getPlayedCards(player).getComponents()) {
            if (prerequisiteCard.equals(card.cardName)) {
                return true;
            }
        }
        return constructionCost.isEmpty(); // Card is free (no construction cost)
    }

    public boolean isAlreadyPlayed(int player, Wonders7GameState wgs) {
        for (Wonder7Card card : wgs.getPlayedCards(player).getComponents()) {
            if (Objects.equals(card.cardName, cardName)) {
                // Player already has an identical structure, can't play another
                return true;
            }
        }
        return false;
    }

    // Checks if the player can play the card, several conditions must be met
    public boolean isPlayable(int player, Wonders7GameState wgs) {
        if (isAlreadyPlayed(player, wgs))
            return false; // If player already has an identical structure (can't play another
        if (isFree(player, wgs)) return true; // If player can play for free (has prerequisite card

        Wonders7GameParameters params = (Wonders7GameParameters) wgs.getGameParameters();

        // Collects the resources player does not have
        HashMap<Wonders7Constants.Resource, Long> neededResources = new HashMap<>();
        for (Wonders7Constants.Resource resource : constructionCost.keySet()) { // Goes through every resource the player needs
            if ((wgs.getPlayerResources(player).get(resource)) < constructionCost.get(resource)) { // If the player does not have resource count, added to needed resources
                if (resource == Coin)
                    return false; // If player can't afford the card (not enough coins)
                neededResources.put(resource, constructionCost.get(resource) - wgs.getPlayerResources(player).get(resource));
            }
        }
        if (neededResources.isEmpty()) return true; // If player can afford the card (no resources needed)

        // Calculates the cost of resources
        int coinCost = constructionCost.getOrDefault(Coin, 0L).intValue();
        int resourceCost = params.nCostNeighbourResource;
        for (Wonders7Constants.Resource resource : neededResources.keySet())
            coinCost += (int) (resourceCost * neededResources.get(resource)); // For each unit of the resource needed
        if (coinCost > wgs.getPlayerResources(player).get(Coin) )
            return false; // If player can't pay the neighbours for the resources needed

        HashMap<Wonders7Constants.Resource, Integer> neighbourLResources = wgs.getPlayerResources((wgs.getNPlayers() + player - 1) % wgs.getNPlayers()); // Resources available to the neighbour on left
        HashMap<Wonders7Constants.Resource, Integer> neighbourRResources = wgs.getPlayerResources((player + 1) % wgs.getNPlayers()); // Resources available to the neighbour on right

        // Calculates combined resources of neighbour and player
        for (Wonders7Constants.Resource resource : constructionCost.keySet()) { // Goes through every resource provided by the neighbour
            int combined = wgs.getPlayerResources(player).get(resource)
                    + neighbourLResources.get(resource)
                    + neighbourRResources.get(resource);
            if (combined < constructionCost.get(resource))
                return false; // Player can't afford card with bought resources
        }

        return true;
    }

    @Override
    public Card copy() {
        return new Wonder7Card(cardName, type, constructionCost, resourcesProduced, prerequisiteCard, componentID);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Wonder7Card) {
            Wonder7Card card = (Wonder7Card) o;
            return super.equals(o) && card.cardName.equals(cardName) &&
                    card.type == type;
        }
        return false;
    }

    public Type getCardType() {
        return type;
    }


    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardName);
    }
}

