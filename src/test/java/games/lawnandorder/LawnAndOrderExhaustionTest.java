package games.lawnandorder;

import core.actions.AbstractAction;
import games.lawnandorder.actions.PlayObject;
import games.lawnandorder.components.LawnCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static games.lawnandorder.LawnAndOrderGameState.Decision.*;
import static games.lawnandorder.LawnAndOrderGameState.Phase.PLAY_OBJECT;
import static games.lawnandorder.LawnAndOrderGameState.PlayerStatus.*;
import static games.lawnandorder.LawnAndOrderRoundTest.*;
import static games.lawnandorder.LawnAndOrderTestUtils.*;
import static games.lawnandorder.components.LawnCard.Attribute.*;
import static org.junit.Assert.*;

/**
 * Running out of Lawn cards: Continue with an empty draw deck, and a player left with no card in hand. Two players.
 */
public class LawnAndOrderExhaustionTest {

    LawnAndOrderForwardModel fm;
    LawnAndOrderGameState state;

    @Before
    public void setup() {
        fm = new LawnAndOrderForwardModel();
        state = newState(2, 1);
    }

    private static void assertNoNullInHands(LawnAndOrderGameState state) {
        for (int p = 0; p < state.getNPlayers(); p++)
            assertFalse("null card in hand " + p, state.getHand(p).getComponents().contains(null));
    }

    @Test
    public void continueWithAnEmptyDrawDeckDrawsNothing() {
        setAgendaTop(state, rule(REPURPOSED));
        LawnCard c0 = card(ORNAMENT, RED, PLASTIC), c1 = card(FURNITURE, BLUE, ILLUMINATED);
        putInHand(state, 0, c0);
        putInHand(state, 1, c1);
        setAsideDrawDeck(state, 0);
        playTurn(fm, state, c0, c1);
        List<LawnCard> hand0 = List.copyOf(state.getHand(0).getComponents());

        decide(fm, state, CONTINUE, CONTINUE);

        // 5 dealt - 1 played + 0 drawn = 4 each; both still hold cards, so both stay in
        assertEquals(4, state.getHand(0).getSize());
        assertEquals(4, state.getHand(1).getSize());
        assertNoNullInHands(state);
        assertEquals(0, state.getDrawDeck().getSize());
        assertEquals(ACTIVE, state.getStatus(0));
        assertEquals(ACTIVE, state.getStatus(1));
        assertEquals(PLAY_OBJECT, state.getGamePhase());
        assertEquals(List.of(0, 1), state.getPlayersStillToChoose());
        // player 0 may play exactly the four cards they held before deciding
        Set<AbstractAction> expected = hand0.stream().map(c -> (AbstractAction) new PlayObject(0, c)).collect(Collectors.toSet());
        assertEquals(expected, actionsFor(fm, state, 0));
        assertAllCardsPresent(state);
    }

    @Test
    public void continuingWithAnEmptyHandAndOneCardInTheDrawDeckDrawsItAndStaysIn() {
        state.status[1] = PASSED;
        setLawn(state, 0, EXAMPLE_LAWN[0], EXAMPLE_LAWN[1], EXAMPLE_LAWN[2]);
        putInHand(state, 0, EXAMPLE_LAWN[3]);
        setAgendaTop(state, rule(REPURPOSED));
        setAsideDrawDeck(state, 1);
        keepOnlyInHand(state, 0, EXAMPLE_LAWN[3]);
        LawnCard last = state.getDrawDeck().peek();

        playTurn(fm, state, EXAMPLE_LAWN[3], null);
        assertEquals(0, state.getHand(0).getSize());
        decide(fm, state, CONTINUE, null);

        // the draw gives player 0 a card, so they continue with it
        assertEquals(List.of(last), state.getHand(0).getComponents());
        assertEquals(0, state.getDrawDeck().getSize());
        assertEquals(ACTIVE, state.getStatus(0));
        assertEquals(PLAY_OBJECT, state.getGamePhase());
        assertEquals(List.of(0), state.getPlayersStillToChoose());
        assertEquals(0, state.getRoundCounter());
        assertAllCardsPresent(state);
    }

    @Test
    public void continuingWithAnEmptyHandAndNothingToDrawPassesAndTheLawnIsImmuneAndScored() {
        setLawn(state, 0, EXAMPLE_LAWN[0], EXAMPLE_LAWN[1], EXAMPLE_LAWN[2]);
        setLawn(state, 1, RED_WATER_LAWN[0], RED_WATER_LAWN[1]);
        LawnCard d = card(WATER_FEATURE, BLUE, OVERSIZED);
        putInHand(state, 0, EXAMPLE_LAWN[3]);
        putInHand(state, 1, RED_WATER_LAWN[2], d);
        setAgendaTop(state, rule(REPURPOSED), rule(ORNAMENT));
        setAsideDrawDeck(state, 0);
        keepOnlyInHand(state, 0, EXAMPLE_LAWN[3]);

        // Turn 1, "No Repurposed": no Repurposed card on either lawn -> 0 citations each
        playTurn(fm, state, EXAMPLE_LAWN[3], RED_WATER_LAWN[2]);
        assertEquals(0, state.getCitations(0));
        assertEquals(0, state.getCitations(1));
        decide(fm, state, CONTINUE, CONTINUE);

        // player 0: 1 - 1 played + 0 drawn = no card -> passed instead; player 1: 5 - 1 = 4 cards, continues
        assertEquals(PASSED, state.getStatus(0));
        assertEquals(ACTIVE, state.getStatus(1));
        assertEquals(0, state.getHand(0).getSize());
        assertEquals(4, state.getHand(1).getSize());
        assertNoNullInHands(state);
        assertEquals(PLAY_OBJECT, state.getGamePhase());
        assertEquals(List.of(1), state.getPlayersStillToChoose());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(), actionsFor(fm, state, 0));
        assertEquals(0, state.getRoundCounter());
        assertAllCardsPresent(state);

        // Turn 2, "No Ornament": player 0 has four Ornaments but has passed, so is immune (0); player 1's Blue
        // Oversized Water Feature is not condemned by "No Repurposed" or "No Ornament" (0)
        playTurn(fm, state, null, d);
        assertEquals(0, state.getCitations(0));
        assertEquals(0, state.getCitations(1));
        decide(fm, state, null, PASS);

        // player 0 scores the example lawn 4/2/1; player 1: four Water Features (4), Red x3 (2) + Blue (0) = 2,
        // Oversized x2 (1) + Illuminated + Plastic = 1
        assertArrayEquals(EXAMPLE_SCORE, tracks(state, 0));
        assertArrayEquals(new int[]{4, 2, 1}, tracks(state, 1));
        assertEquals(1, state.getRoundCounter());
        assertAllCardsPresent(state);
    }

    @Test
    public void theRoundEndsWhenPassingForLackOfCardsLeavesNobodyActive() {
        state.status[1] = PASSED;
        setLawn(state, 1, RED_WATER_LAWN);
        setLawn(state, 0, EXAMPLE_LAWN[0], EXAMPLE_LAWN[1], EXAMPLE_LAWN[2]);
        putInHand(state, 0, EXAMPLE_LAWN[3]);
        setAgendaTop(state, rule(REPURPOSED));
        setAsideDrawDeck(state, 0);
        keepOnlyInHand(state, 0, EXAMPLE_LAWN[3]);

        playTurn(fm, state, EXAMPLE_LAWN[3], null);
        decide(fm, state, CONTINUE, null);

        // player 0 has no card and nothing to draw -> passed; player 1 passed earlier -> the round ends and both
        // lawns score: 4/2/1 and 2/2/0
        assertArrayEquals(EXAMPLE_SCORE, tracks(state, 0));
        assertArrayEquals(RED_WATER_SCORE, tracks(state, 1));
        assertEquals(1, state.getRoundCounter());
        assertTrue(state.isNotTerminal());
        // the new round: set-aside cards are back, a fresh deal of 5 each from 64 -> 64 - 10 = 54 in the deck
        for (int p = 0; p < 2; p++) {
            assertEquals(5, state.getHand(p).getSize());
            assertEquals(ACTIVE, state.getStatus(p));
            assertFalse(state.hasGoodwill(p));
        }
        assertEquals(64 - 10, state.getDrawDeck().getSize());
        assertEquals(0, state.getDiscardDeck().getSize());
        assertEquals(PLAY_OBJECT, state.getGamePhase());
        assertEquals(List.of(0, 1), state.getPlayersStillToChoose());
        assertAllCardsPresent(state);
    }
}
