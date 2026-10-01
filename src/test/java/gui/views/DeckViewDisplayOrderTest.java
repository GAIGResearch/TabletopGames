package gui.views;

import core.components.Deck;
import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.components.FrenchCard.FrenchCardType.*;
import static core.components.FrenchCard.Suite.*;
import static org.junit.Assert.*;

public class DeckViewDisplayOrderTest {

    static class TestDeckView extends DeckView<FrenchCard> {
        TestDeckView(Deck<FrenchCard> deck, boolean visible) {
            super(-1, deck, visible, 10, 10);
        }

        @Override
        public void drawComponent(Graphics2D g, Rectangle rect, FrenchCard component, boolean front) {
        }
    }

    Deck<FrenchCard> hand;

    @Before
    public void setup() {
        hand = new Deck<>("hand", VISIBLE_TO_ALL);
        // addToBottom keeps this order: index 0 is the Two of Clubs
        for (FrenchCard c : List.of(new FrenchCard(Number, Clubs, 2), new FrenchCard(Ace, Hearts),
                new FrenchCard(Number, Spades, 7), new FrenchCard(King, Spades), new FrenchCard(Number, Diamonds, 10),
                new FrenchCard(Queen, Hearts), new FrenchCard(Ace, Spades), new FrenchCard(Jack, Clubs)))
            hand.addToBottom(c);
    }

    @Test
    public void handDisplayOrderIsSpadesHeartsDiamondsClubsAndAceDownToTwo() {
        List<FrenchCard> sorted = new ArrayList<>(hand.getComponents());
        sorted.sort(FrenchCard.HAND_DISPLAY_ORDER);
        assertEquals(List.of(new FrenchCard(Ace, Spades), new FrenchCard(King, Spades), new FrenchCard(Number, Spades, 7),
                new FrenchCard(Ace, Hearts), new FrenchCard(Queen, Hearts), new FrenchCard(Number, Diamonds, 10),
                new FrenchCard(Jack, Clubs), new FrenchCard(Number, Clubs, 2)), sorted);
    }

    @Test
    public void aFaceUpDeckWithADisplayOrderIsLaidOutInThatOrder() {
        TestDeckView view = new TestDeckView(hand, true);
        view.setDisplayOrder(FrenchCard.HAND_DISPLAY_ORDER);
        // deck indices of AS, KS, 7S, AH, QH, 10D, JC, 2C
        assertArrayEquals(new int[]{6, 3, 2, 1, 5, 4, 7, 0}, view.displayIndices(hand));
    }

    @Test
    public void aFaceDownDeckOrOneWithNoDisplayOrderKeepsDeckOrder() {
        int[] deckOrder = {0, 1, 2, 3, 4, 5, 6, 7};
        TestDeckView noOrder = new TestDeckView(hand, true);
        assertArrayEquals(deckOrder, noOrder.displayIndices(hand));

        // sorting a hidden (or partly hidden) hand would reveal the hidden cards' suits and ranks by their positions
        TestDeckView faceDown = new TestDeckView(hand, false);
        faceDown.setDisplayOrder(FrenchCard.HAND_DISPLAY_ORDER);
        assertArrayEquals(deckOrder, faceDown.displayIndices(hand));
    }
}
