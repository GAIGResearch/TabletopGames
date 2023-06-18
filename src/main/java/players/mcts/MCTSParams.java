package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.interfaces.*;
import evaluation.TunableParameters;
import org.json.simple.JSONObject;
import players.PlayerParameters;
import players.simple.BoltzmannActionPlayer;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.Arrays;
import java.util.Random;

import static players.mcts.MCTSEnums.Information.*;
import static players.mcts.MCTSEnums.MASTType.Rollout;
import static players.mcts.MCTSEnums.OpponentTreePolicy.OneTree;
import static players.mcts.MCTSEnums.RolloutTermination.DEFAULT;
import static players.mcts.MCTSEnums.SelectionPolicy.ROBUST;
import static players.mcts.MCTSEnums.Strategies.*;
import static players.mcts.MCTSEnums.TreePolicy.*;

public class MCTSParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10;
    public int maxTreeDepth = 10;
    public double epsilon = 1e-6;
    public MCTSEnums.Information information = Open_Loop;
    public MCTSEnums.MASTType MAST = Rollout;
    public boolean useMAST = false;
    public double MASTGamma = 0.5;
    public double MASTBoltzmann = 0.1;
    public double exp3Boltzmann = 0.1;
    public double hedgeBoltzmann = 0.1;
    public MCTSEnums.Strategies expansionPolicy = RANDOM;
    public MCTSEnums.SelectionPolicy selectionPolicy = ROBUST;
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
    public double MASTDefaultValue = 0.0;

    public MCTSParams() {
        this(System.currentTimeMillis());
    }

    public MCTSParams(long seed) {
        super(seed);
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("MASTBoltzmann", 0.1);
        addTunableParameter("exp3Boltzmann", 0.1);
        addTunableParameter("hedgeBoltzmann", 0.1);
        addTunableParameter("rolloutLength", 10, Arrays.asList(0, 3, 10, 30, 100));
        addTunableParameter("maxTreeDepth", 10, Arrays.asList(1, 3, 10, 30, 100));
        addTunableParameter("epsilon", 1e-6);
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
        if (advantageFunction instanceof TunableParameters) {
            TunableParameters tunableHeuristic = (TunableParameters) advantageFunction;
            for (String name : tunableHeuristic.getParameterNames()) {
                tunableHeuristic.setParameterValue(name, this.getParameterValue("advantageFunction." + name));
            }
        }
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        if (heuristic instanceof TunableParameters) {
            TunableParameters tunableHeuristic = (TunableParameters) heuristic;
            for (String name : tunableHeuristic.getParameterNames()) {
                tunableHeuristic.setParameterValue(name, this.getParameterValue("heuristic." + name));
            }
        }

        rolloutPolicyParams = (TunableParameters) getParameterValue("rolloutPolicyParams");
        if (rolloutPolicyParams != null)
            for (String name : rolloutPolicyParams.getParameterNames())
                rolloutPolicyParams.setParameterValue(name, this.getParameterValue("rolloutPolicyParams." + name))
                        ;
        opponentModelParams = (TunableParameters) getParameterValue("opponentModelParams");

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
    public Object registerChild(String nameSpace, JSONObject json) {
        Object child = super.registerChild(nameSpace, json);
        switch (nameSpace) {
            case "heuristic":
                heuristic = (IStateHeuristic) child;
                setParameterValue("heuristic", child);
                break;
            case "rolloutPolicyParams":
                setParameterValue("rolloutPolicyParams", child);
                break;
            case "opponentModelParams":
                setParameterValue("opponentModelParams", child);
                break;
            case "advantageFunction":
                setParameterValue("advantageFunction", child);
                break;
            default:
                setParameterValue(nameSpace, child);
             //   throw new AssertionError("Unknown child in TunableParameters: " + nameSpace);
        }
        return child;
    }

    @Override
    protected MCTSParams _copy() {
        MCTSParams retValue = new MCTSParams(System.currentTimeMillis());
        retValue.K = K;
        retValue.rolloutLength = rolloutLength;
        retValue.maxTreeDepth = maxTreeDepth;
        retValue.epsilon = epsilon;
        retValue.information = information;
        retValue.MAST = MAST;
        retValue.useMAST = useMAST;
        retValue.MASTGamma = MASTGamma;
        retValue.MASTBoltzmann = MASTBoltzmann;
        retValue.exp3Boltzmann = exp3Boltzmann;
        retValue.hedgeBoltzmann = hedgeBoltzmann;
        retValue.expansionPolicy = expansionPolicy;
        retValue.selectionPolicy = selectionPolicy;
        retValue.treePolicy = treePolicy;
        retValue.opponentTreePolicy = opponentTreePolicy;
        retValue.rolloutType = rolloutType;
        retValue.oppModelType = oppModelType;
        retValue.rolloutClass = rolloutClass;
        retValue.oppModelClass = oppModelClass;
        retValue.rolloutPolicy = rolloutPolicy == null ? null : rolloutPolicy.copy();
        retValue.rolloutPolicyParams = rolloutPolicyParams;
        retValue.opponentModel = opponentModel == null ? null : opponentModel.copy();
        retValue.opponentModelParams = opponentModelParams;
        retValue.exploreEpsilon = exploreEpsilon;
        retValue.advantageFunction = advantageFunction;
        retValue.biasVisits = biasVisits;
        retValue.omaVisits = omaVisits;
        retValue.progressiveWideningConstant = progressiveWideningConstant;
        retValue.progressiveWideningExponent = progressiveWideningExponent;
        retValue.normaliseRewards = normaliseRewards;
        retValue.nodesStoreScoreDelta = nodesStoreScoreDelta;
        retValue.maintainMasterState = maintainMasterState;
        retValue.rolloutTermination = rolloutTermination;
        retValue.heuristic = heuristic;
        retValue.discardStateAfterEachIteration = discardStateAfterEachIteration;
        retValue.paranoid = paranoid;
        retValue.MASTActionKey = MASTActionKey;
        return retValue;
    }


    public AbstractPlayer getOpponentModel() {
        if (opponentModel != null)
            return opponentModel;
        if (oppModelType == PARAMS)
            return (AbstractPlayer) opponentModelParams.instantiate();
        if (oppModelType == MCTSEnums.Strategies.DEFAULT)
            return getRolloutStrategy();
        return constructStrategy(oppModelType, oppModelClass);
    }

    public AbstractPlayer getRolloutStrategy() {
        if (rolloutPolicy != null)
            return rolloutPolicy;
        if (rolloutType == PARAMS)
            return (AbstractPlayer) rolloutPolicyParams.instantiate();
        return constructStrategy(rolloutType, rolloutClass);
    }

    private AbstractPlayer constructStrategy(MCTSEnums.Strategies type, String details) {
        switch (type) {
            case RANDOM:
                return new RandomPlayer(new Random(getRandomSeed()));
            case MAST:
                return new MASTPlayer(MASTActionKey, MASTBoltzmann, 0.0, System.currentTimeMillis(), MASTDefaultValue);
            case CLASS:
                // we have a bespoke Class to instantiate
                return Utils.loadClassFromString(details);
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
        return new MCTSPlayer(this);
    }

}
