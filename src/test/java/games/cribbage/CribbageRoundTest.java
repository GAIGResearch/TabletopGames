package games.cribbage;

import core.components.FrenchCard;
import games.cribbage.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.GameResult.*;
import static games.cribbage.CribbageGameState.CribbageGamePhase.Discard;
import static games.cribbage.CribbageTestUtils.*;
import static org.junit.Assert.*;

/**
 * The end of a round (all cards back, the deal passes) and the end of the game after nRounds.
 * Each round is arranged with arrangePlay, whose crib and starter score nothing in the show, and with
 * one-card hands chosen so that each hand plus the King of Spades scores nothing either: the scores are the
 * pegging points alone.
 */
public class CribbageRoundTest {

    CribbageParameters params;
    CribbageGameState state;
    CribbageForwardModel fm;

    @Before
    public void setup() {
        params = new CribbageParameters();
        params.setRandomSeed(42);
        state = new CribbageGameState(params, 2);
        fm = new CribbageForwardModel();
        fm.setup(state);
    }

    /** Arrange a round in which each player holds one card, and play both: the non-dealer's first. */
    private void playOneCardRound(FrenchCard nonDealerCard, FrenchCard dealerCard) {
        arrangePlay(state, List.of(nonDealerCard), List.of(dealerCard));
        fm.next(state, new PlayCard(nonDealerCard));
        fm.next(state, new PlayCard(dealerCard));
    }

    @Test
    public void roundEndReturnsAllCardsAndDealsAgainWithTheOtherDealer() {
        playOneCardRound(card("4H"), card("6D"));

        assertEquals(1, state.getRoundCounter());
        assertEquals(1, state.getDealer());
        assertEquals(Discard, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());   // the new non-dealer
        assertNull(state.getStarter());
        assertTrue(state.getPlaySequence().isEmpty());
        assertEquals(0, state.getCrib().getSize());
        assertEquals(0, state.getPlayedCards(0).getSize());
        assertEquals(0, state.getPlayedCards(1).getSize());
        assertEquals(6, state.getPlayerHand(0).getSize());
        assertEquals(6, state.getPlayerHand(1).getSize());
        assertEquals(40, state.getDrawDeck().getSize());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
        // the pegging points stay: last card to the dealer of round 1
        assertEquals(1, state.getScore(0));
        assertEquals(0, state.getScore(1));
    }

    /**
     * Two games with different seeds, arranged identically, with the draw deck put in the same fixed order before
     * the round ends. Their next deals can only differ if the draw deck is shuffled at the round end.
     */
    @Test
    public void drawDeckIsShuffledBeforeTheNextDeal() {
        List<List<FrenchCard>> newHands = new ArrayList<>();
        for (long seed : new long[]{1, 2}) {
            CribbageParameters p = new CribbageParameters();
            p.setRandomSeed(seed);
            state = new CribbageGameState(p, 2);
            fm.setup(state);
            arrangePlay(state, List.of(card("4H")), List.of(card("6D")));
            List<FrenchCard> remaining = FULL_DECK.stream().filter(state.drawDeck::contains).toList();
            state.drawDeck.clear();
            remaining.forEach(state.drawDeck::addToBottom);

            fm.next(state, new PlayCard(card("4H")));
            fm.next(state, new PlayCard(card("6D")));
            assertEquals(1, state.getRoundCounter());
            newHands.add(List.copyOf(state.getPlayerHand(0).getComponents()));
        }
        assertNotEquals(newHands.get(0), newHands.get(1));
    }

    @Test
    public void gameEndsAfterTwoRoundsAndTheHigherScoreWins() {
        // round 1, player 0 deals: 7 + 8 = 15 (1) and the last card (1) to player 0
        playOneCardRound(card("7D"), card("8S"));
        assertTrue(state.isNotTerminal());
        assertEquals(2, state.getScore(0));
        // round 2, player 1 deals: last card to player 1
        playOneCardRound(card("4H"), card("6D"));

        assertFalse(state.isNotTerminal());
        assertEquals(2, state.getScore(0));
        assertEquals(1, state.getScore(1));
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void equalScoresAfterTheLastRoundAreADraw() {
        playOneCardRound(card("4H"), card("6D"));   // last card to player 0
        playOneCardRound(card("4H"), card("6D"));   // last card to player 1
        assertFalse(state.isNotTerminal());
        assertEquals(1, state.getScore(0));
        assertEquals(1, state.getScore(1));
        assertEquals(DRAW_GAME, state.getPlayerResults()[0]);
        assertEquals(DRAW_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void gameLastsNRounds() {
        params.setParameterValue("nRounds", 4);
        for (int round = 0; round < 3; round++) {
            playOneCardRound(card("4H"), card("6D"));
            assertTrue("game ended after round " + (round + 1), state.isNotTerminal());
        }
        playOneCardRound(card("4H"), card("6D"));
        assertFalse(state.isNotTerminal());
        assertEquals(DRAW_GAME, state.getPlayerResults()[0]);   // each dealt twice: 2 - 2
    }
}
