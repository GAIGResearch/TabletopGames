package players.heuristics;

import com.ibm.icu.impl.number.parse.PaddingMatcher;
import core.actions.AbstractAction;
import evaluation.features.AutomatedFeatures;
import games.backgammon.BGActionFeatures;
import games.backgammon.BGStateFeatures;
import games.dominion.DominionConstants;
import games.dominion.DominionFGParameters;
import games.dominion.DominionForwardModel;
import games.dominion.DominionGameState;
import games.dominion.actions.*;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import games.dominion.metrics.DomActionFeatures;
import games.dominion.metrics.DomStateFeaturesReduced;
import org.apache.hadoop.shaded.org.eclipse.jetty.util.ajax.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import utilities.JSONUtils;
import utilities.Pair;

import java.util.*;

import static games.dominion.cards.CardType.MARKET;
import static games.dominion.cards.CardType.PROVINCE;
import static org.junit.Assert.*;

public class TestAutomatedFeatures {

    DominionGameState domState = new DominionGameState(new DominionFGParameters(), 4);
    DominionForwardModel fm = new DominionForwardModel();
    JSONObject coefficients;

    LinearStateHeuristic linearStateHeuristic = new LinearStateHeuristic(
            JSONUtils.loadJSONFile("src/test/java/players/heuristics/stateHeuristicWithAutomatedFeatures.json")
    );
    LogisticActionHeuristic logisticActionHeuristic = new LogisticActionHeuristic(
            JSONUtils.loadJSONFile("src/test/java/players/heuristics/actionHeuristicWithAutomatedFeatures.json")
    );

    @Before
    public void setup() {
        fm.setup(domState);
    }

    double BIAS = -1.2988663044789461;
    double treasure = 0.9840346436822138;
    double estate = -0.9191623434294867;
    double duchy = -0.45313761838342737; // duchyCount
    double totalCards = -0.7260896152706188; // totalCards
    double treasureTotal = -0.012495109115765733; // treasureValue:totalCards
    double duchyEstate = 0.5056753891584228; // duchyCount:estateCount

    double a_treasureInHand = -0.03438062348507604;
    double a_BIAS = -0.1399298593458686;
    double a_actionSize = -0.20518405783018512;
    double a_BUY = 0.018926434863914956;
    double a_PLAY = 0.17258252525450438;
    double a_COPPER = 0.07922675611960366;
    double a_treasureInHand_PLAY = 0.2483752153406519;
    double a_ESTATE = 0.2983223198425243;
    double a_SILVER = -0.05206605840583842;
    double a_MOAT = -0.16732454886123663;
    double a_actionSize_SILVER = 0.10607014877168938;
    double a_MILITIA = 0.5195586347419078;
    double a_MERCHANT = 0.08252076625148476;
    double a_SMITHY = -0.006656491964319801;

    @Test
    public void heuristicInitialisation() {
        // check that the heuristic initialises correctly
        int expectedStateFeatures = (new DomStateFeaturesReduced()).names().length;
        assertEquals(expectedStateFeatures, linearStateHeuristic.names().length);
        int additionalFeaturesInJSON = 18; // counted in file
        assertEquals(expectedStateFeatures +
                        additionalFeaturesInJSON,
                logisticActionHeuristic.names().length);
    }

    @Test
    public void testAutomatedStateFeatures() {
        assertEquals(BIAS + treasure * 0.7 + estate * 3 + totalCards * 10 + treasureTotal * 7.0,
                linearStateHeuristic.evaluateState(domState, 0), 0.00001);

        // then buy a card
        fm.next(domState, new EndPhase(DominionGameState.DominionGamePhase.Play));
        fm.next(domState, new EndPhase(DominionGameState.DominionGamePhase.Buy));
        domState.addCard(CardType.SILVER, 0, DominionConstants.DeckType.DISCARD);
        assertEquals(1, domState.getCurrentPlayer());
        assertEquals(BIAS + treasure * 9.0 / 11.0 + estate * 3 + totalCards * 11 + treasureTotal * 9.0,
                linearStateHeuristic.evaluateState(domState, 0), 0.00001);
        assertEquals(BIAS + treasure * 0.7 + estate * 3 + totalCards * 10 + treasureTotal * 7.0,
                linearStateHeuristic.evaluateState(domState, 1), 0.00001);

        domState.addCard(CardType.DUCHY, 1, DominionConstants.DeckType.DISCARD);
        assertEquals(BIAS + treasure * 7.0 / 11.0 + estate * 3 + totalCards * 11 + treasureTotal * 7.0 +
                        duchy + duchyEstate * 3,
                linearStateHeuristic.evaluateState(domState, 1), 0.00001);
    }

    @Test
    public void testAutomatedActionBuyFeatures() {
        fm.next(domState, new EndPhase(DominionGameState.DominionGamePhase.Play));
        // now check values of the actions

        List<AbstractAction> actions = fm.computeAvailableActions(domState);
        double[] values = logisticActionHeuristic.evaluateAllActions(actions, domState);

        for (int i = 0; i < actions.size(); i++) {
            AbstractAction action = actions.get(i);
            int money = domState.getAvailableSpend(0);
            if (action instanceof BuyCard bc) {
                double expectedValue = switch (bc.cardType) {
                    case COPPER -> logisticActionHeuristic.inverseLinkFunction.applyAsDouble(a_COPPER +
                            a_BIAS + a_treasureInHand * money + a_actionSize * actions.size() + a_BUY);
                    case ESTATE -> logisticActionHeuristic.inverseLinkFunction.applyAsDouble(a_ESTATE +
                            a_BIAS + a_treasureInHand * money + a_actionSize * actions.size() + a_BUY);
                    case SILVER -> logisticActionHeuristic.inverseLinkFunction.applyAsDouble(a_SILVER +
                            a_BIAS + a_treasureInHand * money + (a_actionSize_SILVER + a_actionSize) * actions.size() + a_BUY);
                    case MOAT -> logisticActionHeuristic.inverseLinkFunction.applyAsDouble(a_MOAT +
                            a_BIAS + a_treasureInHand * money + a_actionSize * actions.size() + a_BUY);
                    case SMITHY -> logisticActionHeuristic.inverseLinkFunction.applyAsDouble(a_SMITHY +
                            a_BIAS + a_treasureInHand * money + a_actionSize * actions.size() + a_BUY);
                    case MILITIA -> logisticActionHeuristic.inverseLinkFunction.applyAsDouble(a_MILITIA +
                            a_BIAS + a_treasureInHand * money + a_actionSize * actions.size() + a_BUY);
                    case MERCHANT -> logisticActionHeuristic.inverseLinkFunction.applyAsDouble(a_MERCHANT +
                            a_BIAS + a_treasureInHand * money + a_actionSize * actions.size() + a_BUY);
                    default -> -1.0;
                };
                System.out.println("Action: " + action + " expected value: " + expectedValue + " actual value: " + values[i]);
                if (expectedValue > -1.0) {
                    assertEquals(expectedValue, values[i], 0.0001);
                }
            }
        }
    }

    @Test
    public void testAutomatedActionPlayFeatures() {
        domState.addCard(CardType.MILITIA, 1, DominionConstants.DeckType.HAND);
        domState.addCard(CardType.MERCHANT, 1, DominionConstants.DeckType.HAND);
        domState.addCard(CardType.SMITHY, 1, DominionConstants.DeckType.HAND);

        fm.next(domState, new EndPhase(DominionGameState.DominionGamePhase.Play));
        fm.next(domState, new EndPhase(DominionGameState.DominionGamePhase.Buy));
        // now check values of the actions

        List<AbstractAction> actions = fm.computeAvailableActions(domState);
        int money = domState.getDeck(DominionConstants.DeckType.HAND, 1).stream()
                .mapToInt(DominionCard::treasureValue).sum();
        double[] values = logisticActionHeuristic.evaluateAllActions(actions, domState);

        for (int i = 0; i < actions.size(); i++) {
            AbstractAction action = actions.get(i);
            double expectedValue = -1.0;
            if (action instanceof Militia) {
                expectedValue = logisticActionHeuristic.inverseLinkFunction.applyAsDouble(a_MILITIA +
                        a_BIAS + (a_treasureInHand + a_treasureInHand_PLAY) * money + a_actionSize * actions.size() + a_PLAY);
            } else if (action instanceof Merchant) {
                expectedValue = logisticActionHeuristic.inverseLinkFunction.applyAsDouble(a_MERCHANT +
                        a_BIAS + (a_treasureInHand + a_treasureInHand_PLAY) * money + a_actionSize * actions.size() + a_PLAY);
            } else if (action instanceof DominionAction && ((DominionAction) action).type == CardType.SMITHY) {
                expectedValue = logisticActionHeuristic.inverseLinkFunction.applyAsDouble(a_SMITHY +
                        a_BIAS + (a_treasureInHand + a_treasureInHand_PLAY) * money + a_actionSize * actions.size() + a_PLAY);
            } else if (action instanceof EndPhase) {
                expectedValue = logisticActionHeuristic.inverseLinkFunction.applyAsDouble(
                        a_BIAS + a_treasureInHand * money + a_actionSize * actions.size());
            }
            System.out.println("Action: " + action + " expected value: " + expectedValue + " actual value: " + values[i]);
            if (expectedValue > -1.0) {
                assertEquals(expectedValue, values[i], 0.00001);
            }
        }
    }

    @Test
    public void JSONLoadingI() {
        LogisticActionHeuristic actionHeuristic = new LogisticActionHeuristic(
                JSONUtils.loadJSONFile("src/test/java/players/heuristics/NonFilteredHeuristic.json")
        );
        JSONObject json = actionHeuristic.toJSON();
        assertEquals("players.heuristics.LogisticActionHeuristic", json.get("class"));
        assertFalse(json.containsKey("features"));
        assertEquals(21, ((JSONArray) ((JSONObject) json.get("actionFeatures")).get("features")).size());

        assertEquals(21 + (new DomStateFeaturesReduced()).names().length, actionHeuristic.names().length);
    }

    private JSONObject initialiseBackgammonCoefficients(AutomatedFeatures asf) {
        // Then construct JSON with coefficients to load
        coefficients = new JSONObject();
        coefficients.put("BIAS", 0.5);
        coefficients.put(asf.names()[0] + ":" + asf.names()[10], 0.1);
        coefficients.put(asf.names()[12] + ":" + asf.names()[11], -0.2);
        coefficients.put(asf.names()[4], 1.0);

        JSONObject wrapper = new JSONObject();
        wrapper.put("coefficients", coefficients);
        return wrapper;
    }

    @Test
    public void JSONGenerationI() {
        // we create automated features, and then we check that the JSON generation works correctly

        AutomatedFeatures asf = new AutomatedFeatures(new BGStateFeatures(), new BGActionFeatures());
        int startingFeatures = asf.names().length;
        assertEquals(startingFeatures, asf.underlyingAction.names().length + asf.underlyingState.names().length);

        LinearActionHeuristic heuristic = new LinearActionHeuristic(asf, null, new double[0]);
        heuristic.loadCoefficientsFromJSON(initialiseBackgammonCoefficients(asf));

        // Now when we generate the JSON, we should only have features for which we have coefficients
        // NOT including the baseline raw ones - so in this case just the two interactions

        JSONObject json = heuristic.toJSON();
        assertEquals("players.heuristics.LinearActionHeuristic", json.get("class"));
        assertFalse(json.containsKey("features"));
        assertTrue(json.containsKey("actionFeatures"));
        JSONArray features = (JSONArray) ((JSONObject) json.get("actionFeatures")).get("features");
        assertEquals(0, features.size()); // only RAW and interaction

        // Then reconstruct the heuristic from the JSON and check that it has the expected coefficients and features
        LinearActionHeuristic reconstruction = new LinearActionHeuristic(json);
        checkEquivalent(heuristic, reconstruction, new int[]{0, 0, 0});
    }

    @Test
    public void JSONGenerationII() {
        AutomatedFeatures asf = new AutomatedFeatures(new BGStateFeatures(), new BGActionFeatures());

        int barIndex = 0;
        for (int i = 0; i < asf.names().length; i++) {
            if (asf.names()[i].equals("Bar")) {
                barIndex = i;
                break;
            }
        }

        int range0Index = asf.addFeature(new AutomatedFeatures.ColumnDetails(
                "Bar_B0", AutomatedFeatures.featureType.RANGE, null,
                new Pair<>(0.0, 0.5), barIndex, Double.class, new ArrayList<>())
        );
        int range1Index = asf.addFeature(new AutomatedFeatures.ColumnDetails(
                "Bar_B1", AutomatedFeatures.featureType.RANGE, null,
                new Pair<>(0.5, 1.0), barIndex, Double.class, new ArrayList<>())
        );
        asf.addInteraction(2, 4);

        // Then construct JSON with coefficients to load
        JSONObject wrapper = initialiseBackgammonCoefficients(asf);
        coefficients.put("Bar_B1", 0.4);
        coefficients.put("Bar_B0", 0.000000001); // should be ignored

        LinearActionHeuristic heuristic = new LinearActionHeuristic(asf, null, new double[0]);
        heuristic.loadCoefficientsFromJSON(wrapper);

        // Now when we generate the JSON, we should only have features for which we have coefficients
        // NOT including the baseline raw ones - so in this case just the two interactions

        JSONObject json = heuristic.toJSON();
        assertEquals("players.heuristics.LinearActionHeuristic", json.get("class"));
        assertFalse(json.containsKey("features"));
        assertTrue(json.containsKey("actionFeatures"));
        JSONArray features = (JSONArray) ((JSONObject) json.get("actionFeatures")).get("features");
        assertEquals(1, features.size()); // only one of the RANGE features should be included

        // Then reconstruct the heuristic from the JSON and check that it has the expected coefficients and features
        LinearActionHeuristic reconstruction = new LinearActionHeuristic(json);
        checkEquivalent(heuristic, reconstruction, new int[]{2, 2, 0});

        // Check that the RANGE feature is correctly reconstructed (this takes the last position)
        assertEquals("Bar_B1", reconstruction.names()[range0Index]);
    }

    @Test
    public void JSONGenerationIII() {
        // this is to test that if we have a generated feature with a zero coefficient, it is not excluded
        // *if* is it used in some other interaction
        AutomatedFeatures asf = new AutomatedFeatures(new BGStateFeatures(), new BGActionFeatures());

        int barIndex = 0;
        for (int i = 0; i < asf.names().length; i++) {
            if (asf.names()[i].equals("Bar")) {
                barIndex = i;
                break;
            }
        }
        assertTrue(barIndex > 0); // Bar should be present

        int range0Index = asf.addFeature(new AutomatedFeatures.ColumnDetails(
                "Bar_B0", AutomatedFeatures.featureType.RANGE, null,
                new Pair<>(0.0, 0.5), barIndex, Double.class, new ArrayList<>())
        );
        int range1Index = asf.addFeature(new AutomatedFeatures.ColumnDetails(
                "Bar_B1", AutomatedFeatures.featureType.RANGE, null,
                new Pair<>(0.5, 1.0), barIndex, Double.class, new ArrayList<>())
        );
        asf.addInteraction(2, range1Index);  // include treasureValue_B1 in an interaction

        // Then construct JSON with coefficients to load
        JSONObject wrapper = initialiseBackgammonCoefficients(asf);
        coefficients.put("Bar_B1", 0.4);
        coefficients.put("Bar_B0", 0.000000001); // should be ignored
        coefficients.put(asf.names()[2] + ":Bar_B0", -0.674);

        LinearActionHeuristic heuristic = new LinearActionHeuristic(asf, null, new double[0]);
        heuristic.loadCoefficientsFromJSON(wrapper);

        // Now when we generate the JSON, we should only have features for which we have coefficients
        // NOT including the baseline raw ones - so in this case just the two interactions

        JSONObject json = heuristic.toJSON();
        JSONArray features = (JSONArray) ((JSONObject) json.get("actionFeatures")).get("features");
        assertEquals(2, features.size()); // both RANGE features should be included

        // Then reconstruct the heuristic from the JSON and check that it has the expected coefficients and features
        LinearActionHeuristic reconstruction = new LinearActionHeuristic(json);
        checkEquivalent(heuristic, reconstruction, new int[]{1, 1, 0});

        // Check that the RANGE features are correctly reconstructed (this takes the last position)
        assertEquals("Bar_B0", reconstruction.names()[range0Index]);
        assertEquals("Bar_B1", reconstruction.names()[range1Index]);
    }

    @Test
    public void JSONGenerationIV() {
        // we now check with Dominion ASF, which included ENUM features (not covered in I to III above with backgammon)

        AutomatedFeatures asf = new AutomatedFeatures(new DomStateFeaturesReduced(), new DomActionFeatures());

        int cardTypeIndex = asf.underlyingState.names().length + 1; // cardType is the second action feature
        int enum0Index = asf.addFeature(new AutomatedFeatures.ColumnDetails(
                "cardType_PROVINCE", AutomatedFeatures.featureType.ENUM, PROVINCE,
                null, cardTypeIndex, CardType.class, new ArrayList<>())
        );
        int enum1Index = asf.addFeature(new AutomatedFeatures.ColumnDetails(
                "cardType_MARKET", AutomatedFeatures.featureType.ENUM, MARKET,
                null, cardTypeIndex, CardType.class, new ArrayList<>())
        );
        int enum2Index = asf.addFeature(new AutomatedFeatures.ColumnDetails(
                "cardType_VILLAGE", AutomatedFeatures.featureType.ENUM, MARKET,
                null, cardTypeIndex, CardType.class, new ArrayList<>())
        );

        asf.addInteraction(2, enum0Index);  // include treasureValue_PROVINCE in an interaction

        coefficients = new JSONObject();
        coefficients.put("BIAS", 0.5);
        coefficients.put(asf.names()[2] + ":" + asf.names()[10], 0.1);
        coefficients.put(asf.names()[12] + ":" + asf.names()[11], -0.2);
        coefficients.put(asf.names()[4], 1.0);
        coefficients.put("cardType_MARKET", 0.3);
        coefficients.put("cardType_VILLAGE", 0.000000001); // should be ignored
        coefficients.put(asf.names()[2] + ":cardType_PROVINCE", -0.674);

        JSONObject wrapper = new JSONObject();
        wrapper.put("coefficients", coefficients);

        LinearActionHeuristic heuristic = new LinearActionHeuristic(asf, null, new double[0]);
        heuristic.loadCoefficientsFromJSON(wrapper);

        JSONObject json = heuristic.toJSON();
        JSONArray features = (JSONArray) ((JSONObject) json.get("actionFeatures")).get("features");
        assertEquals(2, features.size()); // 2 of 3 ENUM features should be included

        // Then reconstruct the heuristic from the JSON and check that it has the expected coefficients and features
        LinearActionHeuristic reconstruction = new LinearActionHeuristic(json);
        checkEquivalent(heuristic, reconstruction, new int[]{2, 2, 0});  // 2 are dropped (VILLAGE, plus the interactionFeature added)

        // Check that the ENUM features are correctly reconstructed (this takes the last position)
        assertEquals("cardType_MARKET", reconstruction.names()[enum1Index]);
        assertEquals("cardType_PROVINCE", reconstruction.names()[enum0Index]);
        assertEquals(enum2Index, reconstruction.names().length);
    }

    private void checkEquivalent(LinearActionHeuristic heuristic, LinearActionHeuristic reconstruction, int[] reconstructionDifferences) {
        assertEquals(heuristic.names().length, reconstruction.names().length + reconstructionDifferences[0]);
        if (reconstructionDifferences[0] == 0)
            for (int i = 0; i < heuristic.names().length; i++) {
                assertEquals(heuristic.names()[i], reconstruction.names()[i]);
            }
        assertEquals(heuristic.coefficients.length, reconstruction.coefficients.length + reconstructionDifferences[1]);
        if (reconstructionDifferences[1] == 0)
            for (int i = 0; i < heuristic.coefficients.length; i++) {
                assertEquals(heuristic.coefficients[i], reconstruction.coefficients[i], 0.00001);
            }
        assertEquals(heuristic.interactionCoefficients.length, reconstruction.interactionCoefficients.length + reconstructionDifferences[2]);
        if (reconstructionDifferences[2] == 0)
            for (int i = 0; i < heuristic.interactionCoefficients.length; i++) {
                assertEquals(heuristic.interactionCoefficients[i], reconstruction.interactionCoefficients[i], 0.00001);
            }
    }
}
