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
            "PiecesOnBoard", "PiecesOnBoard_Opp",
            "Stacks", "Stacks_Opp",
            "ActionSize"
    };

    private BGForwardModel fm = new BGForwardModel();

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        BGGameState bgState = (BGGameState) state;
        BGParameters params = (BGParameters) state.getGameParameters();
        double[] features = new double[names.length];
        // BorneOff
        features[0] = bgState.getGameScore(playerID);
        features[1] = bgState.getGameScore(1 - playerID);

        // Bar
        features[2] = bgState.getPiecesOnBar(playerID);
        features[3] = bgState.getPiecesOnBar(1 - playerID);

        // HomeBoard
        features[4] = bgState.piecesOnHomeBoard(playerID);
        features[5] = bgState.piecesOnHomeBoard(1 - playerID);

        // MeanToHome and PiecesOnBoard
        int sum = 0, count = 0, boardLen = params.boardSize;
        int singletons = 0;
        int stacks = 0;
        for (int i = 0; i < boardLen; i++) {
            int physicalIndex = bgState.getPhysicalSpace(playerID, i);
            int piecesOnPoint = bgState.getPiecesOnPoint(playerID, physicalIndex);
            sum += piecesOnPoint * i;
            count += piecesOnPoint;
            if (piecesOnPoint == 1) {
                singletons++;
            } else {
                stacks++;
            }
        }
        features[6] = singletons;
        features[8] = count > 0 ? (double) sum / count : 0.0;
        features[10] = sum;
        features[12] = stacks;

        count = 0;
        sum = 0;
        singletons = 0;
        stacks = 0;
        for (int i = 0; i < boardLen; i++) {
            int physicalIndex = bgState.getPhysicalSpace(1 - playerID, i);
            int piecesOnPoint = bgState.getPiecesOnPoint(1 - playerID, physicalIndex);
            sum += piecesOnPoint * i;
            count += piecesOnPoint;
            if (piecesOnPoint == 1) {
                singletons++;
            } else {
                stacks++;
            }
        }
        features[7] = singletons;
        features[9] = count > 0 ? (double) sum / count : 0.0;
        features[11] = sum;
        features[13] = stacks;

        // ActionSize
        features[14] = fm.computeAvailableActions(bgState).size();

        return features;

    }

    @Override
    public String[] names() {
        return names;
    }

}
