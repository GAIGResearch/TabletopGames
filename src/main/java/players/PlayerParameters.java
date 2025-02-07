package players;

import core.AbstractPlayer;
import core.interfaces.IPlayerDecorator;
import core.actions.ActionSpace;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

public class PlayerParameters extends TunableParameters<AbstractPlayer> {

    public double noiseEpsilon = 1e-6;

    // Budget settings
    public PlayerConstants budgetType = PlayerConstants.BUDGET_FM_CALLS;
    public int budget = 4000;
    // breakMS is the number of milliseconds prior to the end of the turn that the player will stop searching
    // this is intended mainly for competition situations, in which overrunning the time limit leads to disqualification.
    // setting breakMS to some number greater than zero then adds a safety margin
    public int breakMS = 0;
    // resetSeedEachGame is a dangerous parameter. If true then the random seed will be reset at the start of each game.
    // otherwise the Random() object will be used from the old game, ensuring that we do not take exactly the same
    // set of actions
    public boolean resetSeedEachGame = false;

    // Heuristic
    public IStateHeuristic gameHeuristic;

    // Action space type for this player
    public ActionSpace actionSpace = new ActionSpace();
    public IPlayerDecorator decorator = null;

    public PlayerParameters() {
        addTunableParameter("budgetType", PlayerConstants.BUDGET_FM_CALLS, Arrays.asList(PlayerConstants.values()));
        addTunableParameter("budget", 4000, Arrays.asList(100, 300, 1000, 3000, 10000, 30000, 100000));
        addTunableParameter("breakMS", 0);
        addTunableParameter("actionSpaceStructure", ActionSpace.Structure.Default, Arrays.asList(ActionSpace.Structure.values()));
        addTunableParameter("actionSpaceFlexibility", ActionSpace.Flexibility.Default, Arrays.asList(ActionSpace.Flexibility.values()));
        addTunableParameter("actionSpaceContext", ActionSpace.Context.Default, Arrays.asList(ActionSpace.Context.values()));
        addTunableParameter("randomSeed", (int) System.currentTimeMillis());
        addTunableParameter("resetSeedEachGame", false);
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("actionRestriction", IPlayerDecorator.class);
    }

    @Override
    protected PlayerParameters _copy() {
        PlayerParameters params = new PlayerParameters();
        // only need to copy fields that are not Tuned (those are done in the super class)
        params.gameHeuristic = gameHeuristic;
        return params;
    }

    @Override
    public void _reset() {
        setRandomSeed( (int) getParameterValue("randomSeed"));
        budget = (int) getParameterValue("budget");
        resetSeedEachGame = (boolean) getParameterValue("resetSeedEachGame");
        breakMS = (int) getParameterValue("breakMS");
        noiseEpsilon = (double) getParameterValue("epsilon");
        budgetType = (PlayerConstants) getParameterValue("budgetType");
        actionSpace = new ActionSpace ((ActionSpace.Structure) getParameterValue("actionSpaceStructure"),
                                        (ActionSpace.Flexibility) getParameterValue("actionSpaceFlexibility"),
                                        (ActionSpace.Context) getParameterValue("actionSpaceContext"));
        decorator = (IPlayerDecorator) getParameterValue("actionRestriction");
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (o instanceof PlayerParameters that) {
            if (gameHeuristic == null && that.gameHeuristic == null) return true;
            if (gameHeuristic == null || that.gameHeuristic == null) return false;
            return gameHeuristic.equals(that.gameHeuristic);
        }
        return false;
    }

    @Override
    public AbstractPlayer instantiate() {
        throw new RuntimeException("PlayerParameters should not be instantiated directly.");
    }


}
