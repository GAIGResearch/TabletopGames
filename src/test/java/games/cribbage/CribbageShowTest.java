package games.cribbage;

import core.components.FrenchCard;
import games.cribbage.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static games.cribbage.CribbageTestUtils.*;
import static org.junit.Assert.*;

/**
 * The show: CribbageUtils.showScore on known hands (4 cards + starter), and the show scored through fm.next
 * when the play ends. Pip values: A 1, court 10. Ranks for runs: Ace low.
 */
public class CribbageShowTest {

    CribbageParameters params;

    @Before
    public void setUp() {
        params = new CribbageParameters();
    }

    /** Hand (or crib) of four cards, then the starter. */
    private int hand(String c1, String c2, String c3, String c4, String starter) {
        return CribbageUtils.showScore(cards(c1, c2, c3, c4), card(starter), false, params);
    }

    private int crib(String c1, String c2, String c3, String c4, String starter) {
        return CribbageUtils.showScore(cards(c1, c2, c3, c4), card(starter), true, params);
    }

    // ---------------- zero and fifteens ----------------

    @Test
    public void aHandWithNothingScoresZero() {
        // pips 1 3 7 9 10: no subset sums to 15 (pairs max 19 without 15; 1+x+y needs x+y=14, 3+x+y needs 12,
        // 7+x+y needs 8: none; four cards = 30 - one card, never 15); no pair; ranks A 3 7 9 K, no run;
        // suits C D H C no flush; no Jack. This is ZERO_CRIB + ZERO_STARTER, used by the round-end tests.
        assertEquals(0, hand("AC", "3D", "7H", "9C", "KS"));
        assertEquals(0, crib("AC", "3D", "7H", "9C", "KS"));
        params.setParameterValue("runsIncludeStarter", true);
        params.setParameterValue("cribFlushNeedsStarter", true);
        assertEquals(0, hand("AC", "3D", "7H", "9C", "KS"));
        assertEquals(0, crib("AC", "3D", "7H", "9C", "KS"));
    }

    @Test
    public void everyCombinationMakingFifteenScoresTwo() {
        // K+5, Q+5, K+3+2, Q+3+2 = 4 fifteens = 8. No pair; runs: K Q 5 2 none (and 2 3 5 with the starter
        // is not a run either); no flush; no Jack.
        assertEquals(8, hand("KH", "QD", "5C", "2S", "3H"));
    }

    @Test
    public void fifteensUseTheStarterAndCountSubsetsOfEverySize() {
        // pips 5 5 5 5 10 (4 fives + a King): K+5 x4, 5+5+5 x4 = 8 fifteens = 16; four fives pair = 12.
        // 5+5+5 made from four 5s is C(4,3)=4 subsets. Total 28.
        assertEquals(28, hand("5H", "5D", "5C", "5S", "KH"));
    }

    // ---------------- pairs ----------------

    @Test
    public void aPairScoresTwoAndCanUseTheStarter() {
        // pips 8 8 1 3 10: 8+1+... no subset makes 15 (8+3+1=12, 10+3+1=14, 8+8=16); no run; no flush; no Jack.
        assertEquals(2, hand("8H", "8D", "AC", "3S", "QH"));
        // the same five cards with one 8 as the starter
        assertEquals(2, hand("8H", "AC", "3S", "QH", "8D"));
    }

    @Test
    public void twoPairsScoreFour() {
        // pips 8 8 3 3 10: no fifteen (8+3+3=14, 10+3=13, 8+8=16); pairs 8-8 and 3-3 = 4
        assertEquals(4, hand("8H", "8D", "3C", "3S", "QH"));
    }

    @Test
    public void threeOfARankScorePairRoyalSixWithOrWithoutTheStarter() {
        // pips 8 8 8 1 3: no fifteen (8+3+1=12, 8+8=16); three 8s = 6; no run; no flush
        assertEquals(6, hand("8H", "8D", "8C", "AS", "3H"));
        assertEquals(6, hand("8H", "8D", "AS", "3H", "8C"));
    }

    @Test
    public void fourOfARankScoreDoublePairRoyalTwelve() {
        // pips 8 8 8 8 1: no fifteen (8+... needs 7); four 8s = 12
        assertEquals(12, hand("8H", "8D", "8C", "8S", "AH"));
        assertEquals(12, hand("8H", "8D", "8C", "AH", "8S"));
    }

    // ---------------- runs ----------------

    @Test
    public void aRunOfThreeScoresThree() {
        // pips 9 10 10 2 7: no fifteen (9+2+... needs 4, 7+2+... needs 6, 10+... needs 5); no pair;
        // run 9-10-J = 3 (7 does not extend it: no 8); no flush; JC with a Heart starter is not his nobs
        assertEquals(3, hand("9H", "10D", "JC", "2S", "7H"));
        params.setParameterValue("runsIncludeStarter", true);
        assertEquals(3, hand("9H", "10D", "JC", "2S", "7H"));
    }

    @Test
    public void aRunOfFourScoresFourNotTwoRunsOfThree() {
        // pips 9 10 10 10 2: no fifteen (2+... needs 13; 9+... needs 6); run 9-10-J-Q = 4; JC not nobs (starter H)
        assertEquals(4, hand("9H", "10D", "JC", "QS", "2H"));
    }

    @Test
    public void aceIsLowInRuns() {
        // A-2-3 is a run: pips 1 2 3 8 6: fifteens 1+8+6 only (two cards: none; four cards = 20 - x, x=5 absent)
        // = 2; run A-2-3 = 3; total 5
        assertEquals(5, hand("AH", "2D", "3C", "8S", "6H"));
        // Q-K-A is not a run: pips 10 10 1 2 7: no fifteen (10+... needs 5: 1+2=3; 7+... needs 8: none;
        // 1+2+... needs 12: none); no pair; A-2 and Q-K are only runs of 2; total 0
        assertEquals(0, hand("QH", "KD", "AC", "2S", "7H"));
    }

    @Test
    public void aDoubleRunInTheHandScoresEachRun() {
        // 6-7-7-8 + K: fifteens 7+8 twice = 4; pair 7-7 = 2; runs 6-7-8 twice = 6; total 12 (same with the
        // starter in runs: K does not extend them)
        assertEquals(12, hand("6H", "7D", "7C", "8S", "KH"));
        params.setParameterValue("runsIncludeStarter", true);
        assertEquals(12, hand("6H", "7D", "7C", "8S", "KH"));
    }

    @Test
    public void runsIgnoreTheStarterByDefault() {
        // 4 6 10 K + 5: fifteens 5+10, 5+K, 4+5+6 = 3 = 6; no pair; hand ranks 4 6 10 K have no run.
        // runsIncludeStarter false: 6.   true: 4-5-6 = 3 more, 9.
        assertEquals(6, hand("4H", "6D", "10C", "KS", "5H"));
        params.setParameterValue("runsIncludeStarter", true);
        assertEquals(9, hand("4H", "6D", "10C", "KS", "5H"));
    }

    @Test
    public void theStarterCanExtendARunOnlyWhenRunsIncludeTheStarter() {
        // 4 5 6 9 + 3: fifteens 6+9, 4+5+6 = 2 = 4 (3+x+y needs x+y=12 from 4 5 6 9: none); no pair.
        // default: run 4-5-6 = 3, total 7.   runsIncludeStarter: run 3-4-5-6 = 4 (not 4-5-6 as well), total 8.
        assertEquals(7, hand("4H", "5D", "6C", "9S", "3H"));
        params.setParameterValue("runsIncludeStarter", true);
        assertEquals(8, hand("4H", "5D", "6C", "9S", "3H"));
    }

    @Test
    public void aDoubleDoubleRunNeedsTheStarter() {
        // 4 5 5 6 + 6: fifteens 4+5+6 with 2 fives x 2 sixes = 4 = 8 (four cards = 26 - x, x=11 absent;
        // no two cards make 15); pairs 5-5 and 6-6 = 4.
        // default: runs 4-5-6 in the hand, two of them = 6; total 8 + 4 + 6 = 18 (the plan's 16 was wrong).
        // runsIncludeStarter: 4-5-6 in 2 x 2 = 4 ways = 12; total 8 + 4 + 12 = 24.
        assertEquals(18, hand("4H", "5D", "5C", "6S", "6H"));
        params.setParameterValue("runsIncludeStarter", true);
        assertEquals(24, hand("4H", "5D", "5C", "6S", "6H"));
    }

    @Test
    public void twoPairsInsideARunGiveFourRunsWithTheStarter() {
        // 5 5 6 7 + 7: no fifteen (no two make 15; 5+5+5? no; 5+6+... needs 4; total 30, four cards = 30 - x,
        // x=15 absent); pairs 5-5, 7-7 = 4.
        // default: hand 5 5 6 7 has 5-6-7 twice = 6; total 10.
        // runsIncludeStarter: 5-6-7 in 2 x 2 = 4 ways = 12; total 16.
        assertEquals(10, hand("5H", "5D", "6C", "7S", "7H"));
        params.setParameterValue("runsIncludeStarter", true);
        assertEquals(16, hand("5H", "5D", "6C", "7S", "7H"));
    }

    @Test
    public void aTripleRunNeedsTheStarter() {
        // 7 7 8 9 + 7: fifteens 7+8 x3 = 6 (three cards are at least 22); three 7s = 6.
        // default: hand 7 7 8 9 has 7-8-9 twice = 6; total 18.
        // runsIncludeStarter: 7-8-9 three times = 9; total 21.
        assertEquals(18, hand("7H", "7D", "8C", "9S", "7S"));
        params.setParameterValue("runsIncludeStarter", true);
        assertEquals(21, hand("7H", "7D", "8C", "9S", "7S"));
    }

    // ---------------- flush ----------------
    // All pips below are even, so there is never a fifteen; no pairs; no runs (2 4 6 8 10); no Jack.

    @Test
    public void aFourCardHandFlushScoresFourAndFiveWithAMatchingStarter() {
        assertEquals(4, hand("2H", "4H", "6H", "8H", "10S"));
        assertEquals(5, hand("2H", "4H", "6H", "8H", "10H"));
    }

    @Test
    public void threeHandCardsAndTheStarterOfOneSuitAreNotAFlush() {
        assertEquals(0, hand("2H", "4H", "6H", "8S", "10H"));
        assertEquals(0, crib("2H", "4H", "6H", "8S", "10H"));
    }

    @Test
    public void aFourCardCribFlushScoresFourByDefaultAndNothingWhenTheCribNeedsTheStarter() {
        assertEquals(4, crib("2H", "4H", "6H", "8H", "10S"));
        params.setParameterValue("cribFlushNeedsStarter", true);
        assertEquals(0, crib("2H", "4H", "6H", "8H", "10S"));
    }

    @Test
    public void aFiveCardCribFlushScoresFiveEitherWay() {
        assertEquals(5, crib("2H", "4H", "6H", "8H", "10H"));
        params.setParameterValue("cribFlushNeedsStarter", true);
        assertEquals(5, crib("2H", "4H", "6H", "8H", "10H"));
    }

    @Test
    public void cribFlushNeedsStarterDoesNotAffectAHand() {
        params.setParameterValue("cribFlushNeedsStarter", true);
        assertEquals(4, hand("2H", "4H", "6H", "8H", "10S"));
    }

    // ---------------- his nobs ----------------
    // pips 10 2 4 6 8: all even, no fifteen; no pair, no run, no flush.

    @Test
    public void aJackOfTheStartersSuitScoresHisNobs() {
        assertEquals(1, hand("JH", "2C", "4D", "6S", "8H"));
        assertEquals(1, crib("JH", "2C", "4D", "6S", "8H"));
    }

    @Test
    public void aJackOfAnotherSuitIsNotHisNobs() {
        assertEquals(0, hand("JC", "2H", "4D", "6S", "8H"));
    }

    @Test
    public void aJackStarterScoresNothingInTheShow() {
        // his heels is scored when the starter is turned up, not in the show
        assertEquals(0, hand("2H", "4D", "6S", "8C", "JH"));
    }

    // ---------------- combinations and point values ----------------

    @Test
    public void theBestHandScoresTwentyNine() {
        // 5 5 5 J + the 5 of the Jack's suit: fifteens J+5 x4, 5+5+5 x4 = 8 = 16; four 5s = 12; his nobs 1 = 29.
        // No run, no flush: the same with either parameter changed, and as a crib.
        assertEquals(29, hand("5H", "5D", "5C", "JS", "5S"));
        assertEquals(29, crib("5H", "5D", "5C", "JS", "5S"));
        params.setParameterValue("runsIncludeStarter", true);
        params.setParameterValue("cribFlushNeedsStarter", true);
        assertEquals(29, hand("5H", "5D", "5C", "JS", "5S"));
        assertEquals(29, crib("5H", "5D", "5C", "JS", "5S"));
    }

    @Test
    public void flushRunFifteensAndNobsTogether() {
        // hand 3H 4H 5H JH + 6H: fifteens 4+5+6, J+5, 3+... (3+x+y needs 12: none of 4 5 6 10 pairs... 6+... no;
        // 3+4+... needs 8: none; 3+5+... needs 7: none; 3+6+... needs 6: none; four cards = 28 - x, x=13 absent)
        // = 2 = 4; no pair; run 3-4-5 in the hand = 3; flush 4 + starter 1 = 5; his nobs 1. Total 13.
        // runsIncludeStarter: 3-4-5-6 = 4, total 14.
        assertEquals(13, hand("3H", "4H", "5H", "JH", "6H"));
        params.setParameterValue("runsIncludeStarter", true);
        assertEquals(14, hand("3H", "4H", "5H", "JH", "6H"));
    }

    @Test
    public void pointValuesComeFromTheParameters() {
        // 29 hand with fifteenPoints 1, doublePairRoyalPoints 6, hisNobsPoints 2: 8 x 1 + 6 + 2 = 16
        params.setParameterValue("fifteenPoints", 1);
        params.setParameterValue("doublePairRoyalPoints", 6);
        params.setParameterValue("hisNobsPoints", 2);
        assertEquals(16, hand("5H", "5D", "5C", "JS", "5S"));
        // 8 8 8 3 3 (no fifteen, no run): default pair royal 6 + pair 2 = 8; with pairRoyalPoints 4, pairPoints 1: 5
        assertEquals(8, hand("8H", "8D", "3C", "3S", "8C"));
        params.setParameterValue("pairRoyalPoints", 4);
        params.setParameterValue("pairPoints", 1);
        assertEquals(5, hand("8H", "8D", "3C", "3S", "8C"));
        // flushPoints 3: four-card flush 3, five-card 4
        params.setParameterValue("flushPoints", 3);
        assertEquals(3, hand("2H", "4H", "6H", "8H", "10S"));
        assertEquals(4, hand("2H", "4H", "6H", "8H", "10H"));
    }

    // ---------------- through fm.next ----------------

    /** Records each scoreShowPart call (player, cards, isCrib) and the scores after it. */
    static class RecordingForwardModel extends CribbageForwardModel {
        final List<String> calls = new ArrayList<>();

        @Override
        void scoreShowPart(CribbageGameState state, int player, List<FrenchCard> cards, boolean isCrib) {
            super.scoreShowPart(state, player, cards, isCrib);
            calls.add(player + " " + new HashSet<>(cards) + " " + isCrib + " -> " + state.getScore(0) + "/" + state.getScore(1));
        }
    }

    @Test
    public void theShowScoresNonDealerThenDealerThenCribWhenTheLastCardIsPlayed() {
        params.setRandomSeed(42);
        CribbageGameState state = new CribbageGameState(params, 2);
        RecordingForwardModel fm = new RecordingForwardModel();
        fm.setup(state);
        // round 1: player 0 deals, player 1 is the non-dealer. Starter KS, crib ZERO_CRIB (scores 0).
        arrangePlay(state, cards("JS"), cards("8H"));
        playedEarlier(state, 1, card("5H"), card("5D"), card("5C"));
        playedEarlier(state, 0, card("2H"), card("4H"), card("6H"));

        fm.next(state, new PlayCard(card("JS")));   // 10
        assertEquals(0, state.getScore(0));
        assertEquals(0, state.getScore(1));
        fm.next(state, new PlayCard(card("8H")));   // 18, no 15 / pair / run; the last card: 1 to player 0

        // non-dealer 5H 5D 5C JS + KS: fifteens J+5 x3, K+5 x3, 5+5+5 = 7 = 14; pair royal 6; his nobs (JS, KS) 1 = 21
        // dealer 2H 4H 6H 8H + KS: all even, no fifteen, no pair/run; flush 4 = 4 (+ 1 last card before the show)
        // crib AC 3D 7H 9C + KS = 0
        assertEquals(List.of(
                "1 " + new HashSet<>(cards("5H", "5D", "5C", "JS")) + " false -> 1/21",
                "0 " + new HashSet<>(cards("2H", "4H", "6H", "8H")) + " false -> 5/21",
                "0 " + new HashSet<>(ZERO_CRIB) + " true -> 5/21"),
                fm.calls);
        assertEquals(5, state.getScore(0));
        assertEquals(21, state.getScore(1));
        // then the round ends as before
        assertEquals(1, state.getRoundCounter());
        assertAllCardsPresent(state);
    }
}
