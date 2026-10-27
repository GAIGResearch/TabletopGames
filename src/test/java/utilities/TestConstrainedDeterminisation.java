package utilities;

import core.CoreConstants;
import core.components.Deck;
import core.components.FrenchCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;

import static core.components.FrenchCard.Suite.*;
import static org.junit.Assert.*;

/**
 * Tests for the constrained variant of {@link DeterminisationUtilities#reshuffle}, which redeals hidden
 * cards subject to per-deck restrictions on what each deck is allowed to hold.
 */
public class TestConstrainedDeterminisation {

    // the perspective player; none of the decks below belong to them, so everything is hidden from them
    private static final int PERSPECTIVE = 9;

    private Deck<FrenchCard> hand(int owner, FrenchCard.Suite... suits) {
        Deck<FrenchCard> deck = new Deck<>("Player " + owner, owner, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        int number = 2;
        for (FrenchCard.Suite suit : suits)
            deck.add(new FrenchCard(FrenchCard.FrenchCardType.Number, suit, number++));
        return deck;
    }

    private List<FrenchCard.Suite> suitsOf(Deck<FrenchCard> deck) {
        List<FrenchCard.Suite> retValue = new ArrayList<>();
        for (FrenchCard card : deck.getComponents()) retValue.add(card.suite);
        return retValue;
    }

    /**
     * Deck 0 can only take Hearts, and deck 1 can take Hearts or Spades. With two of each in the pool the
     * only valid deal is Hearts to deck 0 and Spades to deck 1 - a greedy deal that starts by giving deck 1
     * a Heart paints itself into a corner.
     */
    @Test
    public void tightConstraintsAreAlwaysSatisfied() {
        BiPredicate<Deck<FrenchCard>, FrenchCard> permitted = (deck, card) ->
                deck.getOwnerId() == 0 ? card.suite == Hearts : card.suite == Hearts || card.suite == Spades;

        Random rnd = new Random(7);
        for (int i = 0; i < 50; i++) {
            Deck<FrenchCard> deck0 = hand(0, Hearts, Spades);
            Deck<FrenchCard> deck1 = hand(1, Hearts, Spades);
            assertTrue(DeterminisationUtilities.reshuffle(PERSPECTIVE, List.of(deck0, deck1), c -> true, rnd, permitted));

            assertEquals(List.of(Hearts, Hearts), suitsOf(deck0));
            assertEquals(List.of(Spades, Spades), suitsOf(deck1));
        }
    }

    /**
     * With room to manoeuvre the deal must still respect the constraints, and must vary between calls.
     */
    @Test
    public void looseConstraintsAreSatisfiedAndStillRandom() {
        // deck 0 is void in Hearts, deck 1 is void in Spades, deck 2 is unconstrained
        BiPredicate<Deck<FrenchCard>, FrenchCard> permitted = (deck, card) ->
                switch (deck.getOwnerId()) {
                    case 0 -> card.suite != Hearts;
                    case 1 -> card.suite != Spades;
                    default -> true;
                };

        Random rnd = new Random(7);
        List<List<FrenchCard.Suite>> distinctDeals = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Deck<FrenchCard> deck0 = hand(0, Hearts, Spades, Clubs);
            Deck<FrenchCard> deck1 = hand(1, Hearts, Spades, Diamonds);
            Deck<FrenchCard> deck2 = hand(2, Hearts, Spades, Clubs);
            assertTrue(DeterminisationUtilities.reshuffle(PERSPECTIVE, List.of(deck0, deck1, deck2), c -> true, rnd, permitted));

            assertFalse(suitsOf(deck0).contains(Hearts));
            assertFalse(suitsOf(deck1).contains(Spades));
            // no cards created or destroyed
            List<FrenchCard.Suite> all = new ArrayList<>(suitsOf(deck0));
            all.addAll(suitsOf(deck1));
            all.addAll(suitsOf(deck2));
            assertEquals(3, all.stream().filter(s -> s == Hearts).count());
            assertEquals(3, all.stream().filter(s -> s == Spades).count());
            assertEquals(2, all.stream().filter(s -> s == Clubs).count());
            assertEquals(1, all.stream().filter(s -> s == Diamonds).count());

            if (!distinctDeals.contains(suitsOf(deck2))) distinctDeals.add(suitsOf(deck2));
        }
        assertTrue("The deal should vary between calls", distinctDeals.size() > 1);
    }

    /**
     * If the constraints cannot all be met then we say so, and fall back on an unconstrained reshuffle so
     * that the state is at least still a legal one.
     */
    @Test
    public void inconsistentConstraintsFallBackToAnUnconstrainedShuffle() {
        // both decks claim to be void in Spades, but there are two Spades that have to go somewhere
        BiPredicate<Deck<FrenchCard>, FrenchCard> permitted = (deck, card) -> card.suite != Spades;

        Deck<FrenchCard> deck0 = hand(0, Hearts, Spades);
        Deck<FrenchCard> deck1 = hand(1, Hearts, Spades);
        assertFalse(DeterminisationUtilities.reshuffle(PERSPECTIVE, List.of(deck0, deck1), c -> true, new Random(7), permitted));

        List<FrenchCard.Suite> all = new ArrayList<>(suitsOf(deck0));
        all.addAll(suitsOf(deck1));
        assertEquals(2, all.stream().filter(s -> s == Hearts).count());
        assertEquals(2, all.stream().filter(s -> s == Spades).count());
    }

    /**
     * A deck belonging to the perspective player is visible to them, and so must be left alone.
     */
    @Test
    public void thePerspectivePlayersOwnDeckIsUntouched() {
        BiPredicate<Deck<FrenchCard>, FrenchCard> permitted = (deck, card) -> card.suite != Hearts;

        Deck<FrenchCard> ownHand = hand(PERSPECTIVE, Hearts, Hearts);
        Deck<FrenchCard> otherHand = hand(1, Clubs, Diamonds);
        assertTrue(DeterminisationUtilities.reshuffle(PERSPECTIVE, List.of(ownHand, otherHand), c -> true, new Random(7), permitted));

        assertEquals(List.of(Hearts, Hearts), suitsOf(ownHand));
    }

    /**
     * A null constraint is equivalent to the unconstrained reshuffle, and must give identical results for
     * the same seed so that existing callers are unaffected.
     */
    @Test
    public void nullConstraintMatchesTheUnconstrainedReshuffle() {
        Deck<FrenchCard> constrained0 = hand(0, Hearts, Spades, Clubs);
        Deck<FrenchCard> constrained1 = hand(1, Hearts, Spades, Diamonds);
        DeterminisationUtilities.reshuffle(PERSPECTIVE, List.of(constrained0, constrained1), c -> true, new Random(7), null);

        Deck<FrenchCard> plain0 = hand(0, Hearts, Spades, Clubs);
        Deck<FrenchCard> plain1 = hand(1, Hearts, Spades, Diamonds);
        DeterminisationUtilities.reshuffle(PERSPECTIVE, List.of(plain0, plain1), c -> true, new Random(7));

        assertEquals(suitsOf(plain0), suitsOf(constrained0));
        assertEquals(suitsOf(plain1), suitsOf(constrained1));
    }
}
