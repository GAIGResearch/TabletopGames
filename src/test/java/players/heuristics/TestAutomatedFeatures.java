package players.heuristics;

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
import org.apache.hadoop.shaded.org.eclipse.jetty.util.ajax.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import utilities.JSONUtils;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestAutomatedFeatures {

    DominionGameState domState = new DominionGameState(new DominionFGParameters(), 4);
    DominionForwardModel fm = new DominionForwardModel();

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
    double a_MILITIA = 0.005195586347419078;
    double a_MERCHANT = 0.08252076625148476;
    double a_SMITHY = -0.006656491964319801;

    @Test
    public void testAutomatedStateFeatures() {
        assertEquals(BIAS + treasure * 7 + estate * 3 + totalCards * 10 + treasureTotal * 70,
                linearStateHeuristic.evaluateState(domState, 0), 0.001);

        // then buy a card
        fm.next(domState, new EndPhase(DominionGameState.DominionGamePhase.Play));
        fm.next(domState, new EndPhase(DominionGameState.DominionGamePhase.Buy));
        domState.addCard(CardType.SILVER, 0, DominionConstants.DeckType.DISCARD);
        assertEquals(1, domState.getCurrentPlayer());
        assertEquals(BIAS + treasure * 9 + estate * 3 + totalCards * 11 + treasureTotal * 99,
                linearStateHeuristic.evaluateState(domState, 0), 0.001);
        assertEquals(BIAS + treasure * 7 + estate * 3 + totalCards * 10 + treasureTotal * 70,
                linearStateHeuristic.evaluateState(domState, 1), 0.001);

        domState.addCard(CardType.DUCHY, 1, DominionConstants.DeckType.DISCARD);
        assertEquals(BIAS + treasure * 7 + estate * 3 + totalCards * 11 + treasureTotal * 77 +
                        duchy + duchyEstate * 3,
                linearStateHeuristic.evaluateState(domState, 1), 0.001);
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
                assertEquals(expectedValue, values[i], 0.0001);
            }
        }
    }

    @Test
    public void featureFiltering() {
        LogisticActionHeuristic actionHeuristic = new LogisticActionHeuristic(
                JSONUtils.loadJSONFile("src/test/java/players/heuristics/NonFilteredHeuristic.json")
        );
        JSONObject json = actionHeuristic.toJSON();
        assertEquals("players.heuristics.LogisticActionHeuristic", json.get("class"));
        assertEquals(2, ((JSONArray) ((JSONObject) json.get("features")).get("features")).size());
        assertEquals(21, ((JSONArray) ((JSONObject) json.get("actionFeatures")).get("features")).size());
    }

    @Test
    public void JSONGeneration() {
        // we create automated features, and then we check that the JSON generation works correctly

        AutomatedFeatures asf = new AutomatedFeatures(new BGStateFeatures(), new BGActionFeatures());
        int startingFeatures = asf.names().length;
        assertEquals(startingFeatures, asf.underlyingAction.names().length + asf.underlyingState.names().length);

        // Then construct JSON with coefficients to load
        JSONObject coefficients = new JSONObject();
        coefficients.put("BIAS", 0.5);
        coefficients.put(asf.names()[0] + ":" + asf.names()[10], 0.1);
        coefficients.put(asf.names()[12] + ":" + asf.names()[11], -0.2);
        coefficients.put(asf.names()[4], 1.0);

        JSONObject wrapper = new JSONObject();
        wrapper.put("coefficients", coefficients);

        LinearActionHeuristic heuristic = new LinearActionHeuristic(null, asf, new double[0]);
        heuristic.loadCoefficientsFromJSON(wrapper);


    }
}
