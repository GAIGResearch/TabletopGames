package players.sushiGoAgent;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;

public class SushiGoHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        System.out.println("SushiGoHeuristic was evaluated");
        double score = gs.getGameScore(playerId);
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME)
            return score * 1.5;
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME)
            return score * 0.5;
        return score;
    }
}
