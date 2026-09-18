package games.blackjack;

import core.actions.AbstractAction;
import games.blackjack.actions.Bet;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Betting;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

public class BlackjackBettingTest {

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
        state = new BlackjackGameState(params, 3);
        fm = new BlackjackForwardModel();
        fm.setup(state);
    }

    private Set<AbstractAction> legalActions() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    private static Set<AbstractAction> bets(int... amounts) {
        Set<AbstractAction> result = new HashSet<>();
        for (int a : amounts)
            result.add(new Bet(a));
        return result;
    }

    @Test
    public void withTenChipsEveryEvenBetFromTwoToTenIsLegal() {
        assertEquals(bets(2, 4, 6, 8, 10), legalActions());
    }

    @Test
    public void betsAreCappedByThePlayersChips() {
        state.chips[0] = 6;
        assertEquals(bets(2, 4, 6), legalActions());
        state.chips[0] = 7;     // bets are even: 7 chips allow no more than 6
        assertEquals(bets(2, 4, 6), legalActions());
    }

    @Test
    public void betsAreCappedByMaxBet() {
        // a pair differing only in maxBet, with 20 chips each
        params.setParameterValue("startingChips", 20);
        state = new BlackjackGameState(params, 3);
        fm.setup(state);
        assertEquals(bets(2, 4, 6, 8, 10), legalActions());

        params.setParameterValue("maxBet", 20);
        state = new BlackjackGameState(params, 3);
        fm.setup(state);
        assertEquals(bets(2, 4, 6, 8, 10, 12, 14, 16, 18, 20), legalActions());
    }

    @Test
    public void aBetMovesChipsToTheHandAndTheNextPlayerBets() {
        fm.next(state, new Bet(4));
        assertEquals(6, state.getChips(0));
        assertEquals(4, state.getBet(0, 0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Betting, state.getGamePhase());
        assertEquals("no cards dealt before all have bet", 52, state.getDrawDeck().getSize());
        assertEquals(0, state.getPlayerHand(0, 0).getSize());

        fm.next(state, new Bet(10));
        assertEquals(0, state.getChips(1));
        assertEquals(10, state.getBet(1, 0));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(Betting, state.getGamePhase());
        assertEquals(10, state.getChips(2));
        assertEquals(bets(2, 4, 6, 8, 10), legalActions());
    }

    @Test
    public void afterTheLastBetTheCardsAreDealtInTurnAndPlayerZeroPlays() {
        // draw deck order: first cards to players 0,1,2 - dealer's up card - second cards to 0,1,2 - hole card
        stackDrawDeck(state, "2H 3H 4H 5S 6H 7H 8H 9D 10C");
        fm.next(state, new Bet(2));
        fm.next(state, new Bet(4));
        fm.next(state, new Bet(6));

        assertEquals(setOf("2H 6H"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("3H 7H"), setOf(state.getPlayerHand(1, 0)));
        assertEquals(setOf("4H 8H"), setOf(state.getPlayerHand(2, 0)));
        assertEquals(setOf("5S"), setOf(state.getDealerHand()));
        assertEquals(setOf("9D"), setOf(state.getHoleCard()));
        assertEquals(52 - 2 * 3 - 2, state.getDrawDeck().getSize());
        assertEquals(card("10C"), state.getDrawDeck().peek());

        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getActiveHand());
        assertEquals(Set.of(new Hit(), new Stand()), legalActions());
        assertArrayEquals(new int[]{8, 6, 4}, state.chips);
        assertAllCardsPresent(state);
    }

    @Test
    public void aSinglePlayerIsDealtStraightAfterTheirBet() {
        state = new BlackjackGameState(params, 1);
        fm.setup(state);
        stackDrawDeck(state, "2H 5S 6H 9D");
        fm.next(state, new Bet(10));

        assertEquals(setOf("2H 6H"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("5S"), setOf(state.getDealerHand()));
        assertEquals(setOf("9D"), setOf(state.getHoleCard()));
        assertEquals(48, state.getDrawDeck().getSize());
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getChips(0));
        assertEquals(10, state.getBet(0, 0));
    }
}
