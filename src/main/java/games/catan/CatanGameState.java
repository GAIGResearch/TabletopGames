package games.catan;

import core.AbstractParameters;
import core.AbstractGameState;
import core.components.Area;
import core.components.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CatanGameState extends AbstractGameState {
    private CatanData data;
    protected CatanTile[][] board;

    // Collection of areas, mapped to player ID. -1 is the general game area containing the board, counters and several decks.
    HashMap<Integer, Area> areas;

    // todo get turnorder right
    public CatanGameState(AbstractParameters pp, int nPlayers) {
        super(pp, new CatanTurnOrder(nPlayers, ((CatanParameters)pp).n_actions_per_turn));
        data = new CatanData((CatanParameters)pp);
        data.load(((CatanParameters)gameParameters).getDataPath());

        board = generateBoard();

    }

    private CatanTile[][] generateBoard(){
        board = new CatanTile[7][7];
        for (int x = 0; x < board.length; x++){
            for (int y = 0; y < board[x].length; y++){
                board[x][y] = new CatanTile(x, y);
            }
        }
        return new CatanBoard((CatanParameters)gameParameters).board;
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
        // todo set everything to null
        this.areas = null;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }


    // todo implement methods below

    @Override
    protected AbstractGameState _copy(int playerId) {
        return this;
    }

    @Override
    protected double _getScore(int playerId) {
        return 0;
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return null;
    }

    public CatanTile[][] getBoard(){
        return board;
    }

    void addComponents() {
        super.addAllComponents();
    }
}
