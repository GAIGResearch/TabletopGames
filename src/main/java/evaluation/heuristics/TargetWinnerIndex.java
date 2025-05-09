package evaluation.heuristics;

import core.CoreConstants;
import core.Game;
import core.interfaces.IGameHeuristic;

public class TargetWinnerIndex implements IGameHeuristic {

    public final int winnerIndex;

    public TargetWinnerIndex(int winnerIndex) {
        this.winnerIndex = winnerIndex;
    }

    @Override
    public double evaluateGame(Game game) {
        // +1 for winning (regardless of draw status)
        // 0 otherwise
        if (game.getGameState().getPlayerResults()[winnerIndex] == CoreConstants.GameResult.WIN_GAME) return 1.0;
        return 0.0;
    }
}
