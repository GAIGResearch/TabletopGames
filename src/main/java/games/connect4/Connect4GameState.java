package games.connect4;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.BoardNode;
import core.components.Component;
import core.components.GridBoard;
import core.components.Token;
import core.interfaces.IGridGameState;
import core.interfaces.IPrintable;
import games.GameType;
import utilities.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Connect4GameState extends AbstractGameState implements IPrintable, IGridGameState {

    GridBoard gridBoard;
    LinkedList<Pair<Integer, Integer>> winnerCells;

    public Connect4GameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        winnerCells = new LinkedList<>();
        gridBoard = null;
    }

    /**
     * This returns the player id of the token at the given position. Or -1 if this is empty.
     */
    public int getPlayerAt(int x, int y) {
        BoardNode token = gridBoard.getElement(x, y);
        return token == null ? -1 : token.getOwnerId();
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Connect4;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>() {{
            add(gridBoard);
            addAll(Connect4Constants.playerMapping);
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
     * Score is not relevant for Connect4. This will be 0.0 if a game is nto finished.
     */
    @Override
    public double getGameScore(int playerId) {
        return playerResults[playerId].value;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Connect4GameState that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(gridBoard, that.gridBoard);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        for (int y = 0; y < gridBoard.getHeight(); y++) {
            for (int x = 0; x < gridBoard.getWidth(); x++) {
                if (y != 0 || x != 0) {
                    sb.append(",");
                }
                BoardNode t = gridBoard.getElement(x, y);
                sb.append("\"").append("Grid_").append(x).append('_').append(y).append("\":\"").append(t.getComponentName()).append("\"");
            }
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridBoard);
    }

    @Override
    public GridBoard getGridBoard() {
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
