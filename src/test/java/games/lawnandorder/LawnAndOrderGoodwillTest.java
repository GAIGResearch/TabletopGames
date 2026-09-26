package games.lawnandorder;

import games.lawnandorder.components.LawnCard;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static games.lawnandorder.LawnAndOrderGameState.Phase.CONTINUE_OR_PASS;
import static games.lawnandorder.LawnAndOrderGameState.PlayerStatus.*;
import static games.lawnandorder.LawnAndOrderRoundTest.*;
import static games.lawnandorder.LawnAndOrderTestUtils.*;
import static games.lawnandorder.components.LawnCard.Attribute.*;
import static games.lawnandorder.components.RuleCard.Special.ZERO_TOLERANCE;
import static org.junit.Assert.*;

/**
 * Goodwill: the raised citation limit, and who holds Goodwill from one round to the next. Two players.
 */
public class LawnAndOrderGoodwillTest {

    LawnAndOrderForwardModel fm;
    LawnAndOrderGameState state;

    @Before
    public void setup() {
        fm = new LawnAndOrderForwardModel();
        state = newState(2, 1);
    }

    // ---------------------------------------------------------------- the raised limit

    @Test
    public void withGoodwillFiveCitationsOnFourCardsDoNotBust() {
        state.goodwill[0] = true;
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 4, rule(REPURPOSED));
        playTurn(fm, state, cards);
        // 4 + 1 = 5 citations on 4 cards; limit 4 + 1 (Goodwill) = 5 -> not more, safe (without Goodwill 5 > 4)
        assertEquals(5, state.getCitations(0));
        assertEquals(ACTIVE, state.getStatus(0));
        assertEquals(4 + 1, state.getCitationLimit(0));
        assertEquals(CONTINUE_OR_PASS, state.getGamePhase());
    }

    @Test
    public void withGoodwillSixCitationsOnFourCardsBust() {
        state.goodwill[0] = true;
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 5, rule(REPURPOSED));
        playTurn(fm, state, cards);
        // 5 + 1 = 6 citations on 4 cards; limit 4 + 1 = 5 -> 6 > 5, Cease & Desist
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        assertEquals(0, state.getLawn(0).getSize());
        assertEquals(ACTIVE, state.getStatus(1));
        assertAllCardsPresent(state);
    }

    @Test
    public void withGoodwillBonusTwoSixCitationsOnFourCardsDoNotBust() {
        state = newState(2, 1, Map.of("goodwillBonus", 2));
        state.goodwill[0] = true;
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 5, rule(REPURPOSED));
        playTurn(fm, state, cards);
        // 5 + 1 = 6 citations on 4 cards; limit 4 + 2 (Goodwill, bonus 2) = 6 -> not more, safe
        assertEquals(6, state.getCitations(0));
        assertEquals(ACTIVE, state.getStatus(0));
        assertEquals(4 + 2, state.getCitationLimit(0));
    }

    @Test
    public void withGoodwillAndZeroToleranceFourCitationsOnFourCardsDoNotBust() {
        state.goodwill[0] = true;
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 3, rule(REPURPOSED), special(ZERO_TOLERANCE));
        playTurn(fm, state, cards);
        // 3 + 1 = 4 citations on 4 cards; limit 4 + 1 (Goodwill) - 1 (Zero Tolerance) = 4 -> safe
        // (Zero Tolerance alone would give limit 3 and a Cease & Desist)
        assertTrue(state.isZeroTolerance());
        assertEquals(4, state.getCitations(0));
        assertEquals(ACTIVE, state.getStatus(0));
        assertEquals(4 + 1 - 1, state.getCitationLimit(0));
        // player 1, no Goodwill: 0 citations on 1 card, limit 1 - 1 = 0
        assertEquals(1 - 1, state.getCitationLimit(1));
    }

    @Test
    public void withGoodwillAndZeroToleranceFiveCitationsOnFourCardsBust() {
        state.goodwill[0] = true;
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 4, rule(REPURPOSED), special(ZERO_TOLERANCE));
        playTurn(fm, state, cards);
        // 4 + 1 = 5 citations on 4 cards; limit 4 + 1 - 1 = 4 -> 5 > 4, Cease & Desist
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        assertEquals(ACTIVE, state.getStatus(1));
    }

    // ---------------------------------------------------------------- Goodwill from round to round

    @Test
    public void goodwillIsReturnedAtTheEndOfARoundInWhichThePlayerDidNotBust() {
        state.goodwill[0] = true;
        setLawn(state, 0, card(FURNITURE, BLUE, PLASTIC), card(STRUCTURE, RED, ILLUMINATED));
        setLawn(state, 1, card(ORNAMENT, BLUE, ILLUMINATED));
        assertEquals(2 + 1, state.getCitationLimit(0));

        state.setGamePhase(CONTINUE_OR_PASS);
        everyonePasses(fm, state);

        // both passed, nobody busted: nobody holds Goodwill in the new round; limits 0 cards + 0
        assertEquals(1, state.getRoundCounter());
        assertFalse(state.hasGoodwill(0));
        assertFalse(state.hasGoodwill(1));
        assertEquals(0, state.getCitationLimit(0));
    }

    @Test
    public void bustingWhileHoldingGoodwillGivesGoodwillAgainAndAPlayerWhoPassedLosesIt() {
        state.goodwill[0] = true;
        state.goodwill[1] = true;
        state.status[1] = PASSED;
        setLawn(state, 1, RED_WATER_LAWN);
        LawnCard[] cards = arrangeFourthCardWithOneCitation(state, 5, rule(REPURPOSED));

        playTurn(fm, state, cards[0], null);

        // player 0: 5 + 1 = 6 on 4 cards, limit 4 + 1 = 5 -> Cease & Desist; nobody is active, so the round ends.
        // Next round: player 0 (busted) holds Goodwill again, player 1 (passed) does not.
        assertArrayEquals(new int[]{0, 0, 0}, tracks(state, 0));
        assertArrayEquals(RED_WATER_SCORE, tracks(state, 1));
        assertEquals(1, state.getRoundCounter());
        assertTrue(state.hasGoodwill(0));
        assertFalse(state.hasGoodwill(1));
        assertEquals(0 + 1, state.getCitationLimit(0));
        assertEquals(0, state.getCitationLimit(1));
    }

    // ---------------------------------------------------------------- copy / equals

    @Test
    public void goodwillIsCopiedAndAStateDifferingOnlyInGoodwillIsNotEqual() {
        state.goodwill[1] = true;
        LawnAndOrderGameState copy = (LawnAndOrderGameState) state.copy();
        assertEquals(state, copy);
        assertTrue(copy.hasGoodwill(1));
        assertFalse(copy.hasGoodwill(0));
        // Goodwill is public: every player's view keeps it
        assertTrue(((LawnAndOrderGameState) state.copy(0)).hasGoodwill(1));

        copy.goodwill[1] = false;
        assertNotEquals(state, copy);
        assertTrue(state.hasGoodwill(1));
    }
}
