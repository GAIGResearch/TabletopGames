package players.heuristics;

import core.actions.AbstractAction;
import games.dominion.DominionFGParameters;
import games.dominion.DominionForwardModel;
import games.dominion.DominionGameState;
import games.dominion.actions.BuyCard;
import games.dominion.actions.EndPhase;
import games.dominion.cards.CardType;
import games.dominion.metrics.DomStateFeaturesReduced;
import games.loveletter.*;
import games.loveletter.actions.PlayCard;
import games.loveletter.cards.LoveLetterCard;
import games.loveletter.features.LLActionFeaturesTiny;
import games.loveletter.features.LLStateFeaturesReduced;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestHeuristics {

    DomStateFeaturesReduced dominionFeaturedReduced = new DomStateFeaturesReduced();
    DominionGameState domState = new DominionGameState(new DominionFGParameters(), 4);
    DominionForwardModel fm = new DominionForwardModel();

    LLActionFeaturesTiny llActionFeaturesTiny = new LLActionFeaturesTiny();
    LLStateFeaturesReduced llStateFeaturesReduced = new LLStateFeaturesReduced();
    LoveLetterGameState llState = new LoveLetterGameState(new LoveLetterParameters(), 4);
    LoveLetterForwardModel llFm = new LoveLetterForwardModel();

    @Before
    public void setup() {
        fm.setup(domState);
        llState.getGameParameters().setRandomSeed(393);
        llFm.setup(llState);
    }

    @Test
    public void testLinearStateHeuristic() {
        LinearStateHeuristic linearStateHeuristic = new LinearStateHeuristic(dominionFeaturedReduced,
                "src\\test\\java\\players\\heuristics\\DominionFeatureWeights.json",
                new PureScoreHeuristic());
//            "victoryPoints": 0.1,
//            "treasureValue": 0.2,
//            "actionCards": 0.0,
//            "treasureInHand": 0.0,
//            "estateCount": 0.0,
//            "estateCount:victoryPoints": 0.5,
//            "estateCount:actionCards": 1.0,
//            "totalCards:estateCount:buysLeft": 1.0
        assertEquals(linearStateHeuristic.coefficients.length, linearStateHeuristic.features.names().length + 1);
        assertEquals(0.0, linearStateHeuristic.coefficients[0], 0.01);  // bias not in json
        assertEquals(0.1, linearStateHeuristic.coefficients[1], 0.01); // first actual feature
        // 3 VP = 0.3
        // 7 Copper = 0.7 * 20 = 14
        // estate:VP = 4.5
        // estate:AC = 0.0
        // total:vp = -9.0
        // total:estate:buys = 30.0
        assertEquals(0.3 + 14.0 + 4.5 - 9.0 + 30.0, linearStateHeuristic.evaluateState(domState, 0), 0.01);

        // then buy a card
        fm.next(domState, new EndPhase(DominionGameState.DominionGamePhase.Play));
        fm.next(domState, new BuyCard(CardType.SILVER, 0));
        // this changes treasure value to 9 / 11.
        assertEquals(1, domState.getCurrentPlayer());
        assertEquals(0.3 + 9.0 / 11.0 * 20.0 + 4.5 - 9.0 , linearStateHeuristic.evaluateState(domState, 0), 0.01);
        assertEquals(0.3 + 14.0 + 4.5 - 9.0 + 30.0, linearStateHeuristic.evaluateState(domState, 1), 0.01);
    }

    @Test
    public void testLogisticStateHeuristic() {
        LogisticStateHeuristic logisticStateHeuristic = new LogisticStateHeuristic(dominionFeaturedReduced,
                "src\\test\\java\\players\\heuristics\\DominionFeatureWeightsLogistic.json",
                new WinOnlyHeuristic());
        assertEquals(logisticStateHeuristic.coefficients.length, logisticStateHeuristic.features.names().length + 1);
        assertEquals(-10.0, logisticStateHeuristic.coefficients[0], 0.01);  // bias
        assertEquals(0.1, logisticStateHeuristic.coefficients[1], 0.01); // first actual feature
        // Bias = -10.0
        // 3 VP = 0.3
        // 7 Copper = 0.7 * 20 = 14.0
        // estate:VP = -4.5
        // estate:AC = 0.0
        // total:estate:buys = 30.0
        double expectedValue = 1.0 / (1.0 + Math.exp(- (-10.0 + 0.3 + 20.0 * 0.7 - 4.5 + 30)));
        assertEquals(expectedValue, logisticStateHeuristic.evaluateState(domState, 0), 0.01);

        // then buy a card
        fm.next(domState, new EndPhase(DominionGameState.DominionGamePhase.Play));
        fm.next(domState, new BuyCard(CardType.SILVER, 0));
        double treasureValue = (7.0 + 2.0) / 11.0;
        expectedValue = 1.0 / (1.0 + Math.exp(-(-10.0 + 0.3 + 20.0 * treasureValue - 4.5)));
        assertEquals(expectedValue, logisticStateHeuristic.evaluateState(domState, 0), 0.01);
        assertEquals(1.0, logisticStateHeuristic.evaluateState(domState, 1), 0.01);
    }

    @Test
    public void testActionHeuristicNonASF() {
        llState.getPlayerHandCards().get(0).clear();
        llState.getPlayerHandCards().get(0).add(new LoveLetterCard(games.loveletter.cards.CardType.Handmaid));
        llState.getPlayerHandCards().get(0).add(new LoveLetterCard(games.loveletter.cards.CardType.Guard));
        LinearActionHeuristic linearActionHeuristic = new LinearActionHeuristic(llActionFeaturesTiny, llStateFeaturesReduced,
                "src\\test\\java\\players\\heuristics\\LLFeatureWeights.json") {
        };
        assertEquals(linearActionHeuristic.coefficients.length, linearActionHeuristic.features.names().length + 1 + linearActionHeuristic.actionFeatures.names().length);
        assertEquals(10.0, linearActionHeuristic.coefficients[0], 0.01);  // bias

        List<AbstractAction> actions = llFm.computeAvailableActions(llState);

        // Hand should contain a Handmaid and a Guard (Total Value = 5)
        for (AbstractAction action : actions) {
            PlayCard playCard = (PlayCard) action;
            games.loveletter.cards.CardType cardType = playCard.getCardType();
            if (cardType == games.loveletter.cards.CardType.Guard)
                assertEquals(0.1 * 5.0 / 17.0 + 0.2 + 0.01 + 10.0, linearActionHeuristic.evaluateAction(action, llState), 0.0001);
            else if (cardType == games.loveletter.cards.CardType.Handmaid)
                assertEquals(0.1 * 5.0 / 17.0 + 0.2 + 10.0, linearActionHeuristic.evaluateAction(action, llState), 0.0001);
            else
                throw new AssertionError("Unexpected action: " + action);
        }

        llState.getPlayerHandCards().get(0).clear();
        llState.getPlayerHandCards().get(0).add(new LoveLetterCard(games.loveletter.cards.CardType.Baron));
        llState.getPlayerHandCards().get(0).add(new LoveLetterCard(games.loveletter.cards.CardType.Guard));
        actions = llFm.computeAvailableActions(llState);

        // Hand should contain a Baron and a Guard (Total Value = 4)
        for (AbstractAction action : actions) {
            PlayCard playCard = (PlayCard) action;
            games.loveletter.cards.CardType cardType = playCard.getCardType();
            if (cardType == games.loveletter.cards.CardType.Guard)
                assertEquals(0.1 * 4.0 / 17.0 + 0.2 + 0.01 + 10.0 + 0.07, linearActionHeuristic.evaluateAction(action, llState), 0.0001);
            else if (cardType == games.loveletter.cards.CardType.Baron)
                assertEquals(0.1 * 4.0 / 17.0 + 0.2 + 10.0, linearActionHeuristic.evaluateAction(action, llState), 0.0001);
            else
                throw new AssertionError("Unexpected action: " + action);
        }
    }
}
