package games.wonders7.cards;

import core.components.Card;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7Constants.Resource;
import games.wonders7.Wonders7Constants.TradeSource;
import games.wonders7.Wonders7GameParameters;
import games.wonders7.Wonders7GameState;
import utilities.Pair;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                (!cost.isEmpty() ? ":cost=" + cost : ",free") +
                (!makes.isEmpty() ? ",makes=" + makes : "") + "}  ";
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
        if (isFree(player, wgs))
            return new Pair<>(true, Collections.emptyList()); // If player can play for free (has prerequisite card

        // Collects the resources player does not have
        List<Resource> remainingRequirements = new ArrayList<>();
        for (Resource resource : constructionCost.keySet()) { // Goes through every resource the player needs
            if ((wgs.getPlayerResources(player).get(resource)) < constructionCost.get(resource)) { // If the player does not have resource count, added to needed resources
                if (resource == Coin)
                    return new Pair<>(false, Collections.emptyList()); // If player can't afford the card (not enough coins)
                for (int i = 0; i < constructionCost.get(resource) - wgs.getPlayerResources(player).get(resource); i++)
                    remainingRequirements.add(resource);
            }
        }
        if (remainingRequirements.isEmpty())
            return new Pair<>(true, Collections.emptyList()); // If player can afford the card (no resources needed)
        // at this point we have paid anything for which we have the direct resources
        // Now we consider composite resources and purchase options from neighbours

        // we include this in a single algorithm to allow for recursive analysis of all trading options
        // our composite resources have a cost of zero, so will always be allocated first.

        List<TradeSource> compositeSources = new ArrayList<>();
        for (Resource resource : wgs.getPlayerResources(player).keySet()) {
            if (resource.isComposite()) {
                for (int i = 0; i < wgs.getPlayerResources(player).get(resource); i++)
                    compositeSources.add(new TradeSource(resource, 0, -1));
            }
        }

        // get neighbour resources (that would help satisfy our requirements)
        int leftNeighbour = (wgs.getNPlayers() + player - 1) % wgs.getNPlayers();
        int rightNeighbour = (player + 1) % wgs.getNPlayers();
        List<TradeSource> leftNeighbourHas = extractNeighbourTradeOptions(player, wgs, remainingRequirements, leftNeighbour);
        List<TradeSource> rightNeighbourHas = extractNeighbourTradeOptions(player, wgs, remainingRequirements, rightNeighbour);

        // Now amalgamate these three sets of possibilities
        List<TradeSource> tradeSources = new ArrayList<>(compositeSources);
        tradeSources.addAll(leftNeighbourHas);
        tradeSources.addAll(rightNeighbourHas);

        // sort in increasing order of cost, with composite resources after non-composite
        tradeSources.sort(Comparator.comparingInt(TradeSource::cost).thenComparingInt(ts -> ts.resource().isComposite() ? 1 : 0));

        // now we go through the trade sources in increasing order of cost, and buy the resources we need
        // a composite resource is applied to all possible requirements...which branches the search so that all possibilities are considered

        List<TradingOption> allPossibleOptions = getTradingOptions(
                new TradingOption(Collections.emptyList(),
                        remainingRequirements,
                        tradeSources,
                        wgs.getPlayerResources(player).get(Coin) - constructionCost.getOrDefault(Coin, 0L).intValue()));

        if (allPossibleOptions.isEmpty()) {
            return new Pair<>(false, Collections.emptyList());  // no way to satisfy the requirements
        }
        // Now find the cheapest option
        TradingOption cheapestOption = allPossibleOptions.stream()
                .min(Comparator.comparingInt(o -> o.plannedPurchases.stream().mapToInt(TradeSource::cost).sum()))
                .get();

        // then remove the resources with a cost of zero
        return new Pair<>(true, cheapestOption.plannedPurchases.stream().filter(ts -> ts.cost() > 0).collect(Collectors.toList()));
    }

    private List<TradeSource> extractNeighbourTradeOptions(int player, Wonders7GameState wgs, List<Resource> neededResources,
                                                           int neighbour) {
        List<TradeSource> tradeSources = new ArrayList<>();
        for (Resource resource : wgs.getPlayerResources(neighbour).keySet()) {
            if (!resource.isTradeable()) continue; // ignore Coins, Victory etc. symbols
            // is this relevant to us?
            if (neededResources.stream().anyMatch(resource::includes)) {
                for (int i = 0; i < wgs.getPlayerResources(neighbour).get(resource); i++)
                    tradeSources.add(new TradeSource(resource, wgs.costOfResource(resource, player, neighbour), neighbour));
            }
        }
        return tradeSources;
    }

    // helper record for recursive analysis of all trading options
    record TradingOption(List<TradeSource> plannedPurchases, List<Resource> remainingRequirements,
                         List<TradeSource> tradeSources, int coinsLeft) {
    }

    private List<TradingOption> getTradingOptions(TradingOption current) {
        // We loop over the remaining tradeSources until we find one that can satisfy a requirement.
        // If it can satisfy a unique requirement, we add it to the plannedPurchases,
        // remove the requirement from the remainingRequirements and keep looping.
        List<TradeSource> usedTradeSources = new ArrayList<>(current.plannedPurchases);
        List<TradeSource> remainingTradeSources = new ArrayList<>(current.tradeSources);
        List<Resource> remainingRequirements = new ArrayList<>(current.remainingRequirements);
        int coinsLeft = current.coinsLeft;
        for (TradeSource tradeSource : current.tradeSources) {
            if (tradeSource.cost() > coinsLeft) {
                // we have run out of money, so this branch is invalid
                return Collections.emptyList();
            }
            Set<Resource> matchingRequirements = remainingRequirements.stream()
                    .filter(tradeSource.resource()::includes)
                    .collect(Collectors.toSet());
            if (matchingRequirements.isEmpty()) {
                // this trade source is not relevant, we remove it and do not branch
                remainingTradeSources.remove(tradeSource);
            } else if (matchingRequirements.size() > 1) {
                // we can use this resource to satisfy multiple requirements
                // so we branch
                List<TradingOption> allOptions = new ArrayList<>();
                for (Resource requirement : matchingRequirements) {
                    List<TradeSource> newPlannedPurchases = new ArrayList<>(usedTradeSources);
                    newPlannedPurchases.add(tradeSource);
                    List<Resource> newRemainingRequirements = new ArrayList<>(remainingRequirements);
                    newRemainingRequirements.remove(requirement);
                    List<TradeSource> newTradeSources = new ArrayList<>(remainingTradeSources);
                    newTradeSources.remove(tradeSource);
                    List<TradingOption> newOptions = getTradingOptions(
                            new TradingOption(newPlannedPurchases, newRemainingRequirements, newTradeSources, coinsLeft - tradeSource.cost()));
                    allOptions.addAll(newOptions);
                }
                return allOptions;
            } else {
                // we can use this resource to satisfy a single requirement, so no branching required
                usedTradeSources.add(tradeSource);
                remainingTradeSources.remove(tradeSource);
                remainingRequirements.remove(matchingRequirements.iterator().next());
                coinsLeft -= tradeSource.cost();
                if (remainingRequirements.isEmpty()) {
                    // we have satisfied all requirements, so this is a valid option
                    return List.of(new TradingOption(usedTradeSources, remainingRequirements, remainingTradeSources, coinsLeft));
                }
            }
        }
        // if we get here, we have exhausted all trade sources without meeting requirements, so this branch is invalid
        return Collections.emptyList();
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

