package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateFeatureVector;

public abstract class AbstractStateFeature implements IStateFeatureVector {

    String[] coreNames = new String[]{"SCORE", "SCORE_ADV", "ORDINAL", "OUR_TURN", "HAS_WON", "FINAL_ORD", "ROUND"};

    protected abstract double maxScore();
    protected abstract double maxRounds();
    protected abstract String[] localNames();
    protected abstract double[] localFeatureVector(AbstractGameState gs, int playerID);

    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        double[] localFeatures = localFeatureVector(state, playerID);
        double[] retValue = new double[coreNames.length + localFeatures.length];

        // POINT_ADVANTAGE
        int ordinal = 1;
        double maxOtherScore = -1;
        double ourSc = state.getGameScore(playerID);
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (p == playerID) continue;
            double sc = state.getGameScore(p);
            if (sc > maxOtherScore) {
                maxOtherScore = sc;
                if (sc > ourSc)
                    ordinal++;
            }
        }

        // POINTS
        retValue[0] = ourSc / maxScore();
        // POINT_ADVANTAGE
        retValue[1] = (ourSc - maxOtherScore) / maxScore() * 2.0;
        retValue[2] = ordinal / (double) state.getNPlayers();
        retValue[3] = state.getCurrentPlayer() == playerID ? 1 : 0;
        retValue[4] = state.getPlayerResults()[playerID] == CoreConstants.GameResult.WIN_GAME ? 1.0 : 0.0;
        retValue[5] = state.isNotTerminal() ? 0.0 : state.getOrdinalPosition(playerID) / (double) state.getNPlayers();
        retValue[6] = state.getRoundCounter() / maxRounds();

        System.arraycopy(localFeatures, 0, retValue, coreNames.length, localFeatures.length);
        return retValue;
    }

    @Override
    public String[] names() {
        String[] localNames = localNames();
        String[] retValue = new String[coreNames.length + localNames.length];
        System.arraycopy(coreNames, 0, retValue, 0, coreNames.length);
        System.arraycopy(localNames, 0, retValue, coreNames.length, localNames.length);
        return retValue;
    }
}
