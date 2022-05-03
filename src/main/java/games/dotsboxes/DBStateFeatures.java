package games.dotsboxes;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

import java.io.*;
import java.util.Arrays;

public class DBStateFeatures implements IStateFeatureVector, IStateHeuristic {

    boolean logistic = false;
    String[] names = new String[]{"POINTS", "POINT_ADVANTAGE", "TWO_BOXES", "THREE_BOXES", "ORDINAL", "OUR_TURN", "FILLED_BOXES", "HAS_WON", "FINAL_POSITION", "BIAS"};
    double[] coefficients = new double[]{0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

    public DBStateFeatures() {
        this("", false);
    }

    public DBStateFeatures(String file, boolean logistic) {
        this.logistic = logistic;
        if (!file.isEmpty()) {
            File coeffFile = new File(file);
            try (BufferedReader br = new BufferedReader(new FileReader(coeffFile))) {
                String[] headers = br.readLine().split("\\t");
                if (!Arrays.equals(headers, names)) {
                    throw new AssertionError("Incompatible data in file " + coeffFile);
                }
                coefficients = Arrays.stream(br.readLine().split("\\t")).mapToDouble(Double::parseDouble).toArray();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                throw new AssertionError("File not found : " + coeffFile);
            } catch (IOException e) {
                e.printStackTrace();
                throw new AssertionError("Error accessing : " + coeffFile);
            }
        }
    }

    @Override
    public double[] featureVector(AbstractGameState gs, int playerID) {
        double[] retValue = new double[names.length];
        DBGameState state = (DBGameState) gs;
        // POINTS
        retValue[0] = state.nCellsPerPlayer[playerID];

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

        // POINT_ADVANTAGE
        retValue[1] = state.nCellsPerPlayer[playerID] - maxOtherScore;

        // CELLS
        int[] cellCountByEdges = new int[5];
        for (DBCell cell : state.cells) {
            int edges = state.countCompleteEdges(cell);
            cellCountByEdges[edges]++;
        }
        int multiplier = state.getCurrentPlayer() == playerID ? 1 : -1;
        retValue[2] = cellCountByEdges[2] * multiplier;
        retValue[3] = cellCountByEdges[3] * multiplier;
        retValue[4] = ordinal;
        retValue[5] = state.getCurrentPlayer() == playerID ? 1 : 0;
        retValue[6] = cellCountByEdges[4] * multiplier;
        retValue[7] = state.getPlayerResults()[playerID] == Utils.GameResult.WIN ? 1.0 : 0.0;
        retValue[8] = state.isNotTerminal() ? 0.0 : state.getOrdinalPosition(playerID);
        retValue[9] = 1.0;

        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        double[] phi = featureVector(state, playerId);
        double retValue = 0.0;
        for (int i = 0; i < phi.length; i++) {
            retValue += phi[i] * coefficients[i];
        }
        if (logistic)
            return 1.0 / ( 1.0 + Math.exp(-retValue));
        return retValue;
    }
}
