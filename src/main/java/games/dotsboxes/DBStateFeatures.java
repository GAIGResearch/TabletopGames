package games.dotsboxes;

import core.AbstractGameState;
import players.heuristics.AbstractStateFeature;

public class DBStateFeatures extends AbstractStateFeature {

    String[] localNames = new String[]{"TWO_BOXES", "THREE_BOXES", "FILLED_BOXES"};

    @Override
    protected double maxScore() {
        return 20.0;
    }

    @Override
    protected double maxRounds() {
        return 100.0;
    }

    @Override
    protected String[] localNames() {
        return localNames;
    }

    @Override
    protected double[] localFeatureVector(AbstractGameState gs, int playerID) {
        DBGameState state = (DBGameState) gs;
        double[] retValue = new double[localNames.length];

        // CELLS
        int[] cellCountByEdges = new int[5];
        for (DBCell cell : state.cells) {
            int edges = state.countCompleteEdges(cell);
            cellCountByEdges[edges]++;
        }
        double totalCells = state.cells.size();

        int multiplier = state.getCurrentPlayer() == playerID ? 1 : -1;
        retValue[0] = cellCountByEdges[2] * multiplier / totalCells;
        retValue[1] = cellCountByEdges[3] * multiplier / 5.0;
        retValue[2] = cellCountByEdges[4] / totalCells;

        return retValue;
    }

}
