package players.rl;

public class RLTrainingParams {

    enum Solver {
        Q_LEARNING,
        SARSA
    }

    public Solver solver = Solver.Q_LEARNING;
    public float alpha = 0.1f;
    public float gamma = 0.1f;

    public final int nGames;

    public RLTrainingParams(int nGames) {
        this.nGames = nGames;
    }

}
