package games.tictactoe;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.BoardNode;
import core.components.Component;
import core.components.grid.SparseGrid;
import core.components.grid.SquareCoord;
import core.interfaces.IPrintable;
import games.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class TicTacToeGameState extends AbstractGameState implements IPrintable {

    // the x and o tokens of TicTacToeConstants.playerMapping; a square with none is empty
    SparseGrid<SquareCoord, BoardNode> gridBoard;
    // from the parameters at setup: held here as a copy of the parameters keeps only their tunable values
    int gridSize;

    public TicTacToeGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.TicTacToe;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>() {{
            add(gridBoard);
            addAll(TicTacToeConstants.playerMapping);
        }};
    }

    @Override
    protected TicTacToeGameState _copy(int playerId) {
        TicTacToeGameState s = new TicTacToeGameState(gameParameters.copy(), getNPlayers());
        s.gridBoard = gridBoard.copy();
        s.gridSize = gridSize;
        return s;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new TicTacToeHeuristic().evaluateState(this, playerId);
    }

    /**
     * For TicTacToe this returns 0 unless the game is over. In which case 1 is a win, 0.5 is a draw and 0 is a loss.
     *
     * @param playerId - ID of player whose score we're curious about
     * @return - double, score of current state
     */
    @Override
    public double getGameScore(int playerId) {
        return playerResults[playerId].value;
    }

    /**
     * This returns the player id of the token at the given position. Or -1 if this is empty.
     */
    public int getPlayerAt(int x, int y) {
        BoardNode token = gridBoard.get(new SquareCoord(x, y));
        return token == null ? -1 : token.getOwnerId();
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TicTacToeGameState that)) return false;
        return gridSize == that.gridSize && Objects.equals(gridBoard, that.gridBoard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridBoard);
    }
    @Override
    public String toString() {
        return Objects.hash(gameParameters) + "|" +
                Objects.hash(getAllComponents()) + "|" +
                Objects.hash(gameStatus) + "|" +
                Objects.hash(gamePhase) + "|*|" +
                Objects.hash(gridBoard);
    }

    public SparseGrid<SquareCoord, BoardNode> getGridBoard() {
        return gridBoard;
    }

    /**
     * The number of squares along each side of the board.
     */
    public int getGridSize() {
        return gridSize;
    }

    @Override
    public void printToConsole() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < getGridSize(); y++) {
            for (int x = 0; x < getGridSize(); x++) {
                BoardNode token = gridBoard.get(new SquareCoord(x, y));
                sb.append(token == null ? TicTacToeConstants.emptyCell : token.getComponentName()).append(" ");
            }
            sb.append("\n");
        }
        System.out.print(sb);
    }

}
