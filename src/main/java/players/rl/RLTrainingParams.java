package players.rl;

import core.interfaces.IStateHeuristic;
import players.heuristics.WinOnlyHeuristic;

public class RLTrainingParams {

    enum Solver {
        Q_LEARNING
        // , SARSA
    }

    public Solver solver = Solver.Q_LEARNING;
    public double alpha = 0.1f;
    public double gamma = 0.1f;
    public IStateHeuristic heuristic = new WinOnlyHeuristic();
    public boolean overwriteInfile = false;

    public final int nGames;

    public RLTrainingParams(int nGames) {
        this.nGames = nGames;
    }

}
