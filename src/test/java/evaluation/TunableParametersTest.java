package evaluation;

import core.AbstractPlayer;
import evaluation.optimisation.ITPSearchSpace;
import games.puertorico.PuertoRicoActionHeuristic001;
import org.apache.hadoop.shaded.org.eclipse.jetty.util.ajax.JSON;
import org.apache.spark.sql.catalyst.expressions.Abs$;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import players.heuristics.CoarseTunableHeuristic;
import players.heuristics.OrdinalPosition;
import players.mcts.MCTSEnums;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.simple.BoltzmannActionParams;
import players.simple.BoltzmannActionPlayer;
import players.simple.RandomPlayer;
import utilities.JSONUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static players.PlayerConstants.BUDGET_TIME;
import static players.heuristics.CoarseTunableHeuristic.HeuristicType.*;

public class TunableParametersTest {

    MCTSParams params;
    BoltzmannActionParams bap;

    @Before
    public void setup() {
        params = new MCTSParams();
        bap = new BoltzmannActionParams();
        bap.setParameterValue("temperature", 0.56);
        bap.setParameterValue("epsilon", 0.27);
    }

    @Test
    public void setParameterValueUpdatesLocalValue() {
        params.setParameterValue("rolloutLength", 123);
        assertEquals(123, params.rolloutLength);
        assertEquals(123, params.getParameterValue("rolloutLength"));
    }

    @Test
    public void subParamValuesAreVisibleAtTopLevel() {
        assertTrue(params.getRolloutStrategy() instanceof RandomPlayer);
        params.setParameterValue("rolloutPolicyParams", bap);
        assertEquals(bap, params.getParameterValue("rolloutPolicyParams"));
        assertEquals(0.56, params.getParameterValue("rolloutPolicyParams.temperature"));
        assertEquals(0.27, params.getParameterValue("rolloutPolicyParams.epsilon"));
        assertFalse(params.getRolloutStrategy() instanceof BoltzmannActionPlayer);
        params.setParameterValue("rolloutType", MCTSEnums.Strategies.PARAMS);
        assertTrue(params.getRolloutStrategy() instanceof BoltzmannActionPlayer);
        BoltzmannActionPlayer rollout = (BoltzmannActionPlayer) params.getRolloutStrategy();
        assertEquals(0.56, rollout.temperature, 0.001);
        assertEquals(0.27, rollout.epsilon, 0.001);
        assertEquals(0.56, params.getParameterValue("rolloutPolicyParams.temperature"));
        assertEquals(0.27, params.getParameterValue("rolloutPolicyParams.epsilon"));
    }

    @Test
    public void subParamValuesCanBeChangedFromTopLevel() {
        assertFalse(params.getParameterNames().contains("rolloutPolicyParams.temperature"));
        assertFalse(params.getParameterNames().contains("rolloutPolicyParams.epsilon"));
        params.setParameterValue("rolloutPolicyParams", bap);
        params.setParameterValue("rolloutType", MCTSEnums.Strategies.PARAMS);
        assertTrue(params.getRolloutStrategy() instanceof BoltzmannActionPlayer);
        assertTrue(params.getParameterNames().contains("rolloutPolicyParams.temperature"));
        assertTrue(params.getParameterNames().contains("rolloutPolicyParams.epsilon"));
        params.setParameterValue("rolloutPolicyParams.temperature", 0.78);
        params.setParameterValue("rolloutPolicyParams.epsilon", 0.12);
        BoltzmannActionPlayer rollout = (BoltzmannActionPlayer) params.getRolloutStrategy();
        assertEquals(0.78, rollout.temperature, 0.001);
        assertEquals(0.12, rollout.epsilon, 0.001);
    }

    @Test
    public void copyDoesADeepCopyIncludingSubParams() {
        params.setParameterValue("rolloutPolicyParams", bap);
        MCTSParams newParams = (MCTSParams) params.copy();
        assertEquals(params, newParams);
        assertNotSame(params, newParams);
        assertEquals(bap, newParams.getParameterValue("rolloutPolicyParams"));
        assertNotSame(bap, newParams.getParameterValue("rolloutPolicyParams"));
        assertSame(bap, params.getParameterValue("rolloutPolicyParams"));
    }

    @Test
    public void setParameterValueOnCopyDoesNotUpdateOriginal() {
        params.setParameterValue("rolloutPolicyParams", bap);
        params.setParameterValue("rolloutType", MCTSEnums.Strategies.PARAMS);
        MCTSParams newParams = (MCTSParams) params.copy();
        assertEquals(params, newParams);
        assertNotSame(params, newParams);
        assertEquals(bap, newParams.getParameterValue("rolloutPolicyParams"));
        assertNotSame(bap, newParams.getParameterValue("rolloutPolicyParams"));
        assertSame(bap, params.getParameterValue("rolloutPolicyParams"));

        newParams.setParameterValue("rolloutPolicyParams.temperature", 2.0);
        assertTrue(newParams.getRolloutStrategy() instanceof BoltzmannActionPlayer);
        assertEquals(2.0, ((BoltzmannActionPlayer) newParams.getRolloutStrategy()).temperature, 0.001);
        assertEquals(0.56, ((BoltzmannActionPlayer) params.getRolloutStrategy()).temperature, 0.001);
    }

    @Test
    public void loadSearchSpaceIncludesSubParams() {
        String searchSpace = "src\\test\\java\\evaluation\\MCTSSearch_MASTRollout.json";
        ITPSearchSpace<AbstractPlayer> itp = new ITPSearchSpace<>(params, searchSpace);
        assertEquals(8, itp.getDimensions().size());
        int MASTBoltzmannIndex = itp.getIndexOf("MASTBoltzmann");
        int heuristicTypeIndex = itp.getIndexOf("heuristic.heuristicType");
        assertTrue(MASTBoltzmannIndex > -1);
        assertTrue(heuristicTypeIndex > -1);
        assertEquals(5, itp.allValues(MASTBoltzmannIndex).size());
        assertEquals(Arrays.asList(0.01, 0.1, 1.0, 10.0, 100.0), itp.allValues(MASTBoltzmannIndex));
        assertEquals(3, itp.allValues(heuristicTypeIndex).size());
        List<CoarseTunableHeuristic.HeuristicType> expectedArray = Arrays.asList(WIN_ONLY, SCORE_PLUS, LEADER);
        assertEquals(expectedArray, itp.allValues(heuristicTypeIndex));

        int[] settings = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
        settings[heuristicTypeIndex] = 1;
        settings[MASTBoltzmannIndex] = 3;
        MCTSPlayer agent = (MCTSPlayer) itp.instantiate(settings);
        MCTSParams params = agent.getParameters();
        assertEquals(10.0, params.MASTBoltzmann, 0.001);
        assertTrue(params.getStateHeuristic() instanceof CoarseTunableHeuristic);
        assertEquals(SCORE_PLUS, ((CoarseTunableHeuristic) params.heuristic).getHeuristicType());
    }

    @Test
    public void loadedSubParamsIncludesNonParameterisedObjects() {
        String searchSpace = "src\\test\\java\\evaluation\\MCTSSearch_PR_RolloutPolicy.json";
        ITPSearchSpace<MCTSPlayer> itp = new ITPSearchSpace(params, searchSpace);
        // actionHeuristic is a nested (non-Tunable) class, so check it is not included
        assertEquals(-1, itp.getIndexOf("rolloutPolicyParams.actionHeuristic"));
        assertEquals(-1, itp.getIndexOf("actionHeuristic"));
        // temperature should be
        int temperatureIndex = itp.getIndexOf("rolloutPolicyParams.temperature");
        assertTrue(temperatureIndex > -1);
        assertEquals(5, itp.allValues(temperatureIndex).size());
        assertEquals(Arrays.asList(0.01, 0.1, 1.0, 10.0, 100.0), itp.allValues(temperatureIndex));

        int[] settings = new int[]{0, 0, 0, 0, 0};

        MCTSPlayer agent = itp.instantiate(settings);
        MCTSParams params = agent.getParameters();
        assertTrue(params.getRolloutStrategy() instanceof BoltzmannActionPlayer);
        BoltzmannActionPlayer rollout = (BoltzmannActionPlayer) params.getRolloutStrategy();
        assertEquals(0.01, rollout.temperature, 0.001);
        assertEquals(new PuertoRicoActionHeuristic001(), rollout.getActionHeuristic());
    }

    @Test
    public void copyingParamsChangesRandomSeedOnChildButNotParent() {
        long startingSeed = params.getRandomSeed();
        MCTSParams copy = (MCTSParams) params.copy();
        assertNotEquals(startingSeed, copy.getRandomSeed());
        assertEquals(startingSeed, params.getRandomSeed());
    }

    @Test
    public void toJSONWithDefaults() {
        params.setParameterValue("rolloutPolicyParams", bap);
        params.setParameterValue("maxTreeDepth", 67);

        JSONObject json = params.instanceToJSON(false, Collections.emptyMap());
        assertEquals(0.56, params.getParameterValue("rolloutPolicyParams.temperature"));

        MCTSParams noChange = (MCTSParams) params.instanceFromJSON(json);
        assertTrue(params.allParametersAndValuesEqual(noChange));

        assertEquals(67, json.get("maxTreeDepth"));
        assertEquals(0.56, params.getParameterValue("rolloutPolicyParams.temperature"));
        assertEquals(0.56, ((JSONObject) json.get("rolloutPolicyParams")).get("temperature"));
        assertEquals(1.0, (Double) json.get("K"), 0.002);
        assertEquals(false, json.get("useMASTAsActionHeuristic"));
        assertEquals("BUDGET_FM_CALLS", json.get("budgetType"));

        assertEquals(MCTSParams.class, params.instanceFromJSON(json).getClass());
        MCTSParams fromJSON = (MCTSParams) params.instanceFromJSON(json);
        assertTrue(fromJSON.allParametersAndValuesEqual(noChange));
    }

    @Test
    public void toJSONWithoutDefaults() {
        params.setParameterValue("rolloutPolicyParams", bap);
        params.setParameterValue("maxTreeDepth", 67);
        params.setParameterValue("budgetType", BUDGET_TIME);

        JSONObject json = params.instanceToJSON(true, Collections.emptyMap());

        MCTSParams noChange = (MCTSParams) params.instanceFromJSON(json);
        assertTrue(params.allParametersAndValuesEqual(noChange));

        assertEquals(67, json.get("maxTreeDepth"));
        assertEquals(0.56, ((JSONObject) json.get("rolloutPolicyParams")).get("temperature"));
        assertFalse(json.containsKey("K"));
        assertFalse(json.containsKey("useMASTAsActionHeuristic"));
        assertFalse(json.containsKey("selectionPolicy"));
        assertEquals("BUDGET_TIME", json.get("budgetType"));

        MCTSParams fromJSON = (MCTSParams) params.instanceFromJSON(json);
        assertTrue(fromJSON.allParametersAndValuesEqual(noChange));
    }

    @Test
    public void toJSONWithParameterisedJSONObject() {
        JSONObject json = JSONUtils.loadJSONFile("src/test/java/evaluation/MCTSSearch_Heuristic.json");
        params.setRawJSON(json);
        JSONObject secondHeuristic = (JSONObject) ((JSONArray) json.get("heuristic")).get(1);
        params.setParameterValue("rolloutPolicyParams", bap);
        params.setParameterValue("maxTreeDepth", 67);
        params.setParameterValue("budgetType", BUDGET_TIME);
        params.setParameterValue("heuristic", new OrdinalPosition());   // just needs to be non-default; the actual value is irrelevant
        JSONObject asJSON = params.instanceToJSON(true, Map.of("heuristic", 1));
        assertEquals(asJSON.get("heuristic"), secondHeuristic);
    }

    @Test
    public void settingsFromJSONSuccessful() {
        ITPSearchSpace<MCTSPlayer> itp = new ITPSearchSpace(params, "src/test/java/evaluation/MCTSSearch_Heuristic.json");
        int[] settings = itp.settingsFromJSON("src/test/java/evaluation/MCTSSearch_HeuristicSample.json");
        JSONObject json = JSONUtils.loadJSONFile("src/test/java/evaluation/MCTSSearch_HeuristicSample.json");
        for (int i = 0; i < settings.length; i++) {
            String parameterName = itp.name(i);
            Object value = itp.value(i, settings[i]);
            Object jsonValue = json.get(parameterName);
            if (jsonValue == null) {
                // pick up default
                jsonValue = params.getDefaultParameterValue(parameterName);
            }
            if (!JSONUtils.areValuesEqual(value, jsonValue)) {
                System.out.println(parameterName + " " + value + " " + jsonValue);
            }
            assertTrue(JSONUtils.areValuesEqual(value, jsonValue));
        }
    }

    @Test
    public void settingsFromJSONFailsToMatchSearchSpace() {
        ITPSearchSpace<MCTSPlayer> itp = new ITPSearchSpace(params, "src/test/java/evaluation/MCTSSearch_Heuristic.json");
        try {
            itp.settingsFromJSON("src/test/java/evaluation/MCTSSearch_HeuristicSampleIncorrectI.json");
            fail();
        } catch (AssertionError e) {
            // rolloutLength in JSON is 50, which is not one of the possible tuned values
            System.out.println(e.getMessage());
            assertTrue(e.getMessage().contains("rolloutLength"));
        }
    }

    @Test
    public void settingsFromJSONFailsToMatchDefault() {
        // If the searchSpace defines a fixed value for a parameter, then we should use this
        ITPSearchSpace<AbstractPlayer> itp = new ITPSearchSpace<>(params, "src/test/java/evaluation/MCTSSearch_Heuristic.json");
        try {
            int[] settings = itp.settingsFromJSON("src/test/java/evaluation/MCTSSearch_HeuristicSampleIncorrectII.json");
            JSONObject json = itp.constructAgentJSON(settings);
            fail("Should have thrown an error");
        } catch (AssertionError e) {
            // maxTreeDepth in SampleIncorrectII.json is 20, which is going to be ignored as it differs to that in the main search space
            System.out.println(e.getMessage());
            assertTrue(e.getMessage().contains("maxTreeDepth"));
        }
    }


    @Test
    public void settingsFromNestedJSONSuccessful() {
        ITPSearchSpace<AbstractPlayer> itp = new ITPSearchSpace<>(params, "src/test/java/evaluation/MCTSSearch_MASTRollout.json");
        int[] settings = itp.settingsFromJSON("src/test/java/evaluation/MCTSSearch_MASTRolloutSample.json");
        JSONObject json = JSONUtils.loadJSONFile("src/test/java/evaluation/MCTSSearch_MASTRolloutSample.json");
        for (int i = 0; i < settings.length; i++) {
            String parameterName = itp.name(i);
            // now account for nested parameters
            String[] parts = parameterName.split("\\.");
            Object jsonValue = json;
            for (int j = 0; j < parts.length; j++) {
                jsonValue = ((JSONObject) jsonValue).get(parts[j]);
            }
            Object value = itp.value(i, settings[i]);
            if (jsonValue == null) {
                // pick up default
                if (parameterName.equals("heuristic.heuristicType")) {
                    fail("Should have found a value for " + parameterName);
                }
                jsonValue = params.getDefaultParameterValue(parameterName);
            }
            System.out.println(parameterName + " " + value + " " + jsonValue);
            assertTrue(JSONUtils.areValuesEqual(value, jsonValue));
        }
    }

    @Test
    public void settingsFromJSONPickUpNonDefaults() {
        ITPSearchSpace<MCTSPlayer> itp = new ITPSearchSpace(params, "src/test/java/evaluation/MCTSSearch_MASTRollout.json");
        int[] settings = itp.settingsFromJSON("src/test/java/evaluation/MCTSSearch_MASTRolloutSample.json");
        JSONObject json = itp.constructAgentJSON(settings);
        // We should pick up the default override in MCTSSearch_MASTRollout.json of 0.37
        assertEquals(0.37, (Double) json.get("exploreEpsilon"), 0.001);
    }

    @Test
    public void instanceToJSONWithNestedDefault() {
        ITPSearchSpace<MCTSPlayer> itp = new ITPSearchSpace(params, "src/test/java/evaluation/MCTSSearch_MASTRollout.json");
        int[] settings = itp.settingsFromJSON("src/test/java/evaluation/MCTSSearch_MASTRolloutSample.json");

        assertEquals("heuristic.heuristicType", itp.name(5));
        settings[5] = 0;  // WIN_ONLY is default
        JSONObject json = itp.constructAgentJSON(settings);
        JSONObject heuristicJSON = (JSONObject) json.get("heuristic");
        assertNotNull(heuristicJSON);
        assertEquals("players.heuristics.CoarseTunableHeuristic", heuristicJSON.get("class"));
        assertNull(heuristicJSON.get("heuristicType"));
    }

    @Test
    public void searchSpaceWithJSONSubFile() {
        ITPSearchSpace<MCTSPlayer> itp = new ITPSearchSpace(params, "src/test/java/evaluation/SearchSpaceWithJsonFileRef.json");
        // this fails if it throws an exception
    }

}
