package games.tictactoe;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.components.Token;
import core.interfaces.IGridGameState;
import core.interfaces.IPrintable;
import core.interfaces.IVectorisable;
import games.GameType;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class TicTacToeGameState extends AbstractGameState implements IPrintable, IGridGameState<Token>, IVectorisable {

    GridBoard<Token> gridBoard;

    public TicTacToeGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.TicTacToe;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(gridBoard);
        }};
    }

    @Override
    protected TicTacToeGameState _copy(int playerId) {
        TicTacToeGameState s = new TicTacToeGameState(gameParameters.copy(), getNPlayers());
        s.gridBoard = gridBoard.copy();
        return s;
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
        return playerResults[playerId].value;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TicTacToeGameState)) return false;
        TicTacToeGameState that = (TicTacToeGameState) o;
        return Objects.equals(gridBoard, that.gridBoard);
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

    @Override
    public GridBoard<Token> getGridBoard() {
        return gridBoard;
    }

    @Override
    public void printToConsole() {
        System.out.println(gridBoard.toString());
    }

    @Override
    public String getObservationJson() {
        JSONObject json = new JSONObject();
        String playerSymbol = (getCurrentPlayer() == 0) ? "x" : "o";
        for (int x = 0; x < gridBoard.getWidth(); x++) {
            for (int y = 0; y < gridBoard.getHeight(); y++) {
                String cellSymbol = gridBoard.getElement(x, y).toString();
                if (cellSymbol.equals(playerSymbol)) {
                    json.put(x + "," + y, 1);
                } else if (cellSymbol.equals(".")) {
                    json.put(x + "," + y, 0);
                } else {
                    json.put(x + "," + y, -1);
                }
            }
        }
        return json.toJSONString();
    }

    @Override
    public double[] getObservationVector() {
        List<Double> listVec = new ArrayList<>();
        String playerSymbol = (getCurrentPlayer() == 0) ? "x" : "o";
        for (int x = 0; x < gridBoard.getWidth(); x++) {
            for (int y = 0; y < gridBoard.getHeight(); y++) {
                String cellSymbol = gridBoard.getElement(x, y).toString();
                if (cellSymbol.equals(playerSymbol)) {
                    listVec.add(1.0);
                } else if (cellSymbol.equals(".")) {
                    listVec.add(0.0);
                } else {
                    listVec.add(-1.0);
                }
            }
        }
        return listVec.stream().mapToDouble(i -> i).toArray();
    }

    @Override
    public double[] getNormalizedObservationVector() {
        List<Double> listVec = new ArrayList<>();
        String playerSymbol = (getCurrentPlayer() == 0) ? "x" : "o";
        for (int x = 0; x < gridBoard.getWidth(); x++) {
            for (int y = 0; y < gridBoard.getHeight(); y++) {
                String cellSymbol = gridBoard.getElement(x, y).toString();
                if (cellSymbol.equals(playerSymbol)) {
                    listVec.add(1.0);
                } else if (cellSymbol.equals(".")) {
                    listVec.add(0.5);
                } else {
                    listVec.add(0.0);
                }
            }
        }
        return listVec.stream().mapToDouble(i -> i).toArray();
    }

    @Override
    public int getObservationSpace() {
        return 9;
    }
}
