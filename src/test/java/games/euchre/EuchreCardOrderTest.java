package games.euchre;

import core.components.FrenchCard;
import games.tricktaking.Trick;
import org.junit.Test;

import java.util.Map;

import static core.components.FrenchCard.Suite.*;
import static games.tricktaking.TrickTakingTestUtils.card;
import static org.junit.Assert.*;

/**
 * EuchreCardOrder on its own: the suit each card belongs to and the ranking, once trumps are chosen. The Jack of
 * trumps is the right bower; the other Jack of the same colour (Hearts-Diamonds, Spades-Clubs) is the left bower.
 * Ranks are compared, never checked against particular numbers.
 */
public class EuchreCardOrderTest {

    /**
     * Each suit with its partner of the same colour: the suit of the left bower when that suit is trumps.
     */
    static final Map<FrenchCard.Suite, FrenchCard.Suite> SAME_COLOUR =
            Map.of(Hearts, Diamonds, Diamonds, Hearts, Spades, Clubs, Clubs, Spades);

    static final Map<FrenchCard.Suite, String> CODE = Map.of(Hearts, "H", Diamonds, "D", Spades, "S", Clubs, "C");

    /**
     * Asserts that the cards rank strictly in the order given, highest first.
     */
    static void assertDescending(EuchreCardOrder order, String... codes) {
        for (int i = 1; i < codes.length; i++)
            assertTrue(codes[i - 1] + " should outrank " + codes[i] + " with " + order.trumps() + " trumps",
                    order.rank(card(codes[i - 1])) > order.rank(card(codes[i])));
    }

    @Test
    public void bothBowersBelongToTheTrumpSuitAndTheOtherJacksToTheirOwn() {
        for (FrenchCard.Suite trumps : FrenchCard.Suite.values()) {
            EuchreCardOrder order = new EuchreCardOrder(trumps);
            FrenchCard.Suite left = SAME_COLOUR.get(trumps);
            assertEquals("right bower", trumps, order.suitOf(card("J" + CODE.get(trumps))));
            assertEquals("left bower", trumps, order.suitOf(card("J" + CODE.get(left))));
            for (FrenchCard.Suite other : FrenchCard.Suite.values()) {
                if (other != trumps && other != left)
                    assertEquals("the Jack of the other colour", other, order.suitOf(card("J" + CODE.get(other))));
            }
        }
    }

    @Test
    public void everyCardButTheLeftBowerBelongsToItsPrintedSuit() {
        EuchreCardOrder order = new EuchreCardOrder(Hearts);
        for (String code : new String[]{"9H", "AH", "9D", "10D", "QD", "KD", "AD", "9S", "AS", "10C", "AC"})
            assertEquals(code, card(code).suite, order.suitOf(card(code)));
        // with Clubs trumps, the JS is the left bower, and the Spades keep their suit
        EuchreCardOrder clubs = new EuchreCardOrder(Clubs);
        assertEquals(Clubs, clubs.suitOf(card("JS")));
        assertEquals(Spades, clubs.suitOf(card("AS")));
        assertEquals(Hearts, clubs.suitOf(card("JH")));
    }

    @Test
    public void trumpsRankRightLeftAceKingQueenTenNine() {
        assertDescending(new EuchreCardOrder(Hearts), "JH", "JD", "AH", "KH", "QH", "10H", "9H");
        assertDescending(new EuchreCardOrder(Spades), "JS", "JC", "AS", "KS", "QS", "10S", "9S");
        assertDescending(new EuchreCardOrder(Diamonds), "JD", "JH", "AD", "KD", "QD", "10D", "9D");
        assertDescending(new EuchreCardOrder(Clubs), "JC", "JS", "AC", "KC", "QC", "10C", "9C");
    }

    @Test
    public void otherSuitsRankAceKingQueenJackTenNine() {
        EuchreCardOrder order = new EuchreCardOrder(Hearts);
        // the Jacks of the other colour rank as ordinary Jacks, between the Queen and the 10
        assertDescending(order, "AS", "KS", "QS", "JS", "10S", "9S");
        assertDescending(order, "AC", "KC", "QC", "JC", "10C", "9C");
        // the left bower's printed suit, without its Jack
        assertDescending(order, "AD", "KD", "QD", "10D", "9D");
    }

    @Test
    public void aTrickWithTheOrderIsWonByTheBowers() {
        EuchreCardOrder order = new EuchreCardOrder(Hearts);
        // Hearts trumps, the AD led by player 0; the JD (player 1) is a trump, the only one: player 1 wins
        assertEquals(1, trick(order, 0, "AD", "JD", "KD", "9D").winner(Hearts));
        // hearts led: the right bower (player 2) beats the left bower and the Ace
        assertEquals(2, trick(order, 0, "AH", "JD", "JH", "KH").winner(Hearts));
        // spades led, no trumps: the JS (player 1) beats the 10S and 9S but not the QS (player 3)
        assertEquals(1, trick(order, 0, "10S", "JS", "9S").winner(Hearts));
        assertEquals(3, trick(order, 0, "10S", "JS", "9S", "QS").winner(Hearts));
        // the JC is not a trump with Hearts trumps: spades led, the JC does not win
        assertEquals(0, trick(order, 0, "9S", "JC", "AC", "QC").winner(Hearts));
    }

    static Trick trick(EuchreCardOrder order, int leader, String... codes) {
        Trick t = new Trick("Trick", 4, leader, order);
        for (String c : codes)
            t.addToBottom(card(c));
        return t;
    }
}
