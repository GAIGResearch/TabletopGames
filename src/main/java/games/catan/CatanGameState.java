package games.catan;

import core.AbstractParameters;
import core.AbstractGameState;
import core.CoreConstants;
import core.components.Area;
import core.components.Card;
import core.components.Component;
import core.components.Deck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.catan.actions.OfferPlayerTrade;
import games.catan.components.Edge;
import games.catan.components.Graph;
import games.catan.components.Road;
import games.catan.components.Settlement;
import utilities.Utils;

import java.util.*;

import static core.CoreConstants.*;
import static games.catan.CatanConstants.*;

public class CatanGameState extends AbstractGameState {
    protected CatanTile[][] board;
    protected Graph<Settlement, Road> catanGraph;
    protected Card boughtDevCard; // used to keep a reference to a dev card bought in the current turn to avoid playing it
    protected int[] scores; // score for each player
    protected int[] victoryPoints; // secret points from victory cards
    protected int[] knights; // knight count for each player
    protected int[][] exchangeRates; // exchange rate with bank for each resource
    protected int largestArmy; // playerID of the player currently holding the largest army
    protected int longestRoad; // playerID of the player currently holding the longest road
    protected int longestRoadLength;
    protected OfferPlayerTrade currentTradeOffer; // Holds the current trade offer to allow access between players TODO make primitive
    int rollValue;
    protected Random rnd;

    // GamePhases that may occur in Catan
    public enum CatanGamePhase implements IGamePhase {
        Setup,
        Trade,
        Build,
        Robber,
        Discard,
        Steal
    }

    // Collection of areas, mapped to player ID, -1 is the general game area containing the board, counters and several decks.
    HashMap<Integer, Area> areas;

    public CatanGameState(AbstractParameters pp, int nPlayers) {
        super(pp, new CatanTurnOrder(nPlayers, ((CatanParameters) pp).n_actions_per_turn), GameType.Catan);
        _reset();
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>(areas.values());
    }

    // Getters & setters
    public Component getComponent(int componentId, int playerId) {
        return areas.get(playerId).getComponent(componentId);
    }

    public Component getComponentActingPlayer(int componentId) {
        return areas.get(turnOrder.getCurrentPlayer(this)).getComponent(componentId);
    }

    public Component getComponent(int componentId) {
        return getComponent(componentId, -1);
    }

    Area getArea(int playerId) {
        return areas.get(playerId);
    }

    @Override
    protected void _reset() {
        // set everything to null (except for Random number generator)
        this.areas = null;
        boughtDevCard = null;
        board = null;
        currentTradeOffer = null;
        catanGraph = null;

        CatanParameters pp = (CatanParameters) gameParameters;
        scores = new int[getNPlayers()];
        knights = new int[getNPlayers()];
        exchangeRates = new int[getNPlayers()][CatanParameters.Resources.values().length];
        for (int i = 0; i < exchangeRates.length; i++)
            Arrays.fill(exchangeRates[i], pp.default_exchange_rate);
        victoryPoints = new int[getNPlayers()];
        longestRoadLength = pp.min_longest_road;
        largestArmy = -1;
        longestRoad = -1;
        if (rnd == null)
            rnd = new Random(pp.getRandomSeed());
    }

    @Override
    protected boolean _equals(Object obj) {
        if (obj instanceof CatanGameState) {
            CatanGameState o = (CatanGameState) obj;
            return boughtDevCard.equals(o.boughtDevCard) && catanGraph.equals(o.catanGraph) &&
                    largestArmy == o.largestArmy && longestRoad == o.longestRoad &&
                    longestRoadLength == o.longestRoadLength && o.currentTradeOffer.equals(currentTradeOffer) &&
                    rollValue == o.rollValue && Arrays.equals(scores, o.scores) &&
                    Arrays.equals(victoryPoints, o.victoryPoints) && Arrays.equals(knights, o.knights) &&
                    Arrays.deepEquals(exchangeRates, o.exchangeRates) && Arrays.deepEquals(board, o.board);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(gameParameters, turnOrder, gameStatus, gamePhase, catanGraph, boughtDevCard,
                largestArmy, longestRoad, longestRoadLength, currentTradeOffer, rollValue);
        result = 31 * result + Arrays.hashCode(playerResults);
        result = 31 * result + Arrays.hashCode(scores);
        result = 31 * result + Arrays.hashCode(victoryPoints);
        result = 31 * result + Arrays.hashCode(knights);
        result = 31 * result + Arrays.deepHashCode(exchangeRates);
        result = 31 * result + Arrays.deepHashCode(board);
        return result;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(gameParameters.hashCode()).append("|");
        sb.append(turnOrder.hashCode()).append("|");
        sb.append(gameStatus.hashCode()).append("|");
        sb.append(Arrays.hashCode(playerResults)).append("|*|");
        sb.append(catanGraph.hashCode()).append("|");
        sb.append(boughtDevCard == null ? 0 : boughtDevCard.hashCode()).append("|");
        sb.append(largestArmy).append("|");
        sb.append(longestRoad).append("|");
        sb.append(longestRoadLength).append("|");
        sb.append(rollValue).append("|");
        sb.append(currentTradeOffer == null ? 0 : currentTradeOffer.hashCode()).append("|");
        sb.append(Arrays.hashCode(scores)).append("|");
        sb.append(Arrays.hashCode(victoryPoints)).append("|");
        sb.append(Arrays.hashCode(knights)).append("|");
        sb.append(Arrays.deepHashCode(exchangeRates)).append("|");
        sb.append(Arrays.deepHashCode(board)).append("|");
        return sb.toString();
    }

    public void setBoard(CatanTile[][] board) {
        this.board = board;
    }

    public CatanTile[][] getBoard() {
        return board;
    }

    public void setGraph(Graph graph) {
        this.catanGraph = graph;
    }

    public Graph getGraph() {
        return catanGraph;
    }

    public int getRollValue() {
        return rollValue;
    }

    public void rollDice() {
        /* Rolls 2 random dice given a single random seed */
        int num1 = rnd.nextInt(6);
        int num2 = rnd.nextInt(6);

        rollValue = num1 + num2 + 2;
    }

    public CatanTile getRobber(CatanTile[][] board) {
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                if (tile.hasRobber()) {
                    return tile;
                }
            }
        }
        System.out.println("Robber not found - should not happen");
        return null;
    }

    void addComponents() {
        super.addAllComponents();
    }

    public void addScore(int playerID, int score) {
        if (playerID < scores.length) {
            scores[playerID] += score;
        }
    }

    public void addKnight(int playerID) {
        if (playerID < knights.length) {
            knights[playerID] += 1;
            updateLargestArmy((CatanParameters) getGameParameters());
        }
    }

    public int[] getKnights() {
        return Arrays.copyOf(knights, knights.length);
    }

    public void addVictoryPoint(int playerID) {
        if (playerID < knights.length) {
            victoryPoints[playerID] += 1;
        }
    }

    public int[] getVictoryPoints() {
        return victoryPoints.clone();
    }

    public int[] getPlayerResources(int playerID) {
        Deck<Card> playerHand = (Deck<Card>) this.getComponent(CoreConstants.playerHandHash, playerID);
        int[] resources = new int[CatanParameters.Resources.values().length];

        for (Card card : playerHand.getComponents()) {
            resources[CatanParameters.Resources.valueOf(card.getProperty(cardType).toString()).ordinal()] += 1;
        }
        return resources;
    }

    public int[] getPLayerDevCards(int playerID) {
        Deck<Card> playerDevCards = (Deck<Card>) this.getComponent(developmentDeckHash, playerID);
        int[] devCards = new int[CatanParameters.CardTypes.values().length];

        for (Card card : playerDevCards.getComponents()) {
            devCards[CatanParameters.CardTypes.valueOf(card.getProperty(cardType).toString()).ordinal()] += 1;
        }
        return devCards;
    }

    public void updateLargestArmy(CatanParameters params) {
        /* Checks the army sizes and updates the scores accordingly */
        if (largestArmy == -1) {
            // check if any of them meets the minimum required army size
            for (int i = 0; i < knights.length; i++) {
                if (knights[i] >= params.min_army_size) {
                    largestArmy = i;
                    scores[i] += params.largest_army_value;
                }
            }
        } else {
            int max = knights[largestArmy];
            for (int i = 0; i < knights.length; i++) {
                if (knights[i] > max) {
                    // update scores
                    scores[largestArmy] -= params.largest_army_value;
                    scores[i] += params.largest_army_value;

                    max = knights[i];
                    largestArmy = i;
                }
            }
        }
    }


    public int[] getScores() {
        return scores;
    }

    public int[] getExchangeRates(int playerID) {
        return exchangeRates[playerID];
    }

    public void setBoughtDevCard(Card card) {
        this.boughtDevCard = card;
    }

    public Card getBoughtDevCard() {
        return boughtDevCard;
    }

    public int getRoadDistance(int x, int y, int edge) {
        // As the settlements are the nodes, we expand them to find roads
        // calculates the distance length of the road
        HashSet<Road> roadSet = new HashSet<>();
        HashSet<Road> roadSet2 = new HashSet<>();

        ArrayList<Settlement> dir1 = new ArrayList<>();
        ArrayList<Settlement> dir2 = new ArrayList<>();
        Settlement settl1 = board[x][y].getSettlements()[edge];
        Settlement settl2 = board[x][y].getSettlements()[(edge + 1) % 6];

        dir1.add(settl1);
        dir2.add(settl2);

        // find longest segment, we first follow dir_1 then dir_2
        roadSet = expandRoad(this, roadSet, new ArrayList<>(dir1), new ArrayList<>(dir2));
        roadSet.addAll(expandRoad(this, roadSet2, new ArrayList<>(dir2), new ArrayList<>(dir1)));

//        System.out.println("Current road length is " + roadSet.size() + " for player " + getCurrentPlayer());
        return roadSet.size();
    }

    private static HashSet<Road> expandRoad(CatanGameState gs, HashSet<Road> roadSet, List<Settlement> unexpanded, List<Settlement> expanded) {
        // return length, makes it possible to compare segments
        // modify original set
        if (unexpanded.size() == 0) {
            return roadSet;
        }
        if (unexpanded.size() == 2) {
            // Handle branching
            int length = 0;
            HashSet<Road> longestSegment = new HashSet<>(roadSet);
            for (Settlement settlement : unexpanded) {
                ArrayList<Settlement> toExpand = new ArrayList<>();
                toExpand.add(settlement);
                HashSet<Road> roadSetCopy = new HashSet<>(roadSet);
                roadSetCopy = expandRoad(gs, roadSetCopy, toExpand, expanded);
                if (roadSetCopy.size() >= length) {
                    length = roadSetCopy.size();
                    longestSegment = roadSetCopy;
                }
            }
            roadSet.addAll(longestSegment);
            return roadSet;
        } else {
            // case of expanding a single settlement
            Settlement settlement = unexpanded.remove(0);
            expanded.add(settlement);

            List<Edge<Settlement, Road>> edges = gs.getGraph().getEdges(settlement);
            if (edges != null) {
                for (Edge<Settlement, Road> e : edges) {
                    Road road = e.getValue();
                    if (road.getOwner() == gs.getCurrentPlayer()) {
                        if (expanded.contains(e.getDest())) {
                            // The road used to get here
                            roadSet.add(road);
                        } else {
                            // if settlement belongs to somebody else it's a deadend
                            if (e.getDest().getOwner() == -1 || e.getDest().getOwner() == gs.getCurrentPlayer()) {
                                unexpanded.add(e.getDest());
                            }
                        }
                    }

                }

            }
        }

        // only gets here when explored a single road
        return expandRoad(gs, roadSet, unexpanded, expanded);
    }

    public ArrayList<Road> getRoads() {
        // Function that returns all the roads from the the board
        int counter = 0;
        ArrayList<Road> roads = new ArrayList<>();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                for (int i = 0; i < HEX_SIDES; i++) {
                    // Road has already been set
                    Road road = tile.getRoads()[i];
                    if (road.getOwner() != -1) {
                        roads.add(road);
                        counter += 1;
                    }
                }
            }
        }
        System.out.println("There are " + counter + " roads");
        return roads;
    }

    public ArrayList<Settlement> getSettlements() {
        // Function that returns all the settlements from the the board
        int counter = 0;
        ArrayList<Settlement> settlements = new ArrayList<>();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                for (int i = 0; i < 6; i++) {
                    // Road has already been set
                    Settlement settlement = tile.getSettlements()[i];
                    if (settlement.getOwner() != -1) {
                        settlements.add(settlement);
                        counter += 1;
                    }
                }
            }
        }
        if (getCoreGameParameters().verbose) {
            System.out.println("There are " + counter + " settlements");
        }
        return settlements;
    }

    public int getLongestRoadOwner() {
        return longestRoad;
    }

    public int getLongestRoadLength() {
        return longestRoadLength;
    }

    public ArrayList<Settlement> getPlayersSettlements(int playerId) {
        ArrayList<Settlement> playerSettlements = new ArrayList<>();
        ArrayList<Settlement> allSettlements = getSettlements();
        for (int i = 0; i < allSettlements.size(); i++) {
            if (allSettlements.get(i).getOwner() == playerId) {
                playerSettlements.add(allSettlements.get(i));
            }
        }
        return playerSettlements;
    }

    /* checks if given resources cover the price or not */
    public static boolean checkCost(int[] resources, int[] price) {
        for (int i = 0; i < resources.length; i++) {
            if (resources[i] - price[i] < 0) return false;
        }
        return true;
    }

    /**
     * Swaps cards between players
     *
     * @param gs
     * @param playerID
     * @param otherPlayerID
     * @param playerResourcesToTrade
     * @param otherPlayerResourcesToTrade
     * @return
     */
    public static boolean swapResources(CatanGameState gs, int playerID, int otherPlayerID, int[] playerResourcesToTrade, int[] otherPlayerResourcesToTrade) {
        int[] playerResourcesToTradeCopy = playerResourcesToTrade.clone();
        int[] otherPlayerResourcesToTradeCopy = otherPlayerResourcesToTrade.clone();
        List<Card> playerHand = ((Deck<Card>) gs.getComponent(playerHandHash, playerID)).getComponents();
        List<Card> otherPlayerHand = ((Deck<Card>) gs.getComponent(playerHandHash, otherPlayerID)).getComponents();
        ArrayList<Card> cardsToGiveToPlayer = new ArrayList<>();
        ArrayList<Card> cardsToGiveToOtherPlayer = new ArrayList<>();

        for (Card card : playerHand) {
            int index = CatanParameters.Resources.valueOf(card.getProperty(CatanConstants.cardType).toString()).ordinal();
            if (playerResourcesToTradeCopy[index] > 0) {
                cardsToGiveToOtherPlayer.add(card);
                playerResourcesToTradeCopy[index] -= 1;
            }
        }

        for (Card card : otherPlayerHand) {
            int index = CatanParameters.Resources.valueOf(card.getProperty(CatanConstants.cardType).toString()).ordinal();
            if (otherPlayerResourcesToTradeCopy[index] > 0) {
                cardsToGiveToPlayer.add(card);
                otherPlayerResourcesToTradeCopy[index] -= 1;
            }
        }

        for (int i = 0; i < playerResourcesToTradeCopy.length; i++) {
            if (playerResourcesToTradeCopy[i] > 0) {
                throw new AssertionError("Player does not have enough resources in hand");
            }
            if (otherPlayerResourcesToTradeCopy[i] > 0) {
                throw new AssertionError("Other player does not have enough resources in hand");
            }
        }

        for (int i = 0; i < cardsToGiveToOtherPlayer.size(); i++) {
            Card card = cardsToGiveToOtherPlayer.get(i);
            playerHand.remove(card);
            otherPlayerHand.add(card);
        }
        for (int i = 0; i < cardsToGiveToPlayer.size(); i++) {
            Card card = cardsToGiveToPlayer.get(i);
            otherPlayerHand.remove(card);
            playerHand.add(card);
        }

        return true;
    }

    /* Takes the resource cards specified in the cost array from the current player, returns true if successful */
    public static boolean spendResources(CatanGameState gs, int[] cost) {
        int[] costCopy = cost.clone();
        List<Card> playerHand = ((Deck<Card>) gs.getComponentActingPlayer(playerHandHash)).getComponents();
        ArrayList<Card> cardsToReturn = new ArrayList<>();
        // reduce entries in cost until all of them are 0
        for (int i = 0; i < playerHand.size(); i++) {
            Card card = playerHand.get(i);
            int index = CatanParameters.Resources.valueOf(card.getProperty(CatanConstants.cardType).toString()).ordinal();
            if (costCopy[index] > 0) {
                cardsToReturn.add(card);
                costCopy[index] -= 1;
            }
        }
        // if we got all 0s -> return true; remove them from player and put them back to resourceDeck
        for (int i = 0; i < costCopy.length; i++) {
            if (costCopy[i] > 0) {
                if (gs.getCoreGameParameters().verbose)
                    System.out.println("Player does not have enough resources in hand");
                return false;
            }
        }

        for (int i = 0; i < cardsToReturn.size(); i++) {
            Card card = cardsToReturn.get(i);
            ((Deck<Card>) gs.getComponentActingPlayer(playerHandHash)).remove(card);
            ((Deck<Card>) gs.getComponent(resourceDeckHash)).add(card);
        }
        return true;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        CatanGameState copy = new CatanGameState(getGameParameters(), getNPlayers());
        copy.gamePhase = gamePhase;
        copy.board = copyBoard();
        copy.boughtDevCard = boughtDevCard == null ? null : boughtDevCard.copy();
        copy.catanGraph = catanGraph.copy();
        copy.areas = copyAreas();
        if (playerId != -1) {
            copy.shuffleDevelopmentCards(playerId);
        }

        copy.gameStatus = gameStatus;
        copy.playerResults = playerResults.clone();
        copy.scores = scores.clone();
        copy.knights = knights.clone();
        copy.exchangeRates = new int[getNPlayers()][CatanParameters.Resources.values().length];
        for (int i = 0; i < exchangeRates.length; i++) {
            copy.exchangeRates[i] = exchangeRates[i].clone();
        }
        copy.victoryPoints = victoryPoints.clone();
        copy.longestRoadLength = longestRoadLength;
        copy.largestArmy = largestArmy;
        copy.longestRoad = longestRoad;
        copy.rollValue = rollValue;
        if (currentTradeOffer == null) {
            copy.currentTradeOffer = null;
        } else {
            copy.currentTradeOffer = (OfferPlayerTrade) this.currentTradeOffer.copy();
        }
        copy.rnd = new Random(rnd.nextLong());
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (getPlayerResults()[playerId] == Utils.GameResult.LOSE)
            return -1.0;
        if (getPlayerResults()[playerId] == Utils.GameResult.WIN)
            return 1.0;
        return (double) (scores[playerId]) / 10.0;
    }

    @Override
    public double getGameScore(int playerId) {
        return scores[playerId];
    }

    private HashMap<Integer, Area> copyAreas() {
        HashMap<Integer, Area> copy = new HashMap<>();
        for (int key : areas.keySet()) {
            Area a = areas.get(key);
            if (key != -1) {
                List<Component> oldComponents = areas.get(key).getComponents();
                // TODO: Partial Observability of Resource cards is not currently supported
                // Most Resource cards are countable - except as a result of the Steal action
                // (And, depending on one's interpretation, of Discard.)
                for (Component comp : oldComponents) {
                    a.putComponent(comp.copy());
                }
            }
            copy.put(key, a.copy());
        }
        return copy;
    }

    private void shuffleDevelopmentCards(int playerId) {
        Deck<Card> developmentDeck = (Deck<Card>) getComponent(developmentDeckHash, -1);
        int startingCardsInDeck = developmentDeck.getSize();
        if (getCurrentPlayer() != playerId && boughtDevCard != null) {
            developmentDeck.add(boughtDevCard);
        }
        for (int p = 0; p < getNPlayers(); p++) {
            if (p == playerId)
                continue;
            developmentDeck.add((Deck<Card>) getComponent(developmentDeckHash, p));
        }
        developmentDeck.shuffle(rnd);
        if (getCurrentPlayer() != playerId && boughtDevCard != null) {
            boughtDevCard = developmentDeck.draw();
        }
        for (int p = 0; p < getNPlayers(); p++) {
            if (p == playerId)
                continue;
            Deck<Card> playerDevDeck = (Deck<Card>) getComponent(developmentDeckHash, p);
            int cardsToDraw = playerDevDeck.getSize();
            playerDevDeck.clear();
            for (int i = 0; i < cardsToDraw; i++)
                playerDevDeck.add(developmentDeck.draw());
        }
        if (developmentDeck.getSize() != startingCardsInDeck)
            throw new AssertionError("We should have the same number of cards before as after");
    }


    public boolean checkRoadPlacement(int roadId, CatanTile tile, int player) {
        /*
         * @args:
         * roadId - Id of the road on tile
         * tile - tile on which we would like to build a road
         * gs - Game state */

        Graph<Settlement, Road> graph = getGraph();
        Road road = tile.getRoads()[roadId];

        // check if road is already taken
        if (road.getOwner() != -1) {
            return false;
        }
        // check if there is our settlement along edge
        Settlement settl1 = tile.getSettlements()[roadId];
        Settlement settl2 = tile.getSettlements()[(roadId + 1) % 6];
        if (settl1.getOwner() == player || settl2.getOwner() == player) {
            return true;
        }

        // check if there is a road on a neighbouring edge
        List<Road> roads = graph.getConnections(settl1);
        roads.addAll(graph.getConnections(settl2));
        for (Road rd : roads) {
            if (rd.getOwner() == player) {
                return true;
            }
        }
        return false;
    }

    public void updateExchangeRates(int player, int[] exchangeRates) {
        this.exchangeRates[player] = exchangeRates;
    }

    public boolean checkSettlementPlacement(Settlement settlement, int player) {
        // checks if any of the neighbouring settlements are already taken (distance rule)
        // if yes returns false otherwise true

        // if settlement is taken then cannot replace it
        if (settlement.getOwner() != -1) {
            return false;
        }

        // check if there is a settlement one distance away
        Graph<Settlement, Road> graph = getGraph();
        List<Settlement> settlements = graph.getNeighbourNodes(settlement);
        for (Settlement settl : settlements) {
            if (settl.getOwner() != -1) {
                return false;
            }
        }

        List<Road> roads = graph.getConnections(settlement);
        // check first if we have a road next to the settlement owned by the player
        // Doesn't apply in the setup phase
        if (!getGamePhase().equals(CatanGameState.CatanGamePhase.Setup)) {
            for (Road road : roads) {
                if (road.getOwner() == player) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private CatanTile[][] copyBoard() {
        CatanTile[][] copy = new CatanTile[board.length][board[0].length];
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[0].length; y++) {
                copy[x][y] = board[x][y].copy();
            }
        }
        return copy;
    }


    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<Integer>() {{
            Deck<Card> resourceDeck = (Deck<Card>) getComponent(resourceDeckHash);
            Deck<Card> devDeck = (Deck<Card>) getComponent(developmentDeckHash);
            add(resourceDeck.getComponentID());
            add(devDeck.getComponentID());
            for (Component c : resourceDeck.getComponents()) {
                add(c.getComponentID());
            }
            for (Component c : devDeck.getComponents()) {
                add(c.getComponentID());
            }
        }};
    }

    public OfferPlayerTrade getCurrentTradeOffer() {
        return currentTradeOffer;
    }

    public void setCurrentTradeOffer(OfferPlayerTrade currentTradeOffer) {
        this.currentTradeOffer = currentTradeOffer;
    }
}
