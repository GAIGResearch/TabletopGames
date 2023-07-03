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
        LINEAR, // Make a file every {writeSegmentFactor} steps
        LOGARITHMIC // Make a file every time {steps} = {writeSegmentFactor} * {last written steps}
    }

    public WriteSegmentType writeSegmentType = WriteSegmentType.NONE;
    public int writeSegmentFactor = 0;
    public int writeSegmentMinIterations = 0; // inclusive

    public int updateXIterations; // default value set in constructor;

    public Solver solver = Solver.Q_LEARNING;
    public double alpha = 0.1f;
    public double gamma = 0.1f;
    public IStateHeuristic heuristic = new WinOnlyHeuristic();
    public boolean overwriteInfile = false;

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
