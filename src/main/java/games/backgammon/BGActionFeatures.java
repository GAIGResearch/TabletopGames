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
        if (action instanceof DoNothing) {
            return new double[names.length];
        }
        int boardLength = bgState.getPlayerPieces(0).length;

        MovePiece move = (MovePiece) action;
        double[] features = new double[names.length];

        // if -1 then we are coming in from the bar
        features[0] = move.from < 0 ? boardLength : move.from;

        // -1 means we are bearing off, so -1 is fine
        features[1] = move.to;

        features[2] = move.from < 0 ? boardLength : bgState.getPiecesOnPoint(playerID, move.from);

        // if -1 then we are bearing off
        features[3] = move.to < 0 ? 0 : bgState.getPiecesOnPoint(playerID, move.to);

        // opponent representation is reversed to our, so our position 0 is their position 23
        int opponentID = 1 - playerID;
        features[4] = (move.to >= 0 && bgState.getPiecesOnPoint(opponentID, boardLength - move.to - 1) == 1) ? 1.0 : 0.0;

        // Is this out first die to use?
        features[5] = bgState.getAvailableDiceValues().length == 2 ? 1.0 : 0.0;

        return features;

    }

    @Override
    public String[] names() {
        return names;
    }
}
