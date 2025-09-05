package games.backgammon;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

public class BGStateFeatures implements IStateFeatureVector {


    String[] names = new String[]{
            "BorneOff", "BorneOff_Opp",
            "Bar", "Bar_Opp",
            "HomeBoard", "HomeBoard_Opp",
            "Singletons", "Singletons_Opp",
            "MeanToHome", "MeanToHome_Opp",
            "PiecesOnBoard", "PiecesOnBoard_Opp"
    };

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        BGGameState bgState = (BGGameState) state;
        double[] features = new double[names.length];
        // BorneOff
        features[0] = bgState.getGameScore(playerID);
        features[1] = bgState.getGameScore(1 - playerID);

        // Bar
        features[2] = bgState.piecesOnBar[playerID];
        features[3] = bgState.piecesOnBar[1 - playerID];

        // HomeBoard
        features[4] = bgState.piecesOnHomeBoard(playerID);
        features[5] = bgState.piecesOnHomeBoard(1 - playerID);

        // MeanToHome and PiecesOnBoard
        int sum = 0, count = 0, boardLen = bgState.piecesPerPoint[playerID].length;
        int singletons = 0;
        for (int i = 0; i < boardLen; i++) {
            sum += bgState.piecesPerPoint[playerID][i] * i;
            count += bgState.piecesPerPoint[playerID][i];
            if (bgState.piecesPerPoint[playerID][i] == 1) {
                singletons++;
            }
        }
        features[6] = singletons;
        features[8] = count > 0 ? (double) sum / count : 0.0;
        features[10] = sum;

        count = 0;
        sum = 0;
        singletons = 0;
        boardLen = bgState.piecesPerPoint[1 - playerID].length;
        for (int i = 0; i < boardLen; i++) {
            sum += bgState.piecesPerPoint[1 - playerID][i] * i;
            count += bgState.piecesPerPoint[1 - playerID][i];
            if (bgState.piecesPerPoint[1 - playerID][i] == 1) {
                singletons++;
            }
        }
        features[7] = singletons;
        features[9] = count > 0 ? (double) sum / count : 0.0;
        features[11] = sum;

        return features;

    }

    @Override
    public String[] names() {
        return names;
    }

}
