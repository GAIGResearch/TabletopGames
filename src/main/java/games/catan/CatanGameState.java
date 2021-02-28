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
import games.catan.actions.OfferPlayerTrade;
import games.catan.components.Edge;
import games.catan.components.Graph;
import games.catan.components.Road;
import games.catan.components.Settlement;

import java.util.*;
import java.util.stream.Collectors;

import static core.CoreConstants.VERBOSE;
import static core.CoreConstants.playerHandHash;
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
        super(pp, new CatanTurnOrder(nPlayers, ((CatanParameters)pp).n_actions_per_turn));
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

    @Override
    protected double _getScore(int playerID) {
        return scores[playerID];
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
        return knights.clone();
    }

    public void addVictoryPoint(int playerID){
        if (playerID < knights.length){
            victoryPoints[playerID] += 1;
        }
    }

    public int[] getVictoryPoints(){
        return victoryPoints.clone();
    }

    public int[] getPlayerResources(){
        Deck<Card> playerHand = (Deck<Card>)this.getComponentActingPlayer(CoreConstants.playerHandHash);
        int[] resources = new int[CatanParameters.Resources.values().length];

        for (Card card: playerHand.getComponents()){
            resources[CatanParameters.Resources.valueOf(card.getProperty(cardType).toString()).ordinal()] += 1;
        }
        return resources;
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
        roadSet.add(board[x][y].getRoads()[edge]);

        ArrayList<Settlement> dir1 = new ArrayList<>();
        ArrayList<Settlement> dir2 = new ArrayList<>();
        Settlement settl1 = board[x][y].getSettlements()[edge];
        Settlement settl2 = board[x][y].getSettlements()[(edge+1)%6];

        dir1.addAll(catanGraph.getNeighbourNodes(settl1));
        dir2.addAll(catanGraph.getNeighbourNodes(settl2));

        // find longest segment, we first follow dir_1 then dir_2
        // todo probably crashes, need to look into this
//        expandRoad(this, roadSet, dir1);
//        expandRoad(this, roadSet, dir2);

        return roadSet.size();
    }

    private static int expandRoad(CatanGameState gs, HashSet<Road> roadSet, List<Settlement> unexpanded){
        // return length, makes it possible to compare segments
        // modify original set
        while (unexpanded.size() > 0){
            // Handle branching
            if (unexpanded.size() == 2){
                // road branches -> explore both directions
                List unexpanded_copy = new ArrayList(unexpanded);
                unexpanded.remove(1); // list with first element
                unexpanded_copy.remove(0); // list with second element
                HashSet roadSetCopy = new HashSet(roadSet);
                int length1 = expandRoad(gs, roadSet, unexpanded);
                int length2 = expandRoad(gs, roadSetCopy, unexpanded_copy);
                if (length2 > length1){
                    // has to swap the set if second option is longer
                    roadSet = roadSetCopy;
                }
                unexpanded = new ArrayList<>(); // empty list, fully explored it
            }
            // logic for expanding a node
            Settlement settlement = unexpanded.remove(0);
            List<Edge<Settlement, Road>> edges = gs.getGraph().getEdges(settlement);
            if (edges != null){
                for (Edge<Settlement, Road> e: edges){
                    Road road = e.getValue();
                    if (!roadSet.contains(e.getValue())){
                        // todo check if opponent has a settlement along the longest road as it breaks the longest road
                        if (road.getOwner() == gs.getCurrentPlayer()){
                            // only add it if it's the players and unvisited
                            roadSet.add(road);
                            unexpanded.add(e.getDest());
                        }
                    }
                }
            }
        }
        return roadSet.size();
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
                if (VERBOSE)
                    System.out.println("Player does not have enough resources in hand");
                return false;
            }
            if (otherPlayerResourcesToTradeCopy[i] > 0){
                if (VERBOSE)
                    System.out.println("Other player does not have enough resources in hand");
                return false;
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
        copy.availableActions = new ArrayList<>(availableActions);
        return copy;
    }

    private HashMap<Integer, Area> copyAreas(){
        HashMap<Integer, Area> copy = new HashMap();
        for (Map.Entry<Integer, Area> entry: this.areas.entrySet()){
            copy.put(entry.getKey(), entry.getValue());
        }
        return copy;
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
