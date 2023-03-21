package games.catan;

import core.AbstractGameStateWithTurnOrder;
import core.AbstractParameters;
import core.components.*;
import core.interfaces.IGamePhase;
import core.turnorders.TurnOrder;
import games.GameType;
import games.catan.actions.OfferPlayerTrade;
import games.catan.components.*;

import java.util.*;

import static core.CoreConstants.*;
import static games.catan.CatanConstants.*;

public class CatanGameState extends AbstractGameStateWithTurnOrder {
    protected CatanTile[][] board;
    protected Graph<Settlement, Road> catanGraph;
    protected int[] scores; // score for each player
    protected int[] victoryPoints; // secret points from victory cards
    protected int[] knights; // knight count for each player
    protected HashMap<CatanParameters.Resource, Counter>[] exchangeRates; // exchange rate with bank for each resource
    protected int largestArmy; // playerID of the player currently holding the largest army
    protected int longestRoad; // playerID of the player currently holding the longest road
    protected int longestRoadLength;  // TODO but not largest army size?
    protected OfferPlayerTrade currentTradeOffer; // Holds the current trade offer to allow access between players TODO make primitive
    int rollValue;
    protected Random rnd;

    // TODO: copy and stuff
    HashMap<CatanParameters.Resource, Counter>[] playerResources;
    HashMap<CatanParameters.ActionType, Counter>[] playerTokens;
    Deck<CatanCard>[] playerDevCards;
    HashMap<CatanParameters.Resource, Counter> resourcePool;
    Deck<CatanCard> devCards;

    public HashMap<CatanParameters.ActionType, Counter>[] getPlayerTokens() {
        return playerTokens;
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
    protected TurnOrder _createTurnOrder(int nPlayers) {
        return new CatanTurnOrder(nPlayers, getGameParameters().getMaxRounds());
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Catan;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>(areas.values());
    }

    protected void _reset() {
        // set everything to null (except for Random number generator)
        board = null;
        currentTradeOffer = null;
        catanGraph = null;

        CatanParameters pp = (CatanParameters) gameParameters;
        scores = new int[getNPlayers()];
        knights = new int[getNPlayers()];
        exchangeRates = new int[getNPlayers()][CatanParameters.Resource.values().length];
        for (int[] exchangeRate : exchangeRates) Arrays.fill(exchangeRate, pp.default_exchange_rate);
        victoryPoints = new int[getNPlayers()];
        longestRoadLength = pp.min_longest_road;
        largestArmy = -1;
        longestRoad = -1;
        rnd = null;
    }

    @Override
    protected boolean _equals(Object obj) {
        if (obj instanceof CatanGameState) {
            CatanGameState o = (CatanGameState) obj;
            return catanGraph.equals(o.catanGraph) &&
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
        int result = Objects.hash(gameParameters, turnOrder, gameStatus, gamePhase, catanGraph,
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

    public void setGraph(Graph<Settlement, Road> graph) {
        this.catanGraph = graph;
    }

    public Graph<Settlement, Road> getGraph() {
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
        if (playerID < nPlayers) {
            victoryPoints[playerID] += 1;
        }
    }

    public int[] getVictoryPoints() {
        return victoryPoints.clone();
    }

    public HashMap<CatanParameters.Resource, Counter> getPlayerResources(int playerID) {
        return playerResources[playerID];
    }

    public Deck<CatanCard> getPLayerDevCards(int playerID) {
        return playerDevCards[playerID];
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

    public HashMap<CatanParameters.Resource, Counter> getExchangeRates(int playerID) {
        return exchangeRates[playerID];
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
    public boolean checkCost(HashMap<CatanParameters.Resource, Integer> cost, int playerId) {
        for (Map.Entry<CatanParameters.Resource, Integer> e: cost.entrySet()) {
            if (playerResources[playerId].get(e.getKey()).getValue() < cost.get(e.getKey())) return false;
        }
        return true;
    }

    /**
     * Swaps cards between players
     * @return
     */
    public boolean swapResources(int fromPlayer, int toPlayer, HashMap<CatanParameters.Resource, Integer> resourcesToTrade) {
        for (Map.Entry<CatanParameters.Resource, Integer> e: resourcesToTrade.entrySet()) {
            playerResources[fromPlayer].get(e.getKey()).decrement(e.getValue());
            playerResources[toPlayer].get(e.getKey()).increment(e.getValue());
        }
        return true;
    }

    /* Takes the resource cards specified in the cost array from the current player, returns true if successful */
    public boolean spendResourcesIfPossible(HashMap<CatanParameters.Resource, Integer> cost, int playerId) {
        if (!checkCost(cost, playerId)) return false;
        for (Map.Entry<CatanParameters.Resource, Integer> e: cost.entrySet()) {
            playerResources[playerId].get(e.getKey()).decrement(e.getValue());
        }
        return true;
    }

    @Override
    protected AbstractGameStateWithTurnOrder __copy(int playerId) {
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
        copy.exchangeRates = new int[getNPlayers()][CatanParameters.Resource.values().length];
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
            nCards[p] = playerDevCards[p].getSize();
            for (CatanCard c: playerDevCards[p].getComponents()) {
                devCards.add(c);
            }
            playerDevCards[p].clear();
        }
        devCards.shuffle(rnd);
        for (int p = 0; p < getNPlayers(); p++) {
            if (p == playerId)
                continue;
            for (int i = 0; i < nCards[p]; i++) {
                CatanCard c = devCards.draw();
                playerDevCards[p].add(c);
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
