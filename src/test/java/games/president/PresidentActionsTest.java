package games.president;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static games.president.PresidentTestUtils.*;
import static org.junit.Assert.*;

/**
 * The effects of PlayCards and Pass on the hands, the play pile and the trick counters.
 */
public class PresidentActionsTest {

    PresidentForwardModel fm;

    @Before
    public void setup() {
        fm = new PresidentForwardModel();
    }

    private static long countOf(PresidentGameState state, int player, int number) {
        return state.getPlayerHand(player).getComponents().stream().filter(c -> c.number == number).count();
    }

    @Test
    public void leadingASetMovesThoseCardsFromTheHandToThePlayPile() {
        PresidentGameState state = newState(4, 21, fm);
        giveHand(state, 0, "KH", "KD", "KS", "5C");
        int discards = state.getDiscardPile().getSize();
        int hand1 = state.getPlayerHand(1).getSize();

        play(state, fm, 0, 13, 2);

        // two of the three Kings have gone (which suits is not a rule); the Five stays
        assertEquals(2, state.getPlayerHand(0).getSize());
        assertEquals(1, countOf(state, 0, 13));
        assertTrue(state.getPlayerHand(0).contains(card("5C")));
        assertEquals(List.of(13, 13), numbers(state.getPlayPile()));
        assertEquals(discards, state.getDiscardPile().getSize());
        assertEquals(hand1, state.getPlayerHand(1).getSize());
        assertEquals(0, state.getLastPlayer());
        assertEquals(2, state.getSetSize());
        assertEquals(0, state.getPassesInRow());
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void followingPutsTheSetOnTopAndResetsThePasses() {
        // 5 players: player 1 played a 5 on a 4, players 2 and 3 passed, player 4 to act
        PresidentGameState state = newState(5, 22, fm);
        arrangeTrick(state, 1, 1, 2, 4, "4C", "5H");
        giveHand(state, 4, "6D", "6S", "9C");

        play(state, fm, 4, 6, 1);

        assertEquals(List.of(6, 5, 4), numbers(state.getPlayPile()));   // top first
        assertEquals(1, countOf(state, 4, 6));
        assertTrue(state.getPlayerHand(4).contains(card("9C")));
        assertEquals(2, state.getPlayerHand(4).getSize());
        assertEquals(4, state.getLastPlayer());
        assertEquals(1, state.getSetSize());
        assertEquals(0, state.getPassesInRow());
        assertEquals(0, state.getCurrentPlayer());   // clockwise: 4 -> 0
        assertAllCardsPresent(state);
    }

    @Test
    public void passingCountsThePassAndMovesTheTurnOn() {
        // 4 players: player 0 led a single 5, player 1 to act
        PresidentGameState state = newState(4, 23, fm);
        arrangeTrick(state, 0, 1, 0, 1, "5H");
        List<?> hand1 = List.copyOf(state.getPlayerHand(1).getComponents());

        pass(state, fm, 1);

        assertEquals(1, state.getPassesInRow());
        assertEquals(hand1, state.getPlayerHand(1).getComponents());
        assertEquals(List.of(5), numbers(state.getPlayPile()));
        assertEquals(0, state.getLastPlayer());
        assertEquals(1, state.getSetSize());
        assertEquals(2, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }
}
