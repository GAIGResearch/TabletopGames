package games.scarto;

import core.components.TarotCard;
import games.tricktaking.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.components.TarotCard.*;
import static core.components.TarotCard.Suit.*;
import static games.scarto.ScartoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Full and per-player copies of a mid-deal position, including redeterminisation constrained by known voids.
 */
public class ScartoCopyTest {

    ScartoForwardModel fm;
    ScartoGameState state;

    @Before
    public void setup() {
        // mid-deal: player 0 has won a trick of cups, player 1 has the Fool back from a trick; player 2 has led
        // sword 5 to the current trick and player 0 (holding a sword) is to act
        fm = new ScartoForwardModel();
        state = newState(41, fm);
        setCardsWon(state, 0, cup(KING), cup(2), cup(3));
        setCardsWon(state, 1, fool());
        giveHand(state, 0, sword(7), trump(3), cup(4));
        arrangeTrick(state, 2, sword(5));
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void aFullCopyIsEqualWithTheSameHash() {
        ScartoGameState copy = (ScartoGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void playingOnACopyLeavesTheOriginalUnchanged() {
        ScartoGameState copy = (ScartoGameState) state.copy();
        fm.next(copy, new PlayCard<>(sword(7)));
        assertNotEquals(state, copy);
        assertEquals(List.of(sword(7), trump(3), cup(4)), state.getPlayerHand(0).getComponents());
        assertEquals(List.of(sword(5)), state.getCurrentTrick().getComponents());
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void completingATrickOnACopyLeavesTheOriginalsCardsWonUnchanged() {
        ScartoGameState copy = (ScartoGameState) state.copy();
        fm.next(copy, new PlayCard<>(sword(7)));
        fm.next(copy, fm.computeAvailableActions(copy).get(0));  // player 1 completes the trick
        assertEquals(0, copy.getCurrentTrick().getSize());
        // the winner's pile in the copy grew; the original's piles are as set up
        assertEquals(List.of(cup(KING), cup(2), cup(3)), state.getCardsWon(0).getComponents());
        assertEquals(List.of(fool()), state.getCardsWon(1).getComponents());
        assertEquals(0, state.getCardsWon(2).getSize());
    }

    @Test
    public void everyPlayersCopyReshufflesTheScarto() {
        // nobody has seen the scarto, not even the dealer (player 2): in some of 10 copies it must hold other cards
        for (int player = 0; player < 3; player++) {
            int changed = 0;
            for (int i = 0; i < 10; i++) {
                ScartoGameState copy = (ScartoGameState) state.copy(player);
                if (!setOf(copy.getScarto()).equals(setOf(state.getScarto())))
                    changed++;
            }
            assertTrue("the scarto never changed in 10 copies for player " + player, changed > 0);
        }
    }

    @Test
    public void aPlayersCopyKeepsWhatThePlayerCanSee() {
        Set<TarotCard> hiddenFromZero = new HashSet<>();
        hiddenFromZero.addAll(state.getPlayerHand(1).getComponents());
        hiddenFromZero.addAll(state.getPlayerHand(2).getComponents());
        hiddenFromZero.addAll(state.getScarto().getComponents());

        int changed = 0;
        for (int i = 0; i < 10; i++) {
            ScartoGameState copy = (ScartoGameState) state.copy(0);
            assertEquals(state.getPlayerHand(0).getComponents(), copy.getPlayerHand(0).getComponents());
            assertEquals(state.getCurrentTrick(), copy.getCurrentTrick());
            for (int p = 0; p < 3; p++)
                assertEquals(state.getCardsWon(p).getComponents(), copy.getCardsWon(p).getComponents());
            assertEquals(state.getPlayerHand(1).getSize(), copy.getPlayerHand(1).getSize());
            assertEquals(state.getPlayerHand(2).getSize(), copy.getPlayerHand(2).getSize());
            assertEquals(state.getScarto().getSize(), copy.getScarto().getSize());
            assertAllCardsPresent(copy);

            // the hidden cards are the same cards, shared out afresh
            Set<TarotCard> hidden = new HashSet<>();
            hidden.addAll(copy.getPlayerHand(1).getComponents());
            hidden.addAll(copy.getPlayerHand(2).getComponents());
            hidden.addAll(copy.getScarto().getComponents());
            assertEquals(hiddenFromZero, hidden);
            if (!setOf(copy.getPlayerHand(1)).equals(setOf(state.getPlayerHand(1))))
                changed++;
        }
        assertTrue("player 1's hand never changed in 10 copies", changed > 0);
    }

    @Test
    public void aPlayersCopyDoesNotChangeTheOriginal() {
        List<TarotCard> hand1 = List.copyOf(state.getPlayerHand(1).getComponents());
        List<TarotCard> scarto = List.copyOf(state.getScarto().getComponents());
        state.copy(0);
        assertEquals(hand1, state.getPlayerHand(1).getComponents());
        assertEquals(scarto, state.getScarto().getComponents());
    }

    // ---- redeterminisation constrained by known voids ----

    private static final int N_COPIES = 50;

    /** Moves every card of these suits from player p's hand to the scarto, and records p as void in them. */
    private void makeVoid(int p, TarotCard.Suit... suits) {
        Set<TarotCard.Suit> suitSet = Set.of(suits);
        for (TarotCard c : List.copyOf(state.getPlayerHand(p).getComponents()))
            if (suitSet.contains(c.suit)) {
                state.getPlayerHand(p).remove(c);
                state.getScarto().addToBottom(c);
            }
        state.getKnownVoids().get(p).addAll(suitSet);
    }

    private static boolean holdsAny(ScartoGameState s, int p, TarotCard.Suit... suits) {
        Set<TarotCard.Suit> suitSet = Set.of(suits);
        return s.getPlayerHand(p).getComponents().stream().anyMatch(c -> suitSet.contains(c.suit));
    }

    private Set<TarotCard> hiddenFrom0(ScartoGameState s) {
        Set<TarotCard> hidden = new HashSet<>();
        hidden.addAll(s.getPlayerHand(1).getComponents());
        hidden.addAll(s.getPlayerHand(2).getComponents());
        hidden.addAll(s.getScarto().getComponents());
        return hidden;
    }

    @Test
    public void redeterminisedCopiesNeverGiveAPlayerACardOfASuitTheyAreVoidIn() {
        // player 1 is known void in Swords and Trumps, player 2 in Cups (their hands made to match). Player 0 cannot
        // see hands 1 and 2 or the scarto; the unseen Swords and Trumps (12 + 20) may go only to player 2 or the
        // scarto, the unseen Cups (10) only to player 1 or the scarto - there is room, so every copy must comply
        makeVoid(1, Swords, Trumps);
        makeVoid(2, Cups);
        assertAllCardsPresent(state);
        Set<TarotCard> hidden = hiddenFrom0(state);
        int size1 = state.getPlayerHand(1).getSize(), size2 = state.getPlayerHand(2).getSize();
        int sizeScarto = state.getScarto().getSize();

        int changed = 0;
        for (int i = 0; i < N_COPIES; i++) {
            ScartoGameState copy = (ScartoGameState) state.copy(0);
            assertFalse("copy " + i + " gave player 1 a Sword or Trump: " + copy.getPlayerHand(1),
                    holdsAny(copy, 1, Swords, Trumps));
            assertFalse("copy " + i + " gave player 2 a Cup: " + copy.getPlayerHand(2), holdsAny(copy, 2, Cups));
            assertEquals(size1, copy.getPlayerHand(1).getSize());
            assertEquals(size2, copy.getPlayerHand(2).getSize());
            assertEquals(sizeScarto, copy.getScarto().getSize());
            assertEquals(hidden, hiddenFrom0(copy));
            assertEquals(state.getPlayerHand(0).getComponents(), copy.getPlayerHand(0).getComponents());
            assertEquals(state.getKnownVoids(), copy.getKnownVoids());
            if (!setOf(copy.getPlayerHand(1)).equals(setOf(state.getPlayerHand(1))))
                changed++;
        }
        assertTrue("player 1's hand never changed in " + N_COPIES + " copies", changed > 0);
    }

    @Test
    public void aPlayerVoidInEverySuitMayStillBeGivenTheFool() {
        // player 1 holds only the Fool and is known void in all five suits: the Fool has no suit, so it is the only
        // card that may go to player 1, and every copy must give it back (as seen by player 0, and by player 2)
        giveHand(state, 1, fool());
        state.getKnownVoids().get(1).addAll(Set.of(Swords, Batons, Cups, Coins, Trumps));
        assertAllCardsPresent(state);
        for (int perspective : new int[]{0, 2})
            for (int i = 0; i < N_COPIES; i++) {
                ScartoGameState copy = (ScartoGameState) state.copy(perspective);
                assertEquals("copy " + i + " for player " + perspective,
                        List.of(fool()), copy.getPlayerHand(1).getComponents());
            }
    }

    @Test
    public void theScartoMayHoldCardsOfASuitTheDealerIsVoidIn() {
        // the scarto belongs to no player: the dealer's voids do not constrain it. All Cups but Cup 6 are seen (Cup 4
        // in player 0's hand, the rest in player 0's cardsWon); Cup 6 is in a 3-card scarto. Players 1 and 2 (the
        // dealer) are both known void in Cups, so in every copy Cup 6 must stay in the scarto
        assertEquals(2, state.getDealer());
        setCardsWon(state, 0, cup(KING), cup(2), cup(3), cup(1), cup(5), cup(7), cup(8), cup(9), cup(10),
                cup(KNAVE), cup(CAVALIER), cup(QUEEN));
        setScarto(state, state.getPlayerHand(1), cup(6), baton(1), baton(2));
        state.getKnownVoids().get(1).add(Cups);
        state.getKnownVoids().get(2).add(Cups);
        assertAllCardsPresent(state);
        assertFalse(holdsAny(state, 1, Cups));
        assertFalse(holdsAny(state, 2, Cups));
        for (int i = 0; i < N_COPIES; i++) {
            ScartoGameState copy = (ScartoGameState) state.copy(0);
            assertTrue("copy " + i + " moved Cup 6 out of the scarto: " + copy.getScarto(),
                    copy.getScarto().contains(cup(6)));
            assertEquals(3, copy.getScarto().getSize());
        }
    }

    @Test
    public void withoutRememberVoidsCopiesMayGiveAPlayerACardOfASuitTheyAreVoidIn() {
        // the voids of redeterminisedCopiesNeverGiveAPlayerACardOfASuitTheyAreVoidIn, but with rememberVoids = false
        // they are ignored: with 32 unseen Swords and Trumps among the 70-odd hidden cards, player 1 gets one in some
        // copy
        ((ScartoParameters) state.getGameParameters()).setParameterValue("rememberVoids", false);
        makeVoid(1, Swords, Trumps);
        makeVoid(2, Cups);
        int broken = 0;
        for (int i = 0; i < N_COPIES; i++) {
            ScartoGameState copy = (ScartoGameState) state.copy(0);
            if (holdsAny(copy, 1, Swords, Trumps))
                broken++;
        }
        assertTrue("player 1 never got a Sword or Trump in " + N_COPIES + " copies", broken > 0);
    }
}
