package games.connect4;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.components.Token;
import core.interfaces.IGridGameState;
import core.interfaces.IPrintable;
import core.turnorders.AlternatingTurnOrder;
import games.GameType;
import utilities.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


public class Connect4GameState extends AbstractGameState implements IPrintable, IGridGameState<Token> {

    GridBoard<Token> gridBoard;
    LinkedList<Pair<Integer, Integer>> winnerCells;

    public Connect4GameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new AlternatingTurnOrder(nPlayers), GameType.Connect4);
        winnerCells = new LinkedList<>();
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(gridBoard);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        Connect4GameState s = new Connect4GameState(gameParameters.copy(), getNPlayers());
        s.gridBoard = gridBoard.copy();

        s.winnerCells.clear();
        for (Pair<Integer, Integer> wC : this.winnerCells)
            s.winnerCells.add(wC.copy());

        return s;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new Connect4Heuristic().evaluateState(this, playerId);
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
        return playerResults[playerId].value;
    }

    @Override
    protected void _reset() {
        gridBoard = null;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Connect4GameState)) return false;
        if (!super.equals(o)) return false;
        Connect4GameState that = (Connect4GameState) o;
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

    void registerWinningCells(LinkedList<Pair<Integer, Integer>> winnerCells) {
        this.winnerCells = winnerCells;
    }

    public LinkedList<Pair<Integer, Integer>> getWinningCells() {
        return winnerCells;
    }
}
