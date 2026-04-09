package games.backgammon;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IActionFeatureVector;
import games.backgammon.actions.LoadDice;
import games.backgammon.actions.MovePiece;
import games.backgammon.actions.RollDice;

public class BGActionFeatures implements IActionFeatureVector {

    String[] names = new String[] {
            "FromPosition", "ToPosition",
            "FromOccupancy", "ToOccupancy",
            "Blot", "RemainingDice", "ToHome",
            "IsCheat", "Detection",
            "Delta1", "Delta2", "Delta3", "Delta4", "Delta5", "Delta6"
    };

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID) {
        BGGameState bgState = (BGGameState) state;
        BGParameters params = (BGParameters) state.getGameParameters();
        double[] features = new double[names.length];

        if (action instanceof MovePiece move) {
            int boardLength = bgState.getLengthOfTrack();

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
        }
        // Cheat features
        if (action instanceof LoadDice ld) {
            features[7] = 1.0;
            features[8] = ld.getDetectionChance();
            double[] currentPdf = bgState.getDicePdf(ld.getDieNumber());
            for (int dieRoll = 1; dieRoll <= 6; dieRoll++) {
                features[7 + dieRoll] = ld.getPdf()[dieRoll - 1] - currentPdf[dieRoll - 1];
            }
        }

        return features;
    }

    @Override
    public String[] names() {
        return names;
    }
}
