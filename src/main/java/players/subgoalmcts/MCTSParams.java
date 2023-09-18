package players.subgoalmcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.interfaces.IActionHeuristic;
import core.interfaces.IActionKey;
import core.interfaces.IStateHeuristic;
import core.interfaces.ITunableParameters;
import evaluation.optimisation.TunableParameters;
import players.PlayerParameters;
import players.simple.RandomPlayer;

import java.util.Arrays;

public class MCTSParams extends PlayerParameters {

    public enum BackUpPolicy {
        SUBGOAL_PARENT, NATURAL_PARENT, BOTH
    }

    public enum RecommendationPolicy {
        SUBGOALS, STANDARD
    }

    public double K = Math.sqrt(2);
    public int rolloutLength = 10; // assuming we have a good heuristic
    public int maxTreeDepth = 1000; // effectively no limit
    public double epsilon = 1e-6;
    public boolean useMAST = false;
    public double MASTGamma = 0.5;
    public double MASTBoltzmann = 0.1;
    public double exp3Boltzmann = 0.1;
    public double hedgeBoltzmann = 0.1;
    public boolean paranoid = false;
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
    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;
    public IActionKey MASTActionKey;
    public double MASTDefaultValue = 0.0;
    public BackUpPolicy backUpPolicy = BackUpPolicy.NATURAL_PARENT;
    public RecommendationPolicy recommendationPolicy = RecommendationPolicy.STANDARD;

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
        addTunableParameter("rolloutClass", "");
        addTunableParameter("oppModelClass", "");
        addTunableParameter("rolloutPolicyParams", ITunableParameters.class);
        addTunableParameter("opponentModelParams", ITunableParameters.class);
        addTunableParameter("opponentModel", new RandomPlayer());
        addTunableParameter("exploreEpsilon", 0.1);
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
        addTunableParameter("opponentHeuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
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
        addTunableParameter("backupPolicy", BackUpPolicy.NATURAL_PARENT);
        addTunableParameter("recommendationPolicy", RecommendationPolicy.STANDARD);
    }

    @Override
    public void _reset() {
        super._reset();
        useMAST = false;
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        epsilon = (double) getParameterValue("epsilon");
        exploreEpsilon = (double) getParameterValue("exploreEpsilon");
        MASTBoltzmann = (double) getParameterValue("MASTBoltzmann");
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
        MASTActionKey = (IActionKey) getParameterValue("MASTActionKey");
        MASTDefaultValue = (double) getParameterValue("MASTDefaultValue");

        advantageFunction = (IActionHeuristic) getParameterValue("advantageFunction");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        rolloutPolicyParams = (TunableParameters) getParameterValue("rolloutPolicyParams");
        opponentModelParams = (TunableParameters) getParameterValue("opponentModelParams");

        backUpPolicy = (BackUpPolicy) getParameterValue("backupPolicy");
        recommendationPolicy = (RecommendationPolicy) getParameterValue("recommendationPolicy");

    }

    @Override
    protected MCTSParams _copy() {
        // All the copying is done in TunableParameters.copy()
        // Note that any *local* changes of parameters will not be copied
        // unless they have been 'registered' with setParameterValue("name", value)
        return new MCTSParams(System.currentTimeMillis());
    }

    public IStateHeuristic getHeuristic() {
        return heuristic;
    }

    @Override
    public BasicMCTSPlayer instantiate() {
        return new BasicMCTSPlayer((MCTSParams) this.copy());
    }

}
