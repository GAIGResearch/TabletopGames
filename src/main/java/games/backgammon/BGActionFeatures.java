package games.backgammon;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IActionFeatureVector;

public class BGActionFeatures implements IActionFeatureVector {

    String[] names = new String[] {
            "FromPosition", "ToPosition",
            "FromOccupancy", "ToOccupancy",
            "Blot", "FirstDie"
    };

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID) {
        BGGameState bgState = (BGGameState) state;
        BGParameters params = (BGParameters) state.getGameParameters();
        if (action instanceof DoNothing) {
            return new double[names.length];
        }
        int boardLength = params.boardSize;

        MovePiece move = (MovePiece) action;
        double[] features = new double[names.length];

        // MovePiece uses the physical board positions, so we need to convert these to the distance on the track
        int from = -1;
        int to = -1;
        for (int i = 0 ; i < boardLength; i++) {
            if (bgState.getPhysicalSpace(playerID, i) == move.from) {
                from = i;
            }
            if (bgState.getPhysicalSpace(playerID, i) == move.to) {
                to = i;
            }
        }

        features[0] = from;
        // -1 means we are bearing off, so we use the board length to represent this
        features[1] = to < 0 ? boardLength : to;

        features[2] = bgState.getPiecesOnPoint(playerID, move.from);

        // if -1 then we are bearing off
        features[3] = move.to < 0 ? 0 : bgState.getPiecesOnPoint(playerID, move.to);

        int opponentID = 1 - playerID;
        features[4] = (move.to >= 0 && bgState.getPiecesOnPoint(opponentID, move.to) == 1) ? 1.0 : 0.0;

        // Is this out first die to use?
        features[5] = bgState.getAvailableDiceValues().length == 2 ? 1.0 : 0.0;

        return features;

    }

    @Override
    public String[] names() {
        return names;
    }
}
