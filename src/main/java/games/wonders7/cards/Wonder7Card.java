package games.wonders7.cards;

import core.components.Card;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7Constants.Resource;
import games.wonders7.Wonders7Constants.TradeSource;
import games.wonders7.Wonders7GameParameters;
import games.wonders7.Wonders7GameState;
import utilities.Pair;

import java.util.*;

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
    public final Map<Resource, Long> constructionCost; // The resources required to construct structure
    public final Map<Resource, Long> resourcesProduced; // Resources the card creates
    //public final HashMap<Wonder7Card, Integer> prerequisite; // THE STRUCTURES REQUIRED TO BUILD CARD FOR FREE
    public final String prerequisiteCard;

    // A normal card with construction cost, produces resources
    public Wonder7Card(String name, Type type,
                       Map<Resource, Long> constructionCost,
                       Map<Resource, Long> resourcesProduced) {
        super(name);
        this.cardName = name;
        this.type = type;
        this.constructionCost = constructionCost;
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = "";
    }

    // Card has prerequisite cards
    public Wonder7Card(String name, Type type,
                       Map<Resource, Long> constructionCost,
                       Map<Resource, Long> resourcesProduced, String prerequisiteCard) {
        super(name);
        this.cardName = name;
        this.type = type;
        this.constructionCost = constructionCost;
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = prerequisiteCard;
    }

    // A free card (no construction cost)
    public Wonder7Card(String name, Type type, Map<Resource, Long> resourcesProduced) {
        super(name);
        this.cardName = name;
        this.type = type;
        this.constructionCost = new HashMap<>(); // Card costs nothing
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = "";
    }

    protected Wonder7Card(String name, Type type,
                          Map<Resource, Long> constructionCost,
                          Map<Resource, Long> resourcesProduced, String prerequisiteCard, int componentID) {
        super(name, componentID);
        this.cardName = name;
        this.type = type;
        this.constructionCost = constructionCost;
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = prerequisiteCard;
    }

    public int getNProduced(Resource resource) {
        return resourcesProduced.get(resource).intValue();
    }

    public int getNCost(Resource resource) {
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

    private String mapToStr(Map<Resource, Long> m) {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<Resource, Long> e : m.entrySet()) {
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

    /**
     * This method checks if the player can play the card.
     * Further, if they can play the card it returns the additional costs they have to pay
     * to other players to acquire their resources
     * This is a List<TradeSource> (TradeSource is a record with fields Resource, cost, fromPlayer) defined in Wonders7Constants
     */
    public Pair<Boolean, List<TradeSource>> isPlayable(int player, Wonders7GameState wgs) {
        if (isAlreadyPlayed(player, wgs))
            return new Pair<>(false, Collections.emptyList()); // If player already has an identical structure (can't play another
        if (isFree(player, wgs)) return new Pair<>(true, Collections.emptyList()); // If player can play for free (has prerequisite card

        // Collects the resources player does not have
        Map<Resource, Long> neededResources = new HashMap<>();
        for (Resource resource : constructionCost.keySet()) { // Goes through every resource the player needs
            if ((wgs.getPlayerResources(player).get(resource)) < constructionCost.get(resource)) { // If the player does not have resource count, added to needed resources
                if (resource == Coin)
                    return new Pair<>(false, Collections.emptyList()); // If player can't afford the card (not enough coins)
                neededResources.put(resource, constructionCost.get(resource) - wgs.getPlayerResources(player).get(resource));
            }
        }
        if (neededResources.isEmpty()) return new Pair<>(true, Collections.emptyList()); // If player can afford the card (no resources needed)
        // at this point we have paid anything for which we have the direct resources
        // Now we consider wild cards, and then purchase options from neighbours

        // we now allocate the cost to buy for each of the needed resources (we do this at the individual level in case we pay for one of a pair
        // and use a wildcard for the other


        // get neighbour resources
        int leftNeighbour = (wgs.getNPlayers() + player - 1) % wgs.getNPlayers();
        int rightNeighbour = (player + 1) % wgs.getNPlayers();
        Map<Resource, Integer> neighbourLResources = wgs.getPlayerResources(leftNeighbour); // Resources available to the neighbour on left
        Map<Resource, Integer> neighbourRResources = wgs.getPlayerResources(rightNeighbour); // Resources available to the neighbour on right

        // now many wild resources do we have
        int basicWild = wgs.getPlayerResources(player).getOrDefault(Resource.BasicWild, 0);
        int rareWild = wgs.getPlayerResources(player).getOrDefault(Resource.RareWild, 0);

        // tradeSources lists all the possible purchases from neighbours, with costs
        List<TradeSource> tradeSources = new ArrayList<>();
        int missingBasic = 0;
        int missingRare = 0;
        for (Resource resource : neededResources.keySet()) {
            if (!resource.isBasic() && !resource.isRare()) {
                throw new AssertionError("Unknown construction resource type: " + resource);
            }
            int available = 0;
            for (int i = 0; i < neighbourLResources.getOrDefault(resource, 0); i++) {
                tradeSources.add(new TradeSource(resource, wgs.costOfResource(resource, player, leftNeighbour), leftNeighbour));
                available++;
            }
            for (int i = 0; i < neighbourRResources.getOrDefault(resource, 0); i++) {
                tradeSources.add(new TradeSource(resource, wgs.costOfResource(resource, player, rightNeighbour), rightNeighbour));
                available++;
            }

            // at this stage we can check to see if construction is impossible
            int wild = resource.isBasic() ? basicWild : rareWild;
            int alreadyMissing = resource.isBasic() ? missingBasic : missingRare;
            if (available + wild + alreadyMissing < neededResources.get(resource)) {
                return new Pair<>(false, Collections.emptyList());  // impossible to get needed resources
            }
            if (available < neededResources.get(resource)) {
                if (resource.isBasic()) {
                    missingBasic += (int) (neededResources.get(resource) - available);
                } else {
                    missingRare += (int) (neededResources.get(resource) - available);
                }
            }
        }
        // update the wild resources to be the spare ones left over after using them for the unavailable resources
        basicWild -= missingBasic;
        rareWild -= missingRare;

        // there are potentially sufficient available resources to build the card, so we
        // check if we can afford to buy the resources we need

        tradeSources.sort(Comparator.comparingInt(TradeSource::cost));

        // now we go through the trade sources in increasing order of cost, and buy the resources we need
        // from the previous step we know how many wild resources we have left over, after using them for the unavailable
        // resources; so we use these on the most expensive resources

        List<TradeSource> used = new ArrayList<>();
        int availableCoins = wgs.getPlayerResources(player).get(Coin);
        for (TradeSource tradeSource : tradeSources) {
            if (neededResources.get(tradeSource.resource()) == 0) continue;  // we have already bought all we need
            if (tradeSource.cost() <= availableCoins) {
                used.add(tradeSource);
                availableCoins -= tradeSource.cost();
            } else {
                if (tradeSource.resource().isBasic() && basicWild > 0) {
                    basicWild--;
                } else if (tradeSource.resource().isRare() && rareWild > 0) {
                    rareWild--;
                } else {
                    return new Pair<>(false, Collections.emptyList());  // can't afford to buy the resources
                }
            }
            // remove the resource from the needed list
            neededResources.put(tradeSource.resource(), neededResources.get(tradeSource.resource()) - 1);
        }

        // we can afford the resources. The last check is to see if we can reduce the price with wild cards
        List<TradeSource> usedCopy = new ArrayList<>(used);
        Collections.reverse(usedCopy);  // sort so most expensive first
        for (TradeSource tradeSource : usedCopy) {
            if (tradeSource.resource().isBasic() && basicWild > 0) {
                basicWild--;
                used.remove(tradeSource);
            } else if (tradeSource.resource().isRare() && rareWild > 0) {
                rareWild--;
                used.remove(tradeSource);
            }
        }
        return new Pair<>(true, used);
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

