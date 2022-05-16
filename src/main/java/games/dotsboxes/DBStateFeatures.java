package games.dotsboxes;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import utilities.Utils;

public class DBStateFeatures implements IStateFeatureVector {

    String[] names = new String[]{"POINTS", "POINT_ADVANTAGE", "TWO_BOXES", "THREE_BOXES", "ORDINAL", "OUR_TURN", "FILLED_BOXES", "HAS_WON", "FINAL_POSITION"};

    @Override
    public double[] featureVector(AbstractGameState gs, int playerID) {
        double[] retValue = new double[names.length];
        DBGameState state = (DBGameState) gs;


        // POINT_ADVANTAGE
        int ordinal = 1;
        int maxOtherScore = -1;
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (p == playerID) continue;
            if (state.nCellsPerPlayer[p] > maxOtherScore) {
                maxOtherScore = state.nCellsPerPlayer[p];
                if (state.nCellsPerPlayer[p] > state.nCellsPerPlayer[playerID])
                    ordinal++;
            }
        }

        // CELLS
        int[] cellCountByEdges = new int[5];
        for (DBCell cell : state.cells) {
            int edges = state.countCompleteEdges(cell);
            cellCountByEdges[edges]++;
        }
        double totalCells = state.cells.size();

        // POINTS
        retValue[0] = state.nCellsPerPlayer[playerID] / totalCells * state.getNPlayers();
        // POINT_ADVANTAGE
        retValue[1] = (state.nCellsPerPlayer[playerID] - maxOtherScore) / 10.0;
        int multiplier = state.getCurrentPlayer() == playerID ? 1 : -1;
        retValue[2] = cellCountByEdges[2] * multiplier / totalCells;
        retValue[3] = cellCountByEdges[3] * multiplier / 5.0;
        retValue[4] = ordinal / (double) state.getNPlayers();
        retValue[5] = state.getCurrentPlayer() == playerID ? 1 : 0;
        retValue[6] = cellCountByEdges[4] / totalCells;
        retValue[7] = state.getPlayerResults()[playerID] == Utils.GameResult.WIN ? 1.0 : 0.0;
        retValue[8] = state.isNotTerminal() ? 0.0 : state.getOrdinalPosition(playerID) / (double) state.getNPlayers();

        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }

}
