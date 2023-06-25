package players.rl;

public class RLTrainingParams {

    enum Solver {
        Q_LEARNING,
        SARSA
    }

    public Solver solver = Solver.Q_LEARNING;
    public float alpha = 0.5f;
    public float gamma = 0.5f;
    public int nGames = 10;

}
