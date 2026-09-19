package games.cribbage;

import games.cribbage.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.cribbage.CribbageGameState.CribbageGamePhase.Play;
import static games.cribbage.CribbageTestUtils.*;
import static org.junit.Assert.*;

/**
 * The play with the running total only: legal cards, 15 and 31, automatic go, the end of a count and the last card.
 * No count here has two cards of the same rank next to each other or three consecutive ranks among its last
 * cards, so no pair or run scores.
 * Player 0 deals the first round, so player 1 is the non-dealer and leads.
 */
public class CribbagePlayTest {

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

    @Test
    public void onlyCardsKeepingTheCountWithinThirtyOneAreOffered() {
        arrangePlay(state, cards("2D", "QH"), cards("5S", "6H", "7S", "QD"));
        playedInCount(state, 0, card("10H"));
        playedInCount(state, 1, card("6C"));
        playedInCount(state, 0, card("9S"));
        state.setTurnOwner(0);
        // total 25: 5 makes 30 and 6 makes 31; 7 (32) and Q (35) are over
        assertEquals(25, state.getRunningTotal());
        assertEquals(Set.of(new PlayCard(card("5S")), new PlayCard(card("6H"))),
                new HashSet<>(fm.computeAvailableActions(state)));
    }

    @Test
    public void anyCardMayLeadACount() {
        arrangePlay(state, cards("10H", "4C", "8S", "QD"), cards("5S", "6H"));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new PlayCard(card("10H")), new PlayCard(card("4C")), new PlayCard(card("8S")), new PlayCard(card("QD"))),
                new HashSet<>(fm.computeAvailableActions(state)));
    }

    @Test
    public void playingACardMovesItToPlayedCardsAndTheCountAndPassesTheTurn() {
        arrangePlay(state, cards("10H", "4C"), cards("5S", "6H"));
        fm.next(state, new PlayCard(card("4C")));

        assertEquals(List.of(card("10H")), state.getPlayerHand(1).getComponents());
        assertEquals(List.of(card("4C")), state.getPlayedCards(1).getComponents());
        assertEquals(List.of(card("4C")), state.getPlaySequence());
        assertEquals(4, state.getRunningTotal());
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getScore(0));
        assertEquals(0, state.getScore(1));
        assertAllCardsPresent(state);
    }

    @Test
    public void fifteenInThePlayScoresOnePointByDefault() {
        arrangePlay(state, cards("7D", "2C"), cards("8S", "4H"));
        fm.next(state, new PlayCard(card("7D")));
        fm.next(state, new PlayCard(card("8S")));
        // 7 + 8 = 15, scored by the player of the Eight: playFifteenPoints 1
        assertEquals(1, state.getScore(0));
        assertEquals(0, state.getScore(1));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(15, state.getRunningTotal());
    }

    @Test
    public void fifteenInThePlayScoresTwoWithThePagatSetting() {
        params.setParameterValue("playFifteenPoints", 2);
        arrangePlay(state, cards("7D", "2C"), cards("8S", "4H"));
        fm.next(state, new PlayCard(card("7D")));
        fm.next(state, new PlayCard(card("8S")));
        assertEquals(2, state.getScore(0));
        assertEquals(0, state.getScore(1));
    }

    @Test
    public void thirtyOneScoresAndEndsTheCountWithNoLastCardPoint() {
        arrangePlay(state, cards("4H", "6S"), cards("9D", "8C"));
        playedInCount(state, 1, card("KC"));
        playedInCount(state, 0, card("QH"));
        playedInCount(state, 1, card("2C"));
        state.setTurnOwner(0);
        // 10 + 10 + 2 = 22; the Nine makes 31: thirtyOnePoints 2, and no last card point on top
        fm.next(state, new PlayCard(card("9D")));
        assertEquals(2, state.getScore(0));
        assertEquals(0, state.getScore(1));
        assertTrue(state.getPlaySequence().isEmpty());
        assertEquals(0, state.getRunningTotal());
        // the opponent of the player of the 31 leads the next count, with any card
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new PlayCard(card("4H")), new PlayCard(card("6S"))),
                new HashSet<>(fm.computeAvailableActions(state)));
        assertAllCardsPresent(state);
    }

    @Test
    public void playerContinuesAloneWhenTheOpponentCannotPlayThenScoresTheLastCard() {
        arrangePlay(state, cards("3H", "2D"), cards("8C", "9D"));
        playedInCount(state, 0, card("10H"));
        playedInCount(state, 1, card("6C"));
        playedInCount(state, 0, card("9S"));
        state.setTurnOwner(1);

        // 25 + 3 = 28: player 0 cannot play an Eight (36) or a Nine (37), so player 1 goes on
        fm.next(state, new PlayCard(card("3H")));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(List.of(new PlayCard(card("2D"))), fm.computeAvailableActions(state));

        // 28 + 2 = 30: player 1 has no cards and player 0 still cannot play, so the count ends.
        // Player 1 played the last card (30, not 31): lastCardPoints 1. Player 0, the opponent, leads next.
        fm.next(state, new PlayCard(card("2D")));
        assertEquals(0, state.getScore(0));
        assertEquals(1, state.getScore(1));
        assertTrue(state.getPlaySequence().isEmpty());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(new PlayCard(card("8C")), new PlayCard(card("9D"))),
                new HashSet<>(fm.computeAvailableActions(state)));
        assertAllCardsPresent(state);
    }

    @Test
    public void countEndsWhenNeitherCanPlayAndTheOpponentOfTheLastPlayerLeads() {
        arrangePlay(state, cards("4H", "8S"), cards("QD", "9D"));
        playedInCount(state, 1, card("10C"));
        playedInCount(state, 0, card("6D"));
        playedInCount(state, 1, card("2H"));
        state.setTurnOwner(0);

        // 10 + 6 + 2 = 18, and the Queen makes 28: player 1 cannot play (4 -> 32, 8 -> 36)
        // and neither can player 0 (9 -> 37), so the count ends
        fm.next(state, new PlayCard(card("QD")));
        assertEquals(1, state.getScore(0));   // last card, 28
        assertEquals(0, state.getScore(1));
        assertTrue(state.getPlaySequence().isEmpty());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new PlayCard(card("4H")), new PlayCard(card("8S"))),
                new HashSet<>(fm.computeAvailableActions(state)));
    }

    @Test
    public void lastPlayerLeadsTheNextCountWhenTheOpponentHasNoCards() {
        arrangePlay(state, cards("4H", "5S"), cards());
        playedInCount(state, 0, card("10H"));
        playedInCount(state, 1, card("6C"));
        playedInCount(state, 0, card("9S"));
        state.setTurnOwner(1);

        // 25 + 4 = 29: player 0 has no cards and the Five would make 34, so the count ends:
        // last card to player 1, who leads again as player 0 has no cards
        fm.next(state, new PlayCard(card("4H")));
        assertEquals(1, state.getScore(1));
        assertEquals(0, state.getScore(0));
        assertTrue(state.getPlaySequence().isEmpty());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(List.of(new PlayCard(card("5S"))), fm.computeAvailableActions(state));
    }

    @Test
    public void lastCardOfThePlayScoresLastCardPoints() {
        // The hands (4H + KS, 6D + KS) and the arranged crib score nothing in the show,
        // so the scores after the round are the pegging points alone.
        arrangePlay(state, cards("4H"), cards("6D"));
        fm.next(state, new PlayCard(card("4H")));
        fm.next(state, new PlayCard(card("6D")));
        // 4 + 6 = 10: the dealer played the last card of the play: lastCardPoints 1
        assertEquals(1, state.getScore(0));
        assertEquals(0, state.getScore(1));
    }

    @Test
    public void lastCardOfThePlayMakingThirtyOneScoresOnlyThirtyOne() {
        // The show scores nothing: 10H 2H + KS and QD 9D + KS have no fifteen, pair or run; the crib is ZERO_CRIB
        arrangePlay(state, cards("10H", "2H"), cards("QD", "9D"));
        fm.next(state, new PlayCard(card("10H")));   // 10
        fm.next(state, new PlayCard(card("QD")));    // 20
        fm.next(state, new PlayCard(card("2H")));    // 22
        fm.next(state, new PlayCard(card("9D")));    // 31: thirtyOnePoints 2, no last card point
        assertEquals(2, state.getScore(0));
        assertEquals(0, state.getScore(1));
    }
}
