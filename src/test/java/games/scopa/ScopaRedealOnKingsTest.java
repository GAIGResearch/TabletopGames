package games.scopa;

import core.components.Deck;
import core.components.TarotCard;
import org.junit.Test;

import java.util.List;

import static core.components.TarotCard.*;
import static games.scopa.ScopaTestUtils.*;
import static org.junit.Assert.*;

/**
 * ScopaParameters.redealOnKings: redealing a deal that puts 3 or 4 Kings on the table. Each test deals from a stacked pack
 * with ScopaForwardModel.deal(state, pack): table pack[0..3], the player after the dealer pack[4..6], the other
 * player pack[7..9], the draw deck the rest. In the first deal the dealer is player 1, so player 0 gets pack[4..6].
 */
public class ScopaRedealOnKingsTest {

    ScopaForwardModel fm = new ScopaForwardModel();

    private static long kingsOnTable(ScopaGameState state) {
        return state.getTable().getComponents().stream().filter(c -> c.number == KING).count();
    }

    /** A redealt deal: fewer than 3 Kings on the table, 4 / 3 / 3 / 30, 40 cards, empty piles, same dealer. */
    private static void assertRedealt(ScopaGameState state) {
        assertTrue("Kings on the table: " + state.getTable(), kingsOnTable(state) < 3);
        assertAllCardsPresent(state);
        assertEquals(4, state.getTable().getSize());
        assertEquals(3, state.getPlayerHand(0).getSize());
        assertEquals(3, state.getPlayerHand(1).getSize());
        assertEquals(30, state.getDrawDeck().getSize());
        assertEquals(0, state.getCapturedCards(0).getSize());
        assertEquals(0, state.getCapturedCards(1).getSize());
        assertEquals("dealer", 1, state.getDealer());
        assertEquals("to act", 0, state.getCurrentPlayer());
    }

    @Test
    public void threeKingsOnTheTableAreRedealt() {
        ScopaGameState state = newState(81, fm, params(0, true));
        Deck<TarotCard> pack = stackedPack(new TarotCard(Suit.Swords, KING), new TarotCard(Suit.Batons, KING),
                new TarotCard(Suit.Cups, KING), new TarotCard(Suit.Coins, 1));

        fm.deal(state, pack);

        assertRedealt(state);
    }

    @Test
    public void fourKingsOnTheTableAreRedealt() {
        ScopaGameState state = newState(82, fm, params(0, true));
        Deck<TarotCard> pack = stackedPack(new TarotCard(Suit.Swords, KING), new TarotCard(Suit.Batons, KING),
                new TarotCard(Suit.Cups, KING), new TarotCard(Suit.Coins, KING));

        fm.deal(state, pack);

        assertRedealt(state);
    }

    @Test
    public void withTheOptionOffThreeKingsStayOnTheTable() {
        ScopaGameState state = newState(83, fm, params(0, false));
        Deck<TarotCard> pack = stackedPack(new TarotCard(Suit.Swords, KING), new TarotCard(Suit.Batons, KING),
                new TarotCard(Suit.Cups, KING), new TarotCard(Suit.Coins, 1));
        List<TarotCard> order = List.copyOf(pack.getComponents());

        fm.deal(state, pack);

        assertEquals(setOf(order.get(0), order.get(1), order.get(2), order.get(3)), setOf(state.getTable()));
        assertEquals(3, kingsOnTable(state));
        assertEquals(setOf(order.get(4), order.get(5), order.get(6)), setOf(state.getPlayerHand(0)));
        assertEquals(setOf(order.get(7), order.get(8), order.get(9)), setOf(state.getPlayerHand(1)));
        assertAllCardsPresent(state);
    }

    @Test
    public void twoKingsOnTheTableAreKeptEvenWithTwoMoreInAHand() {
        // table: 2 Kings (pack[0..1]); player 0's hand: the other 2 Kings (pack[4..5]) - 4 dealt, 2 on the table
        ScopaGameState state = newState(84, fm, params(0, true));
        Deck<TarotCard> pack = stackedPack(new TarotCard(Suit.Swords, KING), new TarotCard(Suit.Batons, KING),
                new TarotCard(Suit.Cups, 1), new TarotCard(Suit.Coins, 1),
                new TarotCard(Suit.Cups, KING), new TarotCard(Suit.Coins, KING), new TarotCard(Suit.Swords, 1));
        List<TarotCard> order = List.copyOf(pack.getComponents());

        fm.deal(state, pack);

        assertEquals(setOf(order.get(0), order.get(1), order.get(2), order.get(3)), setOf(state.getTable()));
        assertEquals(setOf(order.get(4), order.get(5), order.get(6)), setOf(state.getPlayerHand(0)));
        assertEquals(setOf(order.get(7), order.get(8), order.get(9)), setOf(state.getPlayerHand(1)));
        assertAllCardsPresent(state);
    }

    @Test
    public void theRedealIsShuffled() {
        // the same position and pack in two states differing only in their random number generator (a copy has its
        // own) redeal differently
        ScopaGameState state = newState(85, fm, params(0, true));
        ScopaGameState other = (ScopaGameState) state.copy();
        assertEquals(state, other);
        TarotCard[] kings = {new TarotCard(Suit.Swords, KING), new TarotCard(Suit.Batons, KING),
                new TarotCard(Suit.Cups, KING)};

        fm.deal(state, stackedPack(kings));
        fm.deal(other, stackedPack(kings));

        assertRedealt(state);
        assertRedealt(other);
        assertFalse("the two redeals are the same",
                setOf(state.getTable()).equals(setOf(other.getTable()))
                        && setOf(state.getPlayerHand(0)).equals(setOf(other.getPlayerHand(0)))
                        && setOf(state.getPlayerHand(1)).equals(setOf(other.getPlayerHand(1))));
    }
}
