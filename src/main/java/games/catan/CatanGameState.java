package games.catan;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import core.interfaces.IGamePhase;
import games.GameType;
import games.catan.actions.OfferPlayerTrade;
import games.catan.components.*;

import java.util.*;

import static core.CoreConstants.*;
import static games.catan.CatanConstants.*;

public class CatanGameState extends AbstractGameState {
    protected CatanTile[][] board;
    protected Graph catanGraph;
    protected int[] scores; // score for each player
    protected int[] victoryPoints; // secret points from victory cards
    protected int[] knights, roadLengths; // knight count and road length for each player
    protected List<HashMap<CatanParameters.Resource, Counter>> exchangeRates; // exchange rate with bank for each resource
    protected int largestArmyOwner; // playerID of the player currently holding the largest army
    protected int longestRoadOwner; // playerID of the player currently holding the longest road
    protected int longestRoadLength, largestArmySize;
    protected OfferPlayerTrade currentTradeOffer; // Holds the current trade offer to allow access between players TODO make primitive
    int rollValue;
    protected Random rnd;

    // TODO: copy and stuff, including roadLengths and largestArmySize above
    List<HashMap<CatanParameters.Resource, Counter>> playerResources;
    List<HashMap<CatanParameters.ActionType, Counter>> playerTokens;
    List<Deck<CatanCard>> playerDevCards;
    HashMap<CatanParameters.Resource, Counter> resourcePool;
    Deck<CatanCard> devCards;
    boolean developmentCardPlayed; // Tracks whether a player has played a development card this turn

    public List<HashMap<CatanParameters.ActionType, Counter>> getPlayerTokens() {
        return playerTokens;
    }

    public Deck<CatanCard> getDevCards() {
        return devCards;
    }

    public boolean noDevelopmentCardPlayed() {
        return !developmentCardPlayed;
    }

    public void setDevelopmentCardPlayed(boolean developmentCardPlayed) {
        this.developmentCardPlayed = developmentCardPlayed;
    }

    // GamePhases that may occur in Catan
    public enum CatanGamePhase implements IGamePhase {
        Setup,
        Trade,
        Build,
        Robber,
        Discard,
        Steal
    }

    public CatanGameState(AbstractParameters pp, int nPlayers) {
        super(pp, nPlayers);
        _reset();
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Catan;
    }

    @Override
    protected List<Component> _getAllComponents() {
        // TODO
        return new ArrayList<>();
    }

    protected void _reset() {
        // set everything to null (except for Random number generator)
        board = null;
        currentTradeOffer = null;
        catanGraph = null;
        scores = null;
        knights = null;
        exchangeRates = null;
        victoryPoints = null;
        longestRoadLength = -1;
        largestArmyOwner = -1;
        longestRoadOwner = -1;
        rnd = null;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatanGameState)) return false;
        if (!super.equals(o)) return false;
        CatanGameState that = (CatanGameState) o;
        return largestArmyOwner == that.largestArmyOwner && longestRoadOwner == that.longestRoadOwner && longestRoadLength == that.longestRoadLength && largestArmySize == that.largestArmySize && rollValue == that.rollValue && Arrays.deepEquals(board, that.board) && Objects.equals(catanGraph, that.catanGraph) && Arrays.equals(scores, that.scores) && Arrays.equals(victoryPoints, that.victoryPoints) && Arrays.equals(knights, that.knights) && Arrays.equals(roadLengths, that.roadLengths) && Objects.equals(exchangeRates, that.exchangeRates) && Objects.equals(currentTradeOffer, that.currentTradeOffer) && Objects.equals(rnd, that.rnd) && Objects.equals(playerResources, that.playerResources) && Objects.equals(playerTokens, that.playerTokens) && Objects.equals(playerDevCards, that.playerDevCards) && Objects.equals(resourcePool, that.resourcePool) && Objects.equals(devCards, that.devCards);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), catanGraph, exchangeRates, largestArmyOwner, longestRoadOwner, longestRoadLength, largestArmySize, currentTradeOffer, rollValue, rnd, playerResources, playerTokens, playerDevCards, resourcePool, devCards);
        result = 31 * result + Arrays.deepHashCode(board);
        result = 31 * result + Arrays.hashCode(scores);
        result = 31 * result + Arrays.hashCode(victoryPoints);
        result = 31 * result + Arrays.hashCode(knights);
        result = 31 * result + Arrays.hashCode(roadLengths);
        return result;
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

    public void setRollValue(int rollValue) {
        this.rollValue = rollValue;
    }

    public int getRollValue() {
        return rollValue;
    }

    public void setLongestRoadLength(int longestRoadLength) {
        this.longestRoadLength = longestRoadLength;
    }

    public int[] getRoadLengths() {
        return roadLengths;
    }

    public CatanTile getRobber(CatanTile[][] board) {
        for (CatanTile[] catanTiles : board) {
            for (CatanTile tile : catanTiles) {
                if (tile.hasRobber()) {
                    return tile;
                }
            }
        }
        System.out.println("Robber not found - should not happen");
        return null;
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
        if (playerID < nPlayers) {
            victoryPoints[playerID] += 1;
        }
    }

    public int[] getVictoryPoints() {
        return victoryPoints.clone();
    }

    public HashMap<CatanParameters.Resource, Counter> getPlayerResources(int playerID) {
        return playerResources.get(playerID);
    }

    public Deck<CatanCard> getPlayerDevCards(int playerID) {
        return playerDevCards.get(playerID);
    }

    public void updateLargestArmy(CatanParameters params) {
        /* Checks the army sizes and updates the scores accordingly */
        if (largestArmyOwner == -1) {
            // check if any of them meets the minimum required army size
            for (int i = 0; i < knights.length; i++) {
                if (knights[i] >= params.min_army_size) {
                    largestArmyOwner = i;
                    scores[i] += params.largest_army_value;
                }
            }
        } else {
            int max = knights[largestArmyOwner];
            for (int i = 0; i < knights.length; i++) {
                if (knights[i] > max) {
                    // update scores
                    scores[largestArmyOwner] -= params.largest_army_value;
                    scores[i] += params.largest_army_value;

                    max = knights[i];
                    largestArmyOwner = i;
                }
            }
        }
    }


    public int[] getScores() {
        return scores;
    }

    public HashMap<CatanParameters.Resource, Counter> getExchangeRates(int playerID) {
        return exchangeRates.get(playerID);
    }

    public int getRoadDistance(int x, int y, int edge) {
        // As the settlements are the nodes, we expand them to find roads
        // calculates the distance length of the road
        HashSet<Road> roadSet = new HashSet<>();
        HashSet<Road> roadSet2 = new HashSet<>();

        ArrayList<Building> dir1 = new ArrayList<>();
        ArrayList<Building> dir2 = new ArrayList<>();
        Building settl1 = board[x][y].getSettlements()[edge];
        Building settl2 = board[x][y].getSettlements()[(edge + 1) % 6];

        dir1.add(settl1);
        dir2.add(settl2);

        // find longest segment, we first follow dir_1 then dir_2
        roadSet = expandRoad(roadSet, new ArrayList<>(dir1), new ArrayList<>(dir2));
        roadSet.addAll(expandRoad(roadSet2, new ArrayList<>(dir2), new ArrayList<>(dir1)));

//        System.out.println("Current road length is " + roadSet.size() + " for player " + getCurrentPlayer());
        return roadSet.size();
    }

    private HashSet<Road> expandRoad(HashSet<Road> roadSet, List<Building> unexpanded, List<Building> expanded) {
        // return length, makes it possible to compare segments
        // modify original set
        if (unexpanded.size() == 0) {
            return roadSet;
        }
        if (unexpanded.size() == 2) {
            // Handle branching
            int length = 0;
            HashSet<Road> longestSegment = new HashSet<>(roadSet);
            for (Building settlement : unexpanded) {
                ArrayList<Building> toExpand = new ArrayList<>();
                toExpand.add(settlement);
                HashSet<Road> roadSetCopy = new HashSet<>(roadSet);
                roadSetCopy = expandRoad(roadSetCopy, toExpand, expanded);
                if (roadSetCopy.size() >= length) {
                    length = roadSetCopy.size();
                    longestSegment = roadSetCopy;
                }
            }
            roadSet.addAll(longestSegment);
            return roadSet;
        } else {
            // case of expanding a single settlement
            Building settlement = unexpanded.remove(0);
            expanded.add(settlement);

            List<Edge> edges = getGraph().getEdges(settlement);
            if (edges != null) {
                for (Edge e : edges) {
                    Road road = e.getRoad();
                    if (road.getOwnerId() == getCurrentPlayer()) {
                        if (expanded.contains(e.getDest())) {
                            // The road used to get here
                            roadSet.add(road);
                        } else {
                            // if settlement belongs to somebody else it's a deadend
                            if (e.getDest().getOwnerId() == -1 || e.getDest().getOwnerId() == getCurrentPlayer()) {
                                unexpanded.add(e.getDest());
                            }
                        }
                    }

                }

            }
        }

        // only gets here when explored a single road
        return expandRoad(roadSet, unexpanded, expanded);
    }

    public int getNResourcesInHand(int player) {
        int deckSize = 0;
        for (Map.Entry<CatanParameters.Resource, Counter> e: playerResources.get(player).entrySet()) {
            deckSize += e.getValue().getValue();
        }
        return deckSize;
    }

    public CatanParameters.Resource pickResourceFromHand(int player, int index) {
        int i = 0;
        for (Map.Entry<CatanParameters.Resource, Counter> e: playerResources.get(player).entrySet()) {
            if (index < i) return e.getKey();
            i += e.getValue().getValue();
        }
        return null;
    }

    public ArrayList<Road> getRoads() {
        // Function that returns all the roads from the board
        int counter = 0;
        ArrayList<Road> roads = new ArrayList<>();
        for (CatanTile[] catanTiles : board) {
            for (CatanTile tile : catanTiles) {
                for (int i = 0; i < HEX_SIDES; i++) {
                    // Road has already been set
                    Road road = tile.getRoads()[i];
                    if (road.getOwnerId() != -1) {
                        roads.add(road);
                        counter += 1;
                    }
                }
            }
        }
        System.out.println("There are " + counter + " roads");
        return roads;
    }

    public ArrayList<Building> getSettlements() {
        // Function that returns all the settlements from the board
        int counter = 0;
        ArrayList<Building> settlements = new ArrayList<>();
        for (CatanTile[] catanTiles : board) {
            for (CatanTile tile : catanTiles) {
                for (int i = 0; i < 6; i++) {
                    // Road has already been set
                    Building settlement = tile.getSettlements()[i];
                    if (settlement.getOwnerId() != -1) {
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
        return longestRoadOwner;
    }

    public void setLongestRoadOwner(int longestRoadOwner) {
        this.longestRoadOwner = longestRoadOwner;
    }

    public int getLongestRoadLength() {
        return longestRoadLength;
    }

    public int getLargestArmySize() {
        return largestArmySize;
    }

    public int getLargestArmyOwner() {
        return largestArmyOwner;
    }

    public void setLargestArmyOwner(int largestArmyOwner) {
        this.largestArmyOwner = largestArmyOwner;
    }

    public void setLargestArmySize(int largestArmySize) {
        this.largestArmySize = largestArmySize;
    }

    public ArrayList<Building> getPlayersSettlements(int playerId) {
        ArrayList<Building> playerSettlements = new ArrayList<>();
        ArrayList<Building> allSettlements = getSettlements();
        for (Building allSettlement : allSettlements) {
            if (allSettlement.getOwnerId() == playerId) {
                playerSettlements.add(allSettlement);
            }
        }
        return playerSettlements;
    }

    /* checks if given resources cover the price or not */
    public boolean checkCost(HashMap<CatanParameters.Resource, Integer> cost, int playerId) {
        for (Map.Entry<CatanParameters.Resource, Integer> e: cost.entrySet()) {
            if (playerResources.get(playerId).get(e.getKey()).getValue() < cost.get(e.getKey())) return false;
        }
        return true;
    }

    /**
     * Swaps cards between players
     * @return - true if successful, false otherwise
     */
    public boolean swapResources(int fromPlayer, int toPlayer, HashMap<CatanParameters.Resource, Integer> resourcesToTrade) {
        for (Map.Entry<CatanParameters.Resource, Integer> e: resourcesToTrade.entrySet()) {
            playerResources.get(fromPlayer).get(e.getKey()).decrement(e.getValue());
            playerResources.get(toPlayer).get(e.getKey()).increment(e.getValue());
        }
        return true;
    }

    /* Takes the resource cards specified in the cost array from the current player, returns true if successful */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean spendResourcesIfPossible(HashMap<CatanParameters.Resource, Integer> cost, int playerId) {
        if (!checkCost(cost, playerId)) return false;
        for (Map.Entry<CatanParameters.Resource, Integer> e: cost.entrySet()) {
            playerResources.get(playerId).get(e.getKey()).decrement(e.getValue());
        }
        return true;
    }

    @Override
    protected CatanGameState _copy(int playerId) {
        CatanGameState copy = new CatanGameState(getGameParameters(), getNPlayers());
        copy.gamePhase = gamePhase;
        copy.board = copyBoard();
        copy.catanGraph = catanGraph.copy();
        if (playerId != -1) {
            copy.shuffleDevelopmentCards(playerId);
        }

        copy.gameStatus = gameStatus;
        copy.playerResults = playerResults.clone();
        copy.scores = scores.clone();
        copy.knights = knights.clone();
//        copy.exchangeRates = new int[getNPlayers()][CatanParameters.Resource.values().length];
//        for (int i = 0; i < exchangeRates.length; i++) {
//            copy.exchangeRates[i] = exchangeRates[i].clone();
//        }
        copy.victoryPoints = victoryPoints.clone();
        copy.longestRoadLength = longestRoadLength;
        copy.largestArmyOwner = largestArmyOwner;
        copy.longestRoadOwner = longestRoadOwner;
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
        if (getPlayerResults()[playerId] == GameResult.LOSE_GAME)
            return -1.0;
        if (getPlayerResults()[playerId] == GameResult.WIN_GAME)
            return 1.0;
        return (double) (scores[playerId]) / 10.0;
    }

    @Override
    public double getGameScore(int playerId) {
        return scores[playerId];
    }

    private void shuffleDevelopmentCards(int playerId) {
        int[] nCards = new int[nPlayers];
        for (int p = 0; p < getNPlayers(); p++) {
            if (p == playerId)
                continue;
            nCards[p] = playerDevCards.get(p).getSize();
            for (CatanCard c: playerDevCards.get(p).getComponents()) {
                devCards.add(c);
            }
            playerDevCards.get(p).clear();
        }
        devCards.shuffle(rnd);
        for (int p = 0; p < getNPlayers(); p++) {
            if (p == playerId)
                continue;
            for (int i = 0; i < nCards[p]; i++) {
                CatanCard c = devCards.draw();
                playerDevCards.get(p).add(c);
            }
        }
    }

    public HashMap<CatanParameters.Resource, Counter> getResourcePool() {
        return resourcePool;
    }

    public boolean checkRoadPlacement(int roadId, CatanTile tile, int player) {
        /*
         * @args:
         * roadId - Id of the road on tile
         * tile - tile on which we would like to build a road
         * gs - Game state */

        Graph graph = getGraph();
        Road road = tile.getRoads()[roadId];

        // check if road is already taken
        if (road.getOwnerId() != -1) {
            return false;
        }
        // check if there is our settlement along edge
        Building settl1 = tile.getSettlements()[roadId];
        Building settl2 = tile.getSettlements()[(roadId + 1) % 6];
        if (settl1.getOwnerId() == player || settl2.getOwnerId() == player) {
            return true;
        }

        // check if there is a road on a neighbouring edge
        List<Edge> roads = graph.getConnections(settl1);
        roads.addAll(graph.getConnections(settl2));
        for (Edge rd : roads) {
            if (rd.getRoad().getOwnerId() == player) {
                return true;
            }
        }
        return false;
    }

    public void updateExchangeRates(int player, HashMap<CatanParameters.Resource, Integer> exchangeRates) {
        for (Map.Entry<CatanParameters.Resource, Integer> e: exchangeRates.entrySet()) {
            this.exchangeRates.get(player).get(e.getKey()).setValue(e.getValue());
        }
    }

    public boolean checkSettlementPlacement(Building settlement, int player) {
        // checks if any of the neighbouring settlements are already taken (distance rule)
        // if yes returns false otherwise true

        // if settlement is taken then cannot replace it
        if (settlement.getOwnerId() != -1) {
            return false;
        }

        // check if there is a settlement one distance away
        Graph graph = getGraph();
        List<Building> settlements = graph.getNeighbourNodes(settlement);
        for (Building settl : settlements) {
            if (settl.getOwnerId() != -1) {
                return false;
            }
        }

        List<Edge> roads = graph.getConnections(settlement);
        // check first if we have a road next to the settlement owned by the player
        // Doesn't apply in the setup phase
        if (!getGamePhase().equals(CatanGameState.CatanGamePhase.Setup)) {
            for (Edge edge : roads) {
                if (edge.getRoad().getOwnerId() == player) {
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
            add(devCards.getComponentID());
            for (Component c : devCards.getComponents()) {
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
