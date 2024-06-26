package games.connect4;
import core.AbstractGameState;
import core.components.Token;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateKey;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Connect4StateVector implements IStateFeatureVector, IStateKey {
    // assume the grid is 8x8 ... if not, write a new StateVector
    private final String[] names = (String[]) IntStream.range(0, 8).boxed().flatMap(row ->
            IntStream.range(0, 3).mapToObj(col -> String.format("%d:%d", row, col))
    ).toArray(String[]::new);

    @Override
    public double[] featureVector(AbstractGameState gs, int playerID) {
        Connect4GameState state = (Connect4GameState) gs;
        String playerChar = Connect4Constants.playerMapping.get(playerID).getTokenType();

        return Arrays.stream(state.gridBoard.flattenGrid()).mapToDouble(c -> {
            String pos = ((Token) c).getTokenType();
            if (pos.equals(playerChar)) {
                return 1.0;
            } else if (pos.equals(Connect4Constants.emptyCell)) {
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

    @Override
    public String getKey(AbstractGameState state) {
        double[] retValue = featureVector(state, state.getCurrentPlayer());
        return String.format("%d-%s", state.getCurrentPlayer(), Arrays.toString(retValue));
    }
}
