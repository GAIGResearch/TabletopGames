package players.mcts;

import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.*;
import evaluation.TunableParameters;
import org.json.simple.JSONObject;
import players.PlayerParameters;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.Arrays;
import java.util.Random;
import java.util.function.ToDoubleBiFunction;

import static players.mcts.MCTSEnums.Information.Open_Loop;
import static players.mcts.MCTSEnums.MASTType.Rollout;
import static players.mcts.MCTSEnums.OpponentTreePolicy.MaxN;
import static players.mcts.MCTSEnums.OpponentTreePolicy.Paranoid;
import static players.mcts.MCTSEnums.RolloutTermination.DEFAULT;
import static players.mcts.MCTSEnums.SelectionPolicy.ROBUST;
import static players.mcts.MCTSEnums.Strategies.PARAMS;
import static players.mcts.MCTSEnums.Strategies.RANDOM;
import static players.mcts.MCTSEnums.TreePolicy.UCB;

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
    public MCTSEnums.Strategies expansionPolicy = RANDOM;
    public MCTSEnums.SelectionPolicy selectionPolicy = ROBUST;
    public MCTSEnums.TreePolicy treePolicy = UCB;
    public MCTSEnums.OpponentTreePolicy opponentTreePolicy = Paranoid;
    public MCTSEnums.Strategies rolloutType = RANDOM;
    public MCTSEnums.Strategies oppModelType = RANDOM;
    public String rolloutClass, oppModelClass = "";
    public AbstractPlayer rolloutPolicy;
    public ITunableParameters rolloutPolicyParams;
    public AbstractPlayer opponentModel;
    public ITunableParameters opponentModelParams;
    public double exploreEpsilon = 0.1;
    public boolean gatherExpertIterationData = false;
    public String expertIterationFileStem = "ExpertIterationData";
    public String expertIterationStateFeatures = "";
    public IStateFeatureVector EIStateFeatureVector;
    public String expertIterationActionFeatures = "";
    public IActionFeatureVector EIActionFeatureVector;
    public IActionHeuristic advantageFunction;
    public int biasVisits = 0;
    public int omaVisits = 0;
    public double progressiveWideningConstant = 0.0; //  Zero indicates switched off (well, less than 1.0)
    public double progressiveWideningExponent = 0.0;
    public boolean normaliseRewards = true;
    public boolean nodesStoreScoreDelta = true;
    public boolean maintainMasterState = false;
    public MCTSEnums.RolloutTermination rolloutTermination = DEFAULT;
    private IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;
    private IStateHeuristic opponentHeuristic = AbstractGameState::getHeuristicScore;

    public MCTSParams() {
        this(System.currentTimeMillis());
    }

    public MCTSParams(long seed) {
        super(seed);
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("boltzmannTemp", 0.1);
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
        addTunableParameter("opponentTreePolicy", MaxN, Arrays.asList(MCTSEnums.OpponentTreePolicy.values()));
        addTunableParameter("exploreEpsilon", 0.1);
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
        addTunableParameter("opponentHeuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
        addTunableParameter("expansionPolicy", MCTSEnums.Strategies.RANDOM, Arrays.asList(MCTSEnums.Strategies.values()));
        addTunableParameter("MAST", Rollout, Arrays.asList(MCTSEnums.MASTType.values()));
        addTunableParameter("MASTGamma", 0.5, Arrays.asList(0.0, 0.5, 0.9, 1.0));
        addTunableParameter("expertIteration", false);
        addTunableParameter("expIterFile", "");
        addTunableParameter("expertIterationStateFeatures", "");
        addTunableParameter("expertIterationActionFeatures", "");
        addTunableParameter("advantageFunction", "");
        addTunableParameter("biasVisits", 0, Arrays.asList(0, 1, 3, 10, 30, 100));
        addTunableParameter("progressiveWideningConstant", 0.0, Arrays.asList(0.0, 1.0, 2.0, 4.0, 8.0, 16.0, 32.0));
        addTunableParameter("progressiveWideningExponent", 0.0, Arrays.asList(0.0, 0.1, 0.2, 0.3, 0.5));
        addTunableParameter("normaliseRewards", true);
        addTunableParameter("nodesStoreScoreDelta", false);
        addTunableParameter("maintainMasterState", false);
        addTunableParameter("advantageFunction", IActionHeuristic.nullReturn);
        addTunableParameter("omaVisits", 0);
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
        selectionPolicy = (MCTSEnums.SelectionPolicy) getParameterValue("selectionPolicy");
        expansionPolicy = (MCTSEnums.Strategies) getParameterValue("expansionPolicy");
        treePolicy = (MCTSEnums.TreePolicy) getParameterValue("treePolicy");
        opponentTreePolicy = (MCTSEnums.OpponentTreePolicy) getParameterValue("opponentTreePolicy");
        exploreEpsilon = (double) getParameterValue("exploreEpsilon");
        MASTBoltzmann = (double) getParameterValue("boltzmannTemp");
        MAST = (MCTSEnums.MASTType) getParameterValue("MAST");
        MASTGamma = (double) getParameterValue("MASTGamma");
        rolloutClass = (String) getParameterValue("rolloutClass");
        oppModelClass = (String) getParameterValue("oppModelClass");
        gatherExpertIterationData = (boolean) getParameterValue("expertIteration");
        expertIterationFileStem = (String) getParameterValue("expIterFile");
        expertIterationStateFeatures = (String) getParameterValue("expertIterationStateFeatures");
        if (!expertIterationStateFeatures.equals(""))
            try {
                EIStateFeatureVector = (IStateFeatureVector) Class.forName(expertIterationStateFeatures).getConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        expertIterationActionFeatures = (String) getParameterValue("expertIterationActionFeatures");
        if (!expertIterationActionFeatures.equals(""))
            try {
                EIActionFeatureVector = (IActionFeatureVector) Class.forName(expertIterationActionFeatures).getConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        advantageFunction = (IActionHeuristic) getParameterValue("advantageFunction");
        biasVisits = (int) getParameterValue("biasVisits");
        omaVisits = (int) getParameterValue("omaVisits");
        progressiveWideningConstant = (double) getParameterValue("progressiveWideningConstant");
        progressiveWideningExponent = (double) getParameterValue("progressiveWideningExponent");
        normaliseRewards = (boolean) getParameterValue("normaliseRewards");
        nodesStoreScoreDelta = (boolean) getParameterValue("nodesStoreScoreDelta");
        maintainMasterState = (boolean) getParameterValue("maintainMasterState");
        if (expansionPolicy == MCTSEnums.Strategies.MAST || rolloutType == MCTSEnums.Strategies.MAST
                || (biasVisits > 0 && advantageFunction == null)) {
            useMAST = true;
        }
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        if (heuristic instanceof TunableParameters) {
            TunableParameters tunableHeuristic = (TunableParameters) heuristic;
            for (String name : tunableHeuristic.getParameterNames()) {
                tunableHeuristic.setParameterValue(name, this.getParameterValue("heuristic." + name));
            }
        }
        // TODO: opponentHeuristic is not currently used
        opponentHeuristic = (IStateHeuristic) getParameterValue("opponentHeuristic");
        if (opponentHeuristic instanceof TunableParameters) {
            TunableParameters tunableHeuristic = (TunableParameters) opponentHeuristic;
            for (String name : tunableHeuristic.getParameterNames()) {
                tunableHeuristic.setParameterValue(name, this.getParameterValue("opponentHeuristic." + name));
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
            case "opponentHeuristic":
                opponentHeuristic = (IStateHeuristic) child;
                setParameterValue("opponentHeuristic", child);
                break;
            case "rolloutPolicyParams":
                setParameterValue("rolloutPolicyParams", child);
                break;
            case "opponentModelParams":
                setParameterValue("opponentModelParams", child);
                break;
            default:
                throw new AssertionError("Unknown child in TunableParameters: " + nameSpace);
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
        retValue.gatherExpertIterationData = gatherExpertIterationData;
        retValue.expertIterationFileStem = expertIterationFileStem;
        retValue.expertIterationStateFeatures = expertIterationStateFeatures;
        retValue.EIStateFeatureVector = EIStateFeatureVector;
        retValue.expertIterationActionFeatures = expertIterationActionFeatures;
        retValue.EIActionFeatureVector = EIActionFeatureVector;
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
        retValue.opponentHeuristic = opponentHeuristic;
        return retValue;
    }


    public AbstractPlayer getOpponentModel() {
        if (opponentModel != null)
            return opponentModel;
        if (oppModelType == PARAMS)
            return (AbstractPlayer) opponentModelParams.instantiate();
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
                return new MASTPlayer(new Random(getRandomSeed()));
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

    public IStateHeuristic getOpponentHeuristic() {
        return opponentHeuristic;
    }

    @Override
    public MCTSPlayer instantiate() {
        return new MCTSPlayer(this);
    }

}
