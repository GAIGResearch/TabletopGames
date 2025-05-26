package players.heuristics;

import core.actions.AbstractAction;
import games.dominion.DominionConstants;
import games.dominion.DominionFGParameters;
import games.dominion.DominionForwardModel;
import games.dominion.DominionGameState;
import games.dominion.actions.*;
import games.dominion.cards.CardType;
import games.dominion.metrics.DomActionFeatures;
import games.dominion.metrics.DomStateFeaturesReduced;
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
    LinearActionHeuristic linearActionHeuristic = new LinearActionHeuristic(
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

    @Test
    public void testAutomatedStateFeatures() {
        assertEquals(BIAS + treasure * 7 + estate * 3 + totalCards * 10 + treasureTotal * 70,
                linearStateHeuristic.evaluateState(domState, 0), 0.001);

        // then buy a card
        fm.next(domState, new EndPhase(DominionGameState.DominionGamePhase.Play));
        fm.next(domState, new BuyCard(CardType.SILVER, 0));
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
        double[] values = linearActionHeuristic.evaluateAllActions(actions, domState);

        for (int i = 0; i < actions.size(); i++) {
            AbstractAction action = actions.get(i);
            if (action instanceof BuyCard bc) {
                switch (bc.cardType) {
                    // TODO:
                }
            }
        }
        fail("Not implemented yet for BuyCard actions");
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
        double[] values = linearActionHeuristic.evaluateAllActions(actions, domState);

        for (int i = 0; i < actions.size(); i++) {
            AbstractAction action = actions.get(i);
            if (action instanceof Militia) {
                // TODO:
            } else if (action instanceof Merchant) {
                // tODO:
            } else if (action instanceof DominionAction) {
                // TODO: Smithy
            } else if (action instanceof EndPhase) {
                // TODO:
            }
        }
        fail("Not implemented yet for PlayCard actions");
    }

    @Test
    public void featureFiltering() {
        LogisticActionHeuristic actionHeuristic = new LogisticActionHeuristic(
                JSONUtils.loadJSONFile("src/test/java/players/heuristics/NonFilteredHeuristic.json")
        );
        JSONObject json = actionHeuristic.toJSON();
        assertEquals("players.heuristics.LogisticActionHeuristic", json.get("class"));
        assertEquals(2, ((JSONArray)((JSONObject) json.get("features")).get("features")).size());
        assertEquals(21, ((JSONArray)((JSONObject) json.get("actionFeatures")).get("features")).size());
    }
}
