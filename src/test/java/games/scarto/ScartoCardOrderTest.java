package games.scarto;

import core.components.TarotCard;
import org.junit.Test;

import static core.components.TarotCard.*;
import static games.scarto.ScartoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Scarto's card order (suits and ranks within a trick) and the trick winner it gives with Trumps as trumps.
 * Ranks are compared, never checked against specific numbers.
 */
public class ScartoCardOrderTest {

    static final ScartoCardOrder ORDER = ScartoCardOrder.INSTANCE;

    /** Each card ranks strictly above the next one. */
    static void assertDescending(TarotCard... cards) {
        for (int i = 0; i + 1 < cards.length; i++)
            assertTrue(cards[i] + " should rank above " + cards[i + 1],
                    ORDER.rank(cards[i]) > ORDER.rank(cards[i + 1]));
    }

    // ---- suits ----

    @Test
    public void suitCardsBelongToTheirSuitTrumpsToTrumpsAndTheFoolToNoSuit() {
        assertEquals(Suit.Swords, ORDER.suitOf(sword(KING)));
        assertEquals(Suit.Batons, ORDER.suitOf(baton(1)));
        assertEquals(Suit.Cups, ORDER.suitOf(cup(5)));
        assertEquals(Suit.Coins, ORDER.suitOf(coin(KNAVE)));
        assertEquals(Suit.Trumps, ORDER.suitOf(trump(PAGAT)));
        assertEquals(Suit.Trumps, ORDER.suitOf(trump(WORLD)));
        assertNull("the Fool belongs to no suit", ORDER.suitOf(fool()));
    }

    // ---- ranks ----

    @Test
    public void swordsRankKingQueenCavalierKnaveThenTenDownToAce() {
        assertDescending(sword(KING), sword(QUEEN), sword(CAVALIER), sword(KNAVE),
                sword(10), sword(9), sword(8), sword(7), sword(6), sword(5), sword(4), sword(3), sword(2), sword(1));
    }

    @Test
    public void batonsRankKingQueenCavalierKnaveThenTenDownToAce() {
        assertDescending(baton(KING), baton(QUEEN), baton(CAVALIER), baton(KNAVE),
                baton(10), baton(9), baton(8), baton(7), baton(6), baton(5), baton(4), baton(3), baton(2), baton(1));
    }

    @Test
    public void cupsRankKingQueenCavalierKnaveThenAceUpToTen() {
        // round suit: the Ace is the highest pip and the 10 the lowest
        assertDescending(cup(KING), cup(QUEEN), cup(CAVALIER), cup(KNAVE),
                cup(1), cup(2), cup(3), cup(4), cup(5), cup(6), cup(7), cup(8), cup(9), cup(10));
    }

    @Test
    public void coinsRankKingQueenCavalierKnaveThenAceUpToTen() {
        assertDescending(coin(KING), coin(QUEEN), coin(CAVALIER), coin(KNAVE),
                coin(1), coin(2), coin(3), coin(4), coin(5), coin(6), coin(7), coin(8), coin(9), coin(10));
    }

    @Test
    public void trumpsRankAngelThenWorldThenNineteenDownToPagat() {
        TarotCard[] trumps = new TarotCard[21];
        trumps[0] = trump(ANGEL);
        trumps[1] = trump(WORLD);
        for (int n = 19; n >= 1; n--)
            trumps[2 + 19 - n] = trump(n);  // 19 at index 2 ... 1 (Pagat) at index 20
        assertDescending(trumps);
    }

    // ---- trick winner (Trick.winner(Trumps) with Scarto's order) ----

    @Test
    public void highestCardOfALongSuitLedWins() {
        // led by 0: sword 10 (p0), sword King (p1), sword Ace (p2) -> King is highest -> player 1
        assertEquals(1, scartoTrick(0, sword(10), sword(KING), sword(1)).winner(Suit.Trumps));
    }

    @Test
    public void inARoundSuitTheAceBeatsTheTen() {
        // led by 1: cup 10 (p1), cup Ace (p2), cup 5 (p0) -> round suit: Ace > 5 > 10 -> player 2
        assertEquals(2, scartoTrick(1, cup(10), cup(1), cup(5)).winner(Suit.Trumps));
    }

    @Test
    public void anOffSuitCardNeverWinsEvenAKing() {
        // led by 0: coin 9 (p0), coin 8 (p1), sword King (p2) -> sword is off-suit and not a trump;
        // of the coins, 8 beats 9 (round suit) -> player 1
        assertEquals(1, scartoTrick(0, coin(9), coin(8), sword(KING)).winner(Suit.Trumps));
    }

    @Test
    public void theHighestTrumpBeatsTheSuitLed() {
        // led by 0: cup King (p0), trump 5 (p1), trump 19 (p2) -> any trump beats cups; 19 > 5 -> player 2
        assertEquals(2, scartoTrick(0, cup(KING), trump(5), trump(19)).winner(Suit.Trumps));
    }

    @Test
    public void theAngelBeatsTheWorld() {
        // led by 2: World (p2), Angel (p0), 19 (p1) -> Angel highest -> player 0
        assertEquals(0, scartoTrick(2, trump(WORLD), trump(ANGEL), trump(19)).winner(Suit.Trumps));
    }

    @Test
    public void theFoolNeverWinsATrickItLeads() {
        // led by 0: Fool (p0), cup 3 (p1), cup 4 (p2) -> lead suit Cups; round suit: 3 beats 4 -> player 1
        assertEquals(1, scartoTrick(0, fool(), cup(3), cup(4)).winner(Suit.Trumps));
    }

    @Test
    public void theFoolNeverWinsATrumpTrick() {
        // led by 0: Pagat (p0), Fool (p1), trump 2 (p2) -> 2 beats the Pagat -> player 2
        assertEquals(2, scartoTrick(0, trump(PAGAT), fool(), trump(2)).winner(Suit.Trumps));
    }
}
