package games.seasaltpaper.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.SeaSaltPaperParameters;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;

public class LeadHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
//        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gs;
        double[] scores = new double[gs.getNPlayers()];
        double max = 0; // max score of every other players
        for (int i=0; i < gs.getNPlayers(); i++) {
            scores[i] = gs.getHeuristicScore(i);
//            scores[i] = HandManager.calculatePoint(this, i);
            if (i != playerId) {
                if (scores[i] > max) {
                    max = scores[i];
                }
            }
        }
        DoubleSummaryStatistics stat = Arrays.stream(scores).summaryStatistics();
//        return (2 * scores[playerId]) - stat.getMax();
//        return scores[playerId] - stat.getMax();
//        return playerTotalScores[playerId] + (2*scores[playerId]) - stat.getMax();
        return scores[playerId] - max;
    }

    @Override
    public double minValue() {
        return -35;
    }

    @Override
    public double maxValue() {
        return 35;
    }
}
