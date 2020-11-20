package players.mcts;

import core.*;
import core.interfaces.*;
import games.dominion.BigMoney;
import games.dominion.PlayActionCards;
import players.PlayerParameters;
import players.simple.RandomPlayer;

import java.util.*;

import static players.mcts.MCTSEnums.SelectionPolicy.*;
import static players.mcts.MCTSEnums.strategies.*;
import static players.mcts.MCTSEnums.TreePolicy.*;
import static players.mcts.MCTSEnums.OpponentTreePolicy.*;

public class MCTSParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10;
    public int maxTreeDepth = 10;
    public double epsilon = 1e-6;
    public MCTSEnums.strategies rolloutType = RANDOM;
    public boolean openLoop = false;
    public boolean redeterminise = false;
    public MCTSEnums.SelectionPolicy selectionPolicy = ROBUST;
    public MCTSEnums.TreePolicy treePolicy = UCB;
    public MCTSEnums.OpponentTreePolicy opponentTreePolicy = Paranoid;
    public double exploreEpsilon = 0.1;

    public MCTSParams() {
        this(System.currentTimeMillis());
    }

    public MCTSParams(long seed) {
        super(seed);
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("rolloutLength", 10, Arrays.asList(6, 8, 10, 12, 20));
        addTunableParameter("maxTreeDepth", 10, Arrays.asList(1, 3, 10, 30));
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("rolloutType", RANDOM);
        addTunableParameter("openLoop", false, Arrays.asList(false, true));
        addTunableParameter("redeterminise", false, Arrays.asList(false, true));
        addTunableParameter("selectionPolicy", ROBUST, Arrays.asList(MCTSEnums.SelectionPolicy.values()));
        addTunableParameter("treePolicy", UCB);
        addTunableParameter("opponentTreePolicy", Paranoid);
        addTunableParameter("exploreEpsilon", 0.1);
    }

    @Override
    public void _reset() {
        super._reset();
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        epsilon = (double) getParameterValue("epsilon");
        rolloutType = (MCTSEnums.strategies) getParameterValue("rolloutType");
        openLoop = (boolean) getParameterValue("openLoop");
        redeterminise = (boolean) getParameterValue("redeterminise");
        selectionPolicy = (MCTSEnums.SelectionPolicy) getParameterValue("selectionPolicy");
        treePolicy = (MCTSEnums.TreePolicy) getParameterValue("treePolicy");
        opponentTreePolicy = (MCTSEnums.OpponentTreePolicy) getParameterValue("opponentTreePolicy");
        exploreEpsilon = (double) getParameterValue("exploreEpsilon");
    }

    @Override
    protected AbstractParameters _copy() {
        return new MCTSParams(System.currentTimeMillis());
    }

    /**
     * @return Returns the AbstractPlayer policy that will take actions during an MCTS rollout.
     *         This defaults to a Random player.
     */
    public AbstractPlayer getRolloutStrategy() {
        switch (rolloutType) {
            case RANDOM:
                return new RandomPlayer(new Random(getRandomSeed()));
            case Dominion_BigMoney:
                return new BigMoney();
            case Dominion_PlayActions:
                return new PlayActionCards(new Random(getRandomSeed()));
            default:
                throw new AssertionError("Unknown rollout type : " + rolloutType);
        }
    }

    public AbstractPlayer getOpponentModel() {
        return new RandomPlayer(new Random(getRandomSeed()));
    }


    @Override
    public MCTSPlayer instantiate() {
        return new MCTSPlayer(this);
    }
}
