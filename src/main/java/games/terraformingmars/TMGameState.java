package games.terraformingmars;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.*;
import core.interfaces.IGamePhase;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.Award;
import games.terraformingmars.rules.Bonus;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.Milestone;
import games.terraformingmars.rules.Requirement;
import utilities.Utils;
import utilities.Vector2D;

import java.util.*;

import static games.terraformingmars.TMTypes.neighbor_directions;

public class TMGameState extends AbstractGameState {

    enum TMPhase implements IGamePhase {
        CorporationSelect,
        Research,
        Actions,
        Production
    }

    // General state info
    GridBoard<TMMapTile> board;
    TMMapTile[] extraTiles;
    Counter[] globalParameters;
    Bonus[] bonuses;
    Deck<TMCard> projectCards, corpCards, discardCards;  // Face-down decks

    // Effects and actions played
    HashSet<AbstractAction>[] playerCardsPlayedEffects;
    HashSet<AbstractAction>[] playerCardsPlayedActions;
    HashSet<ResourceMapping>[] playerResourceMap;  // Effects for turning one resource into another TODO initialize

    // Player-specific counters
    HashMap<TMTypes.Resource, Counter>[] playerResources;
    HashMap<TMTypes.Resource, Counter>[] playerProduction;
    HashMap<TMTypes.Tag, Counter>[] playerCardsPlayedTags;
    HashMap<TMTypes.CardType, Counter>[] playerCardsPlayedTypes;
    HashMap<TMTypes.Tile, Counter>[] tilesPlaced;

    // Player cards
    Deck<TMCard>[] playerHands;
    Deck<TMCard>[] playerCardChoice;
    TMCard[] playerCorporations;

    // Milestones and awards TODO: components?
    Milestone[] milestones;
    Award[] awards;
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
            this.addAll(Arrays.asList(extraTiles));
            this.addAll(Arrays.asList(globalParameters));
            this.addAll(Arrays.asList(playerHands));
            this.addAll(Arrays.asList(playerCardChoice));
//            this.addAll(Arrays.asList(playerCorporations));
            for (int i = 0; i < getNPlayers(); i++) {
                addAll(playerResources[i].values());
                addAll(playerProduction[i].values());
                addAll(playerCardsPlayedTags[i].values());
                addAll(tilesPlaced[i].values());
                addAll(playerCardsPlayedTypes[i].values());
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
        return 0;
    }

    @Override
    protected void _reset() {

    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }

    public static Counter stringToGPCounter(TMGameState gs, String s) {
        for (Counter c: gs.globalParameters) {
            if (c.getComponentName().equalsIgnoreCase(s)) {
                return c;
            }
        }
        return null;
    }

    public static TMTypes.GlobalParameter counterToGP(Counter c) {
        return Utils.searchEnum(TMTypes.GlobalParameter.class, c.getComponentName());
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

    public Bonus[] getBonuses() {
        return bonuses;
    }

    public Counter[] getGlobalParameters() {
        return globalParameters;
    }

    public TMMapTile[] getExtraTiles() {
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

    public HashSet<AbstractAction>[] getPlayerCardsPlayedActions() {
        return playerCardsPlayedActions;
    }

    public HashSet<AbstractAction>[] getPlayerCardsPlayedEffects() {
        return playerCardsPlayedEffects;
    }

    public HashMap<TMTypes.Tile, Counter>[] getTilesPlaced() {
        return tilesPlaced;
    }

    public Milestone[] getMilestones() {
        return milestones;
    }

    public Award[] getAwards() {
        return awards;
    }

    public TMCard[] getPlayerCorporations() {
        return playerCorporations;
    }

    public Deck<TMCard>[] getPlayerCardChoice() {
        return playerCardChoice;
    }

    public boolean canPlayerPay(TMCard card, HashSet<TMTypes.Resource> from, TMTypes.Resource to, int amount) {
        int sum = playerResourceSum(card, from, to);
        return card != null? isCardFree(card, sum) : sum >= amount;
    }

    public int playerResourceSum(TMCard card, HashSet<TMTypes.Resource> from, TMTypes.Resource to) {
        int player = getCurrentPlayer();
        int sum = 0;
        if (from == null) {
            sum = playerResources[player].get(to).getValue();
        }
        // Add resources that this player can use as money for this card
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

    /**
     * Check if player can transform one resource into another, when buying a card
     * @param card - card to buy; can be null, and resource mappings that require card tags will be skipped
     * @param from - resource to transform from; can be null, then all resources in the player's mapping will be checked
     * @param to - resource to transform to
     * @return all resources that can be transformed into given res
     */
    public HashSet<TMTypes.Resource> canPlayerTransform(TMCard card, TMTypes.Resource from, TMTypes.Resource to) {
        HashSet<TMTypes.Resource> resources = new HashSet<>();
        for (ResourceMapping resMap : playerResourceMap[getCurrentPlayer()]) {
            if ((from == null || resMap.from == from) && resMap.to == to && (resMap.requirement == null || resMap.requirement.testCondition(card))) {
                resources.add(resMap.from);
            }
        }
        return resources;
    }

    public void playerPay(TMTypes.Resource resource, int amount) {
        playerResources[getCurrentPlayer()].get(resource).decrement(amount);
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

    public boolean isCardFree(TMCard card) {
        return isCardFree(card, 0);
    }

    public boolean isCardFree(TMCard card, int amountPaid) {
        // TODO: discount effects
        return card.cost - amountPaid <= 0;
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

    static HashSet<Vector2D> getEmptyTilesOfType(TMGameState gs, TMTypes.MapTileType type) {
        HashSet<Vector2D> pos = new HashSet<>();
        for (int i = 0; i < gs.board.getHeight(); i++) {
            for (int j = 0; j < gs.board.getWidth(); j++) {
                TMMapTile mt = gs.board.getElement(j, i);
                if (mt != null && mt.getTilePlaced() == null && mt.getTileType() == type) {
                    pos.add(new Vector2D(j, i));
                }
            }
        }
        return pos;
    }

    static List<Vector2D> getNeighbours(Vector2D cell) {
        ArrayList<Vector2D> neighbors = new ArrayList<>();
        int parity = cell.getY() % 2;
        for (Vector2D v: neighbor_directions[parity]) {
            neighbors.add(cell.add(v));
        }
        return neighbors;
    }

    public static class ResourceMapping {
        public TMTypes.Resource from;
        TMTypes.Resource to;
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
            return Double.compare(that.rate, rate) == 0 &&
                    from == that.from &&
                    to == that.to &&
                    Objects.equals(requirement, that.requirement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to, rate, requirement);
        }
    }
}
