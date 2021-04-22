package games.catan;

import core.AbstractParameters;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
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

import java.util.*;
import java.util.stream.Collectors;

import static core.CoreConstants.*;
import static games.catan.CatanConstants.*;
import static games.pandemic.PandemicConstants.infectionHash;
import static games.pandemic.PandemicConstants.playerDeckHash;

public class CatanGameState extends AbstractGameState {
    protected CatanTile[][] board;
    protected Graph<Settlement, Road> catanGraph;
    protected Card boughtDevCard; // used to keep a reference to a dev card bought in the current turn to avoid playing it
    protected int scores[]; // score for each player
    protected int victoryPoints[]; // secret points from victory cards
    protected int knights[]; // knight count for each player
    protected int exchangeRates[][]; // exchange rate with bank for each resource
    protected int largestArmy = -1; // playerID of the player currently holding the largest army
    protected int longestRoad = -1; // playerID of the player currently holding the longest road
    protected int longestRoadLength = 0;
    protected OfferPlayerTrade currentTradeOffer = null; // Holds the current trade offer to allow access between players
    int rollValue;

    // GamePhases that may occur in Catan
    public enum CatanGamePhase implements IGamePhase {
        Setup,
        Trade,
        Build,
        Robber,
        Discard,
        Steal,
        TradeReaction,
        PlaceRoad,
    }

    // Collection of areas, mapped to player ID, -1 is the general game area containing the board, counters and several decks.
    HashMap<Integer, Area> areas;

    public CatanGameState(AbstractParameters pp, int nPlayers) {
        super(pp, new CatanTurnOrder(nPlayers, ((CatanParameters)pp).n_actions_per_turn), GameType.Catan);
        scores = new int[((CatanParameters) pp).n_players];
        knights = new int[((CatanParameters) pp).n_players];
        exchangeRates = new int[((CatanParameters) pp).n_players][CatanParameters.Resources.values().length];
        for (int i = 0; i < exchangeRates.length; i++)
            Arrays.fill(exchangeRates[i], ((CatanParameters)pp).default_exchange_rate);
        victoryPoints = new int[((CatanParameters) pp).n_players];
        longestRoadLength = ((CatanParameters) pp).min_longest_road;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(areas.values());
        return components;
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
        // set everything to null
        this.areas = null;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }

    public void setBoard(CatanTile[][] board){
        this.board = board;
    }

    public CatanTile[][] getBoard(){
        return board;
    }

    public void setGraph(Graph graph){
        this.catanGraph = graph;
    }

    public Graph getGraph(){
        return catanGraph;
    }

    public int getRollValue(){
        return rollValue;
    }

    public void setRollValue(int rollValue){
        this.rollValue = rollValue;
    }

    public CatanTile getRobber(CatanTile[][] board){
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

    public void addScore(int playerID, int score){
        if (playerID < scores.length) {
            scores[playerID] += score;
        }
    }

    public void addKnight(int playerID){
        if (playerID < knights.length){
            knights[playerID] += 1;
            updateLargestArmy((CatanParameters)getGameParameters());
        }
    }

    public int[] getKnights(){
        return Arrays.copyOf(knights, knights.length);
    }

    public void addVictoryPoint(int playerID){
        if (playerID < knights.length){
            victoryPoints[playerID] += 1;
        }
    }

    public int[] getVictoryPoints(){
        return victoryPoints.clone();
    }

    public int[] getPlayerResources(int playerID){
        Deck<Card> playerHand = (Deck<Card>)this.getComponent(CoreConstants.playerHandHash, playerID);
        int[] resources = new int[CatanParameters.Resources.values().length];

        for (Card card: playerHand.getComponents()){
            resources[CatanParameters.Resources.valueOf(card.getProperty(cardType).toString()).ordinal()] += 1;
        }
        return resources;
    }

    public int[] getPLayerDevCards(int playerID){
        Deck<Card> playerDevCards = (Deck<Card>)this.getComponent(developmentDeckHash, playerID);
        int[] devCards = new int[CatanParameters.CardTypes.values().length];

        for (Card card: playerDevCards.getComponents()){
            devCards[CatanParameters.CardTypes.valueOf(card.getProperty(cardType).toString()).ordinal()] += 1;
        }
        return devCards;
    }

    public int updateLargestArmy(CatanParameters params){
        /* Checks the army sizes and updates the scores accordingly */
        if (largestArmy == -1){
            // check if any of them meets the minimum required army size
            for (int i = 0; i < knights.length; i++){
                if (knights[i] >= params.min_army_size) {
                    largestArmy = i;
                    scores[i] += params.largest_army_value;
                }
            }
        } else{
            int max = knights[largestArmy];
            for (int i = 0; i < knights.length; i++){
                if (knights[i] > max){
                    // update scores
                    scores[largestArmy] -= params.largest_army_value;
                    scores[i] += params.largest_army_value;

                    max = knights[i];
                    largestArmy = i;
                }
            }
        }
        return largestArmy;
    }


    public int[] getScores(){
        return scores;
    }

    public int[] getExchangeRates(){
        return exchangeRates[getCurrentPlayer()];
    }

    public void setBoughtDevCard(Card card){
        this.boughtDevCard = card;
    }

    public Card getBoughtDevCard(){
        return boughtDevCard;
    }

    public int getRoadDistance(int x, int y, int edge){
        // calculates the distance length of the road
        HashSet<Road> roadSet = new HashSet<>();
        HashSet<Road> roadSet2 = new HashSet<>();
//        roadSet.add(board[x][y].getRoads()[edge]);

        ArrayList<Settlement> dir1 = new ArrayList<>();
        ArrayList<Settlement> dir2 = new ArrayList<>();
        Settlement settl1 = board[x][y].getSettlements()[edge];
        Settlement settl2 = board[x][y].getSettlements()[(edge+1)%6];

        dir1.add(settl1);
        dir2.add(settl2);

        // todo this should be done somewhere else in the FM
        // update the placed road in the graph in both directions not just on the board
        for (Edge<Settlement, Road> e: catanGraph.getEdges(settl1)){
            if (e.getDest().equals(settl2)){
                e.getValue().setOwner(getCurrentPlayer());
            }
        }
        for (Edge<Settlement, Road> e: catanGraph.getEdges(settl2)){
            if (e.getDest().equals(settl1)){
                e.getValue().setOwner(getCurrentPlayer());
            }
        }

        // find longest segment, we first follow dir_1 then dir_2
        // todo look into if keeping visited on the first iteration should be kept or not
        roadSet = expandRoad(this, roadSet, new ArrayList<>(dir1), new ArrayList<>(dir2));
        roadSet.addAll(expandRoad(this, roadSet2, new ArrayList<>(dir2), new ArrayList<>(dir1)));

        return roadSet.size();
    }

    private static HashSet<Road> expandRoad(CatanGameState gs, HashSet<Road> roadSet, List<Settlement> unexpanded, List<Settlement> expanded){
        // return length, makes it possible to compare segments
        // modify original set
        if (unexpanded.size() == 0){
            return roadSet;
        }
        if (unexpanded.size() == 2) {
            // Handle branching
            int length = 0;
            for (Settlement settlement : unexpanded) {
                ArrayList<Settlement> toExpand = new ArrayList<>();
                toExpand.add(settlement);
                HashSet roadSetCopy = new HashSet(roadSet);
                roadSetCopy = expandRoad(gs, roadSetCopy, toExpand, expanded);
                if (roadSetCopy.size() >= length) {
                    length = roadSetCopy.size();
                    roadSet = roadSetCopy;
                }
            }
            return roadSet;
        } else {
            // case of expanding a single settlement
            Settlement settlement = unexpanded.remove(0);
            expanded.add(settlement);

            // find which edge took us here and add it to the visited roads
            List<Edge<Settlement, Road>> edges = gs.getGraph().getEdges(settlement);
            boolean roadFound = false;
            if (edges != null){
                for (Edge<Settlement, Road> e: edges){
                    // find road taking to new node
                    if (expanded.contains(e.getDest())){
                        Road road = e.getValue();
                        if (!roadSet.contains(e.getValue())){
                            if (road.getOwner() == gs.getCurrentPlayer()){
                                // only add it if it's the players and unvisited
                                roadSet.add(road);
                                roadFound = true;
                            }
                        }
                    }
                }
                if (roadFound) {
                    // if settlement is not taken or belongs to player we add the neighbouring settlements to the unvisited list
                    if (settlement.getOwner() == -1 || settlement.getOwner() == gs.getCurrentPlayer()) {
                        // Add new settlements to the unexpanded list
                        for (Edge<Settlement, Road> e : edges) {
                            // find road taking to new node
                            // todo could not find destination in visited
                            if (!expanded.contains(e.getDest())) {
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

    public ArrayList<Road> getRoads(){
        // Function that returns all the roads from the the board
        int counter = 0;
        ArrayList<Road> roads = new ArrayList<>();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                for (int i = 0; i < 6; i++) {
                    // Road has already been set
                    Road road = tile.getRoads()[i];
                    if (road.getOwner()!=-1) {
                        roads.add(road);
                        counter += 1;
                    }
                }
            }
        }
        System.out.println("There are " + counter + " roads");
        return roads;
    }

    public ArrayList<Settlement> getSettlements(){
        // Function that returns all the settlements from the the board
        int counter = 0;
        ArrayList<Settlement> settlements = new ArrayList<>();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                for (int i = 0; i < 6; i++) {
                    // Road has already been set
                    Settlement settlement = tile.getSettlements()[i];
                    if (settlement.getOwner()!=-1) {
                        settlements.add(settlement);
                        counter += 1;
                    }
                }
            }
        }
        System.out.println("There are " + counter + " settlements");
        return settlements;
    }

    public int getLongestRoadOwner(){
        return longestRoad;
    }

    public int getLongestRoadLength(){
        return longestRoadLength;
    }

    public ArrayList<Settlement> getPlayersSettlements(int playerId){
        ArrayList<Settlement> playerSettlements = new ArrayList<>();
        ArrayList<Settlement> allSettlements = getSettlements();
        for (int i = 0; i < allSettlements.size(); i++){
            if (allSettlements.get(i).getOwner()==playerId){
                playerSettlements.add(allSettlements.get(i));
            }
        }
        return playerSettlements;
    }

    /* checks if given resources cover the price or not */
    public static boolean checkCost(int[] resources, int[] price){
        for (int i = 0; i < resources.length; i++){
            if (resources[i] - price[i] < 0) return false;
        }
        return true;
    }

    /**
     * Swaps cards between players
     * @param gs
     * @param playerID
     * @param otherPlayerID
     * @param playerResourcesToTrade
     * @param otherPlayerResourcesToTrade
     * @return
     */
    public static boolean swapResources(CatanGameState gs, int playerID, int otherPlayerID, int[] playerResourcesToTrade, int[] otherPlayerResourcesToTrade){
        int[] playerResourcesToTradeCopy = playerResourcesToTrade.clone();
        int[] otherPlayerResourcesToTradeCopy = otherPlayerResourcesToTrade.clone();
        List<Card> playerHand = ((Deck<Card>)gs.getComponent(playerHandHash,playerID)).getComponents();
        List<Card> otherPlayerHand = ((Deck<Card>)gs.getComponent(playerHandHash,otherPlayerID)).getComponents();
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

        for (int i = 0; i < playerResourcesToTradeCopy.length; i++){
            if (playerResourcesToTradeCopy[i] > 0){
                throw new AssertionError("Player does not have enough resources in hand");
            }
            if (otherPlayerResourcesToTradeCopy[i] > 0){
                throw new AssertionError("Other player does not have enough resources in hand");
            }
        }

        for (int i = 0; i < cardsToGiveToOtherPlayer.size(); i++){
            Card card = cardsToGiveToOtherPlayer.get(i);
            playerHand.remove(card);
            otherPlayerHand.add(card);
        }
        for (int i = 0; i < cardsToGiveToPlayer.size(); i++){
            Card card = cardsToGiveToPlayer.get(i);
            otherPlayerHand.remove(card);
            playerHand.add(card);
        }

        return true;
    }

    /* Takes the resource cards specified in the cost array from the current player, returns true if successful */
    public static boolean spendResources(CatanGameState gs, int[] cost){
        int[] costCopy = cost.clone();
        List<Card> playerHand = ((Deck<Card>)gs.getComponentActingPlayer(playerHandHash)).getComponents();
        ArrayList<Card> cardsToReturn = new ArrayList<>();
        // reduce entries in cost until all of them are 0
        for (int i = 0; i < playerHand.size(); i++){
            Card card = playerHand.get(i);
            int index = CatanParameters.Resources.valueOf(card.getProperty(CatanConstants.cardType).toString()).ordinal();
            if (costCopy[index] > 0){
                cardsToReturn.add(card);
                costCopy[index] -= 1;
            }
        }
        // if we got all 0s -> return true; remove them from player and put them back to resourceDeck
        for (int i = 0; i < costCopy.length; i++){
            if (costCopy[i] > 0){
                if (VERBOSE)
                    System.out.println("Player does not have enough resources in hand");
                return false;
            }
        }

        for (int i = 0; i < cardsToReturn.size(); i++){
            Card card = cardsToReturn.get(i);
            ((Deck<Card>)gs.getComponentActingPlayer(playerHandHash)).remove(card);
            ((Deck<Card>)gs.getComponent(resourceDeckHash)).add(card);
        }
        return true;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        // todo check if has anything else
        CatanGameState copy = new CatanGameState(getGameParameters(), getNPlayers());
        copy.gamePhase = gamePhase;
        copy.board = copyBoard();
        copy.catanGraph = catanGraph.copy();
        copy.areas = copyAreas();
        copy.gameStatus = gameStatus;
        copy.playerResults = playerResults.clone();
        copy.scores = scores.clone();
        copy.knights = knights.clone();
        copy.exchangeRates = new int[getNPlayers()][CatanParameters.Resources.values().length];
        for (int i = 0; i < exchangeRates.length; i++){
            copy.exchangeRates[i] = exchangeRates[i].clone();
        }
        copy.victoryPoints = victoryPoints.clone();
        copy.longestRoadLength = longestRoad;
        copy.rollValue = rollValue;
        if (currentTradeOffer == null){
            copy.currentTradeOffer = null;
        } else {
            copy.currentTradeOffer = (OfferPlayerTrade) this.currentTradeOffer.copy();
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        return scores[playerId];
    }

    private HashMap<Integer, Area> copyAreas(){
        HashMap<Integer, Area> copy = new HashMap<>();
        for(int key : areas.keySet()) {
            Area a = areas.get(key);
            if (PARTIAL_OBSERVABLE && key != -1) {
                List<Component> oldComponents = areas.get(key).getComponents();
                // todo need to handle PO
                // todo create a cardpool that players may have, shuffle and distribute them
                for (Component comp: oldComponents) {
                    a.putComponent(comp.copy());
                }
            }
            copy.put(key, a.copy());
        }
        return copy;
    }

    public boolean checkRoadPlacement(int roadId, CatanTile tile, int player){
        /*
         * @args:
         * roadId - Id of the road on tile
         * tile - tile on which we would like to build a road
         * gs - Game state */

        Graph<Settlement, Road> graph = getGraph();
        Road road = tile.getRoads()[roadId];

        // check if road is already taken
        if (road.getOwner() != -1){
            return false;
        }
        // check if there is our settlement along edge
        Settlement settl1 = tile.getSettlements()[roadId];
        Settlement settl2 = tile.getSettlements()[(roadId+1)%6];
        if (settl1.getOwner() == player || settl2.getOwner() == player){
            return true;
        }

        // check if there is a road on a neighbouring edge
        List<Road> roads = graph.getConnections(settl1);
        roads.addAll(graph.getConnections(settl2));
        for (Road rd :roads){
            if (rd.getOwner() == player){
                return true;
            }
        }
        return false;
    }

    public boolean checkSettlementPlacement(Settlement settlement, int player){
        // checks if any of the neighbouring settlements are already taken (distance rule)
        // if yes returns false otherwise true

        // if settlement is taken then cannot replace it
        if (settlement.getOwner() != -1){
            return false;
        }

        // check if there is a settlement one distance away
        Graph<Settlement, Road> graph = getGraph();
        List<Settlement> settlements = graph.getNeighbourNodes(settlement);
        for (Settlement settl: settlements){
            if (settl.getOwner() != -1){
                return false;
            }
        }

        List<Road> roads = graph.getConnections(settlement);
        // check first if we have a road next to the settlement owned by the player
        // Doesn't apply in the setup phase
        if (!getGamePhase().equals(CatanGameState.CatanGamePhase.Setup)){
            for (Road road : roads) {
                if (road.getOwner() == player) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private CatanTile[][] copyBoard(){
        CatanTile[][] copy = new CatanTile[board.length][board[0].length];
        for (int x = 0; x < board.length; x++){
            for (int y = 0; y < board[0].length; y++){
                copy[x][y] = board[x][y].copy();
            }
        }
        return copy;
    }


    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        // todo return unknown components
        return new ArrayList<Integer>() {{
            Deck<Card> resourceDeck = (Deck<Card>) getComponent(playerDeckHash);
            Deck<Card> devDeck = (Deck<Card>) getComponent(developmentDeckHash);
            add(resourceDeck.getComponentID());
            add(devDeck.getComponentID());
            for (Component c: resourceDeck.getComponents()) {
                add(c.getComponentID());
            }
            for (Component c: devDeck.getComponents()) {
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
