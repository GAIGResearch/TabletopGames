package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.interfaces.*;
import evaluation.optimisation.TunableParameters;
import org.json.simple.JSONObject;
import players.PlayerParameters;
import players.simple.BoltzmannActionPlayer;
import players.simple.RandomPlayer;
import utilities.JSONUtils;

import java.util.Arrays;
import java.util.Random;

import static players.mcts.MCTSEnums.Information.*;
import static players.mcts.MCTSEnums.MASTType.Rollout;
import static players.mcts.MCTSEnums.OpponentTreePolicy.OneTree;
import static players.mcts.MCTSEnums.RolloutTermination.DEFAULT;
import static players.mcts.MCTSEnums.SelectionPolicy.ROBUST;
import static players.mcts.MCTSEnums.SelectionPolicy.SIMPLE;
import static players.mcts.MCTSEnums.Strategies.*;
import static players.mcts.MCTSEnums.TreePolicy.*;

public class MCTSParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10; // assuming we have a good heuristic
    public int maxTreeDepth = 1000; // effectively no limit
    public MCTSEnums.Information information = Information_Set;  // this should be the default in TAG, given that most games have hidden information
    public MCTSEnums.MASTType MAST = Rollout;
    public boolean useMAST = false;
    public double MASTGamma = 0.5;
    public double MASTBoltzmann = 0.1;
    public double exp3Boltzmann = 0.1;
    public double hedgeBoltzmann = 0.1;
    public MCTSEnums.Strategies expansionPolicy = RANDOM;
    public MCTSEnums.SelectionPolicy selectionPolicy = SIMPLE;  // In general better than ROBUST
    public MCTSEnums.TreePolicy treePolicy = UCB;
    public MCTSEnums.OpponentTreePolicy opponentTreePolicy = OneTree;
    public boolean paranoid = false;
    public MCTSEnums.Strategies rolloutType = RANDOM;
    public MCTSEnums.Strategies oppModelType = MCTSEnums.Strategies.DEFAULT;  // Default is to use the same as rolloutType
    public String rolloutClass, oppModelClass = "";
    public AbstractPlayer rolloutPolicy;
    public ITunableParameters rolloutPolicyParams;
    public AbstractPlayer opponentModel;
    public ITunableParameters opponentModelParams;
    public double exploreEpsilon = 0.1;
    public IActionHeuristic advantageFunction;
    public int biasVisits = 0;
    public int omaVisits = 0;
    public double progressiveWideningConstant = 0.0; //  Zero indicates switched off (well, less than 1.0)
    public double progressiveWideningExponent = 0.0;
    public boolean normaliseRewards = true;
    public boolean nodesStoreScoreDelta = true;
    public boolean maintainMasterState = false;
    public boolean discardStateAfterEachIteration = true;  // default will remove reference to OpenLoopState in backup(). Saves memory!
    public MCTSEnums.RolloutTermination rolloutTermination = DEFAULT;
    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;
    public IActionKey MASTActionKey;
    public IStateKey MCGSStateKey;
    public boolean MCGSExpandAfterClash = true;
    public double MASTDefaultValue = 0.0;
    public double firstPlayUrgency = 1000000000.0;

    public MCTSParams() {
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("MASTBoltzmann", 0.1);
        addTunableParameter("exp3Boltzmann", 0.1);
        addTunableParameter("hedgeBoltzmann", 0.1);
        addTunableParameter("rolloutLength", 10, Arrays.asList(0, 3, 10, 30, 100));
        addTunableParameter("maxTreeDepth", 10, Arrays.asList(1, 3, 10, 30, 100));
        addTunableParameter("rolloutType", RANDOM, Arrays.asList(MCTSEnums.Strategies.values()));
        addTunableParameter("oppModelType", RANDOM, Arrays.asList(MCTSEnums.Strategies.values()));
        addTunableParameter("rolloutClass", "");
        addTunableParameter("oppModelClass", "");
        addTunableParameter("rolloutPolicyParams", ITunableParameters.class);
        addTunableParameter("rolloutTermination", DEFAULT, Arrays.asList(MCTSEnums.RolloutTermination.values()));
        addTunableParameter("opponentModelParams", ITunableParameters.class);
        addTunableParameter("opponentModel", new RandomPlayer());
        addTunableParameter("information", Open_Loop, Arrays.asList(MCTSEnums.Information.values()));
        addTunableParameter("selectionPolicy", ROBUST, Arrays.asList(MCTSEnums.SelectionPolicy.values()));
        addTunableParameter("treePolicy", UCB, Arrays.asList(MCTSEnums.TreePolicy.values()));
        addTunableParameter("opponentTreePolicy", OneTree, Arrays.asList(MCTSEnums.OpponentTreePolicy.values()));
        addTunableParameter("exploreEpsilon", 0.1);
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
        addTunableParameter("opponentHeuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
        addTunableParameter("expansionPolicy", MCTSEnums.Strategies.RANDOM, Arrays.asList(MCTSEnums.Strategies.values()));
        addTunableParameter("MAST", Rollout, Arrays.asList(MCTSEnums.MASTType.values()));
        addTunableParameter("MASTGamma", 0.5, Arrays.asList(0.0, 0.5, 0.9, 1.0));
        addTunableParameter("expertIterationSpecification", "");
        addTunableParameter("advantageFunction", "");
        addTunableParameter("biasVisits", 0, Arrays.asList(0, 1, 3, 10, 30, 100));
        addTunableParameter("progressiveWideningConstant", 0.0, Arrays.asList(0.0, 1.0, 2.0, 4.0, 8.0, 16.0, 32.0));
        addTunableParameter("progressiveWideningExponent", 0.0, Arrays.asList(0.0, 0.1, 0.2, 0.3, 0.5));
        addTunableParameter("normaliseRewards", true);
        addTunableParameter("nodesStoreScoreDelta", true);
        addTunableParameter("maintainMasterState", false);
        addTunableParameter("discardStateAfterEachIteration", true);
        addTunableParameter("advantageFunction", IActionHeuristic.nullReturn);
        addTunableParameter("omaVisits", 0);
        addTunableParameter("paranoid", false);
        addTunableParameter("MASTActionKey", IActionKey.class);
        addTunableParameter("MASTDefaultValue", 0.0);
        addTunableParameter("MCGSStateKey", IStateKey.class);
        addTunableParameter("MCGSExpandAfterClash", true);
        addTunableParameter("FPU", 1000000000.0);
    }

    @Override
    public void _reset() {
        super._reset();
        useMAST = false;
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        rolloutType = (MCTSEnums.Strategies) getParameterValue("rolloutType");
        rolloutTermination = (MCTSEnums.RolloutTermination) getParameterValue("rolloutTermination");
        oppModelType = (MCTSEnums.Strategies) getParameterValue("oppModelType");
        information = (MCTSEnums.Information) getParameterValue("information");
        treePolicy = (MCTSEnums.TreePolicy) getParameterValue("treePolicy");
        selectionPolicy = (MCTSEnums.SelectionPolicy) getParameterValue("selectionPolicy");
        if (selectionPolicy == MCTSEnums.SelectionPolicy.TREE &&
                (treePolicy == UCB || treePolicy == UCB_Tuned || treePolicy == AlphaGo)) {
            // in this case TREE is equivalent to SIMPLE
            selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        }
        expansionPolicy = (MCTSEnums.Strategies) getParameterValue("expansionPolicy");
        opponentTreePolicy = (MCTSEnums.OpponentTreePolicy) getParameterValue("opponentTreePolicy");
        exploreEpsilon = (double) getParameterValue("exploreEpsilon");
        MASTBoltzmann = (double) getParameterValue("MASTBoltzmann");
        MAST = (MCTSEnums.MASTType) getParameterValue("MAST");
        MASTGamma = (double) getParameterValue("MASTGamma");
        exp3Boltzmann = (double) getParameterValue("exp3Boltzmann");
        hedgeBoltzmann = (double) getParameterValue("hedgeBoltzmann");
        rolloutClass = (String) getParameterValue("rolloutClass");
        oppModelClass = (String) getParameterValue("oppModelClass");

        // We will then instantiate the Expert Iteration data once it is needed
        biasVisits = (int) getParameterValue("biasVisits");
        omaVisits = (int) getParameterValue("omaVisits");
        progressiveWideningConstant = (double) getParameterValue("progressiveWideningConstant");
        progressiveWideningExponent = (double) getParameterValue("progressiveWideningExponent");
        normaliseRewards = (boolean) getParameterValue("normaliseRewards");
        nodesStoreScoreDelta = (boolean) getParameterValue("nodesStoreScoreDelta");
        maintainMasterState = (boolean) getParameterValue("maintainMasterState");
        paranoid = (boolean) getParameterValue("paranoid");
        discardStateAfterEachIteration = (boolean) getParameterValue("discardStateAfterEachIteration");
        if (information == Closed_Loop)
            discardStateAfterEachIteration = false;
        if (expansionPolicy == MCTSEnums.Strategies.MAST || rolloutType == MCTSEnums.Strategies.MAST
                || (biasVisits > 0 && advantageFunction == null)) {
            useMAST = true;
        }
        MASTActionKey = (IActionKey) getParameterValue("MASTActionKey");
        MASTDefaultValue = (double) getParameterValue("MASTDefaultValue");

        advantageFunction = (IActionHeuristic) getParameterValue("advantageFunction");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        MCGSStateKey = (IStateKey) getParameterValue("MCGSStateKey");
        MCGSExpandAfterClash = (boolean) getParameterValue("MCGSExpandAfterClash");
        rolloutPolicyParams = (TunableParameters) getParameterValue("rolloutPolicyParams");
        opponentModelParams = (TunableParameters) getParameterValue("opponentModelParams");
        // we then null those elements of params which are constructed (lazily) from the above
        firstPlayUrgency = (double) getParameterValue("FPU");
        opponentModel = null;
        rolloutPolicy = null;
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
        switch (type) {
            case RANDOM:
                return new RandomPlayer(new Random(getRandomSeed()));
            case MAST:
                return new MASTPlayer(MASTActionKey, MASTBoltzmann, 0.0, getRandomSeed(), MASTDefaultValue);
            case CLASS:
                // we have a bespoke Class to instantiate
                return JSONUtils.loadClassFromString(details);
            case PARAMS:
                throw new AssertionError("PolicyParameters have not been set");
            default:
                throw new AssertionError("Unknown strategy type : " + type);
        }
    }

    public IStateHeuristic getHeuristic() {
        return heuristic;
    }

    @Override
    public MCTSPlayer instantiate() {
        return new MCTSPlayer((MCTSParams) this.copy());
    }

}
