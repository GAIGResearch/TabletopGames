package games.dotsboxes;

import core.AbstractGameState;
import players.heuristics.AbstractStateFeature;

public class DBStateFeatures extends AbstractStateFeature {

    String[] localNames = new String[]{"NO_BOXES", "ONE_BOXES", "TWO_BOXES", "THREE_BOXES", "OPPONENTS_FILLED_BOXES", "OWNED_FILLED_BOXES"};

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

        retValue[0] = cellCountByEdges[0];
        retValue[1] = cellCountByEdges[1];
        retValue[2] = cellCountByEdges[2];
        retValue[3] = cellCountByEdges[3];
        retValue[4] = cellCountByEdges[4] - gs.getGameScore(playerID);
        retValue[5] = gs.getGameScore(playerID);

        return retValue;
    }

}
