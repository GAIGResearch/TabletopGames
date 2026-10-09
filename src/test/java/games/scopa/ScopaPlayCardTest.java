package games.scopa;

import core.CoreConstants;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static core.components.TarotCard.*;
import static games.scopa.ScopaTestUtils.*;
import static org.junit.Assert.*;

/**
 * The effect of playing a card (through fm.next): captures, cards to the table, lastCapturer and scopas.
 */
public class ScopaPlayCardTest {

    ScopaForwardModel fm;
    ScopaGameState state;

    @Before
    public void setup() {
        fm = new ScopaForwardModel();
        state = newState(11, fm);
    }

    @Test
    public void aCaptureMovesThePlayedAndCapturedCardsToThePlayersPile() {
        arrange(state, 0, of(cup(KING), coin(1), baton(CAVALIER)), of(sword(5), sword(6), baton(2)),
                cup(3), sword(7), baton(4), coin(6));
        play(state, fm, 0, capture(cup(KING), cup(3), sword(7)));   // 10 = 3 + 7

        assertEquals(setOf(cup(KING), cup(3), sword(7)), setOf(state.getCapturedCards(0)));
        assertEquals(0, state.getCapturedCards(1).getSize());
        assertEquals(setOf(baton(4), coin(6)), setOf(state.getTable()));
        assertEquals(setOf(coin(1), baton(CAVALIER)), setOf(state.getPlayerHand(0)));
        assertEquals(30, state.getDrawDeck().getSize());
        assertEquals(0, state.getLastCapturer());
        // two cards are left on the table: no scopa
        assertEquals(0, state.getScopas(0));
        // the running deal score: cards 3 v 0 -> P0 1; Coins 0 v 0; no 7 of Coins; no primiera (suits missing)
        assertEquals(1.0, state.getGameScore(0), 0.0);
        assertEquals(0.0, state.getGameScore(1), 0.0);
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void aCardThatCapturesNothingIsAddedToTheTableAndLeavesTheLastCapturer() {
        arrange(state, 0, of(cup(KING), coin(1), baton(CAVALIER)), of(sword(5), sword(6), baton(2)),
                cup(3), sword(7), baton(4), coin(6));
        state.lastCapturer = 1;
        play(state, fm, 0, toTable(coin(1)));

        assertEquals(setOf(cup(3), sword(7), baton(4), coin(6), coin(1)), setOf(state.getTable()));
        assertEquals(setOf(cup(KING), baton(CAVALIER)), setOf(state.getPlayerHand(0)));
        assertEquals(0, state.getCapturedCards(0).getSize());
        assertEquals(0, state.getCapturedCards(1).getSize());
        assertEquals(1, state.getLastCapturer());
        assertEquals(0, state.getScopas(0));
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void aCaptureByPlayerOneGoesToTheirPileAndMakesThemLastCapturer() {
        arrange(state, 1, of(cup(KING), baton(CAVALIER)), of(sword(5), sword(6), baton(2)),
                cup(3), sword(7), baton(4), coin(6), coin(1));
        state.lastCapturer = 0;
        play(state, fm, 1, capture(sword(6), coin(6)));   // same rank

        assertEquals(setOf(sword(6), coin(6)), setOf(state.getCapturedCards(1)));
        assertEquals(0, state.getCapturedCards(0).getSize());
        assertEquals(setOf(cup(3), sword(7), baton(4), coin(1)), setOf(state.getTable()));
        assertEquals(setOf(sword(5), baton(2)), setOf(state.getPlayerHand(1)));
        assertEquals(1, state.getLastCapturer());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void aCaptureThatEmptiesTheTableIsAScopa() {
        arrange(state, 0, of(cup(KING), coin(1), baton(CAVALIER)), of(sword(5), sword(6), baton(2)),
                cup(3), sword(7));
        play(state, fm, 0, capture(cup(KING), cup(3), sword(7)));

        assertEquals(0, state.getTable().getSize());
        assertEquals(1, state.getScopas(0));
        assertEquals(0, state.getScopas(1));
        // P0: scopa 1 + cards (3 v 0) 1 = 2; Coins 0 v 0, no 7 of Coins, no primiera (suits missing)
        assertEquals(2.0, state.getGameScore(0), 0.0);
        assertEquals(0.0, state.getGameScore(1), 0.0);
        assertEquals(setOf(cup(KING), cup(3), sword(7)), setOf(state.getCapturedCards(0)));
    }

    @Test
    public void aSecondScopaAddsToTheFirst() {
        arrange(state, 0, of(cup(KING), coin(1), baton(CAVALIER)), of(sword(5), sword(6), baton(2)),
                cup(3), sword(7));
        state.scopas[0] = 1;
        play(state, fm, 0, capture(cup(KING), cup(3), sword(7)));
        assertEquals(2, state.getScopas(0));
    }

    @Test
    public void playingACardToAnEmptyTableIsNoScopa() {
        // a scopa needs a capture
        arrange(state, 0, of(cup(KING), coin(1), baton(CAVALIER)), of(sword(5), sword(6), baton(2)));
        play(state, fm, 0, toTable(coin(1)));

        assertEquals(List.of(coin(1)), state.getTable().getComponents());
        assertEquals(0, state.getScopas(0));
        assertEquals(0.0, state.getGameScore(0), 0.0);
    }

    @Test
    public void aSweepWithTheLastCardOfTheHandsIsAScopaWhileTheDrawDeckHasCards() {
        // both hands empty after this play, but the draw deck holds 37 cards: not the last play of the deal
        arrange(state, 1, of(), of(cup(KING)), cup(3), sword(7));
        play(state, fm, 1, capture(cup(KING), cup(3), sword(7)));
        assertEquals(1, state.getScopas(1));
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void aSweepWhileTheOpponentStillHoldsACardIsAScopa() {
        // draw deck empty, but player 1 still has a card to play: not the last play of the deal
        arrange(state, 0, of(cup(KING)), of(coin(1)), cup(3), sword(7));
        emptyDrawDeck(state, 1);
        play(state, fm, 0, capture(cup(KING), cup(3), sword(7)));
        assertEquals(1, state.getScopas(0));
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void aSweepWithTheLastCardOfTheDealIsNoScopa() {
        // draw deck and player 0's hand empty; player 1's King is the last card of the deal
        arrange(state, 1, of(), of(cup(KING)), cup(3), sword(7));
        emptyDrawDeck(state, 0);
        state.lastCapturer = 0;
        play(state, fm, 1, capture(cup(KING), cup(3), sword(7)));

        assertEquals(0, state.getScopas(1));
        assertTrue(state.getCapturedCards(1).getComponents().containsAll(List.of(cup(KING), cup(3), sword(7))));
        assertEquals(3, state.getCapturedCards(1).getSize());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertAllCardsPresent(state);
    }
}
