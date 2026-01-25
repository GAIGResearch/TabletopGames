package games.backgammon;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IActionFeatureVector;

public class BGActionFeatures implements IActionFeatureVector {

    String[] names = new String[] {
            "FromPosition", "ToPosition",
            "FromOccupancy", "ToOccupancy",
            "Blot", "RemainingDice", "ToHome",
            "FirstDie"
    };

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID) {
        BGGameState bgState = (BGGameState) state;
        BGParameters params = (BGParameters) state.getGameParameters();
        if (action instanceof DoNothing) {
            return new double[names.length];
        }
        int boardLength = bgState.playerTrackMapping[0].length;

        MovePiece move = (MovePiece) action;
        double[] features = new double[names.length];

        // MovePiece uses the physical board positions, so we need to convert these to the distance on the track
        int from = bgState.getLogicalPosition(playerID, move.from);
        int to = move.to == -1 ? -1 : bgState.getLogicalPosition(playerID, move.to);

        features[0] = from;
        // -1 means we are bearing off, so we use the board length + 1 to represent this
        features[1] = to < 0 ? boardLength + 1 : to;

        features[2] = bgState.getPiecesOnPoint(playerID, move.from);

        // if -1 then we are bearing off
        features[3] = move.to < 0 ? 0 : bgState.getPiecesOnPoint(playerID, move.to);

        int opponentID = 1 - playerID;
        features[4] = (move.to >= 0 && bgState.getPiecesOnPoint(opponentID, move.to) == 1) ? 1.0 : 0.0;

        // Remaining dice
        features[5] = bgState.getAvailableDiceValues().length - 1;

        features[6] = to >= boardLength - params.homeBoardSize ? 1.0 : 0.0;

        // First Die
        features[7] = bgState.getAvailableDiceValues().length == 2 ? 1.0 : 0.0;

        return features;

    }

    @Override
    public String[] names() {
        return names;
    }
}
