package evaluation.heuristics;

import core.AbstractGameState;
import core.Game;
import core.interfaces.IGameHeuristic;

import java.util.DoubleSummaryStatistics;
import java.util.stream.IntStream;

public class TargetScoreDelta implements IGameHeuristic {

    public final int target;

    public TargetScoreDelta(int target) {
        this.target = target;
    }


    @Override
    public double evaluateGame(Game game) {
        AbstractGameState state = game.getGameState();
        DoubleSummaryStatistics stats = IntStream.range(0, state.getNPlayers())
                .mapToDouble(state::getGameScore)
                .summaryStatistics();
        return -Math.abs(stats.getMax() - stats.getMin() - target);
    }
}
