package games.lawnandorder;

import games.lawnandorder.components.LawnCard;
import games.lawnandorder.components.RuleCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static games.lawnandorder.LawnAndOrderGameState.Decision.*;
import static games.lawnandorder.LawnAndOrderGameState.Phase.CONTINUE_OR_PASS;
import static games.lawnandorder.LawnAndOrderGameState.Phase.PLAY_OBJECT;
import static games.lawnandorder.LawnAndOrderGameState.PlayerStatus.*;
import static games.lawnandorder.LawnAndOrderTestUtils.*;
import static games.lawnandorder.components.LawnCard.Attribute.*;
import static games.lawnandorder.components.RuleCard.Special.*;
import static org.junit.Assert.*;

/**
 * The Continue-or-Pass step and the end of a round: its triggers (everyone passed or busted, the agenda empty),
 * scoring the lawns onto the tracks, and the new round that follows. Two players.
 */
public class LawnAndOrderRoundTest {

    /** The rulebook's scoring example: Type 4 (four Ornaments), Colour 1 + 1 (two Pink, two Yellow), Feature 1. */
    static final LawnCard[] EXAMPLE_LAWN = {card(ORNAMENT, PINK, OVERSIZED), card(ORNAMENT, PINK, PLASTIC),
            card(ORNAMENT, YELLOW, OVERSIZED), card(ORNAMENT, YELLOW, ILLUMINATED)};
    static final int[] EXAMPLE_SCORE = {4, 2, 1};
    /** Three Red Water Features: Type 2, Colour 2, Feature 0 (three different features). */
    static final LawnCard[] RED_WATER_LAWN = {card(WATER_FEATURE, RED, OVERSIZED), card(WATER_FEATURE, RED, ILLUMINATED),
            card(WATER_FEATURE, RED, PLASTIC)};
    static final int[] RED_WATER_SCORE = {2, 2, 0};

    LawnAndOrderForwardModel fm;
    LawnAndOrderGameState state;

    @Before
    public void setup() {
        fm = new LawnAndOrderForwardModel();
        state = newState(2, 1);
    }

    @Test
    public void continueDrawsOneCardAndPassDrawsNoneAndEndsThePlayersRound() {
        setAgendaTop(state, rule(REPURPOSED));
        LawnCard c0 = card(ORNAMENT, RED, PLASTIC), c1 = card(FURNITURE, BLUE, ILLUMINATED);
        putInHand(state, 0, c0);
        putInHand(state, 1, c1);
        playTurn(fm, state, c0, c1);
        assertEquals(CONTINUE_OR_PASS, state.getGamePhase());
        LawnCard top = state.getDrawDeck().peek();
        int deckSize = state.getDrawDeck().getSize();

        decide(fm, state, CONTINUE, PASS);

        assertEquals(5, state.getHand(0).getSize());
        assertTrue(state.getHand(0).contains(top));
        assertEquals(4, state.getHand(1).getSize());
        assertEquals(deckSize - 1, state.getDrawDeck().getSize());
        assertEquals(ACTIVE, state.getStatus(0));
        assertEquals(PASSED, state.getStatus(1));
        assertEquals(NONE, state.getDecision(0));
        assertEquals(NONE, state.getDecision(1));
        assertEquals(PLAY_OBJECT, state.getGamePhase());
        assertEquals(List.of(0), state.getPlayersStillToChoose());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getRoundCounter());
        // the passed player keeps their lawn for the end of the round
        assertEquals(List.of(c1), state.getLawn(1).getComponents());
        assertAllCardsPresent(state);
    }

    /** Both lawns arranged, everyone to decide. */
    private void arrangeEndOfRound() {
        setLawn(state, 0, EXAMPLE_LAWN);
        setLawn(state, 1, RED_WATER_LAWN);
        state.setGamePhase(CONTINUE_OR_PASS);
    }

    @Test
    public void whenEveryonePassesTheLawnsAreScoredAndANewRoundIsDealt() {
        arrangeEndOfRound();
        decide(fm, state, PASS, PASS);

        assertArrayEquals(EXAMPLE_SCORE, tracks(state, 0));
        assertArrayEquals(RED_WATER_SCORE, tracks(state, 1));
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        // the new round: a fresh deal and agenda, and a clean slate
        for (int p = 0; p < 2; p++) {
            assertEquals(5, state.getHand(p).getSize());
            assertEquals(0, state.getLawn(p).getSize());
            assertEquals(0, state.getChosenCard(p).getSize());
            assertEquals(0, state.getCitations(p));
            assertEquals(ACTIVE, state.getStatus(p));
            assertEquals(NONE, state.getDecision(p));
        }
        assertEquals(64 - 10, state.getDrawDeck().getSize());
        assertEquals(16 - 2, state.getAgenda().getSize());
        assertEquals(2, state.getInsiderTips().getSize());
        assertEquals(0, state.getRevealedRules().getSize());
        assertEquals(0, state.getDiscardDeck().getSize());
        assertEquals(PLAY_OBJECT, state.getGamePhase());
        assertEquals(List.of(0, 1), state.getPlayersStillToChoose());
        assertAllCardsPresent(state);

        // a second round: the tracks accumulate
        arrangeEndOfRound();
        decide(fm, state, PASS, PASS);
        assertArrayEquals(new int[]{4 + 4, 2 + 2, 1 + 1}, tracks(state, 0));
        assertArrayEquals(new int[]{2 + 2, 2 + 2, 0}, tracks(state, 1));
        assertEquals(2, state.getRoundCounter());
    }

    @Test
    public void theNewRoundIsShuffled() {
        arrangeEndOfRound();
        // the copy has its own random number generator; everything else is equal
        LawnAndOrderGameState other = (LawnAndOrderGameState) state.copy();
        assertEquals(state, other);
        decide(fm, state, PASS, PASS);
        decide(fm, other, PASS, PASS);
        assertEquals(1, other.getRoundCounter());
        assertNotEquals(state.getDrawDeck().getComponents(), other.getDrawDeck().getComponents());
        assertNotEquals(state.getAgenda().getComponents(), other.getAgenda().getComponents());
    }

    @Test
    public void theRoundEndsWhenTheLastActivePlayerBustsAndTheyScoreNothingButHoldGoodwill() {
        state.status[1] = PASSED;
        setLawn(state, 1, RED_WATER_LAWN);
        // lawn 0 would score 4/2/1 with the card played below
        setLawn(state, 0, EXAMPLE_LAWN[0], EXAMPLE_LAWN[1], EXAMPLE_LAWN[2]);
        state.citations[0] = 4;
        revealEarlier(state, rule(ILLUMINATED));
        setAgendaTop(state, rule(BLUE));
        putInHand(state, 0, EXAMPLE_LAWN[3]);

        playTurn(fm, state, EXAMPLE_LAWN[3], null);

        // Ornament / Yellow / Illuminated with Illuminated condemned: 4 + 1 = 5 citations on 4 cards -> Cease & Desist;
        // nobody is active, so the round ends: player 0 scores nothing, player 1 (passed) scores their lawn
        assertArrayEquals(new int[]{0, 0, 0}, tracks(state, 0));
        assertArrayEquals(RED_WATER_SCORE, tracks(state, 1));
        assertEquals(1, state.getRoundCounter());
        assertEquals(ACTIVE, state.getStatus(0));
        assertEquals(ACTIVE, state.getStatus(1));
        assertEquals(5, state.getHand(0).getSize());
        assertEquals(0, state.getDiscardDeck().getSize());
        assertEquals(PLAY_OBJECT, state.getGamePhase());
        // the Cease & Desist gives player 0 Goodwill for the new round: limit 0 cards + 1; player 1 passed: none
        assertTrue(state.hasGoodwill(0));
        assertFalse(state.hasGoodwill(1));
        assertEquals(0 + 1, state.getCitationLimit(0));
        assertEquals(0, state.getCitationLimit(1));
        assertAllCardsPresent(state);
    }

    @Test
    public void aBustedPlayerScoresNothingWhenTheOthersPassLater() {
        setLawn(state, 0, EXAMPLE_LAWN[0], EXAMPLE_LAWN[1], EXAMPLE_LAWN[2]);
        state.citations[0] = 4;
        setLawn(state, 1, RED_WATER_LAWN[0], RED_WATER_LAWN[1]);
        revealEarlier(state, rule(YELLOW));
        setAgendaTop(state, rule(REPURPOSED));
        putInHand(state, 0, EXAMPLE_LAWN[3]);
        putInHand(state, 1, RED_WATER_LAWN[2]);

        playTurn(fm, state, EXAMPLE_LAWN[3], RED_WATER_LAWN[2]);
        // Yellow condemned: player 0 has 5 citations on 4 cards; player 1's Red card is not cited
        assertEquals(CEASE_AND_DESIST, state.getStatus(0));
        assertEquals(List.of(1), state.getPlayersStillToChoose());

        decide(fm, state, null, PASS);
        assertArrayEquals(new int[]{0, 0, 0}, tracks(state, 0));
        assertArrayEquals(RED_WATER_SCORE, tracks(state, 1));
        assertEquals(1, state.getRoundCounter());
    }

    @Test
    public void anEmptyAgendaEndsTheRoundAndTheActivePlayersScoreAsIfTheyHadPassed() {
        state.status[1] = PASSED;
        setLawn(state, 1, RED_WATER_LAWN);
        setLawn(state, 0, EXAMPLE_LAWN[0], EXAMPLE_LAWN[1], EXAMPLE_LAWN[2]);
        // one card left in the agenda, an Administrative Error; every attribute already condemned
        List<RuleCard> revealed = new ArrayList<>();
        for (LawnCard.Attribute a : LawnCard.Attribute.values())
            revealed.add(rule(a));
        revealed.add(special(ADMINISTRATIVE_ERROR));
        setRules(state, List.of(special(ADMINISTRATIVE_ERROR)), List.of(special(ZERO_TOLERANCE), special(EMERGENCY_SESSION)),
                revealed);
        putInHand(state, 0, EXAMPLE_LAWN[3]);

        playTurn(fm, state, EXAMPLE_LAWN[3], null);

        // Ornament, Yellow and Illuminated all condemned: 3 citations on 4 cards, no bust; the Administrative Error
        // empties the agenda, so the round ends and player 0 scores the full example lawn
        assertArrayEquals(EXAMPLE_SCORE, tracks(state, 0));
        assertArrayEquals(RED_WATER_SCORE, tracks(state, 1));
        assertEquals(1, state.getRoundCounter());
        assertEquals(PLAY_OBJECT, state.getGamePhase());
        assertEquals(16 - 2, state.getAgenda().getSize());
        assertEquals(List.of(0, 1), state.getPlayersStillToChoose());
        assertAllCardsPresent(state);
    }
}
