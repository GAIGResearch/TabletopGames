package games.lawnandorder;

import games.lawnandorder.components.LawnCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static games.lawnandorder.LawnAndOrderGameState.Phase.CONTINUE_OR_PASS;
import static games.lawnandorder.LawnAndOrderGameState.PlayerStatus.*;
import static games.lawnandorder.LawnAndOrderTestUtils.*;
import static games.lawnandorder.components.LawnCard.Attribute.*;
import static games.lawnandorder.components.RuleCard.Special.ADMINISTRATIVE_ERROR;
import static org.junit.Assert.*;

/**
 * The resolution of a Play Object step: cards onto lawns (Step 1), immediate citations (Step 2), the HOA Agenda
 * (Step 3), retroactive citations (Step 4) and the Cease &amp; Desist check (Step 5), including the rulebook's
 * citation examples. Two players; the agenda card is always arranged, and is a Standard Rule or an Administrative
 * Error (the other Special Rules are tested in LawnAndOrderSpecialRulesTest).
 */
public class LawnAndOrderCitationTest {

    LawnAndOrderForwardModel fm;
    LawnAndOrderGameState state;

    @Before
    public void setup() {
        fm = new LawnAndOrderForwardModel();
        state = newState(2, 1);
    }

    @Test
    public void rulebookExample1ImmediateCitationForEachCondemnedAttributeOfTheCardPlayed() {
        revealEarlier(state, rule(WATER_FEATURE), rule(PINK));
        setAgendaTop(state, rule(REPURPOSED));
        setLawn(state, 0, card(ORNAMENT, RED, ILLUMINATED), card(FURNITURE, BLUE, PLASTIC));
        LawnCard played = card(WATER_FEATURE, PINK, OVERSIZED);
        LawnCard other = card(STRUCTURE, YELLOW, ILLUMINATED);
        putInHand(state, 0, played);
        putInHand(state, 1, other);
        int agendaSize = state.getAgenda().getSize();

        playTurn(fm, state, played, other);

        // Water Feature +1, Pink +1, Oversized is not condemned; the new "No Repurposed" finds no Repurposed card
        assertEquals(2, state.getCitations(0));
        assertEquals(0, state.getCitations(1));
        // Step 1: the card is on top of the lawn; 3 cards, 2 citations - no Cease & Desist
        assertEquals(3, state.getLawn(0).getSize());
        assertEquals(played, state.getLawn(0).peek());
        assertEquals(List.of(other), state.getLawn(1).getComponents());
        assertEquals(0, state.getChosenCard(0).getSize());
        assertEquals(4, state.getHand(0).getSize());
        assertEquals(ACTIVE, state.getStatus(0));
        // Step 3: the top agenda card is revealed and stays in force
        assertEquals(agendaSize - 1, state.getAgenda().getSize());
        assertEquals(List.of(rule(REPURPOSED), rule(PINK), rule(WATER_FEATURE)), state.getRevealedRules().getComponents());
        assertEquals(CONTINUE_OR_PASS, state.getGamePhase());
        assertEquals(List.of(0, 1), state.getPlayersStillToChoose());
        assertTrue(state.getPlayersStillToChoose().contains(state.getCurrentPlayer()));
        assertAllCardsPresent(state);
    }

    @Test
    public void rulebookExample2NewlyCondemnedAttributeCitesEachLawnCardWithIt() {
        setAgendaTop(state, rule(PLASTIC));
        setLawn(state, 0, card(FURNITURE, BLUE, PLASTIC), card(ORNAMENT, RED, PLASTIC));
        LawnCard played = card(STRUCTURE, YELLOW, ILLUMINATED);
        LawnCard other = card(WATER_FEATURE, PINK, OVERSIZED);
        putInHand(state, 0, played);
        putInHand(state, 1, other);

        playTurn(fm, state, played, other);

        // nothing was condemned before: no immediate citations; "No Plastic": two Plastic cards on lawn 0 -> +2
        assertEquals(2, state.getCitations(0));
        assertEquals(0, state.getCitations(1));
        assertEquals(ACTIVE, state.getStatus(0));   // 3 cards, 2 citations
    }

    @Test
    public void retroactiveCitationsCountTheCardJustPlayedOnce() {
        setAgendaTop(state, rule(PLASTIC));
        setLawn(state, 0, card(ORNAMENT, RED, PLASTIC));
        LawnCard played = card(FURNITURE, BLUE, PLASTIC);
        LawnCard other = card(STRUCTURE, YELLOW, ILLUMINATED);
        putInHand(state, 0, played);
        putInHand(state, 1, other);

        playTurn(fm, state, played, other);

        // Step 2 uses only the rules revealed before this turn (none); Step 4: two Plastic cards on the lawn, the one
        // already there and the one just played -> 2 (not 3: the new rule gives no immediate citation as well)
        assertEquals(2, state.getCitations(0));
        assertEquals(ACTIVE, state.getStatus(0));   // 2 cards, 2 citations
    }

    @Test
    public void passedPlayersAreImmuneToRetroactiveCitations() {
        state.status[1] = PASSED;
        setLawn(state, 1, card(ORNAMENT, BLUE, PLASTIC), card(FURNITURE, RED, PLASTIC));
        setAgendaTop(state, rule(PLASTIC));
        setLawn(state, 0, card(WATER_FEATURE, PINK, PLASTIC));
        LawnCard played = card(STRUCTURE, YELLOW, ILLUMINATED);
        putInHand(state, 0, played);

        playTurn(fm, state, played, null);

        // active player 0: one Plastic card -> +1; player 1 has passed: two Plastic cards but no citation
        assertEquals(1, state.getCitations(0));
        assertEquals(0, state.getCitations(1));
        assertEquals(PASSED, state.getStatus(1));
        assertEquals(2, state.getLawn(1).getSize());
        assertEquals(CONTINUE_OR_PASS, state.getGamePhase());
        assertEquals(List.of(0), state.getPlayersStillToChoose());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void rulebookExample3FourCardsAndFourCitationsDoNotBust() {
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 3, rule(REPURPOSED));
        playTurn(fm, state, cards);
        // 3 + 1 = 4 citations on 4 cards: not more than the lawn size
        assertEquals(4, state.getCitations(0));
        assertEquals(ACTIVE, state.getStatus(0));
        assertEquals(4, state.getLawn(0).getSize());
        assertEquals(List.of(0, 1), state.getPlayersStillToChoose());
    }

    @Test
    public void rulebookExample3FourCardsAndFiveCitationsBust() {
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 4, rule(REPURPOSED));
        List<LawnCard> lawnAfter = new ArrayList<>(state.getLawn(0).getComponents());
        lawnAfter.add(cards[0]);
        List<LawnCard> handAfter = new ArrayList<>(state.getHand(0).getComponents());
        handAfter.remove(cards[0]);

        playTurn(fm, state, cards);

        // 4 + 1 = 5 citations on 4 cards: Cease & Desist
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        assertEquals(0, state.getLawn(0).getSize());
        assertEquals(0, state.getHand(0).getSize());
        assertEquals(0, state.getChosenCard(0).getSize());
        // the lawn goes to the discard deck face up, the hand seen only by its owner
        assertEquals(8, state.getDiscardDeck().getSize());
        for (int i = 0; i < 8; i++) {
            LawnCard c = state.getDiscardDeck().get(i);
            if (lawnAfter.contains(c))
                assertArrayEquals("lawn card " + c, new boolean[]{true, true}, state.getDiscardDeck().getVisibilityOfComponent(i));
            else {
                assertTrue(c + " was neither on the lawn nor in the hand", handAfter.contains(c));
                assertArrayEquals("hand card " + c, new boolean[]{true, false}, state.getDiscardDeck().getVisibilityOfComponent(i));
            }
        }
        List<LawnCard> discarded = new ArrayList<>(lawnAfter);
        discarded.addAll(handAfter);
        assertEquals(multiset(discarded), multiset(state.getDiscardDeck().getComponents()));
        // player 1 is unaffected and alone to decide
        assertEquals(ACTIVE, state.getStatus(1));
        assertEquals(List.of(cards[1]), state.getLawn(1).getComponents());
        assertEquals(CONTINUE_OR_PASS, state.getGamePhase());
        assertEquals(List.of(1), state.getPlayersStillToChoose());
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void retroactiveCitationsCanBustThePlayerInTheSameTurn() {
        setAgendaTop(state, rule(PLASTIC));
        setLawn(state, 0, card(ORNAMENT, RED, PLASTIC), card(FURNITURE, BLUE, PLASTIC));
        state.citations[0] = 1;
        LawnCard played = card(STRUCTURE, YELLOW, PLASTIC);
        LawnCard other = card(WATER_FEATURE, PINK, OVERSIZED);
        putInHand(state, 0, played);
        putInHand(state, 1, other);

        playTurn(fm, state, played, other);

        // no immediate citation; "No Plastic": three Plastic cards -> 1 + 3 = 4 citations on 3 cards
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        assertEquals(0, state.getLawn(0).getSize());
        assertEquals(ACTIVE, state.getStatus(1));
    }

    @Test
    public void administrativeErrorIsRevealedButHasNoEffect() {
        setAgendaTop(state, special(ADMINISTRATIVE_ERROR));
        setLawn(state, 0, card(ORNAMENT, RED, PLASTIC));
        LawnCard played = card(FURNITURE, BLUE, ILLUMINATED);
        LawnCard other = card(STRUCTURE, YELLOW, OVERSIZED);
        putInHand(state, 0, played);
        putInHand(state, 1, other);
        int agendaSize = state.getAgenda().getSize();

        playTurn(fm, state, played, other);

        assertEquals(List.of(special(ADMINISTRATIVE_ERROR)), state.getRevealedRules().getComponents());
        assertEquals(agendaSize - 1, state.getAgenda().getSize());
        assertEquals(0, state.getCitations(0));
        assertEquals(0, state.getCitations(1));
        assertEquals(List.of(0, 1), state.getPlayersStillToChoose());
        assertEquals(CONTINUE_OR_PASS, state.getGamePhase());
    }
}
