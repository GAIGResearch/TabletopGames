package games.tictactoe;

import core.components.GridBoard;
import core.AbstractGameState;
import core.interfaces.IGridGameState;
import core.interfaces.IPrintable;
import core.observations.VectorObservation;
import core.turnorders.AlternatingTurnOrder;
import utilities.Utils;

import java.util.ArrayList;
import java.util.HashMap;


public class TicTacToeGameState extends AbstractGameState implements IPrintable, IGridGameState<Character> {

    GridBoard<Character> gridBoard;
    final ArrayList<Character> playerMapping = new ArrayList<Character>() {{
        add('x');
        add('o');
    }};

    public TicTacToeGameState(TicTacToeGameParameters gameParameters, int nPlayers){
        super(gameParameters, new AlternatingTurnOrder(nPlayers));
    }

    @Override
    public void addAllComponents() {
        allComponents.putComponent(gridBoard);
    }

    @Override
    protected AbstractGameState copy(int playerId) {
        TicTacToeGameState s = new TicTacToeGameState((TicTacToeGameParameters)gameParameters, getNPlayers());
        s.gridBoard = gridBoard.copy();
        return s;
    }

    @Override
    public VectorObservation getVectorObservation() {
        return new VectorObservation<>(gridBoard.flattenGrid());
    }

    @Override
    public double[] getDistanceFeatures(int playerId) {
        return new double[0];
    }

    @Override
    public HashMap<HashMap<Integer, Double>, Utils.GameResult> getTerminalFeatures(int playerId) {
        return null;
    }

    @Override
    public double getScore(int playerId) {
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
