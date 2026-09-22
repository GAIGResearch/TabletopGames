package games.tricktaking;

import core.components.Deck;
import core.components.FrenchCard;
import org.junit.Test;

import java.util.Set;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_OWNER;
import static core.components.FrenchCard.Suite.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * A Trick's CardOrder decides the lead suit, the legal plays, the winner and the voids recorded, and
 * KnownVoids.permits(order) uses it. The tests use a small order of their own, independent of any game: the 2 of
 * Clubs belongs to Hearts and is the highest Heart; every other card is as {@link CardOrder#STANDARD}.
 */
public class CardOrderTest {

    /**
     * 2C is a Heart ranked 20 (above the Ace's 14); all other cards keep their printed suit and number.
     */
    static final CardOrder TWO_OF_CLUBS_TOP_HEART = new CardOrder() {
        @Override
        public FrenchCard.Suite suitOf(FrenchCard card) {
            return card.equals(card("2C")) ? Hearts : card.suite;
        }

        @Override
        public int rank(FrenchCard card) {
            return card.equals(card("2C")) ? 20 : card.number;
        }
    };

    static Trick trickWith(CardOrder order, int leader, String... codes) {
        Trick t = new Trick("Trick", 4, leader, order);
        for (FrenchCard c : cards(codes))
            t.addToBottom(c);
        return t;
    }

    static Trick ordered(int leader, String... codes) {
        return trickWith(TWO_OF_CLUBS_TOP_HEART, leader, codes);
    }

    @Test
    public void theLeadSuitIsTheSuitTheOrderGivesTheLeadCard() {
        assertNull(ordered(0).getLeadSuit());
        assertEquals(Hearts, ordered(0, "2C").getLeadSuit());   // printed Clubs, but a Heart in this order
        assertEquals(Clubs, ordered(0, "3C").getLeadSuit());
        assertEquals(Clubs, trick(0, "2C").getLeadSuit());      // STANDARD: the printed suit
    }

    @Test
    public void theWinnerIsTheHighestInTheSuitLedByTheOrdersRank() {
        // no trumps, hearts led: 2C is a heart ranked 20 > AH's 14, so index 2 (leader 0 -> player 2) wins
        assertEquals(2, ordered(0, "5H", "AH", "2C", "KH").winner(null));
        // STANDARD: the 2C does not follow hearts; AH (player 1) wins
        assertEquals(1, trick(0, "5H", "AH", "2C", "KH").winner(null));
    }

    @Test
    public void aCardTheOrderPutsInTheTrumpSuitIsATrump() {
        // hearts trumps, spades led by player 3: the 2C (player 0) is the only trump and wins over the AS
        assertEquals(0, ordered(3, "AS", "2C", "KS").winner(Hearts));
    }

    @Test
    public void aCardTheOrderTakesOutOfItsPrintedSuitDoesNotFollowIt() {
        // no trumps, clubs led: the 2C is a heart, not a club, so its rank 20 does not count; the 4C (player 2) wins
        assertEquals(2, ordered(0, "3C", "2C", "4C").winner(null));
        // and it cannot win as a heart either, as hearts were not led
        assertEquals(0, ordered(0, "3C", "2C").winner(null));
    }

    @Test
    public void followSuitUsesTheOrdersSuitForTheLeadAndTheHand() {
        // hearts led: the 2C is the hand's only heart and must be played
        assertEquals(cards("2C"), PlayRule.FOLLOW_SUIT.legalPlays(cards("2C", "3C", "4S"), ordered(0, "5H")));
        // clubs led: the 2C is not a club, so only the 3C follows
        assertEquals(cards("3C"), PlayRule.FOLLOW_SUIT.legalPlays(cards("2C", "3C", "4S"), ordered(0, "5C")));
        // clubs led, the hand's only printed club is the 2C: void in clubs, so any card
        assertEquals(cards("2C", "4S"), PlayRule.FOLLOW_SUIT.legalPlays(cards("2C", "4S"), ordered(0, "5C")));
        // the 2C led leads hearts: the 5H must follow, not the 3C
        assertEquals(cards("5H"), PlayRule.FOLLOW_SUIT.legalPlays(cards("5H", "3C"), ordered(0, "2C")));
    }

    @Test
    public void leadRestrictedUsesTheOrdersSuit() {
        PlayRule rule = PlayRule.leadRestricted(Hearts);
        // hearts may not be led while the hand holds anything else, and the 2C is a heart
        assertEquals(cards("4S"), rule.legalPlays(cards("2C", "4S"), ordered(0)));
        // a hand of hearts only (5H and the 2C) may lead either
        assertEquals(cards("2C", "5H"), rule.legalPlays(cards("2C", "5H"), ordered(0)));
        // once led, following is by the order: clubs led, the 2C does not follow
        assertEquals(cards("3C"), rule.legalPlays(cards("2C", "3C"), ordered(0, "5C")));
    }

    @Test
    public void voidsAreRecordedByTheOrdersSuit() {
        KnownVoids kv = new KnownVoids(4);
        kv.record(1, ordered(0, "5H"), card("2C"));      // the 2C follows hearts: nothing learnt
        assertEquals(Set.of(), kv.get(1));
        kv.record(2, ordered(0, "5H"), card("3C"));      // fails to follow hearts
        assertEquals(Set.of(Hearts), kv.get(2));
        kv.record(3, ordered(0, "2C"), card("4C"));      // the 2C led hearts; a club fails to follow hearts
        assertEquals(Set.of(Hearts), kv.get(3));
        kv.record(0, ordered(1, "5C"), card("2C"));      // clubs led; the 2C is not a club
        assertEquals(Set.of(Clubs), kv.get(0));
    }

    @Test
    public void permitsWithAnOrderUsesTheOrdersSuit() {
        KnownVoids kv = new KnownVoids(4);
        kv.get(1).add(Hearts);
        kv.get(2).add(Clubs);
        Deck<FrenchCard> hand1 = new Deck<>("Hand 1", 1, VISIBLE_TO_OWNER);
        Deck<FrenchCard> hand2 = new Deck<>("Hand 2", 2, VISIBLE_TO_OWNER);
        Deck<FrenchCard> noOwner = new Deck<>("Pile", VISIBLE_TO_ALL);
        var permits = kv.permits(TWO_OF_CLUBS_TOP_HEART);

        // player 1 is void in hearts, and so may not get the 2C (a heart), but may get other clubs
        assertFalse(permits.test(hand1, card("2C")));
        assertFalse(permits.test(hand1, card("5H")));
        assertTrue(permits.test(hand1, card("3C")));
        // player 2 is void in clubs, which the 2C is not
        assertTrue(permits.test(hand2, card("2C")));
        assertFalse(permits.test(hand2, card("3C")));
        assertTrue(permits.test(noOwner, card("2C")));
        // with STANDARD, the printed suit
        assertTrue(kv.permits(CardOrder.STANDARD).test(hand1, card("2C")));
        assertFalse(kv.permits(CardOrder.STANDARD).test(hand2, card("2C")));
    }

    @Test
    public void theOrderCountsForEqualityAndIsKeptByCopy() {
        Trick standard = trick(0, "5H", "2C");
        Trick ordered = ordered(0, "5H", "2C");
        assertSame(CardOrder.STANDARD, standard.getOrder());
        assertNotEquals(standard, ordered);
        assertEquals(ordered, ordered(0, "5H", "2C"));

        Trick copy = ordered.copy();
        assertSame(TWO_OF_CLUBS_TOP_HEART, copy.getOrder());
        assertEquals(ordered, copy);
        assertEquals(ordered.hashCode(), copy.hashCode());
    }
}
