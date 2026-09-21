package games.tricktaking;

import core.components.Deck;
import core.components.FrenchCard;
import games.whist.WhistForwardModel;
import games.whist.WhistGameState;
import games.whist.WhistParameters;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static core.components.FrenchCard.Suite.Hearts;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * The shared PlayCard action, executed directly on a Whist state (not through the forward model).
 */
public class PlayCardTest {

    WhistGameState state;

    @Before
    public void setup() {
        WhistParameters params = new WhistParameters();
        params.setRandomSeed(42);
        state = new WhistGameState(params, 4);
        new WhistForwardModel().setup(state);
    }

    /**
     * Moves the card into the player's hand from whichever hand holds it, swapping one of the player's other cards
     * (not in {@code keep}) back, so hand sizes and the 52 cards are unchanged.
     */
    private void moveToHand(int player, FrenchCard card, FrenchCard... keep) {
        Deck<FrenchCard> to = state.getPlayerHand(player);
        if (to.contains(card)) return;
        Set<FrenchCard> kept = Set.of(keep);
        for (int p = 0; p < 4; p++) {
            Deck<FrenchCard> from = state.getPlayerHand(p);
            if (from.contains(card)) {
                FrenchCard swap = to.getComponents().stream().filter(c -> !kept.contains(c)).findFirst().orElseThrow();
                from.remove(card);
                to.remove(swap);
                to.add(card);
                from.add(swap);
                return;
            }
        }
        fail(card + " is in no hand");
    }

    @Test
    public void leadingRemovesTheCardFromTheCurrentPlayersHandAndStartsTheTrick() {
        moveToHand(0, card("5H"));
        assertEquals(0, state.getCurrentPlayer());

        new PlayCard(card("5H")).execute(state);

        assertFalse(state.getPlayerHand(0).contains(card("5H")));
        assertEquals(12, state.getPlayerHand(0).getSize());
        assertEquals(cards("5H"), cardsOf(state.getCurrentTrick()));
        assertEquals(Set.of(), state.getKnownVoids().get(0));   // leading reveals nothing
    }

    @Test
    public void notFollowingRecordsAVoidAndFollowingDoesNot() {
        moveToHand(0, card("5H"));
        moveToHand(1, card("2C"));
        moveToHand(2, card("KH"));
        new PlayCard(card("5H")).execute(state);

        state.setTurnOwner(1);
        new PlayCard(card("2C")).execute(state);
        assertFalse(state.getPlayerHand(1).contains(card("2C")));
        assertEquals(12, state.getPlayerHand(1).getSize());
        assertEquals(12, state.getPlayerHand(0).getSize());   // player 0's hand unchanged since leading
        assertEquals(Set.of(Hearts), state.getKnownVoids().get(1));

        state.setTurnOwner(2);
        new PlayCard(card("KH")).execute(state);
        assertEquals(Set.of(), state.getKnownVoids().get(2));
        assertEquals(cards("5H", "2C", "KH"), cardsOf(state.getCurrentTrick()));
    }

    @Test
    public void playingACardNotInTheHandThrowsAndLeavesTheTrickEmpty() {
        moveToHand(1, card("AS"));   // so player 0 does not hold it
        assertThrows(IllegalArgumentException.class, () -> new PlayCard(card("AS")).execute(state));
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(13, state.getPlayerHand(0).getSize());
    }

    private void setRememberVoids(boolean value) {
        ((WhistParameters) state.getGameParameters()).setParameterValue("rememberVoids", value);
    }

    /** Player 0 leads 5H; player 1 (holding 2C) discards it; player 2 (holding KH) follows. */
    private void leadHeartsAndDiscardAClub() {
        moveToHand(0, card("5H"));
        moveToHand(1, card("2C"));
        moveToHand(2, card("KH"));
        new PlayCard(card("5H")).execute(state);
        state.setTurnOwner(1);
        new PlayCard(card("2C")).execute(state);
        state.setTurnOwner(2);
        new PlayCard(card("KH")).execute(state);
    }

    @Test
    public void withRememberVoidsOffNotFollowingRecordsNothing() {
        WhistParameters params = new WhistParameters();
        params.setRandomSeed(42);
        params.setParameterValue("rememberVoids", false);
        state = new WhistGameState(params, 4);
        new WhistForwardModel().setup(state);

        leadHeartsAndDiscardAClub();

        assertEquals(Set.of(), state.getKnownVoids().get(1));
        assertEquals(cards("5H", "2C", "KH"), cardsOf(state.getCurrentTrick()));   // the play itself still happens
        assertEquals(12, state.getPlayerHand(1).getSize());
    }

    @Test
    public void rememberVoidsIsReadAtTheTimeOfThePlay() {
        // default (on) at setup, switched off before the plays: nothing recorded
        setRememberVoids(false);
        leadHeartsAndDiscardAClub();
        assertEquals(Set.of(), state.getKnownVoids().get(1));
    }

    @Test
    public void withRememberVoidsOffRedeterminisationMayGiveTheSuitToAPlayerWhoFailedToFollow() {
        setRememberVoids(false);
        FrenchCard trumpCard = state.getTrumpCard();
        moveToHand(0, card("5H"));
        // Give player 1 no hearts: swap each of their hearts for a non-heart of player 2's (never the turned-up card)
        Deck<FrenchCard> h1 = state.getPlayerHand(1), h2 = state.getPlayerHand(2);
        for (FrenchCard heart : h1.getComponents().stream().filter(c -> c.suite == Hearts).toList()) {
            FrenchCard swap = h2.getComponents().stream()
                    .filter(c -> c.suite != Hearts && !c.equals(trumpCard)).findFirst().orElseThrow();
            h1.remove(heart);
            h2.remove(swap);
            h1.add(swap);
            h2.add(heart);
        }
        FrenchCard discard = h1.get(0);   // not a heart
        new PlayCard(card("5H")).execute(state);
        state.setTurnOwner(1);
        new PlayCard(discard).execute(state);
        assertEquals(Set.of(), state.getKnownVoids().get(1));
        long heartsHiddenFrom0 = state.getPlayerHand(2).getComponents().stream().filter(c -> c.suite == Hearts).count()
                + state.getPlayerHand(3).getComponents().stream().filter(c -> c.suite == Hearts).count();
        assertTrue("arrangement needs hearts in hands 2/3", heartsHiddenFrom0 > 0);

        int copiesWithHeartsFor1 = 0;
        for (int i = 0; i < 50; i++) {
            WhistGameState copy = (WhistGameState) state.copy(0);
            if (copy.getPlayerHand(1).getComponents().stream().anyMatch(c -> c.suite == Hearts))
                copiesWithHeartsFor1++;
        }
        // nothing recorded, so player 1 is dealt from the unseen cards like anyone else
        assertTrue(copiesWithHeartsFor1 > 0);
    }

    @Test
    public void equalityIsByCard() {
        assertEquals(new PlayCard(card("5H")), new PlayCard(card("5H")));
        assertEquals(new PlayCard(card("5H")).hashCode(), new PlayCard(card("5H")).hashCode());
        assertNotEquals(new PlayCard(card("5H")), new PlayCard(card("5D")));
    }
}
