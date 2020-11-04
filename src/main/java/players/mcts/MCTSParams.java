package players.mcts;

import core.*;
import players.PlayerParameters;
import players.simple.RandomPlayer;

import java.util.*;

import static players.mcts.MCTSEnums.SelectionPolicy.*;
import static players.mcts.MCTSEnums.strategies.*;

public class MCTSParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10;
    public boolean rolloutsEnabled = false;
    public double epsilon = 1e-6;
    public MCTSEnums.strategies rolloutType = RANDOM;
    public boolean openLoop = false;
    public boolean redeterminise = false;
    public boolean selfOnlyInTree = false;
    public MCTSEnums.SelectionPolicy selectionPolicy = ROBUST;

    public MCTSParams() {
        this(System.currentTimeMillis());
    }

    public MCTSParams(long seed) {
        super(seed);
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("rolloutLength", 10, Arrays.asList(6, 8, 10, 12, 20));
        addTunableParameter("rolloutsEnabled", false, Arrays.asList(false, true));
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("rolloutType", RANDOM);
        addTunableParameter("openLoop", false, Arrays.asList(false, true));
        addTunableParameter("redeterminise", false, Arrays.asList(false, true));
        addTunableParameter("selfOnlyInTree", false, Arrays.asList(false, true));
        addTunableParameter("selectionPolicy", ROBUST, Arrays.asList(MCTSEnums.SelectionPolicy.values()));
    }

    @Override
    public void _reset() {
        super._reset();
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        rolloutsEnabled = (boolean) getParameterValue("rolloutsEnabled");
        epsilon = (double) getParameterValue("epsilon");
        rolloutType = (MCTSEnums.strategies) getParameterValue("rolloutType");
        openLoop = (boolean) getParameterValue("openLoop");
        redeterminise = (boolean) getParameterValue("redeterminise");
        selfOnlyInTree = (boolean) getParameterValue("selfOnlyInTree");
        selectionPolicy = (MCTSEnums.SelectionPolicy) getParameterValue("selectionPolicy");
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
        return new RandomPlayer(new Random(getRandomSeed()));
    }

    public AbstractPlayer getOpponentModel() {
        return new RandomPlayer(new Random(getRandomSeed()));
    }


    @Override
    public MCTSPlayer instantiate() {
        return new MCTSPlayer(this);
    }
}
