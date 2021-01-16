package games.catan;

import core.AbstractParameters;
import core.AbstractGameState;
import core.components.Area;
import core.components.Component;
import core.interfaces.IGamePhase;
import games.catan.actions.BuildRoad;
import games.catan.components.Graph;
import games.catan.components.Road;
import games.catan.components.Settlement;

import java.util.*;

import static games.catan.CatanConstants.HEX_SIDES;

public class CatanGameState extends AbstractGameState {
    private CatanData data;
    protected CatanTile[][] board;
    protected Graph<Settlement, Road> catanGraph;
    protected int scores[]; // score for each player - 10 required to win
    protected int knights[]; // knight count for each player
    protected int largestArmy = -1; // playerID of the player currently holding the largest army
    protected int longestRoad = -1; // playerID of the player currently holding the longest road
    int rollValue;

    // In Catan the "setup" phase is when each player can place a road with a settlement twice. The robber phase is when
    // the player rolls a 7. The discarding phase is when the robber is activate and a player has more than 7 resource
    // cards, in this case half of the cards have to be discarded. the TradeReaction is when another player makes an
    // offer, which can be accepted or rejected.
    public enum CatanGamePhase implements IGamePhase {
        Setup,
        Robber,
        Discarding,
        TradeReaction
    }

    // Collection of areas, mapped to player ID, -1 is the general game area containing the board, counters and several decks.
    HashMap<Integer, Area> areas;

    // todo get turnorder right
    public CatanGameState(AbstractParameters pp, int nPlayers) {
        super(pp, new CatanTurnOrder(nPlayers, ((CatanParameters)pp).n_actions_per_turn));
        data = new CatanData((CatanParameters)pp);
        data.load(((CatanParameters)gameParameters).getDataPath());
        scores = new int[((CatanParameters) pp).n_players];
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>(); // areas.values()
//        components.add(tempDeck);
//        components.add(world);
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

    public CatanData getData(){
        return data;
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

    void addComponents() {
        super.addAllComponents();
    }

    public void addScore(int playerID, int score){
        if (playerID < scores.length) {
            scores[playerID] += score;
        }
    }

    public int[] getScores(){
        return scores;
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
    // todo implement methods below

    @Override
    protected AbstractGameState _copy(int playerId) {
        return this;
    }


    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return null;
    }


}
