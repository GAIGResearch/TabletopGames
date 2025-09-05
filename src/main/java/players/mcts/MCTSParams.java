package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.interfaces.*;
import evaluation.optimisation.TunableParameters;
import org.jetbrains.annotations.NotNull;
import players.PlayerParameters;
import players.simple.RandomPlayer;
import utilities.JSONUtils;

import java.util.Arrays;
import java.util.Random;

import static players.mcts.MCTSEnums.Information.*;
import static players.mcts.MCTSEnums.MASTType.*;
import static players.mcts.MCTSEnums.OpponentTreePolicy.OneTree;
import static players.mcts.MCTSEnums.RolloutIncrement.*;
import static players.mcts.MCTSEnums.RolloutTermination.EXACT;
import static players.mcts.MCTSEnums.SelectionPolicy.SIMPLE;
import static players.mcts.MCTSEnums.Strategies.*;
import static players.mcts.MCTSEnums.TreePolicy.*;

public class MCTSParams extends PlayerParameters {

    public double K = 1.0;
    public int rolloutLength = 1000; // effectively to end of game
    public boolean rolloutLengthPerPlayer = false;  // if true, then rolloutLength is multiplied by the number of players
    public int maxTreeDepth = 1000; // effectively no limit
    public MCTSEnums.Information information = Information_Set;  // this should be the default in TAG, given that most games have hidden information
    public MCTSEnums.MASTType MAST = None;
    public boolean useMAST = false;
    public double MASTGamma = 0.0;
    public double MASTDefaultValue = 0.0;
    public double MASTBoltzmann = 0.1;
    public double exp3Boltzmann = 0.1;
    public boolean useMASTAsActionHeuristic = false;
    public MCTSEnums.SelectionPolicy selectionPolicy = SIMPLE;  // In general better than ROBUST
    public MCTSEnums.TreePolicy treePolicy = UCB;
    public MCTSEnums.OpponentTreePolicy opponentTreePolicy = OneTree;
    public boolean paranoid = false;
    public MCTSEnums.RolloutIncrement rolloutIncrementType = TICK;
    public MCTSEnums.Strategies rolloutType = RANDOM;
    public MCTSEnums.Strategies oppModelType = MCTSEnums.Strategies.DEFAULT;  // Default is to use the same as rolloutType
    public String rolloutClass, oppModelClass = "";
    public AbstractPlayer rolloutPolicy;
    public ITunableParameters rolloutPolicyParams;
    public AbstractPlayer opponentModel;
    public ITunableParameters opponentModelParams;
    public double exploreEpsilon = 0.1;
    public int omaVisits = 30;
    public boolean normaliseRewards = true;  // This will automatically normalise rewards to be in the range [0,1]
    // so that K does not need to be tuned to the precise scale of reward in a game
    // It also means that at the end of the game (when rewards are possibly closer to each other, they are still scaled to [0, 1]
    public boolean maintainMasterState = false;
    public boolean discardStateAfterEachIteration = true;  // default will remove reference to OpenLoopState in backup(). Saves memory!
    public MCTSEnums.RolloutTermination rolloutTermination = EXACT;
    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;
    public IActionKey MASTActionKey;
    public IStateKey MCGSStateKey;
    public boolean MCGSExpandAfterClash = true;
    public double firstPlayUrgency = 1e6;
    @NotNull public IActionHeuristic actionHeuristic = IActionHeuristic.nullReturn;
    public int actionHeuristicRecalculationThreshold = 20;
    public boolean pUCT = false;  // in this case we multiply the exploration value in UCB by the probability that the action heuristic would take the action
    public double pUCTTemperature = 0.0;  // If greater than zero we construct a Boltzmann distribution over actions based on the action heuristic
    // if zero (or less) then we use the action heuristic values directly, setting any negative values to zero)
    public int initialiseVisits = 0;  // This is the number of visits to initialise the MCTS tree with (using the actionHeuristic)
    public double progressiveWideningConstant = 0.0; //  Zero indicates switched off (well, less than 1.0)
    public double progressiveWideningExponent = 0.0;
    public double progressiveBias = 0.0;
    public boolean reuseTree = false;
    public MCTSEnums.BackupPolicy backupPolicy = MCTSEnums.BackupPolicy.MonteCarlo;
    public double backupLambda = 1.0;
    public int maxBackupThreshold = 1000000;
    public Class<?> instantiationClass;

    public MCTSParams() {
        addTunableParameter("K", 1.0, Arrays.asList(0.03, 0.1, 0.3, 1.0, 3.0, 10.0, 30.0, 100.0));
        addTunableParameter("MASTBoltzmann", 0.1);
        addTunableParameter("exp3Boltzmann", 0.1);
        addTunableParameter("rolloutLength", 1000, Arrays.asList(0, 3, 10, 30, 100, 1000));
        addTunableParameter("rolloutLengthPerPlayer", false);
        addTunableParameter("maxTreeDepth", 1000, Arrays.asList(1, 3, 10, 30, 100, 1000));
        addTunableParameter("rolloutIncrementType", TICK, Arrays.asList(MCTSEnums.RolloutIncrement.values()));
        addTunableParameter("rolloutType", RANDOM, Arrays.asList(MCTSEnums.Strategies.values()));
        addTunableParameter("oppModelType", DEFAULT, Arrays.asList(MCTSEnums.Strategies.values()));
        addTunableParameter("rolloutClass", "");
        addTunableParameter("oppModelClass", "");
        addTunableParameter("rolloutPolicyParams", ITunableParameters.class);
        addTunableParameter("rolloutTermination", EXACT, Arrays.asList(MCTSEnums.RolloutTermination.values()));
        addTunableParameter("opponentModelParams", ITunableParameters.class);
        addTunableParameter("opponentModel", new RandomPlayer());
        addTunableParameter("information", Information_Set, Arrays.asList(MCTSEnums.Information.values()));
        addTunableParameter("selectionPolicy", SIMPLE, Arrays.asList(MCTSEnums.SelectionPolicy.values()));
        addTunableParameter("treePolicy", UCB, Arrays.asList(MCTSEnums.TreePolicy.values()));
        addTunableParameter("opponentTreePolicy", OneTree, Arrays.asList(MCTSEnums.OpponentTreePolicy.values()));
        addTunableParameter("exploreEpsilon", 0.1);
        addTunableParameter("heuristic", IStateHeuristic.class, AbstractGameState::getHeuristicScore);
        addTunableParameter("MAST", None, Arrays.asList(MCTSEnums.MASTType.values()));
        addTunableParameter("MASTGamma", 0.0, Arrays.asList(0.0, 0.5, 0.9, 1.0));
        addTunableParameter("useMASTAsActionHeuristic", false);
        addTunableParameter("progressiveWideningConstant", 0.0, Arrays.asList(0.0, 1.0, 2.0, 4.0, 8.0, 16.0, 32.0));
        addTunableParameter("progressiveWideningExponent", 0.0, Arrays.asList(0.0, 0.1, 0.2, 0.3, 0.5));
        addTunableParameter("normaliseRewards", true);
        addTunableParameter("maintainMasterState", false);
        addTunableParameter("discardStateAfterEachIteration", true);
        addTunableParameter("omaVisits", 30);
        addTunableParameter("paranoid", false);
        addTunableParameter("MASTActionKey", IActionKey.class);
        addTunableParameter("MASTDefaultValue", 0.0);
        addTunableParameter("MCGSStateKey", IStateKey.class);
        addTunableParameter("MCGSExpandAfterClash", true);
        addTunableParameter("FPU", 1e6);
        addTunableParameter("actionHeuristic", IActionHeuristic.class,  IActionHeuristic.nullReturn);
        addTunableParameter("progressiveBias", 0.0);
        addTunableParameter("pUCT", false);
        addTunableParameter("pUCTTemperature", 0.0);
        addTunableParameter("initialiseVisits", 0);
        addTunableParameter("actionHeuristicRecalculation", 20);
        addTunableParameter("reuseTree", false);
        addTunableParameter("backupPolicy", MCTSEnums.BackupPolicy.MonteCarlo, Arrays.asList(MCTSEnums.BackupPolicy.values()));
        addTunableParameter("backupLambda", 1.0);
        addTunableParameter("maxBackupThreshold", 1000000);
        addTunableParameter("instantiationClass", "players.mcts.MCTSPlayer");
    }

    @Override
    public void _reset() {
        super._reset();
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        rolloutLengthPerPlayer = (boolean) getParameterValue("rolloutLengthPerPlayer");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        rolloutIncrementType = (MCTSEnums.RolloutIncrement) getParameterValue("rolloutIncrementType");
        rolloutType = (MCTSEnums.Strategies) getParameterValue("rolloutType");
        rolloutTermination = (MCTSEnums.RolloutTermination) getParameterValue("rolloutTermination");
        oppModelType = (MCTSEnums.Strategies) getParameterValue("oppModelType");
        information = (MCTSEnums.Information) getParameterValue("information");
        treePolicy = (MCTSEnums.TreePolicy) getParameterValue("treePolicy");
        selectionPolicy = (MCTSEnums.SelectionPolicy) getParameterValue("selectionPolicy");
        opponentTreePolicy = (MCTSEnums.OpponentTreePolicy) getParameterValue("opponentTreePolicy");
        exploreEpsilon = (double) getParameterValue("exploreEpsilon");
        MASTBoltzmann = (double) getParameterValue("MASTBoltzmann");
        MAST = (MCTSEnums.MASTType) getParameterValue("MAST");
        MASTGamma = (double) getParameterValue("MASTGamma");
        exp3Boltzmann = (double) getParameterValue("exp3Boltzmann");
        rolloutClass = (String) getParameterValue("rolloutClass");
        oppModelClass = (String) getParameterValue("oppModelClass");

        progressiveBias = (double) getParameterValue("progressiveBias");
        omaVisits = (int) getParameterValue("omaVisits");
        progressiveWideningConstant = (double) getParameterValue("progressiveWideningConstant");
        progressiveWideningExponent = (double) getParameterValue("progressiveWideningExponent");
        normaliseRewards = (boolean) getParameterValue("normaliseRewards");
        maintainMasterState = (boolean) getParameterValue("maintainMasterState");
        paranoid = (boolean) getParameterValue("paranoid");
        discardStateAfterEachIteration = (boolean) getParameterValue("discardStateAfterEachIteration");
        pUCT = (boolean) getParameterValue("pUCT");
        pUCTTemperature = (double) getParameterValue("pUCTTemperature");
        if (information == Closed_Loop)
            discardStateAfterEachIteration = false;

        MASTActionKey = (IActionKey) getParameterValue("MASTActionKey");
        MASTDefaultValue = (double) getParameterValue("MASTDefaultValue");

        actionHeuristic = (IActionHeuristic) getParameterValue("actionHeuristic");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        MCGSStateKey = (IStateKey) getParameterValue("MCGSStateKey");
        MCGSExpandAfterClash = (boolean) getParameterValue("MCGSExpandAfterClash");
        rolloutPolicyParams = (TunableParameters) getParameterValue("rolloutPolicyParams");
        opponentModelParams = (TunableParameters) getParameterValue("opponentModelParams");
        // we then null those elements of params which are constructed (lazily) from the above
        firstPlayUrgency = (double) getParameterValue("FPU");
        initialiseVisits = (int) getParameterValue("initialiseVisits");
        actionHeuristicRecalculationThreshold = (int) getParameterValue("actionHeuristicRecalculation");
        reuseTree = (boolean) getParameterValue("reuseTree");
        backupPolicy = (MCTSEnums.BackupPolicy) getParameterValue("backupPolicy");
        backupLambda = (double) getParameterValue("backupLambda");
        maxBackupThreshold = (int) getParameterValue("maxBackupThreshold");
        try {
            instantiationClass = Class.forName((String) getParameterValue("instantiationClass"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        opponentModel = null;
        rolloutPolicy = null;
        useMASTAsActionHeuristic = (boolean) getParameterValue("useMASTAsActionHeuristic");
        useMAST = MAST != None;
        // If we are using MAST for rollout or action heuristic, then we need to collect the data
        if (!useMAST && (rolloutType == MCTSEnums.Strategies.MAST ||
                oppModelType == MCTSEnums.Strategies.MAST ||
                useMASTAsActionHeuristic)) {
            System.out.println("Setting MAST to Both instead of None given use of MAST in rollout or action heuristic");
            useMAST = true;
            MAST = Both;
        }
    }

    @Override
    protected MCTSParams _copy() {
        // All the copying is done in TunableParameters.copy()
        // Note that any *local* changes of parameters will not be copied
        // unless they have been 'registered' with setParameterValue("name", value)
        return new MCTSParams();
    }

    public AbstractPlayer getOpponentModel() {
        if (opponentModel == null) {
            if (oppModelType == PARAMS)
                opponentModel = (AbstractPlayer) opponentModelParams.instantiate();
            else if (oppModelType == MCTSEnums.Strategies.DEFAULT)
                opponentModel = getRolloutStrategy();
            else
                opponentModel = constructStrategy(oppModelType, oppModelClass);
            opponentModel.getParameters().actionSpace = actionSpace;  // TODO makes sense?
        }
        return opponentModel;
    }

    public AbstractPlayer getRolloutStrategy() {
        if (rolloutPolicy == null) {
            if (rolloutType == PARAMS)
                rolloutPolicy = (AbstractPlayer) rolloutPolicyParams.instantiate();
            else
                rolloutPolicy = constructStrategy(rolloutType, rolloutClass);
            rolloutPolicy.getParameters().actionSpace = actionSpace;  // TODO makes sense?
        }
        return rolloutPolicy;
    }

    private AbstractPlayer constructStrategy(MCTSEnums.Strategies type, String details) {
        return switch (type) {
            case RANDOM -> new RandomPlayer(new Random(getRandomSeed()));
            case MAST -> new MASTPlayer(MASTActionKey, MASTBoltzmann, 0.0, getRandomSeed(), MASTDefaultValue);
            case CLASS ->
                // we have a bespoke Class to instantiate (for anything other than an AbstractPlayer we could just rely on the core JSON loading)
                    JSONUtils.loadClass(details);
            case PARAMS -> throw new AssertionError("PolicyParameters have not been set");
            default -> throw new AssertionError("Unknown strategy type : " + type);
        };
    }

    @Override
    public IStateHeuristic getStateHeuristic() {
        return heuristic;
    }

    @Override
    public MCTSPlayer instantiate() {
        if (!useMAST && (useMASTAsActionHeuristic || rolloutType == MCTSEnums.Strategies.MAST)) {
            throw new AssertionError("MAST data not being collected, but MAST is being used as the rollout policy or as the action heuristic. Set MAST parameter.");
        }
        if (instantiationClass == null || instantiationClass == MCTSPlayer.class)
            return new MCTSPlayer((MCTSParams) this.copy());
        else {
            // the instantiation Class should implement a constructor that takes a MCTSParams object
            // which we find and then instantiate
            try {
                return (MCTSPlayer) instantiationClass.getConstructor(MCTSParams.class).newInstance(this);
            } catch (Exception e) {
                throw new AssertionError("Could not instantiate class : " + instantiationClass.getName());
            }
        }
    }

}
