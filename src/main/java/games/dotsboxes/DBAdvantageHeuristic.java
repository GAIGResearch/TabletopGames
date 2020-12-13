package games.dotsboxes;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

import java.util.Arrays;

public class DBAdvantageHeuristic implements IStateHeuristic {

    public final double POINT_ADVANTAGE = 0.05;
    public final double POINT_UNDER_LEADER = -0.10;
    /**
     * Returns a score for the state that should be maximised by the player (the bigger, the better).
     * Ideally bounded between [-1, 1].
     *
     * @param gs       - game state to evaluate and score.
     * @param playerId
     * @return - value of given state.
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DBGameState state = (DBGameState) gs;
        Utils.GameResult playerResult = gs.getPlayerResults()[playerId];

        if (playerResult == Utils.GameResult.LOSE)
            return -1;
        if (playerResult == Utils.GameResult.WIN)
            return 1;
        if (playerResult == Utils.GameResult.DRAW)
            return 0;

        int[] deltaToPlayer = new int[state.getNPlayers()];
        double retValue = 0.0;
        for (int p = 0; p < state.getNPlayers(); p++) {
            deltaToPlayer[p] = state.nCellsPerPlayer[playerId] - state.nCellsPerPlayer[p];
            retValue += deltaToPlayer[p] * POINT_ADVANTAGE / state.getNPlayers();
        }
        int maxScore = Arrays.stream(deltaToPlayer).max().getAsInt();

        retValue += POINT_UNDER_LEADER * (maxScore - state.nCellsPerPlayer[playerId]);

        return retValue;
    }
}
