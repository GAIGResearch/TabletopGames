package games.pentegrammai;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;

public class PGActionFeatures implements IActionFeatureVector {

    @Override
    public String[] names() {
        return new String[]{
                "ToHolyLine", "OffHolyLine", "Blot", "PiecesTo", "PiecesFrom", "DistanceChange"
        };
    }

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID) {
        double[] features = new double[names().length];

        PenteGameState pgState = (PenteGameState) state;
        PenteParameters params = (PenteParameters) state.getGameParameters();
        if (action instanceof PenteMoveAction move) {
            int from = move.from;
            int to = move.to;
            int piecesFrom = pgState.board.get(from).size();
            int piecesTo = pgState.board.get(to).size();
            int distFromGoalBefore = (params.boardSize + pgState.playerGoal[playerID] - from) % params.boardSize;
            int distFromGoalAfter = (params.boardSize + pgState.playerGoal[playerID] - to) % params.boardSize;
            features[0] = to == pgState.playerGoal[playerID] ? 1.0 : 0.0; // ToHolyLine
            features[1] = from == pgState.playerGoal[playerID] ? 1.0 : 0.0;   // OffHolyLine
            features[2] = (piecesTo == 1 && pgState.board.get(to).get(0).getOwnerId() != playerID) ? 1.0 : 0.0; // Blot
            features[3] = piecesTo; // PiecesTo
            features[4] = piecesFrom; // PiecesFrom
            features[5] = distFromGoalBefore - distFromGoalAfter; // DistanceChange
        }
        return features;
    }


}
