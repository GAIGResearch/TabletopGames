package games.catan;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.*;
import core.interfaces.IGamePhase;
import games.GameType;
import games.catan.actions.build.BuyAction;
import games.catan.components.Building;
import games.catan.components.CatanCard;
import games.catan.components.CatanTile;

import java.util.*;

import static core.CoreConstants.GameResult;
import static games.catan.CatanConstants.HEX_SIDES;
import static games.catan.stats.CatanMetrics.CatanEvent.LargestArmySteal;

public class CatanGameState extends AbstractGameState {
    protected CatanTile[][] board;
    protected GraphBoardWithEdges catanGraph;
    protected int[] scores; // score for each player
    protected int[] victoryPoints; // secret points from victory cards
    protected int[] knights, roadLengths; // knight count and road length for each player
    protected List<Map<CatanParameters.Resource, Counter>> exchangeRates; // exchange rate with bank for each resource
    protected int largestArmyOwner; // playerID of the player currently holding the largest army
    protected int longestRoadOwner; // playerID of the player currently holding the longest road
    protected int longestRoadLength, largestArmySize;
    int rollValue;
    Random diceRnd;

    List<Map<CatanParameters.Resource, Counter>> playerResources;
    List<Map<BuyAction.BuyType, Counter>> playerTokens;
    List<Deck<CatanCard>> playerDevCards;
    Map<CatanParameters.Resource, Counter> resourcePool;
    Deck<CatanCard> devCards;
    boolean developmentCardPlayed; // Tracks whether a player has played a development card this turn

    AbstractAction tradeOffer;
    public int negotiationStepsCount;
    public int nTradesThisTurn;

    // Details of the current trade offer (if any, may be null)
    public AbstractAction getTradeOffer() {
        return tradeOffer;
    }

    public void setTradeOffer(AbstractAction tradeOffer) {
        this.tradeOffer = tradeOffer;
    }

    public List<Map<BuyAction.BuyType, Counter>> getPlayerTokens() {
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
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Catan;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>() {{
            add(catanGraph);
            for (int i = 0; i < nPlayers; i++) {
                addAll(exchangeRates.get(i).values());
                addAll(playerResources.get(i).values());
                addAll(playerTokens.get(i).values());
            }
            addAll(playerDevCards);
            addAll(resourcePool.values());
            add(devCards);
            for (CatanTile[] tiles: board) {
                this.addAll(Arrays.asList(tiles));
            }
        }};
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatanGameState)) return false;
        if (!super.equals(o)) return false;
        CatanGameState that = (CatanGameState) o;
        return largestArmyOwner == that.largestArmyOwner && longestRoadOwner == that.longestRoadOwner && longestRoadLength == that.longestRoadLength && largestArmySize == that.largestArmySize && rollValue == that.rollValue && developmentCardPlayed == that.developmentCardPlayed && negotiationStepsCount == that.negotiationStepsCount && nTradesThisTurn == that.nTradesThisTurn && Arrays.deepEquals(board, that.board) && Objects.equals(catanGraph, that.catanGraph) && Arrays.equals(scores, that.scores) && Arrays.equals(victoryPoints, that.victoryPoints) && Arrays.equals(knights, that.knights) && Arrays.equals(roadLengths, that.roadLengths) && Objects.equals(exchangeRates, that.exchangeRates) && Objects.equals(playerResources, that.playerResources) && Objects.equals(playerTokens, that.playerTokens) && Objects.equals(playerDevCards, that.playerDevCards) && Objects.equals(resourcePool, that.resourcePool) && Objects.equals(devCards, that.devCards) && Objects.equals(tradeOffer, that.tradeOffer);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), catanGraph, exchangeRates, largestArmyOwner, longestRoadOwner, longestRoadLength, largestArmySize, rollValue, playerResources, playerTokens, playerDevCards, resourcePool, devCards, developmentCardPlayed, tradeOffer, negotiationStepsCount, nTradesThisTurn);
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

    public void setGraph(GraphBoardWithEdges graph) {
        this.catanGraph = graph;
    }

    public GraphBoardWithEdges getGraph() {
        return catanGraph;
    }

    public void setRollValue(int rollValue) {
        this.rollValue = rollValue;
    }

    // value of the currently rolled dice
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

    // The number of knights that each player has
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

    public Map<CatanParameters.Resource, Counter> getPlayerResources(int playerID) {
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
                if (knights[i] > max && largestArmyOwner != i) {
                    logEvent(LargestArmySteal, String.valueOf(i));

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

    public Map<CatanParameters.Resource, Counter> getExchangeRates(int playerID) {
        return exchangeRates.get(playerID);
    }

    // The road distance between two adjacent settlements, one at x, y, the other along the specified edgeIdx
    public int getRoadDistance(int x, int y, int edgeIdx) {
        // As the settlements are the nodes, we expand them to find roads
        // calculates the distance length of the road
        Set<Edge> roadSet = new LinkedHashSet<>();
        Set<Edge> roadSet2 = new LinkedHashSet<>();

        ArrayList<Building> dir1 = new ArrayList<>();
        ArrayList<Building> dir2 = new ArrayList<>();
        Building settl1 = (Building) catanGraph.getNodeByID(board[x][y].getVerticesBoardNodeIDs()[edgeIdx]);
        Building settl2 = (Building) catanGraph.getNodeByID(board[x][y].getVerticesBoardNodeIDs()[(edgeIdx + 1) % 6]);

        dir1.add(settl1);
        dir2.add(settl2);

        // find longest segment, we first follow dir_1 then dir_2
        roadSet = expandRoad(roadSet, new ArrayList<>(dir1), new ArrayList<>(dir2));
        roadSet.addAll(expandRoad(roadSet2, new ArrayList<>(dir2), new ArrayList<>(dir1)));

//        System.out.println("Current road length is " + roadSet.size() + " for player " + getCurrentPlayer());
        return roadSet.size();
    }

    private Set<Edge> expandRoad(Set<Edge> roadSet, List<Building> unexpanded, List<Building> expanded) {
        // return length, makes it possible to compare segments
        // modify original set
        if (unexpanded.size() == 0) {
            return roadSet;
        }
        if (unexpanded.size() == 2) {
            // Handle branching
            int length = 0;
            Set<Edge> longestSegment = new LinkedHashSet<>(roadSet);
            for (Building settlement : unexpanded) {
                ArrayList<Building> toExpand = new ArrayList<>();
                toExpand.add(settlement);
                Set<Edge> roadSetCopy = new LinkedHashSet<>(roadSet);
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

            for (Map.Entry<Edge, BoardNodeWithEdges> e : settlement.getNeighbourEdgeMapping().entrySet()) {
                if (e.getKey().getOwnerId() == getCurrentPlayer()) {
                    if (expanded.contains((Building)e.getValue())) {
                        // The road used to get here
                        roadSet.add(e.getKey());
                    } else {
                        // if settlement belongs to somebody else it's a deadend
                        if (e.getValue().getOwnerId() == -1 || e.getValue().getOwnerId() == getCurrentPlayer()) {
                            unexpanded.add((Building) e.getValue());
                        }
                    }
                }
            }
        }

        // only gets here when explored a single road
        return expandRoad(roadSet, unexpanded, expanded);
    }

    // The number of resource cards in a player's hand
    public int getNResourcesInHand(int player) {
        int deckSize = 0;
        for (Map.Entry<CatanParameters.Resource, Counter> e: playerResources.get(player).entrySet()) {
            if (e.getKey() == CatanParameters.Resource.WILD) continue;
            deckSize += e.getValue().getValue();
        }
        return deckSize;
    }

    public CatanParameters.Resource pickResourceFromHand(int player, int index) {
        int i = 0;
        CatanParameters.Resource res = null;
        for (Map.Entry<CatanParameters.Resource, Counter> e: playerResources.get(player).entrySet()) {
            if (e.getKey() == CatanParameters.Resource.WILD) continue;
            res = e.getKey();
            if (index < i) return res;
            i += e.getValue().getValue();
        }
        if (index < i) return res;
        return null;
    }

    public ArrayList<BoardNodeWithEdges> getSettlements() {
        // Function that returns all the settlements from the board
        return new ArrayList<>(catanGraph.getBoardNodes());
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

    public ArrayList<BoardNodeWithEdges> getPlayersSettlements(int playerId) {
        ArrayList<BoardNodeWithEdges> playerSettlements = new ArrayList<>();
        ArrayList<BoardNodeWithEdges> allSettlements = getSettlements();
        for (BoardNodeWithEdges allSettlement : allSettlements) {
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
    public boolean checkCost(CatanParameters.Resource resource, int nRequired, int playerId) {
        return playerResources.get(playerId).get(resource).getValue() >= nRequired;
    }

    /**
     * Swaps cards between players
     * @return - true if successful, false otherwise
     */
    public boolean swapResources(int fromPlayer, int toPlayer, CatanParameters.Resource resource, int nResources) {
        playerResources.get(fromPlayer).get(resource).decrement(nResources);
        playerResources.get(toPlayer).get(resource).increment(nResources);
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
        CatanGameState copy = new CatanGameState(getGameParameters().copy(), getNPlayers());
        copy.gamePhase = gamePhase;
        copy.board = copyBoard();
        copy.catanGraph = catanGraph.copy();

        copy.gameStatus = gameStatus;
        copy.playerResults = playerResults.clone();
        copy.scores = scores.clone();
        copy.knights = knights.clone();
        copy.roadLengths = roadLengths.clone();

        copy.tradeOffer = tradeOffer != null? tradeOffer.copy() : null;
        copy.negotiationStepsCount = negotiationStepsCount;

        copy.diceRnd = new Random(redeterminisationRnd.nextLong());

        copy.devCards = devCards.copy();

        copy.exchangeRates = new ArrayList<>();
        copy.playerResources = new ArrayList<>();
        copy.playerDevCards = new ArrayList<>();
        copy.playerTokens = new ArrayList<>();
        copy.victoryPoints = victoryPoints.clone();

//        // Resources in hand are shuffled if PO
//        List<CatanParameters.Resource> availableRes = new ArrayList<>();
//
//        // PO
//        if (playerId != -1 || !getCoreGameParameters().partialObservable) {
//            for (CatanParameters.Resource r : resourcePool.keySet()) {
//                int nAvailable = ((CatanParameters) gameParameters).n_resource_cards - resourcePool.get(r).getValue();
//                if (nAvailable > 0) {
//                    for (int j = 0; j < nAvailable; j++) {
//                        availableRes.add(r);
//                    }
//                }
//            }
//        }

        for (int i = 0; i < getNPlayers(); i++) {
            Map<CatanParameters.Resource, Counter> exchangeRate = new HashMap<>();
            for (Map.Entry<CatanParameters.Resource, Counter> e: exchangeRates.get(i).entrySet()) {
                exchangeRate.put(e.getKey(), e.getValue().copy());
            }
            copy.exchangeRates.add(exchangeRate);

            // Resources in hand
            Map<CatanParameters.Resource, Counter> playerRes = new HashMap<>();
            for (Map.Entry<CatanParameters.Resource, Counter> e: playerResources.get(i).entrySet()) {
                playerRes.put(e.getKey(), e.getValue().copy());
            }
            copy.playerResources.add(playerRes);

            // Dev cards in hand
            copy.playerDevCards.add(playerDevCards.get(i).copy());

            // PO
            if (playerId != -1 || !getCoreGameParameters().partialObservable) {
                if (i != playerId) {
                    // Resources in hand are hidden
//                    for (CatanParameters.Resource r : playerResources.get(i).keySet()) {
//                        copy.playerResources.get(i).get(r).setValue(0);
//                    }

                    // VP from dev cards hidden too
                    copy.victoryPoints[i] = 0;
                } else {
//                    // Remove from list of resources that may be in other player's hands the ones we know are in our hand
//                    for (Map.Entry<CatanParameters.Resource, Counter> e: playerResources.get(i).entrySet()) {
//                        for (int j = 0; j < e.getValue().getValue(); j++) {
//                            availableRes.remove(e.getKey());
//                        }
//                    }
                }
            }

            // Player tokens
            Map<BuyAction.BuyType, Counter> playerTok = new HashMap<>();
            for (Map.Entry<BuyAction.BuyType, Counter> e: playerTokens.get(i).entrySet()) {
                playerTok.put(e.getKey(), e.getValue().copy());
            }
            copy.playerTokens.add(playerTok);
        }
        copy.longestRoadLength = longestRoadLength;
        copy.largestArmySize = largestArmySize;
        copy.largestArmyOwner = largestArmyOwner;
        copy.longestRoadOwner = longestRoadOwner;
        copy.rollValue = rollValue;
        copy.nTradesThisTurn = nTradesThisTurn;
        copy.rnd = rnd;

        copy.developmentCardPlayed = developmentCardPlayed;

        copy.resourcePool = new HashMap<>();
        for (Map.Entry<CatanParameters.Resource, Counter> e: resourcePool.entrySet()) {
            copy.resourcePool.put(e.getKey(), e.getValue().copy());
        }

        // PO
        if (playerId != -1 || !getCoreGameParameters().partialObservable) {
            // Combine dev cards with those in hand of unknown players. Shuffle and re-deal to players.
            copy.shuffleDevelopmentCards(playerId);

            // Resources in hand are hidden
//            for (int i = 0; i < nPlayers; i++) {
//                if (i != playerId) {
//                    int nInHand = getNResourcesInHand(i);
//                    for (int j = 0; j < nInHand; j++) {
//                        if (availableRes.isEmpty()) break;
//                        CatanParameters.Resource r = availableRes.remove(rnd.nextInt(availableRes.size()));
//                        copy.playerResources.get(i).get(r).increment();
//                    }
//                }
//
//            }
        }

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
        // Dev cards in hand are hidden and shuffled with the main deck
        int[][] turnCardsWereBoughtIn = new int[nPlayers][];
        for (int p = 0; p < getNPlayers(); p++) {
            if (p == playerId)
                continue;
            devCards.add(playerDevCards.get(p));
            int nCards = playerDevCards.get(p).getSize();
            turnCardsWereBoughtIn[p] = new int[nCards];
            for (int i = 0; i < nCards; i++) {
                turnCardsWereBoughtIn[p][i] = playerDevCards.get(p).get(i).roundCardWasBought;
            }
            playerDevCards.get(p).clear();
        }
        devCards.shuffle(redeterminisationRnd);
        for (int p = 0; p < getNPlayers(); p++) {
            if (p == playerId)
                continue;
            for (int i = 0; i < turnCardsWereBoughtIn[p].length; i++) {
                CatanCard c = devCards.draw();
                c.roundCardWasBought = turnCardsWereBoughtIn[p][i];  // Assign round when card was bought accurately, as this is known information
                playerDevCards.get(p).add(c);
                if (c.cardType == CatanCard.CardType.VICTORY_POINT_CARD){
                    addVictoryPoint(p);
                }
            }
        }
    }

    public Map<CatanParameters.Resource, Counter> getResourcePool() {
        return resourcePool;
    }

    public Building getBuilding(CatanTile tile, int vertex) {
        return (Building) catanGraph.getNodeByID(tile.getVerticesBoardNodeIDs()[vertex]);
    }
    public Building[] getBuildings(CatanTile tile) {
        Building[] buildings = new Building[HEX_SIDES];
        for (int i = 0; i < HEX_SIDES; i++) {
            buildings[i] = getBuilding(tile, i);
        }
        return buildings;
    }
    public Edge getRoad(Building building, CatanTile tile, int edge) {
        return building.getEdgeByID(tile.getEdgeIDs()[edge]);
    }
    public Edge getRoad(CatanTile tile, int vertex, int edge) {
        return getBuilding(tile, vertex).getEdgeByID(tile.getEdgeIDs()[edge]);
    }
    public Edge[] getRoads(CatanTile tile) {
        Edge[] roads = new Edge[HEX_SIDES];
        for (int i = 0; i < HEX_SIDES; i++) {
            roads[i] = getRoad(tile, i, i);
        }
        return roads;
    }

    /**
     * Check if can place road on edge of tile
     * @param edge - Edge
     * @param tile- tile on which we would like to build a road
     * @param player- playerID
     * @return true if can place road on given edge, false otherwise
     */
    public boolean checkRoadPlacement(CatanTile tile, int v1, int v2, Edge edge, int player) {
        GraphBoardWithEdges graph = getGraph();
        BoardNodeWithEdges origin = graph.getNodeByID(tile.getVerticesBoardNodeIDs()[v1]);
        BoardNodeWithEdges end = graph.getNodeByID(tile.getVerticesBoardNodeIDs()[v2]);

        // check if road is already taken
        if (edge == null || edge.getOwnerId() != -1) {
            return false;
        }

        // check if there is our settlement along edge
        if (origin.getOwnerId() == player || end.getOwnerId() == player) {
            return true;
        }

        // check if there is a road of ours on a neighbouring edge
        for (Edge rd : origin.getEdges()) {
            if (rd.getOwnerId() == player) {
                return true;
            }
        }
        for (Edge rd : end.getEdges()) {
            if (rd.getOwnerId() == player) {
                return true;
            }
        }
        return false;
    }

    public boolean checkSettlementPlacement(Building settlement, int player) {
        // checks if any of the neighbouring settlements are already taken (distance rule)
        // if yes returns false otherwise true

        // if settlement is taken then cannot replace it
        if (settlement.getOwnerId() != -1) {
            return false;
        }

        // check if there is a settlement one distance away
        Map<Edge, BoardNodeWithEdges> neighboursWithRoads = settlement.getNeighbourEdgeMapping();
        for (Map.Entry<Edge, BoardNodeWithEdges> e : neighboursWithRoads.entrySet()) {
            if (e.getValue().getOwnerId() != -1) {
                return false;
            }
        }
        // check if we have a road next to the intended settlement owned by the player
        // Doesn't apply in the setup phase
        if (!getGamePhase().equals(CatanGameState.CatanGamePhase.Setup)) {
            for (Map.Entry<Edge, BoardNodeWithEdges> e : neighboursWithRoads.entrySet()) {
                if (e.getKey().getOwnerId() == player) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
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
}
