package players.mcts;

import core.interfaces.IStateHeuristic;

@SuppressWarnings("WeakerAccess")
public class MCTSParams {

    // Parameters
    public double K = Math.sqrt(2);
    public int ROLLOUT_LENGTH = 10;//10;
    public boolean ROLOUTS_ENABLED = false;

    public final int STOP_TIME = 0;
    public final int STOP_ITERATIONS = 1;
    public final int STOP_FMCALLS = 2;

    public double epsilon = 1e-6;

    // Budget settings
    public int stop_type = STOP_TIME;
    public int num_iterations = 200;
    public int num_fmcalls = 4000;
    public int num_time = 100; //milliseconds

    public long seed = System.currentTimeMillis();

    // Heuristic
    public IStateHeuristic gameHeuristic;
}
