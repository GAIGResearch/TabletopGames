package games.terraformingmars;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import core.interfaces.IGamePhase;
import games.terraformingmars.actions.PlaceTile;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.*;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.effects.Bonus;
import games.terraformingmars.rules.effects.Effect;
import games.terraformingmars.rules.requirements.ActionTypeRequirement;
import games.terraformingmars.rules.requirements.Requirement;
import games.terraformingmars.rules.requirements.TagsPlayedRequirement;
import utilities.Utils;
import utilities.Vector2D;

import java.util.*;

public class TMGameState extends AbstractGameState {

    enum TMPhase implements IGamePhase {
        CorporationSelect,
        Research,
        Actions,
        Production
    }

    // General state info
    int generation;
    GridBoard<TMMapTile> board;
    HashSet<TMMapTile> extraTiles;
    HashMap<TMTypes.GlobalParameter, Counter> globalParameters;
    HashSet<Bonus> bonuses;
    Deck<TMCard> projectCards, corpCards, discardCards;  // Face-down decks

    // Effects and actions played
    HashSet<TMAction>[] playerExtraActions;
    HashSet<ResourceMapping>[] playerResourceMap;  // Effects for turning one resource into another
    HashMap<Requirement, Integer>[] playerDiscountEffects;
    HashSet<Effect>[] playerPersistingEffects;

    // Player-specific counters
    HashMap<TMTypes.Resource, Counter>[] playerResources;
    HashMap<TMTypes.Resource, Boolean>[] playerResourceIncreaseGen;  // True if this resource was increased this gen
    HashMap<TMTypes.Resource, Counter>[] playerProduction;
    HashMap<TMTypes.Tag, Counter>[] playerCardsPlayedTags;
    HashMap<TMTypes.CardType, Counter>[] playerCardsPlayedTypes;
    HashMap<TMTypes.Tile, Counter>[] playerTilesPlaced;
    Counter[] playerCardPoints;  // Points gathered by playing cards

    // Player cards
    Deck<TMCard>[] playerHands;
    Deck<TMCard>[] playerComplicatedPointCards;  // Cards played that can gather resources
    Deck<TMCard>[] playedCards;  // Cards played that can gather resources
    Deck<TMCard>[] playerCardChoice;
    TMCard[] playerCorporations;

    // Milestones and awards TODO: components?
    HashSet<Milestone> milestones;
    HashSet<Award> awards;
    Counter nMilestonesClaimed;
    Counter nAwardsFunded;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     */
    public TMGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new TMTurnOrder(nPlayers, ((TMGameParameters)gameParameters).nActionsPerPlayer));
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(board);
            add(projectCards);
            add(corpCards);
            add(nAwardsFunded);
            add(nMilestonesClaimed);
            this.addAll(extraTiles);
            this.addAll(globalParameters.values());
            this.addAll(Arrays.asList(playerHands));
            this.addAll(Arrays.asList(playerCardChoice));
            this.addAll(Arrays.asList(playerComplicatedPointCards));
            this.addAll(Arrays.asList(playedCards));
            this.addAll(Arrays.asList(playerCardPoints));
            for (int i = 0; i < getNPlayers(); i++) {
                addAll(playerResources[i].values());
                addAll(playerProduction[i].values());
                addAll(playerCardsPlayedTags[i].values());
                addAll(playerTilesPlaced[i].values());
                addAll(playerCardsPlayedTypes[i].values());
                if (playerCorporations[i] != null) {
                    add(playerCorporations[i]);
                }
            }
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
//        TMGameState copy = new TMGameState(gameParameters, getNPlayers());  // TODO

        return this;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        return playerResources[playerId].get(TMTypes.Resource.TR).getValue();
//        return countPoints(playerId);
    }

    @Override
    protected void _reset() {

    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TMGameState)) return false;
        if (!super.equals(o)) return false;
        TMGameState that = (TMGameState) o;
        return generation == that.generation && Objects.equals(board, that.board) && Objects.equals(extraTiles, that.extraTiles) && Objects.equals(globalParameters, that.globalParameters) && Objects.equals(bonuses, that.bonuses) && Objects.equals(projectCards, that.projectCards) && Objects.equals(corpCards, that.corpCards) && Objects.equals(discardCards, that.discardCards) && Arrays.equals(playerExtraActions, that.playerExtraActions) && Arrays.equals(playerResourceMap, that.playerResourceMap) && Arrays.equals(playerDiscountEffects, that.playerDiscountEffects) && Arrays.equals(playerPersistingEffects, that.playerPersistingEffects) && Arrays.equals(playerResources, that.playerResources) && Arrays.equals(playerResourceIncreaseGen, that.playerResourceIncreaseGen) && Arrays.equals(playerProduction, that.playerProduction) && Arrays.equals(playerCardsPlayedTags, that.playerCardsPlayedTags) && Arrays.equals(playerCardsPlayedTypes, that.playerCardsPlayedTypes) && Arrays.equals(playerTilesPlaced, that.playerTilesPlaced) && Arrays.equals(playerCardPoints, that.playerCardPoints) && Arrays.equals(playerHands, that.playerHands) && Arrays.equals(playerComplicatedPointCards, that.playerComplicatedPointCards) && Arrays.equals(playerCardChoice, that.playerCardChoice) && Arrays.equals(playerCorporations, that.playerCorporations) && Objects.equals(milestones, that.milestones) && Objects.equals(awards, that.awards) && Objects.equals(nMilestonesClaimed, that.nMilestonesClaimed) && Objects.equals(nAwardsFunded, that.nAwardsFunded);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), generation, board, extraTiles, globalParameters, bonuses, projectCards, corpCards, discardCards, milestones, awards, nMilestonesClaimed, nAwardsFunded);
        result = 31 * result + Arrays.hashCode(playerExtraActions);
        result = 31 * result + Arrays.hashCode(playerResourceMap);
        result = 31 * result + Arrays.hashCode(playerDiscountEffects);
        result = 31 * result + Arrays.hashCode(playerPersistingEffects);
        result = 31 * result + Arrays.hashCode(playerResources);
        result = 31 * result + Arrays.hashCode(playerResourceIncreaseGen);
        result = 31 * result + Arrays.hashCode(playerProduction);
        result = 31 * result + Arrays.hashCode(playerCardsPlayedTags);
        result = 31 * result + Arrays.hashCode(playerCardsPlayedTypes);
        result = 31 * result + Arrays.hashCode(playerTilesPlaced);
        result = 31 * result + Arrays.hashCode(playerCardPoints);
        result = 31 * result + Arrays.hashCode(playerHands);
        result = 31 * result + Arrays.hashCode(playerComplicatedPointCards);
        result = 31 * result + Arrays.hashCode(playerCardChoice);
        result = 31 * result + Arrays.hashCode(playerCorporations);
        return result;
    }

    /*
     * Public API
     */

    public HashMap<TMTypes.Resource, Counter>[] getPlayerProduction() {
        return playerProduction;
    }

    public HashMap<TMTypes.Resource, Counter>[] getPlayerResources() {
        return playerResources;
    }

    public GridBoard<TMMapTile> getBoard() {
        return board;
    }

    public HashSet<Bonus> getBonuses() {
        return bonuses;
    }

    public HashMap<TMTypes.GlobalParameter, Counter> getGlobalParameters() {
        return globalParameters;
    }

    public HashSet<TMMapTile> getExtraTiles() {
        return extraTiles;
    }

    public Deck<TMCard>[] getPlayerHands() {
        return playerHands;
    }

    public HashMap<TMTypes.Tag, Counter>[] getPlayerCardsPlayedTags() {
        return playerCardsPlayedTags;
    }

    public HashMap<TMTypes.CardType, Counter>[] getPlayerCardsPlayedTypes() {
        return playerCardsPlayedTypes;
    }

    public HashSet<TMAction>[] getPlayerExtraActions() {
        return playerExtraActions;
    }

    public HashMap<TMTypes.Tile, Counter>[] getPlayerTilesPlaced() {
        return playerTilesPlaced;
    }

    public HashSet<Milestone> getMilestones() {
        return milestones;
    }

    public HashSet<Award> getAwards() {
        return awards;
    }

    public TMCard[] getPlayerCorporations() {
        return playerCorporations;
    }

    public boolean allCorpChosen() {
        for (int i = 0; i < getNPlayers(); i++) {
            if (playerCorporations[i] == null) return false;
        }
        return true;
    }

    public Deck<TMCard>[] getPlayerCardChoice() {
        return playerCardChoice;
    }

    public Deck<TMCard> getDiscardCards() {
        return discardCards;
    }

    public Deck<TMCard> getCorpCards() {
        return corpCards;
    }

    public Deck<TMCard> getProjectCards() {
        return projectCards;
    }

    public HashSet<ResourceMapping>[] getPlayerResourceMap() {
        return playerResourceMap;
    }

    public Counter getnAwardsFunded() {
        return nAwardsFunded;
    }

    public Counter getnMilestonesClaimed() {
        return nMilestonesClaimed;
    }

    public int getGeneration() {
        return generation;
    }

    public HashMap<TMTypes.Resource, Boolean>[] getPlayerResourceIncreaseGen() {
        return playerResourceIncreaseGen;
    }

    public HashSet<Effect>[] getPlayerPersistingEffects() {
        return playerPersistingEffects;
    }

    public HashMap<Requirement, Integer>[] getPlayerDiscountEffects() {
        return playerDiscountEffects;
    }

    public Deck<TMCard>[] getPlayerComplicatedPointCards() {
        return playerComplicatedPointCards;
    }

    public Counter[] getPlayerCardPoints() {
        return playerCardPoints;
    }

    public Deck<TMCard>[] getPlayedCards() {
        return playedCards;
    }

    public int discountActionTypeCost(TMAction action, int player) {
        // Apply tag discount effects
        int discount = 0;
        if (player == -1) player = getCurrentPlayer();
        for (Map.Entry<Requirement,Integer> e: playerDiscountEffects[player].entrySet()) {
            if (e.getKey() instanceof ActionTypeRequirement) {
                if (e.getKey().testCondition(action)) {
                    discount += e.getValue();
                }
            }
        }
        return discount;
    }

    public int discountCardCost(TMCard card, int player) {
        // Apply tag discount effects
        int discount = 0;
        if (player == -1) player = getCurrentPlayer();
        for (TMTypes.Tag t: card.tags) {
            for (Map.Entry<Requirement,Integer> e: playerDiscountEffects[player].entrySet()) {
                if (e.getKey() instanceof TagsPlayedRequirement) {
                    boolean found = false;
                    for (TMTypes.Tag tt: ((TagsPlayedRequirement) e.getKey()).tags) {
                        if (tt == t) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        discount += e.getValue();
                    }
                }
            }
        }
        return discount;
    }

    public boolean isCardFree(TMCard card, int player) {
        return isCardFree(card, 0, player);
    }

    public boolean isCardFree(TMCard card, int amountPaid, int player) {
        return card.cost - discountCardCost(card, player) - amountPaid <= 0;
    }

    public Counter stringToGPCounter(String s) {
        TMTypes.GlobalParameter p = Utils.searchEnum(TMTypes.GlobalParameter.class, s);
        if (p != null) return globalParameters.get(p);
        return null;
    }

    public Counter stringToGPOrPlayerResCounter(String s, int player) {
        if (player == -1) player = getCurrentPlayer();
        Counter which = stringToGPCounter(s);

        if (which == null) {
            // A resource or production instead
            TMTypes.Resource res = TMTypes.Resource.valueOf(s.split("prod")[0]);
            if (s.contains("prod")) {
                which = playerProduction[player].get(res);
            } else {
                which = playerResources[player].get(res);
            }
        }
        return which;
    }

    public static TMTypes.GlobalParameter counterToGP(Counter c) {
        return Utils.searchEnum(TMTypes.GlobalParameter.class, c.getComponentName());
    }

    public boolean canPlayerPay(int player, TMTypes.Resource res, int amount) {
        // Production check
        return canPlayerPay(player, null, null, res, amount, true);
    }

    public boolean canPlayerPay(int player, TMCard card, HashSet<TMTypes.Resource> from, TMTypes.Resource to, int amount) {
        return canPlayerPay(player, card, from, to, amount, false);
    }

    public boolean canPlayerPay(int player, TMCard card, HashSet<TMTypes.Resource> from, TMTypes.Resource to, int amount, boolean production) {
        if (production) return playerProduction[player].get(to).getValue() >= amount;

        int sum = playerResourceSum(player, card, from, to);
        return card != null? isCardFree(card, sum, -1) : sum >= amount;
    }

    public int playerResourceSum(int player, TMCard card, HashSet<TMTypes.Resource> from, TMTypes.Resource to) {
        if (from == null || from.size() > 0) {
            int sum = playerResources[player].get(to).getValue();  // All resources can be exchanged for themselves at rate 1.0

            // Add resources that this player can use as the "to" resource for this action
            for (ResourceMapping resMap : playerResourceMap[player]) {
                if ((from == null || from.contains(resMap.from))
                        && resMap.to == to
                        && (resMap.requirement == null || resMap.requirement.testCondition(card))) {
                    int n = playerResources[player].get(resMap.from).getValue();
                    sum += n * resMap.rate;
                }
            }
            return sum;
        }
        return 0;
    }

    /**
     * Check if player can transform one resource into another, when buying a card
     * @param card - card to buy; can be null, and resource mappings that require card tags will be skipped
     * @param from - resource to transform from; can be null, then all resources in the player's mapping will be checked
     * @param to - resource to transform to
     * @return all resources that can be transformed into given res
     */
    public HashSet<TMTypes.Resource> canPlayerTransform(int player, TMCard card, TMTypes.Resource from, TMTypes.Resource to) {
        HashSet<TMTypes.Resource> resources = new HashSet<>();
        for (ResourceMapping resMap : playerResourceMap[player]) {
            if ((from == null || resMap.from == from) && resMap.to == to && (resMap.requirement == null || resMap.requirement.testCondition(card))) {
                if (playerResources[player].get(resMap.from).getValue() > 0) {
                    resources.add(resMap.from);
                }
            }
        }
        return resources;
    }

    public void playerPay(int player, TMTypes.Resource resource, int amount) {
        playerResources[player].get(resource).decrement(Math.abs(amount));
    }

    public double getResourceMapRate(TMTypes.Resource from, TMTypes.Resource to) {
        double rate = 1.;
        for (ResourceMapping rm: playerResourceMap[getCurrentPlayer()]) {
            if (rm.from == from && rm.to == to) {
                rate = rm.rate;
                break;
            }
        }
        return rate;
    }

    public void addDiscountEffects(HashMap<Requirement, Integer> effects) {
        int player = getCurrentPlayer();
        for (Requirement r: effects.keySet()) {
            if (playerDiscountEffects[player].containsKey(r)) {
                playerDiscountEffects[player].put(r, playerDiscountEffects[player].get(r) + effects.get(r));
            } else {
                playerDiscountEffects[player].put(r, effects.get(r));
            }
        }
    }

    public void addPersistingEffects(Effect[] effects) {
        int player = getCurrentPlayer();
        playerPersistingEffects[player].addAll(Arrays.asList(effects));
    }

    // if add is false, replace instead
    public void addResourceMappings(HashSet<ResourceMapping> maps, boolean add) {
        int player = getCurrentPlayer();
        HashSet<ResourceMapping> toRemove = new HashSet<>();
        HashSet<ResourceMapping> toAdd = new HashSet<>();
        for (ResourceMapping resMapNew : maps) {
            boolean added = false;
            for (ResourceMapping resMap : playerResourceMap[player]) {
                if (resMap.from == resMapNew.from && resMap.to == resMapNew.to) {
                    if (resMapNew.requirement == null || resMapNew.requirement.equals(resMap.requirement)) {
                        if (add) {
                            resMap.rate += resMapNew.rate;
                        } else {
                            toRemove.add(resMap);
                            toAdd.add(resMapNew);
                        }
                        added = true;
                    }
                }
            }
            if (!added) toAdd.add(resMapNew);
        }
        playerResourceMap[player].removeAll(toRemove);
        playerResourceMap[player].addAll(toAdd);
    }

    public boolean hasPlacedTile(int player) {
        for (TMTypes.Tile t: playerTilesPlaced[player].keySet()) {
            if (t.canBeOwned() && playerTilesPlaced[player].get(t).getValue() > 0) return true;
        }
        return false;
    }

    public boolean anyTilesPlaced() {
        for (int i = 0; i < getNPlayers(); i++) {
            for (Counter c : playerTilesPlaced[i].values()) {
                if (c.getValue() > 0) return true;
            }
        }
        return false;
    }

    public boolean anyTilesPlaced(TMTypes.Tile type) {
        for (int i = 0; i < getNPlayers(); i++) {
            if (playerTilesPlaced[i].get(type).getValue() > 0) return true;
        }
        return false;
    }

    public int countPoints(int player) {
        int points = playerResources[player].get(TMTypes.Resource.TR).getValue();
        TMGameParameters params = (TMGameParameters) gameParameters;
        // Add milestones
        for (Milestone m: milestones) {
            if (m.isClaimed() && m.claimed == player) {
                points += params.nPointsMilestone;
            }
        }
        // Add awards
        for (Award a: awards) {
            if (a.isClaimed()) {
                int best = -1;
                HashSet<Integer> bestPlayer = new HashSet<>();
                HashSet<Integer> secondBestPlayer = new HashSet<>();
                for (int i = 0; i < getNPlayers(); i++) {
                    int playerPoints = a.checkProgress(this, i);
                    if (playerPoints > best) {
                        secondBestPlayer = new HashSet<>(bestPlayer);
                        bestPlayer.clear();
                        bestPlayer.add(i);
                        best = playerPoints;
                    }
                }
                for (int i = 0; i < getNPlayers(); i++) {
                    int playerPoints = a.checkProgress(this, i);
                    if (playerPoints == best) {
                        bestPlayer.add(i);
                    }
                }
                if (bestPlayer.contains(player)) points += params.nPointsAwardFirst;
                if (getNPlayers() > 2 && secondBestPlayer.contains(player) && bestPlayer.size() == 1) points += params.nPointsAwardSecond;
            }
        }
        // Add greeneries on board
        points += playerTilesPlaced[player].get(TMTypes.Tile.Greenery).getValue();

        // Add cities on board TODO

        // Add points on cards
        points += playerCardPoints[player].getValue();

        for (TMCard card: playerComplicatedPointCards[player].getComponents()) {
            if (card == null) {
                continue;  // TODO: shouldn't happen
            }
            if (card.pointsThreshold != null) {
                if (card.pointsResource != null) {
                    if (card.nResourcesOnCard >= card.pointsThreshold) {
                        points += card.nPoints;
                    }
                }
            } else {
                if (card.pointsResource != null) {
                    points += card.nPoints * card.nResourcesOnCard;
                } else if (card.pointsTag != null) {
                    points += card.nPoints * playerCardsPlayedTags[player].get(card.pointsTag).getValue();
                } else if (card.pointsTile != null) {
                    if (card.pointsTileAdjacent) {
                        // only adjacent tiles count
                        TMMapTile mt = (TMMapTile) getComponentById(card.mapTileIDTilePlaced);
                        List<Vector2D> neighbours = PlaceTile.getNeighbours(new Vector2D(mt.getX(), mt.getY()));
                        for (Vector2D n: neighbours) {
                            TMMapTile e = board.getElement(n.getX(), n.getY());
                            if (e != null && e.getTilePlaced() == card.pointsTile) {
                                points += card.nPoints;
                            }
                        }
                    } else {
                        points += card.nPoints * playerTilesPlaced[player].get(card.pointsTile).getValue();
                    }
                }
            }
        }
        return points;
    }

    public static class ResourceMapping {
        public TMTypes.Resource from;
        public TMTypes.Resource to;
        public double rate;
        Requirement<TMCard> requirement;
        public ResourceMapping(TMTypes.Resource from, TMTypes.Resource to, double rate, Requirement<TMCard> requirement) {
            this.from = from;
            this.to = to;
            this.rate = rate;
            this.requirement = requirement;
        }
        // TODO: copy

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ResourceMapping)) return false;
            ResourceMapping that = (ResourceMapping) o;
            return from == that.from &&
                    to == that.to &&
                    (requirement == null && that.requirement == null || Objects.equals(requirement, that.requirement));
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to, requirement);
        }
    }
}
