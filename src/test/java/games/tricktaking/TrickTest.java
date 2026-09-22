package games.tricktaking;

import org.junit.Test;

import static core.components.FrenchCard.Suite.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

public class TrickTest {

    @Test
    public void playAppendsCardsInPlayOrderAndTheFirstCardSetsTheLeadSuit() {
        Trick t = new Trick("Trick", 4, 0);
        assertNull(t.getLeadSuit());
        t.play(card("5H"));
        assertEquals(Hearts, t.getLeadSuit());
        t.play(card("KS"));
        t.play(card("2C"));
        // index 0 is the lead; later off-suit cards do not change the lead suit
        assertEquals(cards("5H", "KS", "2C"), cardsOf(t));
        assertEquals(Hearts, t.getLeadSuit());
    }

    @Test
    public void playerOfWrapsRoundFromTheLeader() {
        // leader 3 of 4: index i was played by (3 + i) % 4
        Trick t = trick(3, "5H", "6H", "7H", "8H");
        assertEquals(3, t.playerOf(0));
        assertEquals(0, t.playerOf(1));
        assertEquals(1, t.playerOf(2));
        assertEquals(2, t.playerOf(3));
        // leader 1: index 2 is player 3
        assertEquals(3, trick(1, "5H", "6H", "7H").playerOf(2));
    }

    @Test
    public void isCompleteOnlyOnceEveryPlayerHasPlayed() {
        Trick t = new Trick("Trick", 4, 2);
        assertFalse(t.isComplete());
        String[] codes = {"5H", "6H", "7H"};
        for (String c : codes) {
            t.addToBottom(card(c));
            assertFalse("complete after " + t.getSize() + " cards", t.isComplete());
        }
        t.addToBottom(card("8H"));
        assertTrue(t.isComplete());

        // with 3 players, 3 cards complete the trick
        Trick three = new Trick("Trick", 3, 0);
        three.addToBottom(card("5H"));
        three.addToBottom(card("6H"));
        assertFalse(three.isComplete());
        three.addToBottom(card("7H"));
        assertTrue(three.isComplete());
    }

    @Test
    public void highestCardOfTheLeadSuitWinsWithNoTrumpPlayed() {
        // no trumps: KH (index 1) is the highest heart; leader 0 -> player 1
        assertEquals(1, trick(0, "5H", "KH", "2H", "9H").winner(null));
        // spades are trumps but none was played: same
        assertEquals(1, trick(0, "5H", "KH", "2H", "9H").winner(Spades));
        // after two cards, the winner so far: 8H (index 1) over 5H, leader 0 -> player 1
        assertEquals(1, trick(0, "5H", "8H").winner(Spades));
        // 10 < J < Q: QD at index 3, leader 2 -> player (2 + 3) % 4 = 1
        assertEquals(1, trick(2, "10D", "JD", "4D", "QD").winner(Clubs));
    }

    @Test
    public void aHigherCardOfAnotherNonTrumpSuitDoesNotWin() {
        // leader 2, spades trumps: AD (index 1) is off-suit; 10H (index 2) is the highest heart -> player (2 + 2) % 4 = 0
        assertEquals(0, trick(2, "9H", "AD", "10H", "KC").winner(Spades));
        // no trumps, leader 1: AH (index 1) is off-suit; 4C (index 2) is the highest club -> player (1 + 2) % 4 = 3
        assertEquals(3, trick(1, "3C", "AH", "4C", "2C").winner(null));
    }

    @Test
    public void anyTrumpBeatsTheLeadSuit() {
        // spades trumps, leader 0: the 2 of Spades (index 2) beats the Ace of Hearts led -> player 2
        assertEquals(2, trick(0, "AH", "KH", "2S", "QH").winner(Spades));
    }

    @Test
    public void theHighestOfSeveralTrumpsWins() {
        // diamonds trumps, leader 1: 3D (index 1) and JD (index 2) are trumps; JD is higher -> player (1 + 2) % 4 = 3
        assertEquals(3, trick(1, "10C", "3D", "JD", "AC").winner(Diamonds));
    }

    @Test
    public void aceIsHigh() {
        // no trumps, leader 0: AC (index 2) beats KC -> player 2
        assertEquals(2, trick(0, "KC", "2C", "AC", "QC").winner(null));
        // hearts trumps, leader 3: AH (index 2) beats KH (index 1) -> player (3 + 2) % 4 = 1
        assertEquals(1, trick(3, "5S", "KH", "AH", "7S").winner(Hearts));
    }

    @Test
    public void whenTrumpsAreLedTheHighestTrumpWins() {
        // clubs trumps and led, leader 0: 9C (index 3) is the highest club; AH is off-suit -> player 3
        assertEquals(3, trick(0, "4C", "AH", "3C", "9C").winner(Clubs));
    }

    @Test
    public void copyIsEqualAndIndependentAndEqualityDependsOnTheLeader() {
        Trick t = trick(1, "5H", "KH");
        Trick copy = t.copy();
        assertEquals(t, copy);
        assertEquals(t.hashCode(), copy.hashCode());

        copy.addToBottom(card("2H"));
        assertEquals(cards("5H", "KH"), cardsOf(t));
        assertNotEquals(t, copy);

        // same cards (none), different leader
        assertNotEquals(trick(1), trick(2));
        assertNotEquals(trick(1).hashCode(), trick(2).hashCode());
    }

    @Test
    public void tricksWithTheSameCardsAndLeaderAreEqualWhateverTheirComponentID() {
        Trick a = trick(1, "5H", "KH");
        Trick b = trick(1, "5H", "KH");
        assertNotEquals(a.getComponentID(), b.getComponentID());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    /**
     * A 4-player trick in which the given player sits out, with the cards added in play order.
     */
    private static Trick trickWithout(int sittingOut, int leader, String... codes) {
        Trick t = new Trick("Trick", 4, leader, CardOrder.STANDARD, sittingOut);
        for (String c : codes)
            t.addToBottom(card(c));
        return t;
    }

    @Test
    public void playerOfSkipsThePlayerSittingOut() {
        // leader 3, player 1 sits out: 3, 0, then 2 (1 skipped)
        Trick t = trickWithout(1, 3, "5H", "6H", "7H");
        assertEquals(3, t.playerOf(0));
        assertEquals(0, t.playerOf(1));
        assertEquals(2, t.playerOf(2));
        // leader 0, player 1 sits out: 0, then 2 (1 skipped), 3
        Trick u = trickWithout(1, 0, "5H", "6H", "7H");
        assertEquals(0, u.playerOf(0));
        assertEquals(2, u.playerOf(1));
        assertEquals(3, u.playerOf(2));
        // leader 2, player 3 sits out: 2, then 0 (3 skipped), 1
        Trick v = trickWithout(3, 2);
        assertEquals(2, v.playerOf(0));
        assertEquals(0, v.playerOf(1));
        assertEquals(1, v.playerOf(2));
        assertEquals(3, v.getSittingOut());
    }

    @Test
    public void withAPlayerSittingOutThreeCardsCompleteTheTrick() {
        Trick t = trickWithout(2, 1);
        t.addToBottom(card("5H"));
        t.addToBottom(card("6H"));
        assertFalse(t.isComplete());
        t.addToBottom(card("7H"));
        // 4 players - 1 sitting out = 3 cards
        assertTrue(t.isComplete());
    }

    @Test
    public void theWinnerIsReportedByPlayerSkippingThePlayerSittingOut() {
        // leader 3, player 1 sits out: 5H by 3, 6H by 0, KH (index 2, the highest) by 2 - not by (3 + 2) % 4 = 1
        assertEquals(2, trickWithout(1, 3, "5H", "6H", "KH").winner(null));
        // leader 0, player 1 sits out: 2S trump at index 1 is player 2's
        assertEquals(2, trickWithout(1, 0, "AH", "2S", "KH").winner(Spades));
    }

    @Test
    public void byDefaultNobodySitsOutAndEveryPlayerPlays() {
        assertEquals(-1, new Trick("Trick", 4, 0).getSittingOut());
        assertEquals(-1, new Trick("Trick", 4, 0, CardOrder.STANDARD).getSittingOut());
        // with -1, index 1 of leader 0 is player 1 and the trick needs all 4 cards
        Trick t = trickWithout(-1, 0, "5H", "6H", "7H");
        assertEquals(1, t.playerOf(1));
        assertFalse(t.isComplete());
        assertEquals(trick(0, "5H", "6H", "7H"), t);
    }

    @Test
    public void equalityAndCopyIncludeThePlayerSittingOut() {
        Trick t = trickWithout(1, 0, "5H", "KH");
        Trick copy = t.copy();
        assertEquals(1, copy.getSittingOut());
        assertEquals(t, copy);
        assertEquals(t.hashCode(), copy.hashCode());
        // same cards and leader, a different player (or nobody) sitting out
        assertNotEquals(t, trickWithout(3, 0, "5H", "KH"));
        assertNotEquals(t, trick(0, "5H", "KH"));
        assertNotEquals(t.hashCode(), trick(0, "5H", "KH").hashCode());
    }
}
