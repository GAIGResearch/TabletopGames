package games.tictactoe;

import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.AbstractGameState;
import core.components.Token;
import core.interfaces.IGridGameState;
import core.interfaces.IPrintable;
import core.interfaces.IVectorObservation;
import games.GameType;
import utilities.VectorObservation;
import core.turnorders.AlternatingTurnOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class TicTacToeGameState extends AbstractGameState implements IPrintable, IGridGameState<Token>, IVectorObservation {

    GridBoard<Token> gridBoard;

    public TicTacToeGameState(AbstractParameters gameParameters, int nPlayers){
        super(gameParameters, new AlternatingTurnOrder(nPlayers), GameType.TicTacToe);
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>(){{ add(gridBoard); }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        TicTacToeGameState s = new TicTacToeGameState(gameParameters.copy(), getNPlayers());
        s.gridBoard = gridBoard.copy();
        return s;
    }

    @Override
    public VectorObservation getVectorObservation() {
        return new VectorObservation<>(gridBoard.flattenGrid());
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new TicTacToeHeuristic().evaluateState(this, playerId);
    }

    /**
     * This provides the current score in game turns. This will only be relevant for games that have the concept
     * of victory points, etc.
     * If a game does not support this directly, then just return 0.0
     *
     * @param playerId
     * @return - double, score of current state
     */
    @Override
    public double getGameScore(int playerId) {
        return 0;
    }

    @Override
    protected void _reset() {
        gridBoard = null;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TicTacToeGameState)) return false;
        if (!super.equals(o)) return false;
        TicTacToeGameState that = (TicTacToeGameState) o;
        return Objects.equals(gridBoard, that.gridBoard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridBoard);
    }

    @Override
    public GridBoard<Token> getGridBoard() {
        return gridBoard;
    }

    @Override
    public void printToConsole() {
        System.out.println(gridBoard.toString());
    }
}
