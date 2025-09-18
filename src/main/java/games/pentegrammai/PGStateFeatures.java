package games.pentegrammai;

import core.AbstractGameState;
import core.components.Token;
import core.interfaces.IStateFeatureVector;
import games.backgammon.BGGameState;
import games.backgammon.BGParameters;
import games.root.actions.PassGamePhase;

import java.util.List;

public class PGStateFeatures implements IStateFeatureVector {

    String[] names = new String[]{
            "EmptySpaces",
            "Singletons", "Singletons_Opp",
            "Stacks", "Stacks_Opp",
            "HolyLine", "HolyLine_Opp",
            "Doubletons", "Doubletons_Opp",
            "Triplets", "Triplets_Opp",
            "MeanToHoly", "MeanToHoly_Opp",
            "DieToHoly", "DieToHoly_Opp",
            "OffBoard", "OffBoard_Opp"
    };

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        PenteGameState pgState = (PenteGameState) state;
        PenteParameters params = (PenteParameters) state.getGameParameters();
        double[] features = new double[names.length];
        // Empty spaces
        features[0] = pgState.board.stream().filter(List::isEmpty).count();

        // MeanToHome and piece arrangements
        int[] sum = new int[2];
        int[] singletons = new int[2];
        int[] doubletons = new int[2];
        int[] triplets = new int[2];
        int[] holyLine = new int[2];
        int[] stacks = new int[2];
        int[] dieToHoly = new int[2];
        for (int i = 0; i < params.boardSize; i++) {
            List<Token> pieces = pgState.board.get(i);
            if (!pieces.isEmpty()) {
                int firstPlayerPieces = Math.toIntExact(pieces.stream().filter(p -> p.getOwnerId() == 0).count());
                int secondPlayerPieces = pieces.size() - firstPlayerPieces;

                int firstPlayerDistance = (params.boardSize + pgState.playerGoal[0] - i) % params.boardSize;
                int secondPlayerDistance = (params.boardSize + pgState.playerGoal[1] - i) % params.boardSize;

                if (firstPlayerPieces > 0) {
                    sum[0] += firstPlayerPieces * firstPlayerDistance;
                    if (firstPlayerPieces == 1) {
                        singletons[0]++;
                    } else if (firstPlayerPieces == 2) {
                        doubletons[0]++;
                    } else if (firstPlayerPieces == 3) {
                        triplets[0]++;
                    }
                    if (firstPlayerPieces > 1) {
                        stacks[0]++;
                    }
                    if (i == pgState.playerGoal[0]) {
                        holyLine[0] = firstPlayerPieces;
                    }
                    if (firstPlayerDistance <= 6) {
                        dieToHoly[0] += firstPlayerPieces;
                    }
                }
                if (secondPlayerPieces > 0) {
                    sum[1] += secondPlayerPieces * secondPlayerDistance;
                    if (secondPlayerPieces == 1) {
                        singletons[1]++;
                    } else if (secondPlayerPieces == 2) {
                        doubletons[1]++;
                    } else if (secondPlayerPieces == 3) {
                        triplets[1]++;
                    }
                    if (secondPlayerPieces > 1) {
                        stacks[1]++;
                    }
                    if (i == pgState.playerGoal[1]) {
                        holyLine[1] = secondPlayerPieces;
                    }
                    if (secondPlayerDistance <= 6) {
                        dieToHoly[1] += secondPlayerPieces;
                    }
                }
            }
        }
        features[1] = singletons[playerID];
        features[2] = singletons[1 - playerID];
        features[3] = stacks[playerID];
        features[4] = stacks[1 - playerID];
        features[5] = holyLine[playerID];
        features[6] = holyLine[1 - playerID];
        features[7] = doubletons[playerID];
        features[8] = doubletons[1 - playerID];
        features[9] = triplets[playerID];
        features[10] = triplets[1 - playerID];
        features[11] = (double) sum[playerID] / params.boardSize / 2;
        features[12] = (double) sum[1 - playerID] / params.boardSize / 2;
        features[13] = dieToHoly[playerID];
        features[14] = dieToHoly[1 - playerID];
        features[15] = pgState.offBoard.stream().filter(t -> t.getOwnerId() == playerID).count();
        features[16] = pgState.offBoard.stream().filter(t -> t.getOwnerId() == 1 - playerID).count();
        return features;

    }

    @Override
    public String[] names() {
        return names;
    }

}
