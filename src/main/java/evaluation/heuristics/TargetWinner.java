package evaluation.heuristics;

import core.Game;
import core.interfaces.IGameHeuristic;
import utilities.Utils;

public class TargetWinner implements IGameHeuristic {

    public final String winnerName;

    public TargetWinner(String winnerName) {
        this.winnerName = winnerName;
    }

    @Override
    public double evaluateGame(Game game) {
        // +1 for winning (regardless of draw status)
        // 0 otherwise
        for (int p = 0; p < game.getPlayers().size(); p++) {
            if (game.getPlayers().get(p).toString().equals(winnerName))
                if (game.getGameState().getPlayerResults()[p] == Utils.GameResult.WIN)
                    return 1.0;
        }
        return 0.0;
    }
}
