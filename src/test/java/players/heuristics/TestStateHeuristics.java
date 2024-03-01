package players.heuristics;

import games.dominion.DominionFGParameters;
import games.dominion.DominionForwardModel;
import games.dominion.DominionGameState;
import games.dominion.actions.BuyCard;
import games.dominion.actions.EndPhase;
import games.dominion.cards.CardType;
import games.dominion.metrics.DomStateFeaturesReduced;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestStateHeuristics {

    DomStateFeaturesReduced dominionFeaturedReduced = new DomStateFeaturesReduced();
    DominionGameState state = new DominionGameState(new DominionFGParameters(), 4);
    DominionForwardModel fm = new DominionForwardModel();

    @Before
    public void setup() {
        fm.setup(state);
    }


//            "victoryPoints": 0.1,
//            "treasureValue": 0.2,
//            "actionCards": 0.0,
//            "treasureInHand": 0.0,
//            "estateCount": 0.0,
//            "estateCount:victoryPoints": 0.5,
//            "estateCount:actionCards": 1.0,
//            "totalCards:estateCount:buysLeft": 1.0

    @Test
    public void testLinearStateHeuristic() {
        LinearStateHeuristic linearStateHeuristic = new LinearStateHeuristic(dominionFeaturedReduced,
                "src\\test\\java\\players\\heuristics\\DominionFeatureWeights.json",
                new PureScoreHeuristic());
        assertEquals(linearStateHeuristic.coefficients.length, linearStateHeuristic.features.names().length + 1);
        assertEquals(0.0, linearStateHeuristic.coefficients[0], 0.01);  // bias not in json
        assertEquals(0.1, linearStateHeuristic.coefficients[1], 0.01); // first actual feature
        // 3 VP = 0.3
        // 7 Copper = 1.4
        // estate:VP = 4.5
        // estate:AC = 0.0
        // total:estate:buys = 30.0
        assertEquals(36.2, linearStateHeuristic.evaluateState(state, 0), 0.01);

        // then buy a card
        fm.next(state, new EndPhase());
        fm.next(state, new BuyCard(CardType.SILVER, 0));
        assertEquals(6.6, linearStateHeuristic.evaluateState(state, 0), 0.01);
        assertEquals(36.2, linearStateHeuristic.evaluateState(state, 1), 0.01);
    }

    @Test
    public void testLogisticStateHeuristic() {
        LogisticStateHeuristic logisticStateHeuristic = new LogisticStateHeuristic(dominionFeaturedReduced,
                "src\\test\\java\\players\\heuristics\\DominionFeatureWeightsLogistic.json",
                new WinOnlyHeuristic());
        assertEquals(logisticStateHeuristic.coefficients.length, logisticStateHeuristic.features.names().length + 1);
        assertEquals(-10.0, logisticStateHeuristic.coefficients[0], 0.01);  // bias
        assertEquals(0.1, logisticStateHeuristic.coefficients[1], 0.01); // first actual feature
        // 3 VP = 0.3
        // 7 Copper = 1.4
        // estate:VP = 4.5
        // estate:AC = 0.0
        // total:estate:buys = 30.0
        assertEquals(1.0, logisticStateHeuristic.evaluateState(state, 0), 0.01);

        // then buy a card
        fm.next(state, new EndPhase());
        fm.next(state, new BuyCard(CardType.SILVER, 0));
        double expectedValue = 1.0 / ( 1.0 + Math.exp(10.0-6.6));
        assertEquals(expectedValue, logisticStateHeuristic.evaluateState(state, 0), 0.01);
        assertEquals(1.0, logisticStateHeuristic.evaluateState(state, 1), 0.01);
    }
}
