package games.monopoly;

import core.CoreConstants.GameResult;
import games.monopoly.actions.EndTurn;
import org.junit.Before;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Payments, bankruptcy, the last player standing, maxRounds and the ranking by net worth.
 */
public class MonopolyEndGameTest {

    MonopolyParameters params;
    MonopolyGameState state;
    MonopolyForwardModel fm;
    final MonopolySquare oldKent = sq("Old Kent Road"), whitechapel = sq("Whitechapel Road"), mayfair = sq("Mayfair");

    @Before
    public void setup() {
        params = new MonopolyParameters();
        state = newState(3, 7, params);
        fm = new MonopolyForwardModel();
        startTurn(state, 0, sq("GO"));
    }

    private void newPlayers(int nPlayers) {
        state = newState(nPlayers, 7, params);
        startTurn(state, 0, sq("GO"));
    }

    @Test
    public void aPaymentTheCashCoversGoesToTheCreditor() {
        state.pay(0, 2, 300);
        assertEquals(1500 - 300, state.getCash(0));
        assertEquals(1500 + 300, state.getCash(2));
        state.pay(0, -1, 100);
        assertEquals(1500 - 300 - 100, state.getCash(0));
        assertEquals(1500 * 2 + 300, state.getCash(1) + state.getCash(2));
        // paying exactly the cash in hand is not bankruptcy
        state.pay(0, 1, 1100);
        assertEquals(0, state.getCash(0));
        assertEquals(1500 + 1100, state.getCash(1));
        assertFalse(state.isBankrupt(0));
    }

    @Test
    public void rentBeyondTheCashMakesThePlayerBankruptToTheOwner() {
        state.setCash(0, 10);
        give(state, 0, oldKent);
        give(state, 1, mayfair);
        state.setPosition(0, sq("Liverpool Street Station"));
        roll(state, fm, 1, 3);
        // 35 + 4 = Mayfair, rent 50 > 10 cash (+ 30 mortgage value of Old Kent Road)
        assertTrue(state.isBankrupt(0));
        assertEquals(3, state.getFinalPlace(0));
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(0, state.getCash(0));
        assertEquals(1500 + 10, state.getCash(1));
        assertEquals(1, state.getOwner(oldKent));
        assertTrue(state.getProperties(0).isEmpty());
        assertEquals(1500, state.getCash(2));
        // the turn passes at once to the next player
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
        assertEquals(GAME_ONGOING, state.getGameStatus());
    }

    @Test
    public void aLaterBankruptTakesTheNextPlaceUp() {
        newPlayers(4);
        eliminate(state, 3, 4);
        state.goBankrupt(0, -1);
        assertEquals(4 - 1, state.getFinalPlace(0));
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(2, state.getNPlayersIn());
    }

    @Test
    public void taxBeyondTheCashMakesThePlayerBankruptToTheBank() {
        state.setCash(0, 50);
        give(state, 0, oldKent);
        state.setMortgaged(oldKent, true);
        roll(state, fm, 1, 3);
        // GO + 4 = Income Tax, 200 > 50 cash
        assertTrue(state.isBankrupt(0));
        assertEquals(3, state.getFinalPlace(0));
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(0, state.getCash(0));
        // the property goes back to the Bank, unmortgaged, and is auctioned; nobody bids, so the Bank keeps it
        assertEquals(-1, state.getOwner(oldKent));
        assertFalse(state.isMortgaged(oldKent));
        assertEquals(oldKent, passAll(state, fm));
        assertEquals(-1, state.getOwner(oldKent));
        assertEquals(1500, state.getCash(1));
        assertEquals(1500, state.getCash(2));
        // then the turn passes to the next player
        assertFalse(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
    }

    @Test
    public void theLastPlayerStandingWins() {
        newPlayers(2);
        state.setCash(0, 10);
        give(state, 1, mayfair);
        state.setPosition(0, sq("Liverpool Street Station"));
        roll(state, fm, 1, 3);
        assertEquals(GAME_END, state.getGameStatus());
        assertEquals(WIN_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(2, state.getFinalPlace(0));
    }

    @Test
    public void theGameGoesOnWhileTwoPlayersAreIn() {
        newPlayers(4);
        eliminate(state, 2, 4);
        state.setCash(0, 10);
        give(state, 1, mayfair);
        state.setPosition(0, sq("Liverpool Street Station"));
        roll(state, fm, 1, 3);
        assertEquals(3, state.getFinalPlace(0));
        assertEquals(GAME_ONGOING, state.getGameStatus());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void theNetWorthCountsCashPricesMortgageValuesAndBuildings() {
        state.setCash(0, 100);
        give(state, 0, oldKent, whitechapel, mayfair);
        state.setBuildings(oldKent, 2);
        state.setMortgaged(whitechapel, true);
        state.setBuildings(mayfair, MonopolyParameters.HOTEL);
        assertEquals(100 + 60 /* Old Kent Road */ + 2 * 50 /* houses */ + 30 /* Whitechapel mortgaged */
                + 400 /* Mayfair */ + 5 * 200 /* hotel */, state.getNetWorth(0));
    }

    /** Four players: 0 worth 930, 1 worth 900, 2 out (4th), 3 worth p3Cash; player 3 ends the round. */
    private void endLastRound(int maxRounds, int p3Cash) {
        params.setParameterValue("maxRounds", maxRounds);
        newPlayers(4);
        state.setCash(0, 900);
        give(state, 0, oldKent);
        state.setMortgaged(oldKent, true); // + 30
        state.setCash(1, 500);
        give(state, 1, mayfair); // + 400
        eliminate(state, 2, 4);
        state.setCash(3, p3Cash);
        state.setTurnOwner(3);
        state.setGamePhase(MonopolyGamePhase.MANAGE);
        fm.next(state, new EndTurn());
    }

    @Test
    public void theGameEndsAfterMaxRoundsRankingThePlayersByNetWorth() {
        endLastRound(1, 1000);
        assertEquals(GAME_END, state.getGameStatus());
        assertEquals(2, state.getOrdinalPosition(0));
        assertEquals(3, state.getOrdinalPosition(1));
        assertEquals(4, state.getOrdinalPosition(2));
        assertEquals(1, state.getOrdinalPosition(3));
        assertArrayEquals(new GameResult[]{LOSE_GAME, LOSE_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void playersTiedOnNetWorthShareTheirPlace() {
        endLastRound(1, 930);
        assertEquals(GAME_END, state.getGameStatus());
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(1, state.getOrdinalPosition(3));
        assertEquals(3, state.getOrdinalPosition(1));
        assertArrayEquals(new GameResult[]{DRAW_GAME, LOSE_GAME, LOSE_GAME, DRAW_GAME}, state.getPlayerResults());
    }

    @Test
    public void theGameGoesOnBeforeMaxRounds() {
        endLastRound(2, 1000);
        assertEquals(GAME_ONGOING, state.getGameStatus());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, state.getRoundCounter());
    }
}
