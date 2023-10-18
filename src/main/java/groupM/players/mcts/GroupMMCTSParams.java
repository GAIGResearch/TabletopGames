package groupM.players.mcts;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import players.PlayerParameters;

import java.util.Arrays;


public class GroupMMCTSParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10; // assuming we have a good heuristic
    public int maxTreeDepth = 100; // effectively no limit
    public double epsilon = 1e-6;
    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;
    public MCTSEnums.ExplorationStrategy exporationStrategy = MCTSEnums.ExplorationStrategy.UCB1;
    public TreeNodeFactory treeNodeFactory;
    public String name = "GroupM MCTS";

    public GroupMMCTSParams() {
        this(System.currentTimeMillis());
    }

    public GroupMMCTSParams(long seed) {
        super(seed);
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("rolloutLength", 10, Arrays.asList(0, 3, 10, 30, 100));
        addTunableParameter("maxTreeDepth", 100, Arrays.asList(1, 3, 10, 30, 100));
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
        addTunableParameter("explorationStrategy", MCTSEnums.ExplorationStrategy.UCB1, Arrays.asList(MCTSEnums.ExplorationStrategy.UCB1));
        treeNodeFactory = new TreeNodeFactory(exporationStrategy);    
    }

    @Override
    public void _reset() {
        super._reset();
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        epsilon = (double) getParameterValue("epsilon");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        name = (String) getParameterValue("name");
        exporationStrategy = (MCTSEnums.ExplorationStrategy) getParameterValue("explorationStrategy");
        treeNodeFactory = new TreeNodeFactory(exporationStrategy);    
    }

    @Override
    protected GroupMMCTSParams _copy() {
        // All the copying is done in TunableParameters.copy()
        // Note that any *local* changes of parameters will not be copied
        // unless they have been 'registered' with setParameterValue("name", value)
        return new GroupMMCTSParams(System.currentTimeMillis());
    }

    public IStateHeuristic getHeuristic() {
        return heuristic;
    }

    @Override
    public GroupMMCTSPlayer instantiate() {
        return new GroupMMCTSPlayer((GroupMMCTSParams) this.copy());
    }

}
