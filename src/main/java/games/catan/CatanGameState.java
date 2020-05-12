package games.catan;

import core.AbstractGameState;
import core.ForwardModel;
import core.GameParameters;
import core.actions.IAction;
import core.components.Board;
import core.observations.IObservation;
import core.turnorder.TurnOrder;

import java.util.List;

public class CatanGameState extends AbstractGameState {
    CatanData _data;
    public Board board;

    // todo get turnorder right
    public CatanGameState(GameParameters pp, ForwardModel model, int nPlayers) {
        super(pp, model, nPlayers, new CatanTurnOrder(nPlayers, ((CatanParameters)pp).n_actions_per_turn));

        _data = new CatanData();
        _data.load(((CatanParameters)gameParameters).getDataPath());
    }

    private Board setupBoard(){
        board = new Board();
        // setup all the hexes in the game
        return board;
    }

    @Override
    public IObservation getObservation(int player) {
        // todo player sees board + cards in hand
        return null;
    }

    @Override
    public List<IAction> computeAvailableActions() {
        return null;
    }

    @Override
    public void setComponents() {

    }
}
