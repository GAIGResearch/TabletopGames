package games.rummy;

import core.CoreConstants.GameResult;
import core.components.FrenchCard;
import games.rummy.actions.Discard;
import games.rummy.actions.DrawCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.rummy.RummyTestUtils.*;
import static games.rummy.actions.LayOff.Position.HIGH;
import static org.junit.Assert.*;

/**
 * The end of the game (an empty draw deck, an empty hand, the turn cap), checked after each turn, and the final
 * scores and results.
 */
public class RummyGameEndTest {

    RummyForwardModel fm;
    RummyGameState state;

    @Before
    public void setup() {
        fm = new RummyForwardModel();
        state = newState(3, 21, fm);
    }

    /** The current player draws the top card of the draw deck and discards the same card. */
    private void drawAndDiscardTheSameCard() {
        FrenchCard top = state.getDrawDeck().peek();
        fm.next(state, new DrawCard(false));
        fm.next(state, new Discard(top));
    }

    @Test
    public void theGameEndsAfterTheDiscardOfTheTurnThatEmptiesTheDrawDeck() {
        setDrawDeck(state, "KS");
        giveHand(state, 0, "2H", "5D", "9C", "JS");
        fm.next(state, new DrawCard(false));

        // the draw deck is empty, but the player still discards
        assertEquals(0, state.getDrawDeck().getSize());
        assertTrue("the game does not end at the draw", state.isNotTerminal());
        assertEquals(RummyGameState.Phase.PLAY, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(new Discard(card("2H")), new Discard(card("5D")), new Discard(card("9C")),
                new Discard(card("JS")), new Discard(card("KS"))), actionSet(state, fm));

        fm.next(state, new Discard(card("KS")));
        assertFalse("the game ends after the discard", state.isNotTerminal());
        assertEquals(card("KS"), state.getDiscardPile().peek());
        assertAllCardsPresent(state);
    }

    @Test
    public void theGameGoesOnWhileTheDrawDeckHasCards() {
        setDrawDeck(state, "KS", "4H");
        giveHand(state, 0, "2H", "5D", "9C", "JS");
        drawAndDiscardTheSameCard();
        // one card left for player 1
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new DrawCard(false), new DrawCard(true)), actionSet(state, fm));
    }

    @Test
    public void discardingTheLastCardEndsTheGameWithNoPointsForThatPlayer() {
        // player 0 has no cards, takes the 7H from the discard pile, and may discard it as the only card
        giveHand(state, 0);
        giveHand(state, 1, "3S", "6D");
        giveHand(state, 2, "AH", "10C");
        setDiscardPile(state, "7H");
        assertTrue("guard: the draw deck is not empty", state.getDrawDeck().getSize() > 0);
        fm.next(state, new DrawCard(true));
        assertEquals(Set.of(new Discard(card("7H"))), actionSet(state, fm));
        fm.next(state, new Discard(card("7H")));

        assertFalse(state.isNotTerminal());
        assertEquals(0, state.getPlayerHand(0).getSize());
        // points: player 0 none, player 1 3 + 6 = 9, player 2 1 + 10 = 11
        assertEquals(0.0, state.getGameScore(0), 0.0);
        assertEquals(-9.0, state.getGameScore(1), 0.0);
        assertEquals(-11.0, state.getGameScore(2), 0.0);
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    /** Players 1 and 2 hold 3S 6D (3 + 6 = 9 points) and AH 10C (1 + 10 = 11 points). */
    private void arrangeOpponents() {
        giveHand(state, 1, "3S", "6D");
        giveHand(state, 2, "AH", "10C");
    }

    /** Asserts that the game is over, with player 0 winning on no points and players 1 and 2 losing on 9 and 11. */
    private void assertPlayerZeroWentOut() {
        assertFalse(state.isNotTerminal());
        assertEquals(0, state.getPlayerHand(0).getSize());
        assertEquals(0.0, state.getGameScore(0), 0.0);
        assertEquals(-9.0, state.getGameScore(1), 0.0);
        assertEquals(-11.0, state.getGameScore(2), 0.0);
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void meldingTheLastCardsEndsTheTurnAndTheGameWithoutADiscard() {
        giveHand(state, 0, "6C", "7C");
        arrangeOpponents();
        setDrawDeck(state, "8C", "KD");
        fm.next(state, new DrawCard(false));
        assertEquals(union(discards("6C", "7C", "8C"), meld("6C", "7C", "8C")), actionSet(state, fm));
        fm.next(state, meld("6C", "7C", "8C"));

        assertPlayerZeroWentOut();
        assertEquals(cards("6C", "7C", "8C"), meldCards(state, 0));
        // the game ends on the empty hand, not the draw deck, which still holds the KD
        assertEquals(1, state.getDrawDeck().getSize());
        // the turn ended as a discard turn ends
        assertEquals(1, state.getTurnCounter());
        assertNull(state.getTakenCard());
        assertFalse(state.hasMeldedThisTurn());
    }

    @Test
    public void layingOffTheLastCardEndsTheTurnAndTheGameWithoutADiscard() {
        addMeld(state, "9H", "10H", "JH");
        giveHand(state, 0);
        arrangeOpponents();
        setDiscardPile(state, "QH", "4D");
        fm.next(state, new DrawCard(true));
        // the taken QH is the only card: it may be discarded or laid off above the JH
        assertEquals(Set.of(new Discard(card("QH")), layOff("QH", HIGH)),
                actionSet(state, fm));
        fm.next(state, layOff("QH", HIGH));

        assertPlayerZeroWentOut();
        assertEquals(cards("9H", "10H", "JH", "QH"), meldCards(state, 0));
        assertEquals(List.of(card("4D")), state.getDiscardPile().getComponents());
        assertEquals(1, state.getTurnCounter());
        assertNull(state.getTakenCard());
        assertFalse(state.hasMeldedThisTurn());
    }

    @Test
    public void discardingTheLastCardAfterMeldingEndsTheGame() {
        giveHand(state, 0, "6C", "7C", "2D");
        arrangeOpponents();
        setDrawDeck(state, "8C", "KD");
        fm.next(state, new DrawCard(false));
        fm.next(state, meld("6C", "7C", "8C"));
        assertTrue("one card left: the turn goes on", state.isNotTerminal());
        assertEquals(Set.of(new Discard(card("2D"))), actionSet(state, fm));
        fm.next(state, new Discard(card("2D")));

        assertPlayerZeroWentOut();
        assertEquals(card("2D"), state.getDiscardPile().peek());
    }

    @Test
    public void theFewestPointsInHandWins() {
        // points: player 0 5 + 7 = 12; player 1 Ace 1 + King 10 = 11; player 2 Queen 10 + 4 = 14
        giveHand(state, 0, "5H", "7C");
        giveHand(state, 1, "AS", "KD");
        giveHand(state, 2, "QD", "4C");
        setDrawDeck(state, "2S");
        drawAndDiscardTheSameCard();

        assertFalse(state.isNotTerminal());
        assertEquals(-12.0, state.getGameScore(0), 0.0);
        assertEquals(-11.0, state.getGameScore(1), 0.0);
        assertEquals(-14.0, state.getGameScore(2), 0.0);
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void playersSharingTheFewestPointsShareFirstPlace() {
        // points: player 0 5 + 6 = 11; player 1 Ace 1 + Jack 10 = 11; player 2 Queen 10 + 4 = 14
        giveHand(state, 0, "5H", "6C");
        giveHand(state, 1, "AS", "JD");
        giveHand(state, 2, "QD", "4C");
        setDrawDeck(state, "2S");
        drawAndDiscardTheSameCard();

        assertFalse(state.isNotTerminal());
        assertArrayEquals(new GameResult[]{DRAW_GAME, DRAW_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void theGameEndsAtTheEndOfTheLastTurnAllowed() {
        RummyParameters params = new RummyParameters();
        params.setParameterValue("maxTurnsPerDeal", 3);
        state = newState(params, 2, 21, fm);
        // 2 players: draw deck 52 - 20 - 1 = 31, so 3 turns do not empty it
        drawAndDiscardTheSameCard();
        drawAndDiscardTheSameCard();
        assertTrue("2 turns of 3", state.isNotTerminal());
        assertEquals(0, state.getCurrentPlayer());
        FrenchCard top = state.getDrawDeck().peek();
        fm.next(state, new DrawCard(false));
        assertTrue("the third turn is not over at the draw", state.isNotTerminal());
        fm.next(state, new Discard(top));

        assertFalse("the game ends after the third turn", state.isNotTerminal());
        assertEquals(31 - 3, state.getDrawDeck().getSize());
        for (int p = 0; p < 2; p++)
            assertEquals(-pointsByRule(state.getPlayerHand(p)), state.getGameScore(p), 0.0);
    }

    @Test
    public void theDefaultTurnCapDoesNotEndTheGameAfterThreeTurns() {
        state = newState(2, 21, fm);
        for (int t = 0; t < 3; t++)
            drawAndDiscardTheSameCard();
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getCurrentPlayer());
    }
}
