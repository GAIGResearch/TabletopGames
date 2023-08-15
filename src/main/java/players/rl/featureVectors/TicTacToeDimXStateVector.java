package players.rl.featureVectors;

import core.AbstractGameState;
import core.components.Token;
import core.interfaces.IStateFeatureVector;
import games.tictactoe.TicTacToeConstants;
import games.tictactoe.TicTacToeGameState;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.stream.IntStream;

public abstract class TicTacToeDimXStateVector implements IStateFeatureVector {

    private final int dims;
    private final String[] names;

    public TicTacToeDimXStateVector(int dims) {
        if (dims <= 0)
            throw new IllegalArgumentException("Cannot have non-positive dimension");
        this.dims = dims;
        // assume the grid is 3x3 ... if not, write a new StateVector
        String[] names1D = (String[]) IntStream.range(0, 3).boxed()
                .flatMap(row -> IntStream.range(0, 3).mapToObj(col -> String.format("%d:%d", row, col)))
                .toArray(String[]::new);
        names = addDimensions(names1D);
    }

    @Override
    public double[] featureVector(AbstractGameState gs, int playerID) {
        TicTacToeGameState state = (TicTacToeGameState) gs;
        String playerChar = TicTacToeConstants.playerMapping.get(playerID).getTokenType();

        Double[] featuresDim1 = Arrays.stream(state.gridBoard.flattenGrid()).mapToDouble(c -> {
            String pos = ((Token) c).getTokenType();
            if (pos.equals(playerChar)) {
                return 1.0;
            } else if (pos.equals(TicTacToeConstants.emptyCell)) {
                return 0.0;
            } else { // opponent's piece
                return -1.0;
            }
        }).boxed().toArray(Double[]::new);

        return Arrays.stream(addDimensions(featuresDim1)).mapToDouble(Double::doubleValue).toArray();
    }

    private String[] addDimensions(String[] dim1) {
        List<String> allDims = new LinkedList<>(Arrays.asList(dim1));
        for (int dim = 1; dim < dims; dim++) {
            int allDimsSize = allDims.size();
            for (String item : dim1) {
                String[] coords1 = item.split(":");
                int val1 = 3 * Integer.parseInt(coords1[0]) + Integer.parseInt(coords1[1]);
                for (int i = 0; i < allDimsSize; i++) {
                    String[] coords2 = allDims.get(i).split("/")[0].split(":");
                    int val2 = 3 * Integer.parseInt(coords2[0]) + Integer.parseInt(coords2[1]);
                    if (val1 < val2)
                        allDims.add(item + "/" + allDims.get(i));
                }
            }
        }
        return allDims.toArray(String[]::new);
    }

    private Double[] addDimensions(Double[] dim1) {
        Double[] ret = new Double[names.length];
        for (int i = 0; i < names.length; i++) {
            String[] coords = names[i].split("/");
            double value = 0.0;
            for (int j = 0; j < coords.length; j++) {
                String[] coord = coords[j].split(":");
                int val = 3 * Integer.parseInt(coord[0]) + Integer.parseInt(coord[1]);
                value += dim1[val];
            }
            ret[i] = value;
        }
        return ret;
    }

    @Override
    public String[] names() {
        return names;
    }
}