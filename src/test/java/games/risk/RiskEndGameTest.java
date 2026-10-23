package games.risk;

import games.risk.actions.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.GameResult.*;
import static games.risk.WorldMap.*;
import static games.risk.RiskTestUtils.*;
import static org.junit.Assert.*;

/**
 * Elimination, world domination and the round cap with its ranking.
 */
public class RiskEndGameTest {

    RiskForwardModel fm = new RiskForwardModel();

    /**
     * 4 players: player 0 attacks from Indonesia (6 armies) into Siam, player 2's only territory (1 army);
     * player 3 holds Argentina, player 1 the rest.
     */
    private RiskGameState fourPlayersWithPlayer2OnSiam() {
        RiskGameState state = newState(4, 7, null);
        fillBoard(state, 1);
        give(state, 0, 6, INDONESIA);
        give(state, 2, 1, SIAM);
        give(state, 3, 1, ARGENTINA);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        return state;
    }

    @Test
    public void capturingTheLastTerritoryEliminatesThePlayerInLastPlace() {
        RiskGameState state = fourPlayersWithPlayer2OnSiam();
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertTrue(state.isEliminated(2));
        assertEquals(4, state.getFinalPlace(2)); // first out of 4
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
        assertEquals(GAME_ONGOING, state.getGameStatus());
        for (int p : new int[]{0, 1, 3}) {
            assertFalse(state.isEliminated(p));
            assertEquals(GAME_ONGOING, state.getPlayerResults()[p]);
        }
        // the capture still needs its move
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
    }

    @Test
    public void capturingANonFinalTerritoryDoesNotEliminate() {
        RiskGameState state = fourPlayersWithPlayer2OnSiam();
        give(state, 2, 1, CHINA);
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertEquals(0, state.getOwner(SIAM));
        assertFalse(state.isEliminated(2));
        assertEquals(GAME_ONGOING, state.getPlayerResults()[2]);
    }

    @Test
    public void secondPlayerOutTakesTheNextPlaceUp() {
        RiskGameState state = fourPlayersWithPlayer2OnSiam();
        give(state, 1, 1, SIAM);
        eliminate(state, 2, 4);
        give(state, 0, 4, BRAZIL); // Brazil borders Argentina, player 3's only territory
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(BRAZIL, ARGENTINA, 3));
        assertTrue(state.isEliminated(3));
        assertEquals(3, state.getFinalPlace(3)); // 4 - 1 already out
        assertEquals(LOSE_GAME, state.getPlayerResults()[3]);
    }

    @Test
    public void eliminatedPlayersAreSkippedInTurnOrder() {
        RiskGameState state = fourPlayersWithPlayer2OnSiam();
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        fm.next(state, new EndAttack());
        fm.next(state, new EndTurn());
        assertEquals(1, state.getCurrentPlayer());
        // player 1 ends their turn: player 2 is out, so player 3
        startPlay(state, 1, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new EndTurn());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(3, state.getArmiesToPlace(3)); // 1 territory -> minimum 3
    }

    @Test
    public void capturingTheFortySecondTerritoryWinsAtOnce() {
        // 3 players: player 2 already out; player 1 holds only Siam, player 0 everything else
        RiskGameState state = newState(3, 7, null);
        fillBoard(state, 0);
        give(state, 0, 4, INDONESIA);
        give(state, 1, 1, SIAM);
        eliminate(state, 2, 3);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertEquals(42, state.getNTerritories(0));
        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
        assertEquals(2, state.getFinalPlace(1)); // 3 - 1 already out
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(1));
        assertEquals(3, state.getOrdinalPosition(2));
    }

    /**
     * 4 players: players 0 and 1 hold 21 territories each; player 1 has one more army (22 v 21).
     * Player 2 went out first (place 4), player 3 second (place 3).
     */
    private RiskGameState tiedOnTerritories(RiskParameters params) {
        RiskGameState state = newState(4, 7, params);
        List<RiskTerritory> all = WorldMap.ALL;
        List<RiskTerritory> half0 = new ArrayList<>(), half1 = new ArrayList<>();
        for (int i = 0; i < 42; i++)
            (i % 2 == 0 ? half0 : half1).add(all.get(i));
        give(state, 0, 1, half0);
        give(state, 1, 1, half1);
        give(state, 1, 2, half1.get(0));
        eliminate(state, 2, 4);
        eliminate(state, 3, 3);
        return state;
    }

    @Test
    public void ordinalPositionIsTerritoriesThenArmiesThenFinalPlace() {
        RiskGameState state = tiedOnTerritories(null);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        assertEquals(2, state.getOrdinalPosition(0));
        assertEquals(1, state.getOrdinalPosition(1));
        assertEquals(4, state.getOrdinalPosition(2));
        assertEquals(3, state.getOrdinalPosition(3));

        // territories count before armies: player 0 takes one of player 1's (22 v 20 territories),
        // while player 1 gets more armies (19 + 10 = 29 v 22)
        give(state, 0, 1, state.getTerritories(1).get(1));
        give(state, 1, 10, state.getTerritories(1).get(0));
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(1));
    }

    @Test
    public void gameEndsWhenTheRoundCapIsReachedAndRanksThePlayers() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("maxRounds", 1);
        RiskGameState state = tiedOnTerritories(params);
        // player 1 ends their turn; the next player still in is 0, back round the table: round 1 = maxRounds
        startPlay(state, 1, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new EndTurn());
        assertFalse(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertEquals(WIN_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[3]);
    }

    @Test
    public void gameContinuesBeforeTheRoundCap() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("maxRounds", 2);
        RiskGameState state = tiedOnTerritories(params);
        startPlay(state, 1, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new EndTurn());
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
    }
}
