package games.president;

import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.president.actions.PlayCards;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static games.president.PresidentGameState.Phase.EXCHANGE;
import static games.president.PresidentGameState.Phase.PLAY;
import static games.president.PresidentTestUtils.*;
import static org.junit.Assert.*;

/**
 * Playing to a target score (targetScore > 1): the deal end, the start of the next deal, and the end of the game.
 */
public class PresidentMultiDealTest {

    PresidentForwardModel fm;

    @Before
    public void setup() {
        fm = new PresidentForwardModel();
    }

    /**
     * 4 players, targetScore 11, exchangeCards 1. Player 2 is already out (President); player 0 goes out second
     * (+1), player 3 third (0), leaving player 1 as the Scum. Scores before: 4, 2, 10, 1.
     */
    private PresidentGameState playToADealEndBelowTheTarget(PresidentParameters params) {
        PresidentGameState state = newState(params, 4, 71, fm);
        arrangeOut(state, 2);
        state.playerScores[0] = 4;
        state.playerScores[1] = 2;
        state.playerScores[2] = 10;
        state.playerScores[3] = 1;
        giveHand(state, 0, "5H");
        giveHand(state, 1, "3C", "4D");
        giveHand(state, 3, "KS");
        state.setTurnOwner(0);

        play(state, fm, 0, 5, 1);    // 0 goes out second: 4 + 1 = 5
        pass(state, fm, 1);          // 2 is out, so 3 is next
        play(state, fm, 3, 13, 1);   // 3 goes out third; only player 1 holds cards
        return state;
    }

    @Test
    public void aDealEndBelowTheTargetStartsANewDealWithTheExchange() {
        PresidentGameState state = playToADealEndBelowTheTarget(multiDealParams(11, 1));

        // scores 5, 2, 10, 1: the highest, 10, is below 11
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertArrayEquals(new int[]{5, 2, 10, 1}, state.playerScores);

        // everything reset for the new deal
        assertEquals(0, state.getPlayPile().getSize());
        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals(List.of(), state.getFinishingOrder());
        assertEquals(-1, state.getLastPlayer());
        assertEquals(0, state.getSetSize());
        assertEquals(0, state.getPassesInRow());
        assertAllCardsPresent(state);

        // 13 cards each; then the Scum (1) gives their highest card to the President (2): 12 and 14
        assertEquals(13, state.getPlayerHand(0).getSize());
        assertEquals(12, state.getPlayerHand(1).getSize());
        assertEquals(14, state.getPlayerHand(2).getSize());
        assertEquals(13, state.getPlayerHand(3).getSize());

        // the President chooses a card to give back
        assertEquals(EXCHANGE, state.getGamePhase());
        assertEquals(1, state.getScum());
        assertEquals(1, state.getCardsToGive());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(giveCardsFor(state.getPlayerHand(2)), actionSet(state, fm));

        // the card received is above every card the Scum kept (by rank, then suit order)
        int scumBest = state.getPlayerHand(1).getComponents().stream().mapToInt(PresidentTestUtils::exchangeOrder).max().orElseThrow();
        assertTrue("the President holds no card above all the Scum's",
                state.getPlayerHand(2).getComponents().stream().anyMatch(c -> exchangeOrder(c) > scumBest));
    }

    @Test
    public void withNoExchangeTheNewDealIsDealtFromThePresidentWhoLeads() {
        // 5 players, targetScore 11, exchangeCards 0: 3, 0 and 1 are out in that order; player 2 goes out fourth
        // with the 9, leaving player 4 as the Scum
        PresidentGameState state = newState(multiDealParams(11, 0), 5, 72, fm);
        arrangeOut(state, 3, 0, 1);
        state.playerScores[0] = 1;
        state.playerScores[1] = 0;
        state.playerScores[2] = 4;
        state.playerScores[3] = 6;
        state.playerScores[4] = 2;
        giveHand(state, 2, "9D");
        giveHand(state, 4, "4S", "8C");
        state.setTurnOwner(2);

        play(state, fm, 2, 9, 1);

        // the fourth out scores 0: scores unchanged, the highest 6 < 11
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertArrayEquals(new int[]{1, 0, 4, 6, 2}, state.playerScores);
        // 52 = 5 x 10 + 2: dealt from the President (3), players 3 and 4 get 11, the others 10; no exchange
        assertEquals(10, state.getPlayerHand(0).getSize());
        assertEquals(10, state.getPlayerHand(1).getSize());
        assertEquals(10, state.getPlayerHand(2).getSize());
        assertEquals(11, state.getPlayerHand(3).getSize());
        assertEquals(11, state.getPlayerHand(4).getSize());
        assertEquals(0, state.getPlayPile().getSize());
        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals(List.of(), state.getFinishingOrder());
        assertEquals(-1, state.getLastPlayer());
        assertEquals(0, state.getSetSize());
        assertEquals(0, state.getPassesInRow());
        assertAllCardsPresent(state);

        // the President leads straight away: every set held, no Pass
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(-1, state.getScum());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(leadSets(state, 3), actionSet(state, fm));
    }

    /** The PlayCards player p may lead. */
    private static Set<AbstractAction> leadSets(PresidentGameState state, int p) {
        int[] held = new int[15];
        for (FrenchCard c : state.getPlayerHand(p).getComponents())
            held[c.number]++;
        Set<AbstractAction> sets = new HashSet<>();
        for (int number = 2; number <= 14; number++)
            for (int count = 1; count <= held[number]; count++)
                sets.add(new PlayCards(number, count));
        return sets;
    }

    @Test
    public void theGameEndsWhenAScoreReachesTheTargetAndATieGoesToTheEarlierFinisher() {
        // 4 players, targetScore 5. Scores before: 4, 3, 0, 0. Player 1 goes out first (3 + 2 = 5), player 0
        // second (4 + 1 = 5), player 2 third, player 3 is the Scum
        PresidentGameState state = newState(multiDealParams(5, 1), 4, 73, fm);
        state.playerScores[0] = 4;
        state.playerScores[1] = 3;
        giveHand(state, 1, "7H");
        giveHand(state, 0, "8S");
        giveHand(state, 2, "3C");
        giveHand(state, 3, "4H", "5H");
        state.setTurnOwner(1);

        play(state, fm, 1, 7, 1);
        pass(state, fm, 2);
        pass(state, fm, 3);
        play(state, fm, 0, 8, 1);
        pass(state, fm, 2);
        pass(state, fm, 3);          // cleared; 0 and 1 are out, so 2 leads
        play(state, fm, 2, 3, 1);

        // 5 >= 5: the game ends
        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(List.of(1, 0, 2, 3), state.getFinishingOrder());
        assertArrayEquals(new int[]{5, 5, 0, 0}, state.playerScores);
        // 0 and 1 tie on 5: player 1 went out first in the last deal
        assertEquals(1, state.getOrdinalPosition(1));
        assertEquals(2, state.getOrdinalPosition(0));
        assertEquals(3, state.getOrdinalPosition(2));
        assertEquals(4, state.getOrdinalPosition(3));
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, LOSE_GAME},
                state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void theHighestScoreWinsEvenIfAnotherPlayerWasPresidentInTheLastDeal() {
        // 4 players, targetScore 5. Scores before: 4, 0, 0, 1. Player 3 goes out first (1 + 2 = 3), player 0
        // second (4 + 1 = 5), player 1 third, player 2 is the Scum
        PresidentGameState state = newState(multiDealParams(5, 1), 4, 74, fm);
        state.playerScores[0] = 4;
        state.playerScores[3] = 1;
        giveHand(state, 3, "7H");
        giveHand(state, 0, "8S");
        giveHand(state, 1, "3C");
        giveHand(state, 2, "4H", "5H");
        state.setTurnOwner(3);

        play(state, fm, 3, 7, 1);
        play(state, fm, 0, 8, 1);
        pass(state, fm, 1);
        pass(state, fm, 2);          // cleared; 3 and 0 are out, so 1 leads
        play(state, fm, 1, 3, 1);

        assertFalse(state.isNotTerminal());
        assertEquals(List.of(3, 0, 1, 2), state.getFinishingOrder());
        assertArrayEquals(new int[]{5, 0, 0, 3}, state.playerScores);
        // by score 0 (5), 3 (3); then 1 and 2 on 0, player 1 went out first
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(3));
        assertEquals(3, state.getOrdinalPosition(1));
        assertEquals(4, state.getOrdinalPosition(2));
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME, LOSE_GAME},
                state.getPlayerResults());
    }

    @Test
    public void theGameEndsAfterMaxRoundsDealsBelowTheTarget() {
        // the same deal end as above (scores 5, 2, 10, 1, all below 11), but maxRounds 1: the first deal is the last
        PresidentParameters params = multiDealParams(11, 1);
        params.setMaxRounds(1);
        PresidentGameState state = playToADealEndBelowTheTarget(params);

        assertFalse(state.isNotTerminal());
        assertArrayEquals(new int[]{5, 2, 10, 1}, state.playerScores);
        // by score: 2 (10), 0 (5), 1 (2), 3 (1)
        assertEquals(1, state.getOrdinalPosition(2));
        assertEquals(2, state.getOrdinalPosition(0));
        assertEquals(3, state.getOrdinalPosition(1));
        assertEquals(4, state.getOrdinalPosition(3));
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME},
                state.getPlayerResults());
    }
}
