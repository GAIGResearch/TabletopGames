package games.catan;

import core.AbstractParameters;
import core.AbstractGameState;
import core.components.Area;
import core.components.Component;
import core.interfaces.IGamePhase;

import java.util.*;

public class CatanGameState extends AbstractGameState {
    private CatanData data;
    protected CatanTile[][] board;
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
        if (playerID < scores.length) {
            System.out.println("Invalid playerID was used to getPlayerScore");
            return -1;
        }
        return scores[playerID];
    }

    public void setBoard(CatanTile[][] board){
        this.board = board;
    }

    public CatanTile[][] getBoard(){
        return board;
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
