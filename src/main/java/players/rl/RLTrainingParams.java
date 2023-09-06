package players.rl;

import core.interfaces.IStateHeuristic;
import players.heuristics.WinOnlyHeuristic;

public class RLTrainingParams {

    enum Solver {
        Q_LEARNING
        // , SARSA
    }

    enum WriteSegmentType {
        NONE, // Only make a single final output file
        LINEAR(0, (n, factor) -> n + factor),
        LOGARITHMIC(1, (n, factor) -> n * factor);

        interface Operator {
            int operate(int n, int factor);
        }

        final int n0;
        final Operator operator;

        WriteSegmentType() {
            n0 = 0; // Unused
            operator = null; // Unused
        }

        WriteSegmentType(int n0, Operator operator) {
            this.n0 = n0;
            this.operator = operator;
        }
    }

    public WriteSegmentType writeSegmentType = WriteSegmentType.NONE;
    public int writeSegmentFactor = 0;
    public int writeSegmentMinIterations = 0; // inclusive
    public int updateXIterations; // default value set in constructor;

    public Solver solver = Solver.Q_LEARNING;
    public double alpha = 0.0125f;
    public double gamma = 0.875f;
    public IStateHeuristic heuristic = new WinOnlyHeuristic();
    public String outfilePrefix = null;

    public final String gameName;
    public final int nGames;
    public final int nPlayers;

    public RLTrainingParams(String gameName, int nPlayers, int nGames) {
        this.gameName = gameName;
        this.nPlayers = nPlayers;
        this.nGames = nGames;
        this.updateXIterations = nGames;
    }

}
