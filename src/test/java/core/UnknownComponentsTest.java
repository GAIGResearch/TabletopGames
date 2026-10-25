package core;

import core.components.Card;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.GameType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.VisibilityMode.*;
import static org.junit.Assert.*;

/**
 * Checks AbstractGameState.getUnknownComponentsIds() for each VisibilityMode of a plain Deck.
 */
public class UnknownComponentsTest {

    private static Deck<Card> deck(CoreConstants.VisibilityMode mode, int size, int owner) {
        Deck<Card> deck = new Deck<>("deck", owner, mode);
        for (int i = 0; i < size; i++)
            deck.add(new Card("card" + i));
        return deck;
    }

    private static List<Integer> ids(List<Card> cards) {
        return cards.stream().map(Component::getComponentID).toList();
    }

    @Test
    public void topVisibleHidesEverythingButTheTop() {
        Deck<Card> deck = deck(TOP_VISIBLE_TO_ALL, 5, -1);
        List<Integer> unknown = new StubState(deck).getUnknownComponentsIds(0);
        assertEquals(ids(deck.getComponents().subList(1, 5)), unknown);
    }

    @Test
    public void bottomVisibleHidesEverythingButTheBottom() {
        Deck<Card> deck = deck(BOTTOM_VISIBLE_TO_ALL, 5, -1);
        List<Integer> unknown = new StubState(deck).getUnknownComponentsIds(0);
        assertEquals(ids(deck.getComponents().subList(0, 4)), unknown);
    }

    @Test
    public void emptyTopOrBottomVisibleDecksHaveNothingUnknown() {
        assertTrue(new StubState(deck(TOP_VISIBLE_TO_ALL, 0, -1)).getUnknownComponentsIds(0).isEmpty());
        assertTrue(new StubState(deck(BOTTOM_VISIBLE_TO_ALL, 0, -1)).getUnknownComponentsIds(0).isEmpty());
    }

    @Test
    public void visibleToAllHidesNothing() {
        assertTrue(new StubState(deck(VISIBLE_TO_ALL, 3, -1)).getUnknownComponentsIds(0).isEmpty());
    }

    @Test
    public void hiddenToAllHidesEverything() {
        Deck<Card> deck = deck(HIDDEN_TO_ALL, 3, -1);
        assertEquals(ids(deck.getComponents()), new StubState(deck).getUnknownComponentsIds(0));
    }

    @Test
    public void visibleToOwnerHidesFromOthersOnly() {
        Deck<Card> deck = deck(VISIBLE_TO_OWNER, 3, 1);
        StubState state = new StubState(deck);
        assertTrue(state.getUnknownComponentsIds(1).isEmpty());
        assertEquals(ids(deck.getComponents()), state.getUnknownComponentsIds(0));
    }

    @Test
    public void partialObservableDeckUsesPerCardVisibility() {
        PartialObservableDeck<Card> deck = new PartialObservableDeck<>("pod", -1, 2, HIDDEN_TO_ALL);
        Card hidden = new Card("hidden");
        Card seen = new Card("seen");
        deck.add(hidden);
        deck.add(seen, new boolean[]{true, false});
        StubState state = new StubState(deck);
        assertEquals(List.of(hidden.getComponentID()), state.getUnknownComponentsIds(0));
        assertEquals(List.of(seen.getComponentID(), hidden.getComponentID()), state.getUnknownComponentsIds(1));
    }

    /**
     * Minimal game state whose only top-level component is the given deck
     */
    static class StubState extends AbstractGameState {
        final List<Component> components = new ArrayList<>();

        StubState(Component... components) {
            super(new StubParameters(), 2);
            this.components.addAll(List.of(components));
        }

        @Override
        protected GameType _getGameType() {
            return null;
        }

        @Override
        protected List<Component> _getAllComponents() {
            return components;
        }

        @Override
        protected AbstractGameState _copy(int playerId) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected double _getHeuristicScore(int playerId) {
            return 0;
        }

        @Override
        public double getGameScore(int playerId) {
            return 0;
        }

        @Override
        protected boolean _equals(Object o) {
            return o == this;
        }
    }

    static class StubParameters extends AbstractParameters {
        @Override
        protected AbstractParameters _copy() {
            return new StubParameters();
        }

        @Override
        protected boolean _equals(Object o) {
            return o instanceof StubParameters;
        }
    }
}
