package players.mcts;

import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.interfaces.IStateHeuristic;
import core.interfaces.ITunableParameters;
import evaluation.TunableParameters;
import org.json.simple.JSONObject;
import players.PlayerParameters;
import players.simple.RandomPlayer;

import java.util.Arrays;
import java.util.Random;

import static players.mcts.MCTSEnums.MASTType.*;
import static players.mcts.MCTSEnums.OpponentTreePolicy.MaxN;
import static players.mcts.MCTSEnums.OpponentTreePolicy.Paranoid;
import static players.mcts.MCTSEnums.SelectionPolicy.ROBUST;
import static players.mcts.MCTSEnums.Strategies.RANDOM;
import static players.mcts.MCTSEnums.TreePolicy.UCB;

public class MCTSParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10;
    public int maxTreeDepth = 10;
    public double epsilon = 1e-6;
    public boolean openLoop = false;
    public boolean redeterminise = false;
    public MCTSEnums.MASTType MAST = Rollout;
    public boolean useMAST = false;
    public double MASTBoltzmann = 0.0;
    public MCTSEnums.Strategies expansionPolicy = RANDOM;
    public MCTSEnums.Strategies rolloutType = RANDOM;
    public MCTSEnums.SelectionPolicy selectionPolicy = ROBUST;
    public MCTSEnums.TreePolicy treePolicy = UCB;
    public MCTSEnums.OpponentTreePolicy opponentTreePolicy = Paranoid;
    public double exploreEpsilon = 0.1;
    private IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;

    public MCTSParams() {
        this(System.currentTimeMillis());
    }

    public MCTSParams(long seed) {
        super(seed);
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("boltzmannTemp", 1.0);
        addTunableParameter("rolloutLength", 10, Arrays.asList(6, 8, 10, 12, 20));
        addTunableParameter("maxTreeDepth", 10, Arrays.asList(1, 3, 10, 30));
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("rolloutType", MCTSEnums.Strategies.RANDOM);
        addTunableParameter("openLoop", false, Arrays.asList(false, true));
        addTunableParameter("redeterminise", false, Arrays.asList(false, true));
        addTunableParameter("selectionPolicy", ROBUST, Arrays.asList(MCTSEnums.SelectionPolicy.values()));
        addTunableParameter("treePolicy", UCB);
        addTunableParameter("opponentTreePolicy", MaxN);
        addTunableParameter("exploreEpsilon", 0.1);
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
        addTunableParameter("expansionType", MCTSEnums.Strategies.RANDOM);
        addTunableParameter("MAST", Rollout);
    }

    @Override
    public void _reset() {
        super._reset();
        useMAST = false;
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        epsilon = (double) getParameterValue("epsilon");
        rolloutType = (MCTSEnums.Strategies) getParameterValue("rolloutType");
        openLoop = (boolean) getParameterValue("openLoop");
        redeterminise = (boolean) getParameterValue("redeterminise");
        selectionPolicy = (MCTSEnums.SelectionPolicy) getParameterValue("selectionPolicy");
        MCTSEnums.Strategies expansionPolicy = (MCTSEnums.Strategies) getParameterValue("expansionPolicy");
        treePolicy = (MCTSEnums.TreePolicy) getParameterValue("treePolicy");
        opponentTreePolicy = (MCTSEnums.OpponentTreePolicy) getParameterValue("opponentTreePolicy");
        exploreEpsilon = (double) getParameterValue("exploreEpsilon");
        MASTBoltzmann = (double) getParameterValue("boltzmannTemp");
        MAST = (MCTSEnums.MASTType) getParameterValue("MAST");
        if (expansionPolicy == MCTSEnums.Strategies.MAST || rolloutType == MCTSEnums.Strategies.MAST) {
            useMAST = true;
        }
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        if (heuristic instanceof TunableParameters) {
            TunableParameters tunableHeuristic = (TunableParameters) heuristic;
            for (String name : tunableHeuristic.getParameterNames()) {
                tunableHeuristic.setParameterValue(name, this.getParameterValue("heuristic." + name));
            }
        }
    }

    /**
     * Any nested tunable parameter space is highly likely to be an IStateHeuristic
     * If it is, then we set this as the heuristic after the parent code in TunableParameters
     * has done the work to merge the search spaces together.
     *
     * @param json The raw JSON
     * @return The instantiated object
     */
    @Override
    public ITunableParameters registerChild(String nameSpace, JSONObject json) {
        ITunableParameters child = super.registerChild(nameSpace, json);
        if (child instanceof IStateHeuristic) {
            heuristic = (IStateHeuristic) child;
            setParameterValue("heuristic", child);
        }
        return child;
    }

    @Override
    protected AbstractParameters _copy() {
        return new MCTSParams(System.currentTimeMillis());
    }


    /**
     * @return Returns the AbstractPlayer policy that will take actions during an MCTS rollout.
     * This defaults to a Random player.
     */
    public AbstractPlayer getRolloutStrategy() {
        switch (rolloutType) {
            case RANDOM:
                return new RandomPlayer(new Random(getRandomSeed()));
            case MAST:
                return new MASTPlayer(new Random(getRandomSeed()));
            default:
                throw new AssertionError("Unknown rollout type : " + rolloutType);
        }
    }

    public AbstractPlayer getOpponentModel() {
        return new RandomPlayer(new Random(getRandomSeed()));
    }

    public IStateHeuristic getHeuristic() {
        return heuristic;
    }

    @Override
    public MCTSPlayer instantiate() {
        return new MCTSPlayer(this);
    }

}
