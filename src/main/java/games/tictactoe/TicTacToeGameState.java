package games.tictactoe;

import core.AbstractGameParameters;
import core.components.Component;
import core.components.GridBoard;
import core.AbstractGameState;
import core.interfaces.IGridGameState;
import core.interfaces.IPrintable;
import core.observations.VectorObservation;
import core.turnorders.AlternatingTurnOrder;
import utilities.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class TicTacToeGameState extends AbstractGameState implements IPrintable, IGridGameState<Character> {

    GridBoard<Character> gridBoard;
    final ArrayList<Character> playerMapping = new ArrayList<Character>() {{
        add('x');
        add('o');
    }};

    public TicTacToeGameState(AbstractGameParameters gameParameters, int nPlayers){
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
    protected VectorObservation _getVectorObservation() {
        return new VectorObservation<>(gridBoard.flattenGrid());
    }

    @Override
    protected double[] _getDistanceFeatures(int playerId) {
        return new double[0];
    }

    @Override
    protected HashMap<HashMap<Integer, Double>, Utils.GameResult> _getTerminalFeatures(int playerId) {
        return null;
    }

    @Override
    protected double _getScore(int playerId) {
        if (getGameStatus() == Utils.GameResult.GAME_WIN)
            return 1;
        else if (getGameStatus() == Utils.GameResult.GAME_DRAW)
            return 0;
        else if (getGameStatus() == Utils.GameResult.GAME_LOSE)
            return -1;
        else
            return 0;

//        int nChars = 0;
//        for (int i = 0; i < gridBoard.getWidth(); i++) {
//            for (int j = 0; j < gridBoard.getHeight(); j++) {
//                if (gridBoard.getElement(i, j) == playerMapping.get(playerId)) nChars++;
//            }
//        }
//        return nChars;
    }

    @Override
    public GridBoard<Character> getGridBoard() {
        return gridBoard;
    }

    @Override
    public void printToConsole() {
        System.out.println(gridBoard.toString());
    }
}
