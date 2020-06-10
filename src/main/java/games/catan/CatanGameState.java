package games.catan;

import core.AbstractGameParameters;
import core.AbstractGameState;
import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.components.Component;
import core.components.GraphBoard;
import games.pandemic.PandemicParameters;
import games.pandemic.PandemicTurnOrder;

import java.util.List;

public class CatanGameState extends AbstractGameState {
    CatanData _data;
    public GraphBoard board;

    // todo get turnorder right
    public CatanGameState(AbstractGameParameters pp, int nPlayers) {
        super(pp, new CatanTurnOrder(nPlayers, ((CatanParameters)pp).n_actions_per_turn));

        _data = new CatanData();
        _data.load(((CatanParameters)gameParameters).getDataPath());
    }

    private GraphBoard setupBoard(){
        board = new GraphBoard();
        // setup all the hexes in the game
        return board;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return null;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        return null;
    }

    @Override
    protected double _getScore(int playerId) {
        return 0;
    }

    @Override
    protected void _reset() {

    }
}
