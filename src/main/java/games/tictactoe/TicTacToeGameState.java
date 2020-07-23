package games.tictactoe;

import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.AbstractGameState;
import core.interfaces.IGridGameState;
import core.interfaces.IPrintable;
import core.interfaces.IVectorObservation;
import utilities.VectorObservation;
import core.turnorders.AlternatingTurnOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class TicTacToeGameState extends AbstractGameState implements IPrintable, IGridGameState<Character>, IVectorObservation {

    GridBoard<Character> gridBoard;
    final ArrayList<Character> playerMapping = new ArrayList<Character>() {{
        add('x');
        add('o');
    }};

    public TicTacToeGameState(AbstractParameters gameParameters, int nPlayers){
        super(gameParameters, new AlternatingTurnOrder(nPlayers));
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
    protected double _getScore(int playerId) {
        return new TicTacToeHeuristic().evaluateState(this, playerId);
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<>();  // Always fully observable
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
        return Objects.equals(gridBoard, that.gridBoard) &&
                Objects.equals(playerMapping, that.playerMapping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridBoard, playerMapping);
    }

    @Override
    public GridBoard<Character> getGridBoard() {
        return gridBoard;
    }

    public ArrayList<Character> getPlayerMapping() {
        return playerMapping;
    }

    @Override
    public void printToConsole() {
        System.out.println(gridBoard.toString());
    }
}
