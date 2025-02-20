package games.terraformingmars;

import core.AbstractGameStateWithTurnOrder;
import core.AbstractParameters;
import core.components.*;
import core.interfaces.IGamePhase;
import core.turnorders.TurnOrder;
import games.GameType;
import games.terraformingmars.actions.PlaceTile;
import games.terraformingmars.actions.TMAction;
import games.terraformingmars.components.*;
import games.terraformingmars.rules.Discount;
import games.terraformingmars.rules.effects.Bonus;
import games.terraformingmars.rules.effects.Effect;
import games.terraformingmars.rules.requirements.ActionTypeRequirement;
import games.terraformingmars.rules.requirements.Requirement;
import games.terraformingmars.rules.requirements.TagsPlayedRequirement;
import utilities.Pair;
import utilities.Utils;
import utilities.Vector2D;

import java.util.*;

import static games.terraformingmars.TMGameState.TMPhase.CorporationSelect;

public class TMGameState extends AbstractGameStateWithTurnOrder {

    enum TMPhase implements IGamePhase {
        CorporationSelect,
        Research,
        Actions,
        Production
    }

    // General state info
    int generation;
    GridBoard board;
    HashSet<TMMapTile> extraTiles;
    HashMap<TMTypes.GlobalParameter, GlobalParameter> globalParameters;
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

    // Milestones and awards
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
        super(gameParameters, nPlayers);
    }
    @Override
    protected TurnOrder _createTurnOrder(int nPlayers) {
        return new TMTurnOrder(nPlayers, ((TMGameParameters) gameParameters).nActionsPerPlayer);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.TerraformingMars;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(board);
            add(projectCards);
            add(corpCards);
            add(discardCards);
            add(nAwardsFunded);
            add(nMilestonesClaimed);
            addAll(extraTiles);
            addAll(milestones);
            addAll(awards);
            addAll(globalParameters.values());
            addAll(Arrays.asList(playerHands));
            addAll(Arrays.asList(playerCardChoice));
            addAll(Arrays.asList(playerComplicatedPointCards));
            addAll(Arrays.asList(playedCards));
            addAll(Arrays.asList(playerCardPoints));
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
    protected AbstractGameStateWithTurnOrder __copy(int playerId) {
        TMGameState copy = new TMGameState(gameParameters.copy(), getNPlayers());

        // General public info
        copy.generation = generation;
        copy.board = board.emptyCopy();  // Deep copy of board
        for (int i = 0; i < board.getHeight(); i++) {
            for (int j = 0; j < board.getWidth(); j++) {
                if (board.getElement(j, i) != null) {
                    copy.board.setElement(j, i, board.getElement(j, i).copy());
                } else {
                    copy.board.setElement(j, i, null);
                }
            }
        }
        copy.extraTiles = new HashSet<>();
        for (TMMapTile mt : extraTiles) {
            copy.extraTiles.add(mt.copy());
        }
        copy.globalParameters = new HashMap<>();
        for (TMTypes.GlobalParameter p : globalParameters.keySet()) {
            copy.globalParameters.put(p, globalParameters.get(p).copy());
        }
        copy.bonuses = new HashSet<>();
        for (Bonus b : bonuses) {
            copy.bonuses.add(b.copy());
        }
        copy.milestones = new HashSet<>();
        for (Milestone m : milestones) {
            copy.milestones.add(m.copy());
        }
        copy.awards = new HashSet<>();
        for (Award a : awards) {
            copy.awards.add(a.copy());
        }
        copy.nMilestonesClaimed = nMilestonesClaimed.copy();
        copy.nAwardsFunded = nAwardsFunded.copy();

        // Face-down decks
        copy.projectCards = projectCards.copy();
        copy.corpCards = corpCards.copy();
        copy.discardCards = discardCards.copy(); // TODO: some of these are unknown

        // Player-specific public info
        copy.playerExtraActions = new HashSet[getNPlayers()];
        copy.playerResourceMap = new HashSet[getNPlayers()];
        copy.playerPersistingEffects = new HashSet[getNPlayers()];
        copy.playerDiscountEffects = new HashMap[getNPlayers()];
        copy.playerResources = new HashMap[getNPlayers()];
        copy.playerResourceIncreaseGen = new HashMap[getNPlayers()];
        copy.playerProduction = new HashMap[getNPlayers()];
        copy.playerCardsPlayedTags = new HashMap[getNPlayers()];
        copy.playerCardsPlayedTypes = new HashMap[getNPlayers()];
        copy.playerTilesPlaced = new HashMap[getNPlayers()];
        copy.playerCardPoints = new Counter[getNPlayers()];
        copy.playerComplicatedPointCards = new Deck[getNPlayers()];
        copy.playedCards = new Deck[getNPlayers()];
        copy.playerCorporations = new TMCard[getNPlayers()];
        for (int i = 0; i < getNPlayers(); i++) {
            copy.playerExtraActions[i] = new HashSet<>();
            copy.playerResourceMap[i] = new HashSet<>();
            copy.playerPersistingEffects[i] = new HashSet<>();
            copy.playerDiscountEffects[i] = new HashMap<>();
            copy.playerResources[i] = new HashMap<>();
            copy.playerResourceIncreaseGen[i] = new HashMap<>();
            copy.playerProduction[i] = new HashMap<>();
            copy.playerCardsPlayedTags[i] = new HashMap<>();
            copy.playerCardsPlayedTypes[i] = new HashMap<>();
            copy.playerTilesPlaced[i] = new HashMap<>();
            copy.playerCardPoints[i] = playerCardPoints[i].copy();
            copy.playerComplicatedPointCards[i] = playerComplicatedPointCards[i].copy();
            copy.playedCards[i] = playedCards[i].copy();
            if (playerCorporations[i] != null) {
                copy.playerCorporations[i] = playerCorporations[i].copy();
            }
            for (TMAction a : playerExtraActions[i]) {
                copy.playerExtraActions[i].add(a.copy());
            }
            for (ResourceMapping rm : playerResourceMap[i]) {
                copy.playerResourceMap[i].add(rm.copy());
            }
            for (Requirement r : playerDiscountEffects[i].keySet()) {
                copy.playerDiscountEffects[i].put(r.copy(), playerDiscountEffects[i].get(r));
            }
            for (Effect e : playerPersistingEffects[i]) {
                copy.playerPersistingEffects[i].add(e.copy());
            }
            for (TMTypes.Resource r : playerResources[i].keySet()) {
                copy.playerResources[i].put(r, playerResources[i].get(r).copy());
                copy.playerResourceIncreaseGen[i].put(r, playerResourceIncreaseGen[i].get(r));
            }
            for (TMTypes.Resource r : playerProduction[i].keySet()) {
                copy.playerProduction[i].put(r, playerProduction[i].get(r).copy());
            }
            for (TMTypes.Tag t : playerCardsPlayedTags[i].keySet()) {
                copy.playerCardsPlayedTags[i].put(t, playerCardsPlayedTags[i].get(t).copy());
            }
            for (TMTypes.CardType t : playerCardsPlayedTypes[i].keySet()) {
                copy.playerCardsPlayedTypes[i].put(t, playerCardsPlayedTypes[i].get(t).copy());
            }
            for (TMTypes.Tile t : playerTilesPlaced[i].keySet()) {
                copy.playerTilesPlaced[i].put(t, playerTilesPlaced[i].get(t).copy());
            }
        }

        // Player-specific hidden info
        copy.playerHands = new Deck[getNPlayers()];
        copy.playerCardChoice = new Deck[getNPlayers()];
        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            for (int i = 0; i < getNPlayers(); i++) {
                copy.playerHands[i] = playerHands[i].copy();
                copy.playerCardChoice[i] = playerCardChoice[i].copy();
                if (i != playerId) {  // Player knows what cards they have, but shuffle for opponents, all project cards together and deal new
                    copy.projectCards.add(copy.playerHands[i]);
                    if (gamePhase != CorporationSelect) {  // corporation selection is public info, card choice only hidden afterwards
                        copy.projectCards.add(copy.playerCardChoice[i]);
                        copy.playerCardChoice[i].clear();
                    }
                    copy.playerHands[i].clear();
                }
            }
            copy.corpCards.shuffle(rnd);
            copy.projectCards.shuffle(rnd);
            for (int i = 0; i < getNPlayers(); i++) {
                if (i != playerId) {
                    for (int j = 0; j < playerHands[i].getSize(); j++) {
                        copy.playerHands[i].add(copy.drawCard());
                    }
                    if (gamePhase != CorporationSelect) {
                        for (int j = 0; j < playerCardChoice[i].getSize(); j++) {
                            copy.playerCardChoice[i].add(copy.drawCard());
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < getNPlayers(); i++) {
                copy.playerHands[i] = playerHands[i].copy();
                copy.playerCardChoice[i] = playerCardChoice[i].copy();
            }
        }

        return copy;
    }

    public TMCard drawCard() {
        // Reshuffle discards into draw pile if empty
        if (projectCards.getSize() == 0) {
            projectCards.add(discardCards);
            discardCards.clear();
            projectCards.shuffle(rnd);
        }
        return projectCards.draw();
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
//        return new TMHeuristic().evaluateState(this, playerId);
//        return getGameScore(playerId);
        return countPoints(playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        return playerResources[playerId].get(TMTypes.Resource.TR).getValue();
//        return countPoints(playerId);
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TMGameState)) return false;
        TMGameState that = (TMGameState) o;
        return generation == that.generation
                && Objects.equals(board, that.board)
                && Objects.equals(extraTiles, that.extraTiles)
                && Objects.equals(globalParameters, that.globalParameters)
                && Objects.equals(bonuses, that.bonuses)
                && Objects.equals(projectCards, that.projectCards)
                && Objects.equals(corpCards, that.corpCards)
                && Objects.equals(discardCards, that.discardCards)
                && Arrays.equals(playerExtraActions, that.playerExtraActions)
                && Arrays.equals(playerResourceMap, that.playerResourceMap)
                && Arrays.equals(playerDiscountEffects, that.playerDiscountEffects)
                && Arrays.equals(playerPersistingEffects, that.playerPersistingEffects)
                && Arrays.equals(playerResources, that.playerResources)
                && Arrays.equals(playerResourceIncreaseGen, that.playerResourceIncreaseGen)
                && Arrays.equals(playerProduction, that.playerProduction)
                && Arrays.equals(playerCardsPlayedTags, that.playerCardsPlayedTags)
                && Arrays.equals(playerCardsPlayedTypes, that.playerCardsPlayedTypes)
                && Arrays.equals(playerTilesPlaced, that.playerTilesPlaced)
                && Arrays.equals(playerCardPoints, that.playerCardPoints)
                && Arrays.equals(playerHands, that.playerHands)
                && Arrays.equals(playerComplicatedPointCards, that.playerComplicatedPointCards)
                && Arrays.equals(playerCardChoice, that.playerCardChoice)
                && Arrays.equals(playerCorporations, that.playerCorporations)
                && Objects.equals(milestones, that.milestones)
                && Objects.equals(awards, that.awards)
                && Objects.equals(nMilestonesClaimed, that.nMilestonesClaimed)
                && Objects.equals(nAwardsFunded, that.nAwardsFunded);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), generation, board, extraTiles, globalParameters, bonuses,
                projectCards, corpCards, discardCards, milestones, awards, nMilestonesClaimed, nAwardsFunded);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int result = Objects.hash(gameParameters);
        sb.append(result).append("|");
        result = Objects.hash(turnOrder);
        sb.append(result).append("|");
        result = Objects.hash(getAllComponents());
        sb.append(result).append("|");
        result = Objects.hash(gameStatus);
        sb.append(result).append("|");
        result = Objects.hash(gamePhase);
        sb.append(result).append("|");
        result = Arrays.hashCode(playerResults);
        sb.append(result).append("|*|");
        result = Objects.hash(generation);
        sb.append(result).append("|");
        result = Objects.hash(board);
        sb.append(result).append("|");
        result = Objects.hash(extraTiles);
        sb.append(result).append("|");
        result = Objects.hash(globalParameters);
        sb.append(result).append("|");
        result = Objects.hash(bonuses);
        sb.append(result).append("|2|");
        result = Objects.hash(projectCards);
        sb.append(result).append("|3|");
        result = Objects.hash(corpCards);
        sb.append(result).append("|4|");
        result = Objects.hash(discardCards);
        sb.append(result).append("|5|");
        result = Objects.hash(milestones);
        sb.append(result).append("|6|");
        result = Objects.hash(awards);
        sb.append(result).append("|7|");
        result = Objects.hash(nMilestonesClaimed, nAwardsFunded);
        sb.append(result).append("|8|");
        result = Arrays.hashCode(playerExtraActions);
        result = 31 * result + Arrays.hashCode(playerResourceMap);
        result = 31 * result + Arrays.hashCode(playerDiscountEffects);
        result = 31 * result + Arrays.hashCode(playerPersistingEffects);
        result = 31 * result + Arrays.hashCode(playerResources);
        result = 31 * result + Arrays.hashCode(playerResourceIncreaseGen);
        sb.append(result).append("|9|");
        result = Arrays.hashCode(playerProduction);
        sb.append(result).append("|10|");
        result = Arrays.hashCode(playerCardsPlayedTags);
        sb.append(result).append("|10|");
        result = Arrays.hashCode(playerCardsPlayedTypes);
        sb.append(result).append("|12|");
        result = Arrays.hashCode(playerTilesPlaced);
        sb.append(result).append("|13|");
        result = Arrays.hashCode(playerCardPoints);
        sb.append(result).append("|14|");
        result = Arrays.hashCode(playerHands);
        sb.append(result).append("|15|");
        result = Arrays.hashCode(playerComplicatedPointCards);
        sb.append(result).append("|16|");
        result = Arrays.hashCode(playerCardChoice);
        sb.append(result).append("|17|");
        result = Arrays.hashCode(playerCorporations);
        sb.append(result);
        return sb.toString();
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

    public GridBoard getBoard() {
        return board;
    }

    public HashSet<Bonus> getBonuses() {
        return bonuses;
    }

    public HashMap<TMTypes.GlobalParameter, GlobalParameter> getGlobalParameters() {
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
        for (Map.Entry<Requirement, Integer> e : playerDiscountEffects[player].entrySet()) {
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
        for (TMTypes.Tag t : card.tags) {
            for (Map.Entry<Requirement, Integer> e : playerDiscountEffects[player].entrySet()) {
                if (e.getKey() instanceof TagsPlayedRequirement) {
                    boolean found = false;
                    for (TMTypes.Tag tt : ((TagsPlayedRequirement) e.getKey()).tags) {
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
        if (player == -3) return true;  // In solo play, this is the neutral player

        if (production) {
            Counter c = playerProduction[player].get(to);
            if (c.getMinimum() < 0) return c.getValue() + Math.abs(c.getMinimum()) >= amount;
            return c.getValue() >= amount;
        }

        int sum = playerResourceSum(player, card, from, to, true);
        return card != null ? isCardFree(card, sum, -1) : sum >= amount;
    }

    public int playerResourceSum(int player, TMCard card, HashSet<TMTypes.Resource> from, TMTypes.Resource to, boolean itself) {
        if (from == null || from.size() > 0) {
            int sum = 0;
            if (itself || from != null && from.contains(to))
                sum = playerResources[player].get(to).getValue();  // All resources can be exchanged for themselves at rate 1.0

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
     *
     * @param card - card to buy; can be null, and resource mappings that require card tags will be skipped
     * @param from - resource to transform from; can be null, then all resources in the player's mapping will be checked
     * @param to   - resource to transform to
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
        for (ResourceMapping rm : playerResourceMap[getCurrentPlayer()]) {
            if (rm.from == from && rm.to == to) {
                rate = rm.rate;
                break;
            }
        }
        return rate;
    }

    public void addDiscountEffects(LinkedList<Discount> discounts) {
        int player = getCurrentPlayer();
        for(Discount d : discounts){
            Requirement r = d.a;
            int amount = d.b;
            if (playerDiscountEffects[player].containsKey(r)) {
                playerDiscountEffects[player].put(r, playerDiscountEffects[player].get(r) + amount);
            } else {
                playerDiscountEffects[player].put(r, amount);
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
        for (TMTypes.Tile t : playerTilesPlaced[player].keySet()) {
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
        return getNPlayers() == 1;
    }

    public boolean anyTilesPlaced(TMTypes.Tile type) {
        for (int i = 0; i < getNPlayers(); i++) {
            if (playerTilesPlaced[i].get(type).getValue() > 0) return true;
        }
        return getNPlayers() == 1 && (type == TMTypes.Tile.City || type == TMTypes.Tile.Greenery);
    }

    public int countPoints(int player) {
        // Add TR
        int points = playerResources[player].get(TMTypes.Resource.TR).getValue();
        // Add milestones
        points += countPointsMilestones(player);
        // Add awards
        points += countPointsAwards(player);
        // Add points from board
        points += countPointsBoard(player);
        // Add points on cards
        points += countPointsCards(player);
        return points;
    }

    public int countPointsMilestones(int player) {
        TMGameParameters params = (TMGameParameters) gameParameters;
        int points = 0;
        for (Milestone m : milestones) {
            if (m.isClaimed() && m.claimed == player) {
                points += params.nPointsMilestone;
            }
        }
        return points;
    }

    public int countPointsAwards(int player) {
        TMGameParameters params = (TMGameParameters) gameParameters;
        int points = 0;
        for (Award a : awards) {
            Pair<HashSet<Integer>, HashSet<Integer>> winners = awardWinner(a);
            if (winners != null) {
                if (winners.a.contains(player)) points += params.nPointsAwardFirst;
                if (winners.b.contains(player) && winners.a.size() == 1)
                    points += params.nPointsAwardSecond;
            }
        }
        return points;
    }

    public Pair<HashSet<Integer>, HashSet<Integer>> awardWinner(Award a) {
        if (a.isClaimed()) {
            int best = -1;
            int secondBest = -1;
            HashSet<Integer> bestPlayer = new HashSet<>();
            HashSet<Integer> secondBestPlayer = new HashSet<>();
            for (int i = 0; i < getNPlayers(); i++) {
                int playerPoints = a.checkProgress(this, i);
                if (playerPoints >= best) {
                    if (playerPoints > best) {
                        secondBestPlayer = new HashSet<>(bestPlayer);
                        secondBest = best;
                        bestPlayer.clear();
                        bestPlayer.add(i);
                        best = playerPoints;
                    }
                } else if (playerPoints > secondBest) {
                    secondBestPlayer.clear();
                    secondBestPlayer.add(i);
                    secondBest = playerPoints;
                }
            }
            for (int i = 0; i < getNPlayers(); i++) {
                int playerPoints = a.checkProgress(this, i);
                if (playerPoints == best) {
                    bestPlayer.add(i);
                } else if (playerPoints == secondBest) {
                    secondBestPlayer.add(i);
                }
            }
            if (getNPlayers() <= 2 || bestPlayer.size() > 1)
                secondBestPlayer.clear();  // No second-best awarded unless there are 3 or more players, and only 1 got first place
            return new Pair<>(bestPlayer, secondBestPlayer);
        }
        return null;
    }

    public int countPointsBoard(int player) {
        int points = 0;
        // Greeneries
        points += playerTilesPlaced[player].get(TMTypes.Tile.Greenery).getValue();
        // Add cities on board
        for (int i = 0; i < board.getHeight(); i++) {
            for (int j = 0; j < board.getWidth(); j++) {
                TMMapTile mt = (TMMapTile) board.getElement(j, i);
                if (mt != null && mt.getTilePlaced() == TMTypes.Tile.City) {
                    // Count adjacent greeneries
                    points += PlaceTile.nAdjacentTiles(this, mt, TMTypes.Tile.Greenery);
                }
            }
        }
        return points;
    }

    public int countPointsCards(int player) {
        int points = 0;

        // Normal points
        points += playerCardPoints[player].getValue();
        // Complicated points
        for (TMCard card : playerComplicatedPointCards[player].getComponents()) {
            if (card == null) {
                continue;
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
                    if (card.pointsTileAdjacent && card.mapTileIDTilePlaced >= 0) {  // TODO: mapTileIDPlaced should have been set in this case, bug
                        // only adjacent tiles count
                        TMMapTile mt = (TMMapTile) getComponentById(card.mapTileIDTilePlaced);
                        List<Vector2D> neighbours = PlaceTile.getNeighbours(new Vector2D(mt.getX(), mt.getY()));
                        for (Vector2D n : neighbours) {
                            TMMapTile e = (TMMapTile) board.getElement(n.getX(), n.getY());
                            if (e != null && e.getTilePlaced() == card.pointsTile) {
                                points += card.nPoints;
                            }
                        }
                    } else {
                        points += card.nPoints * playerTilesPlaced[player].get(card.pointsTile).getValue();
                    }
                } else if (card.getComponentName().equalsIgnoreCase("capital")) {
                    // x VP per Ocean adjacent
                    int position = card.mapTileIDTilePlaced;
                    TMMapTile mt = (TMMapTile) getComponentById(position);
                    points += card.nPoints * PlaceTile.nAdjacentTiles(this, mt, TMTypes.Tile.Ocean);
                }
            }
        }
        return points;
    }

    public static class ResourceMapping {
        public final TMTypes.Resource from;
        public final TMTypes.Resource to;
        public double rate;
        public Requirement<TMCard> requirement;

        public ResourceMapping(TMTypes.Resource from, TMTypes.Resource to, double rate, Requirement<TMCard> requirement) {
            this.from = from;
            this.to = to;
            this.rate = rate;
            this.requirement = requirement;
        }

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

        public ResourceMapping copy() {
            ResourceMapping rm = new ResourceMapping(from, to, rate, requirement);
            if (requirement != null) {
                rm.requirement = requirement.copy();
            }
            return rm;
        }
    }
}
