package games.lawnandorder;

import games.lawnandorder.components.LawnCard;
import games.lawnandorder.components.RuleCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static games.lawnandorder.LawnAndOrderGameState.Decision.CONTINUE;
import static games.lawnandorder.LawnAndOrderGameState.Phase.CONTINUE_OR_PASS;
import static games.lawnandorder.LawnAndOrderGameState.Phase.PLAY_OBJECT;
import static games.lawnandorder.LawnAndOrderGameState.PlayerStatus.*;
import static games.lawnandorder.LawnAndOrderRoundTest.*;
import static games.lawnandorder.LawnAndOrderTestUtils.*;
import static games.lawnandorder.components.LawnCard.Attribute.*;
import static games.lawnandorder.components.RuleCard.Special.*;
import static org.junit.Assert.*;

/**
 * The Special Rules Emergency Session and Zero Tolerance Policy. Two players unless stated.
 */
public class LawnAndOrderSpecialRulesTest {

    LawnAndOrderForwardModel fm;
    LawnAndOrderGameState state;

    @Before
    public void setup() {
        fm = new LawnAndOrderForwardModel();
        state = newState(2, 1);
    }

    // ---------------------------------------------------------------- Emergency Session

    @Test
    public void emergencySessionRevealsTheNextTwoRulesAndEachCitesEveryActiveLawn() {
        state = newState(3, 1);
        state.status[2] = PASSED;
        setLawn(state, 2, card(WATER_FEATURE, RED, PLASTIC), card(ORNAMENT, YELLOW, PLASTIC));
        setAgendaTop(state, special(EMERGENCY_SESSION), rule(PLASTIC), rule(RED), rule(BLUE));
        setLawn(state, 0, card(ORNAMENT, RED, ILLUMINATED), card(FURNITURE, YELLOW, PLASTIC), card(WATER_FEATURE, PINK, OVERSIZED));
        setLawn(state, 1, card(ORNAMENT, BLUE, PLASTIC));
        LawnCard c0 = card(STRUCTURE, RED, PLASTIC), c1 = card(FURNITURE, RED, OVERSIZED);
        putInHand(state, 0, c0);
        putInHand(state, 1, c1);
        int agendaSize = state.getAgenda().getSize();

        playTurn(fm, state, c0, c1, null);

        // nothing condemned before: no immediate citations. The Emergency Session reveals "No Plastic" and "No Red".
        // Player 0: Plastic x2 (Furniture Yellow Plastic, Structure Red Plastic) + Red x2 (Ornament Red Illuminated,
        // Structure Red Plastic) = 4 on 4 cards -> no bust
        assertEquals(4, state.getCitations(0));
        assertEquals(ACTIVE, state.getStatus(0));
        // Player 1: Plastic x1 + Red x1 = 2 on 2 cards -> no bust ("No Blue" is not revealed: it would make 3)
        assertEquals(2, state.getCitations(1));
        assertEquals(ACTIVE, state.getStatus(1));
        // Player 2 has passed: two Plastic cards, one Red, but immune
        assertEquals(0, state.getCitations(2));
        assertEquals(PASSED, state.getStatus(2));
        // three cards revealed this turn, in order; "No Blue" is next
        assertEquals(agendaSize - 3, state.getAgenda().getSize());
        assertEquals(rule(BLUE), state.getAgenda().peek());
        assertEquals(List.of(rule(RED), rule(PLASTIC), special(EMERGENCY_SESSION)), state.getRevealedRules().getComponents());
        assertFalse(state.isZeroTolerance());
        assertEquals(CONTINUE_OR_PASS, state.getGamePhase());
        assertEquals(List.of(0, 1), state.getPlayersStillToChoose());
        assertAllCardsPresent(state);
    }

    /**
     * Puts Emergency Session, "No Plastic" and "No Red" on top of the agenda and Ornament Red Plastic on lawn 0.
     * Returns {Furniture Red Plastic in player 0's hand, a card with no Plastic or Red in player 1's hand}.
     */
    private LawnCard[] emergencySessionOverPlasticAndRed() {
        setAgendaTop(state, special(EMERGENCY_SESSION), rule(PLASTIC), rule(RED));
        setLawn(state, 0, card(ORNAMENT, RED, PLASTIC));
        LawnCard c0 = card(FURNITURE, RED, PLASTIC), c1 = card(STRUCTURE, YELLOW, ILLUMINATED);
        putInHand(state, 0, c0);
        putInHand(state, 1, c1);
        return new LawnCard[]{c0, c1};
    }

    @Test
    public void emergencySessionChecksCeaseAndDesistOnceAfterBothReveals() {
        LawnCard[] cards = emergencySessionOverPlasticAndRed();
        playTurn(fm, state, cards);
        // Plastic x2 + Red x2 = 4 on 2 cards -> Cease & Desist (either rule alone would give 2 on 2: no bust)
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        assertEquals(0, state.getLawn(0).getSize());
        assertEquals(ACTIVE, state.getStatus(1));
        assertEquals(0, state.getCitations(1));
        assertEquals(List.of(rule(RED), rule(PLASTIC), special(EMERGENCY_SESSION)), state.getRevealedRules().getComponents());
        assertEquals(List.of(1), state.getPlayersStillToChoose());
        assertAllCardsPresent(state);
    }

    @Test
    public void withOneEmergencySessionRevealOnlyTheNextRuleIsRevealed() {
        state = newState(2, 1, Map.of("emergencySessionReveals", 1));
        LawnCard[] cards = emergencySessionOverPlasticAndRed();
        int agendaSize = state.getAgenda().getSize();
        playTurn(fm, state, cards);
        // only "No Plastic": 2 Plastic cards -> 2 on 2 cards, no bust; "No Red" stays on top of the agenda
        assertEquals(2, state.getCitations(0));
        assertEquals(ACTIVE, state.getStatus(0));
        assertEquals(agendaSize - 2, state.getAgenda().getSize());
        assertEquals(rule(RED), state.getAgenda().peek());
        assertEquals(List.of(rule(PLASTIC), special(EMERGENCY_SESSION)), state.getRevealedRules().getComponents());
    }

    @Test
    public void emergencySessionRevealingZeroToleranceAppliesAtThatTurnsCheck() {
        setAgendaTop(state, special(EMERGENCY_SESSION), special(ZERO_TOLERANCE), rule(PLASTIC));
        setLawn(state, 0, card(ORNAMENT, RED, PLASTIC), card(FURNITURE, BLUE, ILLUMINATED));
        state.citations[0] = 1;
        setLawn(state, 1, card(WATER_FEATURE, PINK, OVERSIZED));
        LawnCard c0 = card(STRUCTURE, YELLOW, PLASTIC), c1 = card(ORNAMENT, YELLOW, ILLUMINATED);
        putInHand(state, 0, c0);
        putInHand(state, 1, c1);

        playTurn(fm, state, c0, c1);

        // Player 0: 1 + Plastic x2 = 3 on 3 cards; Zero Tolerance limit 3 - 1 = 2 -> 3 > 2, Cease & Desist
        // (without Zero Tolerance 3 on 3 would survive). Player 1: 0 citations on 2 cards, limit 1 -> safe.
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        assertEquals(ACTIVE, state.getStatus(1));
        assertEquals(0, state.getCitations(1));
        assertTrue(state.isZeroTolerance());
        assertEquals(2 - 1, state.getCitationLimit(1));
        assertEquals(List.of(rule(PLASTIC), special(ZERO_TOLERANCE), special(EMERGENCY_SESSION)),
                state.getRevealedRules().getComponents());
        assertEquals(List.of(1), state.getPlayersStillToChoose());
        assertAllCardsPresent(state);
    }

    @Test
    public void emergencySessionWithOneCardLeftRevealsItAndTheRoundEnds() {
        // agenda: Emergency Session then "No Plastic" only; every other attribute condemned in earlier turns
        List<RuleCard> revealed = new ArrayList<>();
        for (LawnCard.Attribute a : LawnCard.Attribute.values())
            if (a != PLASTIC) revealed.add(rule(a));
        revealed.add(special(ADMINISTRATIVE_ERROR));
        setRules(state, List.of(special(EMERGENCY_SESSION), rule(PLASTIC)),
                List.of(special(ADMINISTRATIVE_ERROR), special(ZERO_TOLERANCE)), revealed);
        setLawn(state, 0, card(FURNITURE, BLUE, PLASTIC));
        state.citations[0] = 0;
        setLawn(state, 1, RED_WATER_LAWN[0], RED_WATER_LAWN[1]);
        state.citations[1] = 0;
        LawnCard c0 = card(ORNAMENT, BLUE, PLASTIC), c1 = RED_WATER_LAWN[2];
        putInHand(state, 0, c0);
        putInHand(state, 1, c1);

        playTurn(fm, state, c0, c1);

        // Player 0: immediate Ornament + Blue = 2; "No Plastic": 2 Plastic cards = +2 -> 4 on 2, Cease & Desist
        // (without "No Plastic" 2 on 2 would survive and score Colour 1, Feature 1). Player 1: immediate Water
        // Feature + Red = 2; "No Plastic" +1 -> 3 on 3, safe. The agenda is now empty: the round ends and player 1
        // scores as if they had passed.
        assertArrayEquals(new int[]{0, 0, 0}, tracks(state, 0));
        assertArrayEquals(RED_WATER_SCORE, tracks(state, 1));
        assertEquals(1, state.getRoundCounter());
        assertEquals(PLAY_OBJECT, state.getGamePhase());
        assertEquals(16 - 2, state.getAgenda().getSize());
        assertEquals(0, state.getRevealedRules().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void emergencySessionAsTheLastAgendaCardRevealsNothingMoreAndTheRoundEnds() {
        state.status[1] = PASSED;
        setLawn(state, 1, RED_WATER_LAWN);
        setLawn(state, 0, EXAMPLE_LAWN[0], EXAMPLE_LAWN[1], EXAMPLE_LAWN[2]);
        List<RuleCard> revealed = new ArrayList<>();
        for (LawnCard.Attribute a : LawnCard.Attribute.values())
            revealed.add(rule(a));
        revealed.add(special(ADMINISTRATIVE_ERROR));
        setRules(state, List.of(special(EMERGENCY_SESSION)), List.of(special(ADMINISTRATIVE_ERROR), special(ZERO_TOLERANCE)),
                revealed);
        state.citations[0] = 0;
        putInHand(state, 0, EXAMPLE_LAWN[3]);

        playTurn(fm, state, EXAMPLE_LAWN[3], null);

        // immediate Ornament + Yellow + Illuminated = 3 on 4 cards, safe; the Emergency Session finds nothing to
        // reveal (the Insider Tips are not part of the agenda); the agenda is empty, so the round ends
        assertArrayEquals(EXAMPLE_SCORE, tracks(state, 0));
        assertArrayEquals(RED_WATER_SCORE, tracks(state, 1));
        assertEquals(1, state.getRoundCounter());
        assertEquals(16 - 2, state.getAgenda().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void rulesRevealedByAnEmergencySessionGiveImmediateCitationsInLaterTurns() {
        setAgendaTop(state, special(EMERGENCY_SESSION), rule(PLASTIC), rule(RED), rule(REPURPOSED));
        LawnCard a0 = card(FURNITURE, BLUE, ILLUMINATED), a1 = card(STRUCTURE, YELLOW, OVERSIZED);
        LawnCard b0 = card(ORNAMENT, RED, PLASTIC), b1 = card(WATER_FEATURE, PINK, ILLUMINATED);
        putInHand(state, 0, a0, b0);
        putInHand(state, 1, a1, b1);

        // Turn 1: neither card is Plastic or Red
        playTurn(fm, state, a0, a1);
        assertArrayEquals(new int[]{0, 0}, state.citations);
        decide(fm, state, CONTINUE, CONTINUE);

        // Turn 2: Red and Plastic, both condemned by the earlier Emergency Session: +2 immediately;
        // "No Repurposed" finds nothing -> 2 on 2 cards, safe
        playTurn(fm, state, b0, b1);
        assertArrayEquals(new int[]{2, 0}, state.citations);
        assertEquals(ACTIVE, state.getStatus(0));
        assertEquals(List.of(rule(REPURPOSED), rule(RED), rule(PLASTIC), special(EMERGENCY_SESSION)),
                state.getRevealedRules().getComponents());
    }

    // ---------------------------------------------------------------- Zero Tolerance

    @Test
    public void rulebookWithZeroToleranceCitationsEqualToCardCountTriggerCeaseAndDesist() {
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 3, rule(REPURPOSED), special(ZERO_TOLERANCE));
        playTurn(fm, state, cards);
        // 3 + 1 = 4 citations on 4 cards; Zero Tolerance limit 4 - 1 = 3 -> 4 > 3, Cease & Desist
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        assertEquals(0, state.getLawn(0).getSize());
        assertEquals(ACTIVE, state.getStatus(1));
        assertEquals(List.of(1), state.getPlayersStillToChoose());
        assertAllCardsPresent(state);
    }

    @Test
    public void withZeroToleranceFourCardsAndThreeCitationsDoNotBust() {
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 2, rule(REPURPOSED), special(ZERO_TOLERANCE));
        playTurn(fm, state, cards);
        // 2 + 1 = 3 citations on 4 cards; limit 4 - 1 = 3 -> not more, safe
        assertEquals(3, state.getCitations(0));
        assertEquals(ACTIVE, state.getStatus(0));
        assertTrue(state.isZeroTolerance());
        assertEquals(4 - 1, state.getCitationLimit(0));
    }

    @Test
    public void withZeroToleranceReductionTwoFourCardsAndThreeCitationsBust() {
        state = newState(2, 1, Map.of("zeroToleranceReduction", 2));
        // player 1 needs a lawn big enough to survive the larger reduction
        setLawn(state, 1, card(WATER_FEATURE, RED, ILLUMINATED), card(STRUCTURE, BLUE, PLASTIC));
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 2, rule(REPURPOSED), special(ZERO_TOLERANCE));
        playTurn(fm, state, cards);
        // 2 + 1 = 3 citations on 4 cards; limit 4 - 2 = 2 -> 3 > 2, Cease & Desist
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        // player 1: 0 citations on 3 cards, limit 3 - 2 = 1 -> safe
        assertEquals(0, state.getCitations(1));
        assertEquals(ACTIVE, state.getStatus(1));
        assertEquals(3 - 2, state.getCitationLimit(1));
    }

    @Test
    public void zeroToleranceRevealedThisTurnAppliesAtThatTurnsCheck() {
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 3, special(ZERO_TOLERANCE));
        playTurn(fm, state, cards);
        // the Pink card: 3 + 1 = 4 on 4 cards; Zero Tolerance is revealed in Step 3 and applies at once: limit 3
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        assertTrue(state.isZeroTolerance());
        assertEquals(List.of(special(ZERO_TOLERANCE), rule(PINK)), state.getRevealedRules().getComponents());
    }

    @Test
    public void zeroToleranceStillAppliesInALaterTurnOfTheRound() {
        revealEarlier(state, rule(PINK), rule(OVERSIZED));
        setAgendaTop(state, special(ZERO_TOLERANCE), rule(REPURPOSED));
        setLawn(state, 0, card(FURNITURE, BLUE, PLASTIC), card(STRUCTURE, RED, ILLUMINATED));
        state.citations[0] = 2;
        LawnCard a0 = card(WATER_FEATURE, YELLOW, ILLUMINATED), b0 = card(ORNAMENT, PINK, OVERSIZED);
        LawnCard a1 = card(ORNAMENT, BLUE, ILLUMINATED), b1 = card(FURNITURE, YELLOW, PLASTIC);
        putInHand(state, 0, a0, b0);
        putInHand(state, 1, a1, b1);

        // Turn 1: harmless cards; Zero Tolerance revealed. Player 0: 2 on 3 cards, limit 2 -> safe
        playTurn(fm, state, a0, a1);
        assertEquals(ACTIVE, state.getStatus(0));
        assertTrue(state.isZeroTolerance());
        assertEquals(3 - 1, state.getCitationLimit(0));
        decide(fm, state, CONTINUE, CONTINUE);

        // Turn 2: Pink and Oversized, both condemned: 2 + 2 = 4 on 4 cards; still Zero Tolerance, limit 3 -> bust.
        // Player 1: 0 on 2 cards, limit 1 -> safe
        playTurn(fm, state, b0, b1);
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        assertEquals(ACTIVE, state.getStatus(1));
        assertTrue(state.isZeroTolerance());
        assertEquals(List.of(1), state.getPlayersStillToChoose());
        assertAllCardsPresent(state);
    }

    @Test
    public void aPassedPlayerWithCitationsEqualToCardsIsNotBustedByZeroTolerance() {
        state.status[1] = PASSED;
        setLawn(state, 1, RED_WATER_LAWN);
        state.citations[1] = 3;
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 3, special(ZERO_TOLERANCE));

        playTurn(fm, state, cards[0], null);

        // Player 0 (active): 3 + 1 = 4 on 4 cards, limit 3 -> Cease & Desist, so nobody is active and the round ends.
        // Player 1 (passed): 3 on 3 cards, never checked again -> scores the Red Water lawn
        assertArrayEquals(new int[]{0, 0, 0}, tracks(state, 0));
        assertArrayEquals(RED_WATER_SCORE, tracks(state, 1));
        assertEquals(1, state.getRoundCounter());
        assertAllCardsPresent(state);
    }

    @Test
    public void zeroToleranceEndsWithTheRound() {
        revealEarlier(state, special(ZERO_TOLERANCE));
        setLawn(state, 0, card(FURNITURE, BLUE, PLASTIC), card(STRUCTURE, RED, ILLUMINATED));
        assertTrue(state.isZeroTolerance());
        assertEquals(2 - 1, state.getCitationLimit(0));

        state.setGamePhase(CONTINUE_OR_PASS);
        everyonePasses(fm, state);

        // the new round: nothing revealed, empty lawns, the full limit (0 - 0)
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getRevealedRules().getSize());
        assertFalse(state.isZeroTolerance());
        assertEquals(0, state.getCitationLimit(0));
    }
}
