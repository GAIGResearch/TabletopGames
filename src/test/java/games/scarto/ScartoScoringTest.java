package games.scarto;

import core.components.TarotCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static core.components.TarotCard.*;
import static games.scarto.ScartoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Card points, pile points, points per deal, each player's score and the heuristic score mid-deal.
 */
public class ScartoScoringTest {

    ScartoParameters params;

    @Before
    public void setup() {
        params = new ScartoParameters();
    }

    // ---- card points ----

    @Test
    public void kingsThePagatAndTheAngelScoreFour() {
        assertEquals(4, params.cardPoints(sword(KING)));
        assertEquals(4, params.cardPoints(cup(KING)));
        assertEquals(4, params.cardPoints(trump(PAGAT)));
        assertEquals(4, params.cardPoints(trump(ANGEL)));
    }

    @Test
    public void queensAndTheFoolScoreThree() {
        assertEquals(3, params.cardPoints(baton(QUEEN)));
        assertEquals(3, params.cardPoints(coin(QUEEN)));
        assertEquals(3, params.cardPoints(fool()));
    }

    @Test
    public void cavaliersScoreTwoAndKnavesOne() {
        assertEquals(2, params.cardPoints(sword(CAVALIER)));
        assertEquals(2, params.cardPoints(cup(CAVALIER)));
        assertEquals(1, params.cardPoints(baton(KNAVE)));
        assertEquals(1, params.cardPoints(coin(KNAVE)));
    }

    @Test
    public void theWorldOtherTrumpsAndPipsScoreNothing() {
        assertEquals(0, params.cardPoints(trump(WORLD)));
        assertEquals(0, params.cardPoints(trump(19)));
        assertEquals(0, params.cardPoints(trump(2)));
        assertEquals(0, params.cardPoints(cup(1)));
        assertEquals(0, params.cardPoints(sword(10)));
        assertEquals(0, params.cardPoints(coin(5)));
    }

    @Test
    public void changingKingPointsChangesTheKingsValueOnly() {
        params.setParameterValue("kingPoints", 5);
        assertEquals(5, params.cardPoints(sword(KING)));
        assertEquals(4, params.cardPoints(trump(PAGAT)));
        assertEquals(3, params.cardPoints(cup(QUEEN)));
    }

    // ---- pile points ----

    @Test
    public void aThreeCardPileScoresItsCardsPlusOne() {
        // King 4 + Queen 3 + cup 5 0 = 7, plus (3 + 1) / 3 = 1 -> 8
        assertEquals(8, params.pilePoints(List.of(cup(KING), cup(QUEEN), cup(5))));
    }

    @Test
    public void aTwoCardPileScoresItsCardsPlusOne() {
        // Knave 1 + coin 2 0 = 1, plus (2 + 1) / 3 = 1 -> 2
        assertEquals(2, params.pilePoints(List.of(coin(KNAVE), coin(2))));
    }

    @Test
    public void aFourCardPileWithTheFoolScoresItsCardsPlusOne() {
        // Fool 3 + King 4 + 3 0 + 4 0 = 7, plus (4 + 1) / 3 = 1 -> 8
        assertEquals(8, params.pilePoints(List.of(fool(), sword(KING), sword(3), sword(4))));
    }

    @Test
    public void aPileOfTheFoolAloneScoresOnlyTheFool() {
        // Fool 3, plus (1 + 1) / 3 = 0 -> 3
        assertEquals(3, params.pilePoints(List.of(fool())));
    }

    @Test
    public void anEmptyPileScoresNothing() {
        // (0 + 1) / 3 = 0
        assertEquals(0, params.pilePoints(List.of()));
    }

    @Test
    public void aSixCardPileScoresItsCardsPlusTwo() {
        // Pagat 4 + Cavalier 2 + 4 x 0 = 6, plus (6 + 1) / 3 = 2 -> 8
        assertEquals(8, params.pilePoints(List.of(trump(PAGAT), baton(CAVALIER), baton(2), baton(3), trump(8), cup(9))));
    }

    @Test
    public void pilePointsUseTheCurrentParameterValues() {
        params.setParameterValue("kingPoints", 5);
        // King 5 + Queen 3 + cup 5 0 = 8, plus (3 + 1) / 3 = 1 -> 9
        assertEquals(9, params.pilePoints(List.of(cup(KING), cup(QUEEN), cup(5))));
    }

    // ---- points per deal ----

    @Test
    public void aDealHasSeventySevenPoints() {
        // card points in the pack: 4 Kings x 4 = 16, Pagat + Angel 4 + 4 = 8, 4 Queens x 3 = 12, Fool 3,
        // 4 Cavaliers x 2 = 8, 4 Knaves x 1 = 4 -> 16 + 8 + 12 + 3 + 8 + 4 = 51; tricks: 78 / 3 = 26 -> 77
        assertEquals(77, params.pointsPerDeal());
    }

    @Test
    public void pointsPerDealFollowTheParameters() {
        params.setParameterValue("kingPoints", 5);
        // 51 + 4 Kings x (5 - 4) = 55, + 26 -> 81
        assertEquals(81, params.pointsPerDeal());
    }

    // ---- player scores ----

    /**
     * Mid-deal position: dealer 2. Scarto {sword King, World, coin 3}; cardsWon: p0 {coin Cavalier, Pagat,
     * trump 5}, p1 nothing, p2 {cup Queen, cup 2, baton Knave}. The scarto is arranged last (spilling to hand 0).
     */
    ScartoGameState arrangeScores() {
        ScartoForwardModel fm = new ScartoForwardModel();
        ScartoGameState state = newState(23, fm);
        setCardsWon(state, 0, coin(CAVALIER), trump(PAGAT), trump(5));
        setCardsWon(state, 2, cup(QUEEN), cup(2), baton(KNAVE));
        setScarto(state, state.playerHands.get(0), sword(KING), trump(WORLD), coin(3));
        assertAllCardsPresent(state);
        assertEquals(2, state.getDealer());
        assertTrue(state.isNotTerminal());
        return state;
    }

    @Test
    public void aNonDealersScoreIsTheirCardsWon() {
        ScartoGameState state = arrangeScores();
        // p0: Cavalier 2 + Pagat 4 + 5 0 = 6, plus (3 + 1) / 3 = 1 -> 7 (the scarto would make it 12)
        assertEquals(7, state.getGameScore(0), 1e-9);
        // p1: nothing won -> (0 + 1) / 3 = 0
        assertEquals(0, state.getGameScore(1), 1e-9);
    }

    @Test
    public void theDealersScoreIncludesTheScarto() {
        ScartoGameState state = arrangeScores();
        // p2 = cardsWon + scarto, 6 cards: Queen 3 + 2 0 + Knave 1 + King 4 + World 0 + 3 0 = 8,
        // plus (6 + 1) / 3 = 2 -> 10 (without the scarto: 4 + 1 = 5)
        assertEquals(10, state.getGameScore(2), 1e-9);
    }

    @Test
    public void scoresUseTheGamesParameters() {
        ScartoGameState state = arrangeScores();
        ((ScartoParameters) state.getGameParameters()).setParameterValue("kingPoints", 5);
        // the scarto's King now scores 5: 8 + 1 + 2 -> 11
        assertEquals(11, state.getGameScore(2), 1e-9);
    }

    @Test
    public void theHeuristicMidDealIsTheScoreOverTheDealsPoints() {
        ScartoGameState state = arrangeScores();
        assertEquals(7.0 / 77, state.getHeuristicScore(0), 1e-9);
        assertEquals(0.0, state.getHeuristicScore(1), 1e-9);
        assertEquals(10.0 / 77, state.getHeuristicScore(2), 1e-9);
    }
}
