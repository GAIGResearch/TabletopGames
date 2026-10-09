package games.lawnandorder;

import games.lawnandorder.actions.Continue;
import games.lawnandorder.actions.PlayObject;
import games.lawnandorder.components.LawnCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static games.lawnandorder.LawnAndOrderGameState.Decision.*;
import static games.lawnandorder.LawnAndOrderTestUtils.*;
import static games.lawnandorder.components.LawnCard.Attribute.*;
import static org.junit.Assert.*;

/**
 * Full copies, and what a copy for a player hides and reshuffles.
 */
public class LawnAndOrderCopyTest {

    LawnAndOrderForwardModel fm;
    LawnAndOrderGameState state;

    @Before
    public void setup() {
        fm = new LawnAndOrderForwardModel();
        state = newState(3, 4);
        setAgendaTop(state, rule(REPURPOSED), rule(PLASTIC));
    }

    /** Plays one turn in which everyone plays a card and continues, then gives the turn to player 0. */
    private void playOneTurn() {
        playTurn(fm, state, state.getHand(0).get(0), state.getHand(1).get(0), state.getHand(2).get(0));
        decide(fm, state, CONTINUE, CONTINUE, CONTINUE);
        // the rules do not say who holds the turn after a resolution; from player 0 it passes 0, 1, 2
        state.setTurnOwner(0);
    }

    @Test
    public void fullCopyIsEqualAndIndependent() {
        playOneTurn();
        LawnCard x = state.getHand(0).get(1);
        fm.next(state, new PlayObject(0, x));

        LawnAndOrderGameState copy = (LawnAndOrderGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        fm.next(copy, new PlayObject(1, copy.getHand(1).get(0)));
        assertNotEquals(state, copy);
        assertEquals(List.of(x), state.getChosenCard(0).getComponents());
        assertEquals(0, state.getChosenCard(1).getSize());
        assertEquals(5, state.getHand(1).getSize());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, copy.getChosenCard(1).getSize());
    }

    @Test
    public void copyForAPlayerReturnsTheOthersChosenCardsToTheirHands() {
        playOneTurn();
        LawnCard x = state.getHand(0).get(1), y = state.getHand(1).get(2);
        fm.next(state, new PlayObject(0, x));
        fm.next(state, new PlayObject(1, y));
        assertEquals(2, state.getCurrentPlayer());

        // player 2, still to choose, sees no choice made and holds the turn
        LawnAndOrderGameState seenBy2 = (LawnAndOrderGameState) state.copy(2);
        assertEquals(0, seenBy2.getChosenCard(0).getSize());
        assertEquals(0, seenBy2.getChosenCard(1).getSize());
        assertEquals(5, seenBy2.getHand(0).getSize());
        assertEquals(5, seenBy2.getHand(1).getSize());
        assertEquals(List.of(0, 1, 2), seenBy2.getPlayersStillToChoose());
        assertEquals(2, seenBy2.getCurrentPlayer());
        assertEquals(state.getHand(2).getComponents(), seenBy2.getHand(2).getComponents());
        assertAllCardsPresent(seenBy2);

        // player 1 keeps their own choice; player 0's goes back to their hand
        LawnAndOrderGameState seenBy1 = (LawnAndOrderGameState) state.copy(1);
        assertEquals(List.of(y), seenBy1.getChosenCard(1).getComponents());
        assertEquals(state.getHand(1).getComponents(), seenBy1.getHand(1).getComponents());
        assertEquals(0, seenBy1.getChosenCard(0).getSize());
        assertEquals(5, seenBy1.getHand(0).getSize());
        assertEquals(List.of(0, 2), seenBy1.getPlayersStillToChoose());
        assertTrue(seenBy1.getPlayersStillToChoose().contains(seenBy1.getCurrentPlayer()));
        assertAllCardsPresent(seenBy1);

        // face-up cards unchanged; the master state untouched
        for (int p = 0; p < 3; p++)
            assertEquals(state.getLawn(p).getComponents(), seenBy1.getLawn(p).getComponents());
        assertEquals(state.getRevealedRules().getComponents(), seenBy1.getRevealedRules().getComponents());
        assertEquals(List.of(x), state.getChosenCard(0).getComponents());
        assertEquals(List.of(y), state.getChosenCard(1).getComponents());
    }

    @Test
    public void copyForAPlayerResetsTheOthersDecisions() {
        playTurn(fm, state, state.getHand(0).get(0), state.getHand(1).get(0), state.getHand(2).get(0));
        int d = state.getCurrentPlayer();
        fm.next(state, new Continue(d));
        int q = state.getPlayersStillToChoose().get(0);

        // a player still to decide does not see d's decision, and holds the turn
        LawnAndOrderGameState seenByQ = (LawnAndOrderGameState) state.copy(q);
        assertEquals(NONE, seenByQ.getDecision(d));
        assertEquals(List.of(0, 1, 2), seenByQ.getPlayersStillToChoose());
        assertEquals(q, seenByQ.getCurrentPlayer());

        // d keeps their own decision
        LawnAndOrderGameState seenByD = (LawnAndOrderGameState) state.copy(d);
        assertEquals(CONTINUE, seenByD.getDecision(d));
        assertEquals(state.getPlayersStillToChoose(), seenByD.getPlayersStillToChoose());
        assertFalse(seenByD.getPlayersStillToChoose().contains(d));
        assertEquals(CONTINUE, state.getDecision(d));
    }

    @Test
    public void copyForAPlayerReshufflesTheHiddenCards() {
        playOneTurn();
        LawnAndOrderGameState a = (LawnAndOrderGameState) state.copy(0);
        LawnAndOrderGameState b = (LawnAndOrderGameState) state.copy(0);
        for (LawnAndOrderGameState copy : List.of(a, b)) {
            assertEquals(state.getHand(0).getComponents(), copy.getHand(0).getComponents());
            for (int p = 0; p < 3; p++) {
                assertEquals(state.getHand(p).getSize(), copy.getHand(p).getSize());
                assertEquals(state.getLawn(p).getComponents(), copy.getLawn(p).getComponents());
            }
            assertEquals(state.getRevealedRules().getComponents(), copy.getRevealedRules().getComponents());
            assertEquals(state.getAgenda().getSize(), copy.getAgenda().getSize());
            assertEquals(state.getInsiderTips().getSize(), copy.getInsiderTips().getSize());
            assertAllCardsPresent(copy);
        }
        // two copies that differ only in the random numbers drawn for them
        assertNotEquals(new HashSet<>(a.getHand(1).getComponents()), new HashSet<>(b.getHand(1).getComponents()));
        assertNotEquals(a.getDrawDeck().getComponents(), b.getDrawDeck().getComponents());
        assertNotEquals(a.getAgenda().getComponents(), b.getAgenda().getComponents());
    }

    @Test
    public void aBustedHandIsHiddenFromTheOthersButTheBustedLawnIsNot() {
        state = newState(2, 4);
        revealEarlier(state, rule(PINK));
        setAgendaTop(state, rule(REPURPOSED));
        setLawn(state, 0, card(FURNITURE, BLUE, PLASTIC));
        state.citations[0] = 2;
        LawnCard played = card(ORNAMENT, PINK, OVERSIZED), other = card(STRUCTURE, RED, ILLUMINATED);
        putInHand(state, 0, played);
        putInHand(state, 1, other);
        // Pink condemned: 2 + 1 = 3 citations on 2 cards -> Cease & Desist (2 lawn cards and 4 hand cards discarded)
        playTurn(fm, state, played, other);
        assertEquals("arrangement: player 0 busted", LawnAndOrderGameState.PlayerStatus.CEASE_AND_DESIST, state.getStatus(0));
        assertEquals(2 + 4, state.getDiscardDeck().getSize());

        LawnAndOrderGameState seenBy0 = (LawnAndOrderGameState) state.copy(0);
        assertEquals(state.getDiscardDeck().getComponents(), seenBy0.getDiscardDeck().getComponents());
        LawnAndOrderGameState seenBy1 = (LawnAndOrderGameState) state.copy(1);
        assertEquals(state.getDiscardDeck().getVisibleComponents(1), seenBy1.getDiscardDeck().getVisibleComponents(1));
        assertAllCardsPresent(seenBy1);
        // the visible cards are unchanged, so a difference is in the busted hand, redrawn from the unseen cards
        boolean redrawn = false;
        for (int i = 0; i < 20 && !redrawn; i++) {
            LawnAndOrderGameState copy = (LawnAndOrderGameState) state.copy(1);
            redrawn = !copy.getDiscardDeck().getComponents().equals(state.getDiscardDeck().getComponents());
        }
        assertTrue("the busted hand is redrawn in player 1's copies", redrawn);
    }
}
