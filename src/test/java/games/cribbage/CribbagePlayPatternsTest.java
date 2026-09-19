package games.cribbage;

import games.cribbage.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;

import static games.cribbage.CribbageUtils.playPairPoints;
import static games.cribbage.CribbageUtils.playRunPoints;
import static games.cribbage.CribbageTestUtils.*;
import static org.junit.Assert.*;

/**
 * Pairs and runs in the play, scored by the player of the last card, within the current count only.
 * Unit tests call CribbageUtils.playPairPoints and playRunPoints on known sequences; the rest drive the play with fm.next.
 * Player 0 deals the first round, so player 1 is the non-dealer and leads.
 * Every fm.next scenario leaves a card in at least one hand, so the play never ends and the show is not scored.
 */
public class CribbagePlayPatternsTest {

    CribbageParameters params;
    CribbageGameState state;
    CribbageForwardModel fm;

    @Before
    public void setup() {
        params = new CribbageParameters();
        params.setRandomSeed(42);
        state = new CribbageGameState(params, 2);
        fm = new CribbageForwardModel();
        fm.setup(state);
    }

    // ---------- pairs: unit ----------

    @Test
    public void aPairScoresTwo() {
        assertEquals(2, playPairPoints(cards("5H", "5D"), params));
    }

    @Test
    public void threeOfARankInARowScoresSix() {
        assertEquals(6, playPairPoints(cards("5H", "5D", "5C"), params));
    }

    @Test
    public void fourOfARankInARowScoresTwelve() {
        assertEquals(12, playPairPoints(cards("5H", "5D", "5C", "5S"), params));
    }

    @Test
    public void aPairBrokenByAnotherCardScoresNothing() {
        assertEquals(0, playPairPoints(cards("5H", "6D", "5C"), params));
    }

    @Test
    public void onlyTheCardsImmediatelyBeforeTheLastCountTowardsAPair() {
        // the first Five is cut off by the Nine: a pair (2), not a pair royal
        assertEquals(2, playPairPoints(cards("5S", "9D", "5H", "5C"), params));
        // the earlier pair does not score again when an unmatched card is played
        assertEquals(0, playPairPoints(cards("5H", "5D", "6C"), params));
    }

    @Test
    public void cardsOfEqualPipValueButDifferentRankAreNotAPair() {
        assertEquals(0, playPairPoints(cards("10H", "KD"), params));
        assertEquals(0, playPairPoints(cards("JH", "QD"), params));
    }

    @Test
    public void aSingleCardIsNotAPair() {
        assertEquals(0, playPairPoints(cards("5H"), params));
    }

    @Test
    public void pairPointsComeFromTheParameters() {
        params.setParameterValue("pairPoints", 1);
        params.setParameterValue("pairRoyalPoints", 3);
        params.setParameterValue("doublePairRoyalPoints", 5);
        assertEquals(1, playPairPoints(cards("8H", "8D"), params));
        assertEquals(3, playPairPoints(cards("8H", "8D", "8C"), params));
        assertEquals(5, playPairPoints(cards("8H", "8D", "8C", "8S"), params));
    }

    // ---------- runs: unit ----------

    @Test
    public void threeConsecutiveRanksInAnyOrderScoreThree() {
        assertEquals(3, playRunPoints(cards("3H", "5D", "4C")));
        assertEquals(3, playRunPoints(cards("3H", "4D", "5C")));
    }

    @Test
    public void extendingARunScoresItsFullLength() {
        // 3-5-4 then 6: the last four are 3-4-5-6
        assertEquals(4, playRunPoints(cards("3H", "5D", "4C", "6S")));
        assertEquals(5, playRunPoints(cards("7H", "3D", "5C", "4S", "6H")));
    }

    @Test
    public void theLongestRunCountsEvenWhenTheShorterEndingIsNotARun() {
        // last three 3-6-4 are not a run, but the last four 5-3-6-4 are 3-4-5-6
        assertEquals(4, playRunPoints(cards("5H", "3D", "6C", "4S")));
    }

    @Test
    public void aCardBeforeTheRunDoesNotExtendIt() {
        // 9 then 4-5-3: only the last three are a run
        assertEquals(3, playRunPoints(cards("9H", "4D", "5C", "3S")));
    }

    @Test
    public void aRepeatedRankBreaksARun() {
        assertEquals(0, playRunPoints(cards("3H", "4D", "4C", "5S")));
        // 4-5-4: the last three repeat a rank, so neither three nor four cards are a run
        assertEquals(0, playRunPoints(cards("3H", "4D", "5C", "4S")));
        // a duplicate filling the span: max - min is N - 1 but a rank is missing
        assertEquals(0, playRunPoints(cards("2H", "4D", "4C", "5S")));
        assertEquals(0, playRunPoints(cards("3H", "5D", "3C")));
    }

    @Test
    public void aRepeatedRankBeforeTheRunDoesNotSpoilIt() {
        // 5 3 4 5: the last three 3-4-5 are a run; the first Five is outside it
        assertEquals(3, playRunPoints(cards("5H", "3D", "4C", "5S")));
    }

    @Test
    public void aGapInTheRanksIsNotARun() {
        assertEquals(0, playRunPoints(cards("3H", "5D", "9C", "4S")));
        assertEquals(0, playRunPoints(cards("3H", "5D", "6C")));
    }

    @Test
    public void twoCardsAreNotARun() {
        assertEquals(0, playRunPoints(cards("3H", "4D")));
    }

    @Test
    public void aceIsLowInARun() {
        assertEquals(3, playRunPoints(cards("AH", "2D", "3C")));
        assertEquals(3, playRunPoints(cards("2H", "3D", "AC")));
        assertEquals(0, playRunPoints(cards("QH", "KD", "AC")));
        assertEquals(0, playRunPoints(cards("KH", "AD", "2C")));
    }

    @Test
    public void courtCardsRunByRank() {
        // J-Q-K all have pip value 10 but ranks 11-12-13
        assertEquals(3, playRunPoints(cards("QH", "JD", "KC")));
        assertEquals(4, playRunPoints(cards("10S", "QH", "JD", "KC")));
    }

    // ---------- through the forward model ----------

    @Test
    public void pairsInThePlayScoreForThePlayerOfTheCard() {
        arrangePlay(state, cards("2C", "2H", "10D"), cards("2D", "2S", "10S"));
        fm.next(state, new PlayCard(card("2C")));   // total 2
        fm.next(state, new PlayCard(card("2D")));   // total 4: pair, player 0 +2
        assertEquals(2, state.getScore(0));
        assertEquals(0, state.getScore(1));
        fm.next(state, new PlayCard(card("2H")));   // total 6: pair royal, player 1 +6
        assertEquals(6, state.getScore(1));
        fm.next(state, new PlayCard(card("2S")));   // total 8: double pair royal, player 0 +12
        assertEquals(2 + 12, state.getScore(0));
        assertEquals(6, state.getScore(1));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void runsInThePlayScoreForThePlayerOfTheCard() {
        arrangePlay(state, cards("2C", "3H", "QH"), cards("4D", "5S", "QD"));
        fm.next(state, new PlayCard(card("2C")));   // total 2
        fm.next(state, new PlayCard(card("4D")));   // total 6
        fm.next(state, new PlayCard(card("3H")));   // total 9: run 2-4-3, player 1 +3
        assertEquals(3, state.getScore(1));
        assertEquals(0, state.getScore(0));
        fm.next(state, new PlayCard(card("5S")));   // total 14: run 2-3-4-5, player 0 +4
        assertEquals(4, state.getScore(0));
        assertEquals(3, state.getScore(1));
    }

    @Test
    public void aFifteenThatIsAlsoAPairScoresBoth() {
        arrangePlay(state, cards("3H", "6H", "QH"), cards("6S", "QD"));
        fm.next(state, new PlayCard(card("3H")));   // total 3
        fm.next(state, new PlayCard(card("6S")));   // total 9
        fm.next(state, new PlayCard(card("6H")));   // total 15 and a pair: 1 + 2
        assertEquals(1 + 2, state.getScore(1));
        assertEquals(0, state.getScore(0));
    }

    @Test
    public void aFifteenThatIsAlsoARunScoresBoth() {
        arrangePlay(state, cards("4C", "5H", "QH"), cards("6D", "QD"));
        fm.next(state, new PlayCard(card("4C")));   // total 4
        fm.next(state, new PlayCard(card("6D")));   // total 10
        fm.next(state, new PlayCard(card("5H")));   // total 15 and run 4-6-5: 1 + 3
        assertEquals(1 + 3, state.getScore(1));
        assertEquals(0, state.getScore(0));
    }

    @Test
    public void thirtyOneThatIsAlsoAPairRoyalScoresBoth() {
        arrangePlay(state, cards("10D"), cards("8S", "4H"));
        playedInCount(state, 1, card("7C"));
        playedInCount(state, 0, card("8D"));
        playedInCount(state, 1, card("8H"));
        state.setTurnOwner(0);
        // 7 + 8 + 8 = 23; the third Eight makes 31 and a pair royal: 2 + 6
        fm.next(state, new PlayCard(card("8S")));
        assertEquals(2 + 6, state.getScore(0));
        assertEquals(0, state.getScore(1));
        assertTrue(state.getPlaySequence().isEmpty());
    }

    @Test
    public void noRunOrPairAcrossAThirtyOneReset() {
        arrangePlay(state, cards("4H", "QH"), cards("6S", "4C"));
        playedInCount(state, 1, card("KH"));
        playedInCount(state, 0, card("10D"));
        playedInCount(state, 1, card("5C"));
        state.setTurnOwner(0);
        fm.next(state, new PlayCard(card("6S")));   // 10 + 10 + 5 + 6 = 31: player 0 +2, new count
        assertEquals(2, state.getScore(0));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("4H")));   // 5-6-4 would be a run, but the count was reset
        assertEquals(0, state.getScore(1));
        fm.next(state, new PlayCard(card("4C")));   // pair within the new count: player 0 +2
        assertEquals(2 + 2, state.getScore(0));
        assertEquals(0, state.getScore(1));
    }

    @Test
    public void noPairAcrossAGoReset() {
        arrangePlay(state, cards("2H", "QH"), cards("2C", "QD"));
        playedInCount(state, 1, card("10H"));
        playedInCount(state, 0, card("KD"));
        playedInCount(state, 1, card("8D"));
        state.setTurnOwner(0);
        // 10 + 10 + 8 + 2 = 30: neither can play (2 or Q would pass 31), last card to player 0, new count
        fm.next(state, new PlayCard(card("2C")));
        assertEquals(1, state.getScore(0));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("2H")));   // would pair the Two before the reset
        assertEquals(0, state.getScore(1));
        assertEquals(1, state.getScore(0));
    }
}
