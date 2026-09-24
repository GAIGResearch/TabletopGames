package games.klaverjassen;

import core.components.FrenchCard;
import org.junit.Test;

import static core.components.FrenchCard.Suite.*;
import static games.tricktaking.TrickTakingTestUtils.cards;
import static org.junit.Assert.assertEquals;

/**
 * KlaverjassenUtils.roem over the four cards of a trick, with the default values unless a test changes them.
 */
public class KlaverjassenRoemTest {

    private final KlaverjassenParameters params = new KlaverjassenParameters();

    private int roem(FrenchCard.Suite trumps, String... codes) {
        return KlaverjassenUtils.roem(cards(codes), trumps, params);
    }

    // ---- runs ----

    @Test
    public void aRunOfThreeInAPlainSuitIsTwenty() {
        // 8-9-10 of Hearts, Clubs trumps; the 7 of Spades is not in the run
        assertEquals(20, roem(Clubs, "8H", "9H", "10H", "7S"));
    }

    @Test
    public void theOrderOfPlayDoesNotMatter() {
        assertEquals(20, roem(Clubs, "10H", "7S", "8H", "9H"));
        assertEquals(20, roem(Clubs, "7S", "9H", "10H", "8H"));
    }

    @Test
    public void runsFollowTheSequenceOrderNotTheTrickRanking() {
        // A K Q J 10 9 8 7: 10-J-Q, J-Q-K, 9-10-J and Q-K-A are runs, although 10-J-Q, 9-10-J and Q-K-A are not
        // consecutive in the plain-suit ranking A 10 K Q J 9 8 7
        assertEquals(20, roem(Hearts, "10S", "JS", "QS", "7D"));
        assertEquals(20, roem(Hearts, "JS", "QS", "KS", "7D"));
        assertEquals(20, roem(Hearts, "9S", "10S", "JS", "7D"));
        assertEquals(20, roem(Hearts, "QS", "KS", "AS", "7D"));
    }

    @Test
    public void cardsConsecutiveOnlyInTheTrickRankingAreNoRun() {
        // A 10 K of a plain suit: consecutive in the ranking A 10 K Q J 9 8 7, not in the sequence
        assertEquals(0, roem(Hearts, "AS", "10S", "KS", "7D"));
        // J 9 A of trumps: consecutive in the trump ranking J 9 A 10 K Q 8 7, not in the sequence
        assertEquals(0, roem(Hearts, "JH", "9H", "AH", "7D"));
    }

    @Test
    public void aRunOfFourIsFiftyAndNotAlsoTwoRunsOfThree() {
        // 7-8-9-10 of Diamonds: 50, not 50 + 20 + 20 nor 20 + 20
        assertEquals(50, roem(Clubs, "7D", "8D", "9D", "10D"));
        // J-Q-K-A of Diamonds (not trumps, so the King and Queen are not stuk): 50
        assertEquals(50, roem(Clubs, "AD", "JD", "KD", "QD"));
    }

    @Test
    public void runsInTrumpsCountTheSameAsInPlainSuits() {
        // 7-8-9 of trumps: 20 (the trump ranking J 9 A 10 K Q 8 7 is irrelevant)
        assertEquals(20, roem(Hearts, "7H", "8H", "9H", "AS"));
        // 9-10-J of trumps: 20
        assertEquals(20, roem(Hearts, "9H", "JH", "10H", "AS"));
        // 7-8-9-10 of trumps: 50
        assertEquals(50, roem(Hearts, "7H", "8H", "9H", "10H"));
    }

    @Test
    public void consecutiveNumbersOfMixedSuitsAreNoRun() {
        // 7 8 9 10 in four suits
        assertEquals(0, roem(Clubs, "7H", "8D", "9S", "10C"));
        // 8-10 of Hearts with the 9 of Spades between them: only a gap in Hearts
        assertEquals(0, roem(Clubs, "8H", "9S", "10H", "JD"));
        // J Q K with the Queen in another suit
        assertEquals(0, roem(Clubs, "JH", "QD", "KH", "7S"));
    }

    @Test
    public void cardsWithAGapOrOnlyTwoInARowAreNoRun() {
        // 7-8 and 10-J of Hearts: two pairs, the 9 missing
        assertEquals(0, roem(Clubs, "7H", "8H", "10H", "JH"));
        // 8, 10 and Queen of Spades
        assertEquals(0, roem(Clubs, "8S", "10S", "QS", "7H"));
        // two in a row only: 9-10 of Diamonds
        assertEquals(0, roem(Clubs, "9D", "10D", "7H", "AS"));
    }

    @Test
    public void theSequenceDoesNotWrapFromAceToSeven() {
        assertEquals(0, roem(Clubs, "AH", "7H", "8H", "QS"));
        assertEquals(0, roem(Clubs, "KH", "AH", "7H", "QS"));
        // K-A-7-8 of Hearts: K-A and 7-8 are pairs, no run
        assertEquals(0, roem(Clubs, "KH", "AH", "7H", "8H"));
    }

    // ---- stuk ----

    @Test
    public void theKingAndQueenOfTrumpsAreStukForTwenty() {
        assertEquals(20, roem(Spades, "KS", "QS", "7H", "8D"));
        assertEquals(20, roem(Spades, "7H", "QS", "8D", "KS"));
    }

    @Test
    public void theKingAndQueenOfAPlainSuitAreNotStuk() {
        // Spades K+Q with Hearts trumps
        assertEquals(0, roem(Hearts, "KS", "QS", "7H", "8D"));
        // a King of trumps with the Queen of another suit
        assertEquals(0, roem(Hearts, "KH", "QS", "7C", "8D"));
    }

    @Test
    public void stukIsAddedToARunOfThree() {
        // K-Q-J of trumps: run 20 + stuk 20 = 40
        assertEquals(40, roem(Spades, "KS", "QS", "JS", "7H"));
        // Q-K-A of trumps: run 20 + stuk 20 = 40
        assertEquals(40, roem(Spades, "AS", "7H", "KS", "QS"));
    }

    @Test
    public void stukIsAddedToARunOfFour() {
        // J-Q-K-A of trumps: run 50 + stuk 20 = 70
        assertEquals(70, roem(Spades, "JS", "QS", "KS", "AS"));
        // 10-J-Q-K of trumps: run 50 + stuk 20 = 70
        assertEquals(70, roem(Spades, "10S", "KS", "JS", "QS"));
    }

    @Test
    public void aRunInAPlainSuitWithoutTheTrumpKingAndQueenHasNoStuk() {
        // J-Q-K of Spades with Hearts trumps: run only, 20
        assertEquals(20, roem(Hearts, "JS", "QS", "KS", "7H"));
    }

    // ---- four of a kind ----

    @Test
    public void fourKingsQueensAcesOrTensAreAHundred() {
        // four Kings, Spades trumps: 100 (only one trump King and no trump Queen, so no stuk)
        assertEquals(100, roem(Spades, "KH", "KD", "KC", "KS"));
        assertEquals(100, roem(Spades, "QH", "QD", "QC", "QS"));
        assertEquals(100, roem(Spades, "AH", "AD", "AC", "AS"));
        assertEquals(100, roem(Spades, "10H", "10D", "10C", "10S"));
    }

    @Test
    public void fourJacksAreTwoHundred() {
        assertEquals(200, roem(Spades, "JH", "JD", "JC", "JS"));
    }

    @Test
    public void fourNinesEightsOrSevensAreNothing() {
        assertEquals(0, roem(Spades, "9H", "9D", "9C", "9S"));
        assertEquals(0, roem(Spades, "8H", "8D", "8C", "8S"));
        assertEquals(0, roem(Spades, "7H", "7D", "7C", "7S"));
    }

    @Test
    public void threeOfAKindIsNothing() {
        assertEquals(0, roem(Spades, "JH", "JD", "JC", "7S"));
        assertEquals(0, roem(Spades, "AH", "AD", "AC", "7S"));
    }

    // ---- values from the parameters ----

    @Test
    public void theValuesComeFromTheParameters() {
        params.setParameterValue("runOfThreeBonus", 30);
        params.setParameterValue("runOfFourBonus", 60);
        params.setParameterValue("stukBonus", 25);
        params.setParameterValue("fourOfAKindBonus", 150);
        params.setParameterValue("fourJacksBonus", 250);

        assertEquals(30, roem(Clubs, "8H", "9H", "10H", "7S"));
        assertEquals(60, roem(Clubs, "7D", "8D", "9D", "10D"));
        assertEquals(25, roem(Spades, "KS", "QS", "7H", "8D"));
        // K-Q-J of trumps: 30 + 25 = 55; J-Q-K-A of trumps: 60 + 25 = 85
        assertEquals(55, roem(Spades, "KS", "QS", "JS", "7H"));
        assertEquals(85, roem(Spades, "JS", "QS", "KS", "AS"));
        assertEquals(150, roem(Spades, "KH", "KD", "KC", "KS"));
        assertEquals(250, roem(Spades, "JH", "JD", "JC", "JS"));
    }
}
