package games.klaverjassen;

import core.components.FrenchCard;
import org.junit.Test;

import java.util.List;

import static core.components.FrenchCard.Suite.*;
import static games.klaverjassen.KlaverjassenTestUtils.trick;
import static games.tricktaking.TrickTakingTestUtils.cards;
import static org.junit.Assert.*;

/**
 * KlaverjassenCardOrder.rank and the trick winner it gives through Trick.winner.
 */
public class KlaverjassenCardOrderTest {

    private static final String[] RANKS_TRUMPS = {"J", "9", "A", "10", "K", "Q", "8", "7"};   // highest first
    private static final String[] RANKS_PLAIN = {"A", "10", "K", "Q", "J", "9", "8", "7"};    // highest first

    private static List<FrenchCard> suitInOrder(String[] ranks, String suitCode) {
        String[] codes = new String[ranks.length];
        for (int i = 0; i < ranks.length; i++)
            codes[i] = ranks[i] + suitCode;
        return cards(codes);
    }

    private static void assertStrictlyDescending(KlaverjassenCardOrder order, List<FrenchCard> highestFirst) {
        for (int i = 0; i + 1 < highestFirst.size(); i++) {
            FrenchCard higher = highestFirst.get(i), lower = highestFirst.get(i + 1);
            assertTrue(higher + " should outrank " + lower + " with " + order.trumps() + " trumps",
                    order.rank(higher) > order.rank(lower));
        }
    }

    @Test
    public void trumpsRankJackNineAceTenKingQueenEightSeven() {
        String[] suitCodes = {"H", "D", "C", "S"};
        FrenchCard.Suite[] suits = {Hearts, Diamonds, Clubs, Spades};
        for (int s = 0; s < 4; s++)
            assertStrictlyDescending(new KlaverjassenCardOrder(suits[s]), suitInOrder(RANKS_TRUMPS, suitCodes[s]));
    }

    @Test
    public void otherSuitsRankAceTenKingQueenJackNineEightSeven() {
        KlaverjassenCardOrder heartsTrumps = new KlaverjassenCardOrder(Hearts);
        for (String suitCode : new String[]{"D", "C", "S"})
            assertStrictlyDescending(heartsTrumps, suitInOrder(RANKS_PLAIN, suitCode));
        // and Hearts ranks as a plain suit when another suit is trumps
        assertStrictlyDescending(new KlaverjassenCardOrder(Spades), suitInOrder(RANKS_PLAIN, "H"));
    }

    @Test
    public void aTrumpSevenBeatsAPlainAce() {
        // hearts trumps, player 2 leads the Ace of Spades; player 3 trumps with the 7 of Hearts
        assertEquals(3, trick(Hearts, 2, "AS", "7H", "KS", "10S").winner(Hearts));
    }

    @Test
    public void theTrumpNineBeatsTheTrumpAceAndTheJackBeatsTheNine() {
        // hearts trumps led by player 0: 9H (player 1) beats AH, 10H and KH
        assertEquals(1, trick(Hearts, 0, "AH", "9H", "10H", "KH").winner(Hearts));
        // led by player 1: JH (player 2) beats 9H, AH and 10H
        assertEquals(2, trick(Hearts, 1, "9H", "JH", "AH", "10H").winner(Hearts));
        // clubs trumps, diamonds led by player 0: the trump Ace (player 2) beats the trump 10 and King
        assertEquals(2, trick(Clubs, 0, "AD", "10C", "AC", "KC").winner(Clubs));
        // a trump Queen (player 1) beats a trump 8 (player 2), though 8 is played later
        assertEquals(1, trick(Clubs, 0, "AD", "QC", "8C", "10D").winner(Clubs));
    }

    @Test
    public void inAPlainSuitTheTenBeatsTheKingAndTheJack() {
        // hearts trumps, spades led by player 0: 10S (player 1) beats KS, JS and QS
        // (by FrenchCard number the King would win)
        assertEquals(1, trick(Hearts, 0, "KS", "10S", "JS", "QS").winner(Hearts));
    }

    @Test
    public void aCardOfAnotherPlainSuitNeverWins() {
        // hearts trumps, player 3 leads 7S; player 0's Ace of Diamonds is off-suit; 9S (player 2) is the highest spade
        assertEquals(2, trick(Hearts, 3, "7S", "AD", "8S", "9S").winner(Hearts));
    }
}
