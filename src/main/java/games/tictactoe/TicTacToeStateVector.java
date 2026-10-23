package games.tictactoe;

import core.AbstractGameState;
import core.components.BoardNode;
import core.interfaces.IStateFeatureVector;

import java.util.stream.IntStream;

public class TicTacToeStateVector implements IStateFeatureVector {

    // assume the grid is 3x3 ... if not, write a new StateVector
    private final String[] names = IntStream.range(0, 3).boxed().flatMap(row ->
            IntStream.range(0, 3).mapToObj(col -> String.format("%d:%d", row, col))
    ).toArray(String[]::new);

    @Override
    public double[] doubleVector(AbstractGameState gs, int playerID) {
        TicTacToeGameState state = (TicTacToeGameState) gs;
        String playerChar = TicTacToeConstants.playerMapping.get(playerID).getComponentName();

        // row by row
        double[] retValue = new double[state.getGridSize() * state.getGridSize()];
        for (int y = 0; y < state.getGridSize(); y++) {
            for (int x = 0; x < state.getGridSize(); x++) {
                BoardNode c = state.gridBoard.getElement(x, y);
                if (c == null) {
                    retValue[y * state.getGridSize() + x] = 0.0;
                } else if (c.getComponentName().equals(playerChar)) {
                    retValue[y * state.getGridSize() + x] = 1.0;
                } else { // opponent's piece
                    retValue[y * state.getGridSize() + x] = -1.0;
                }
            }
        }
        return retValue;

    }

    @Override
    public String[] names() {
        return names;
    }
}