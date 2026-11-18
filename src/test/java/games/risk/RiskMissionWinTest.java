package games.risk;

import games.risk.actions.*;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.risk.RiskTestUtils.*;
import static games.risk.WorldContinents.*;
import static games.risk.WorldMap.*;
import static games.risk.WorldMissions.*;
import static org.junit.Assert.*;

/**
 * Secret Mission play: who eliminated whom, the destroy missions, when a mission win is checked and who wins it, and
 * the final positions.
 */
public class RiskMissionWinTest {

    RiskForwardModel fm = new RiskForwardModel();

    /**
     * 4 players, secretMission (unless params say otherwise), player 0 attacking in ATTACK:
     * player 0 Indonesia (6); player 2 Siam (1) only; player 3 North America, South America and Europe (9 + 4 + 7 =
     * 20); player 1 the rest (Africa 6 + Asia 11 + Australia 3 = 20). Missions, none complete: player 0 18 territories
     * with 2 armies; player 1 Asia and Africa (lacks Siam); player 2 North America and Australia; player 3 Europe,
     * Australia and a 3rd (lacks Australia).
     */
    private RiskGameState base(RiskParameters params) {
        RiskGameState state = newState(4, 7, params);
        fillBoard(state, 1);
        give(state, 3, 1, continent(NORTH_AMERICA));
        give(state, 3, 1, continent(SOUTH_AMERICA));
        give(state, 3, 1, continent(EUROPE));
        give(state, 0, 6, INDONESIA);
        give(state, 2, 1, SIAM);
        if (((RiskParameters) state.getGameParameters()).secretMission)
            giveMissions(state, OCCUPY_18_TWO_ARMIES, ASIA_AFRICA, NA_AUSTRALIA, EUROPE_AUSTRALIA_3RD);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        return state;
    }

    private RiskGameState base() {
        return base(missionParams());
    }

    private void assertWonBy(RiskGameState state, int winner) {
        assertEquals(winner, state.getMissionWinner());
        assertFalse(state.isNotTerminal());
        for (int p = 0; p < state.getNPlayers(); p++)
            assertEquals("player " + p, p == winner ? WIN_GAME : LOSE_GAME, state.getPlayerResults()[p]);
    }

    private void assertOngoing(RiskGameState state) {
        assertEquals(-1, state.getMissionWinner());
        assertTrue(state.isNotTerminal());
        assertEquals(GAME_ONGOING, state.getPlayerResults()[0]);
    }

    // ---- who eliminated whom ----

    @Test
    public void anAttackThatEliminatesRecordsTheAttacker() {
        RiskGameState state = base();
        assertEquals(-1, state.getEliminatedBy(2));
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertTrue(state.isEliminated(2));
        assertEquals(0, state.getEliminatedBy(2));
        for (int p : new int[]{0, 1, 3})
            assertEquals(-1, state.getEliminatedBy(p));
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        assertOngoing(state); // nobody holds destroy 2 (it is unused), and no mission is complete
    }

    @Test
    public void aBlitzThatEliminatesRecordsTheAttacker() {
        RiskGameState state = base();
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Blitz(INDONESIA, SIAM));
        assertTrue(state.isEliminated(2));
        assertEquals(0, state.getEliminatedBy(2));
    }

    @Test
    public void aDefendWithThatLosesTheLastTerritoryRecordsTheAttacker() {
        RiskParameters params = missionParams();
        params.setParameterValue("defenderChoosesDice", true);
        RiskGameState state = base(params);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new DefendWith(INDONESIA, SIAM, 3, 1));
        assertTrue(state.isEliminated(2));
        assertEquals(0, state.getEliminatedBy(2));
    }

    // ---- destroy missions ----

    @Test
    public void eliminatingTheTargetWinsForTheDestroyHolderOnceTheMoveInIsMade() {
        RiskGameState state = base();
        giveMission(state, 0, destroy(2));
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        // the capture's move-in is still to choose: no win yet
        assertOngoing(state);
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        assertWonBy(state, 0);
    }

    @Test
    public void whenSomeoneElseEliminatesTheTargetTheDestroyHolderTakesTheTargetsMission() {
        RiskGameState state = base();
        giveMission(state, 1, destroy(2)); // player 1's Asia and Africa goes to the unused missions
        // player 2 holds North America and Australia; player 0 eliminates player 2
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        assertEquals(NA_AUSTRALIA, state.getMission(1));
        assertNull("player 2's card went to player 1", state.getMission(2));
        assertTrue("player 1's old card is out of the game", state.getUnusedMissions().contains(destroy(2)));
        assertTrue(sameCards(ALL_MISSIONS, allMissionCards(state)));
        // player 1 does not win: North America is player 3's
        assertOngoing(state);
        assertEquals(OCCUPY_18_TWO_ARMIES, state.getMission(0));
    }

    @Test
    public void aTakenOverCardNamingItsNewHolderIsOccupy24AndCanWinAtOnce() {
        RiskGameState state = base();
        // player 1 also takes South America: 20 + 4 = 24 territories (player 3 has 16)
        give(state, 1, 1, continent(SOUTH_AMERICA));
        giveMission(state, 1, destroy(2));
        giveMission(state, 2, destroy(1));
        assertEquals(24, state.getNTerritories(1));
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertOngoing(state);
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        // player 1 now holds "destroy 1" = themselves -> occupy 24 territories, held: player 1 wins (not player 0,
        // whose turn it is, as player 0's mission is not complete)
        assertEquals(destroy(1), state.getMission(1));
        assertTrue(state.getUnusedMissions().contains(destroy(2)));
        assertWonBy(state, 1);
    }

    @Test
    public void aTakenOverCardNamingItsNewHolderDoesNotWinWith23Territories() {
        RiskGameState state = base();
        // player 1: 20 + Venezuela, Peru, Brazil = 23 territories
        give(state, 1, 1, VENEZUELA, PERU, BRAZIL);
        giveMission(state, 1, destroy(2));
        giveMission(state, 2, destroy(1));
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        assertEquals(destroy(1), state.getMission(1));
        assertOngoing(state); // 23 < 24
    }

    // ---- the win: when, who first, and the positions ----

    /**
     * 4 players, player 3 already out (4th): player 0 Asia + Australia (12 + 4 = 16), player 1 Europe + South
     * America (7 + 4 = 11), player 2 North America + Africa (9 + 6 = 15), all 1 army each.
     * Missions: 0 Asia and Australia (complete), 1 Europe, South America and a 3rd (no 3rd: not complete),
     * 2 North America and Africa (complete), 3 North America and Australia.
     */
    private RiskGameState twoCompleteMissions(int current) {
        RiskGameState state = newState(4, 7, missionParams());
        give(state, 0, 1, continent(ASIA));
        give(state, 0, 1, continent(WorldContinents.AUSTRALIA));
        give(state, 1, 1, continent(EUROPE));
        give(state, 1, 1, continent(SOUTH_AMERICA));
        give(state, 2, 1, continent(NORTH_AMERICA));
        give(state, 2, 1, continent(AFRICA));
        eliminate(state, 3, 4);
        giveMissions(state, ASIA_AUSTRALIA, EUROPE_SOUTH_AMERICA_3RD, NA_AFRICA, NA_AUSTRALIA);
        startPlay(state, current, RiskGamePhase.ATTACK, 0);
        return state;
    }

    @Test
    public void whenSeveralMissionsAreCompleteTheCurrentPlayerWins() {
        // players 0 and 2 are complete; it is player 2's turn, so player 2 wins though player 0 comes first by index
        RiskGameState state = twoCompleteMissions(2);
        fm.next(state, new EndAttack());
        assertWonBy(state, 2);

        state = twoCompleteMissions(0);
        fm.next(state, new EndAttack());
        assertWonBy(state, 0);
    }

    @Test
    public void theMissionWinnerIsFirstThenThePlayersStillInByTerritoriesThenTheEliminated() {
        RiskGameState state = twoCompleteMissions(2);
        fm.next(state, new EndAttack());
        // winner 2 (15 territories) is 1st; player 0 (16) 2nd and player 1 (11) 3rd though player 0 holds more
        // than the winner; player 3, out first of 4, is 4th
        assertEquals(1, state.getOrdinalPosition(2));
        assertEquals(2, state.getOrdinalPosition(0));
        assertEquals(3, state.getOrdinalPosition(1));
        assertEquals(4, state.getOrdinalPosition(3));
    }

    @Test
    public void theMissionIsCheckedAfterEveryActionEvenAPlacement() {
        RiskGameState state = newState(4, 7, missionParams());
        fillBoard(state, 1);
        // player 0: the first 18 territories (index order) with 2 armies, but the 18th with 1 - 17 count
        give(state, 0, 2, ALL.subList(0, 18));
        give(state, 0, 1, ALL.get(17));
        give(state, 2, 1, SIAM);
        give(state, 3, 1, EASTERN_AUSTRALIA);
        // player 1: 42 - 18 - 2 = 22 territories (Asia lacks Siam), 1 army each
        giveMissions(state, OCCUPY_18_TWO_ARMIES, ASIA_AFRICA, NA_AUSTRALIA, EUROPE_AUSTRALIA_3RD);
        startPlay(state, 0, RiskGamePhase.REINFORCE, 3);
        fm.next(state, new PlaceArmy(ALL.get(0))); // still 17 with 2 or more
        assertOngoing(state);
        fm.next(state, new PlaceArmy(ALL.get(17))); // 18 with 2 or more
        assertWonBy(state, 0);
        assertEquals(1, state.getArmiesToPlace(0)); // 3 - 2 placed: the game ended with an army still to place
    }

    @Test
    public void withoutSecretMissionCompletingWhatWouldBeAMissionDoesNotWin() {
        RiskGameState state = newState(4, 7, null);
        fillBoard(state, 1);
        give(state, 0, 2, ALL.subList(0, 24)); // 24 territories, each with 2: would complete both occupy missions
        give(state, 2, 1, SIAM);
        give(state, 3, 1, EASTERN_AUSTRALIA);
        startPlay(state, 0, RiskGamePhase.REINFORCE, 2);
        fm.next(state, new PlaceArmy(ALL.get(0)));
        assertOngoing(state);
        assertNull(state.getMission(0));
    }

    /**
     * Walk-through: player 0 holds Australia and all of Asia but Siam (4 + 11 = 15), with the mission Asia and
     * Australia; player 2 holds only Siam. Player 0 reinforces Indonesia, attacks Siam, eliminates player 2 and wins
     * once the armies have moved in.
     */
    @Test
    public void aTurnThatCompletesAConquestWinsWhenTheArmiesMoveIn() {
        RiskGameState state = newState(4, 7, missionParams());
        fillBoard(state, 1);
        give(state, 3, 1, continent(NORTH_AMERICA));
        give(state, 3, 1, continent(SOUTH_AMERICA));
        give(state, 3, 1, continent(EUROPE));
        give(state, 0, 1, continent(ASIA));
        give(state, 0, 1, continent(WorldContinents.AUSTRALIA));
        give(state, 0, 6, INDONESIA);
        give(state, 2, 1, SIAM);
        // player 1: Africa (6); player 3: 20
        giveMissions(state, ASIA_AUSTRALIA, ASIA_AFRICA, NA_AUSTRALIA, EUROPE_AUSTRALIA_3RD);
        startPlay(state, 0, RiskGamePhase.REINFORCE, 3);

        for (int i = 0; i < 3; i++) {
            fm.next(state, new PlaceArmy(INDONESIA));
            assertOngoing(state);
        }
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(9, state.getArmies(INDONESIA)); // 6 + 3

        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        // Siam is player 0's, so Asia is held - but the move-in is pending
        assertEquals(0, state.getOwner(SIAM));
        assertTrue(state.isEliminated(2));
        assertEquals(0, state.getEliminatedBy(2));
        assertOngoing(state);
        assertEquals(0, state.getCurrentPlayer());

        fm.next(state, new MoveArmies(INDONESIA, SIAM, 5));
        assertWonBy(state, 0);
        assertEquals(1, state.getOrdinalPosition(0));
        // player 3 (20 territories) 2nd, player 1 (6) 3rd, player 2 out first: 4th
        assertEquals(2, state.getOrdinalPosition(3));
        assertEquals(3, state.getOrdinalPosition(1));
        assertEquals(4, state.getOrdinalPosition(2));
        assertTrue(sameCards(ALL_MISSIONS, allMissionCards(state)));
    }
}
