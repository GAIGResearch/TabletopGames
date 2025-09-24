package games.tictactoe;

import core.AbstractGameState;
import core.components.Token;
import core.interfaces.IStateFeatureVector;

import java.util.Arrays;
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

        return Arrays.stream(state.gridBoard.flattenGrid()).mapToDouble(c -> {
            String pos = c.getComponentName();
            if (pos.equals(playerChar)) {
                return 1.0;
            } else if (pos.equals(TicTacToeConstants.emptyCell)) {
                return 0.0;
            } else { // opponent's piece
                return -1.0;
            }
        }).toArray();

    }

    @Override
    public String[] names() {
        return names;
    }
}