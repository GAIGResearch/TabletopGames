package games.connect4;
import core.AbstractGameState;
import core.components.BoardNode;
import core.components.grid.SquareCoord;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateKey;

import java.util.stream.IntStream;

public class Connect4StateVector implements IStateFeatureVector, IStateKey {
    // assume the grid is 8x8 ... if not, write a new StateVector
    private final String[] names = (String[]) IntStream.range(0, 8).boxed().flatMap(row ->
            IntStream.range(0, 3).mapToObj(col -> String.format("%d:%d", row, col))
    ).toArray(String[]::new);

    @Override
    public double[] doubleVector(AbstractGameState gs, int playerID) {
        Connect4GameState state = (Connect4GameState) gs;
        String playerChar = Connect4Constants.playerMapping.get(playerID).getComponentName();

        // row by row
        double[] retValue = new double[state.getWidth() * state.getHeight()];
        for (int y = 0; y < state.getHeight(); y++) {
            for (int x = 0; x < state.getWidth(); x++) {
                BoardNode c = state.getGridBoard().get(new SquareCoord(x, y));
                if (c == null) {
                    retValue[y * state.getWidth() + x] = 0.0;
                } else if (c.getComponentName().equals(playerChar)) {
                    retValue[y * state.getWidth() + x] = 1.0;
                } else { // opponent's piece
                    retValue[y * state.getWidth() + x] = -1.0;
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
