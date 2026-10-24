package games.monopoly;

import games.monopoly.actions.BuyDecision;
import games.monopoly.actions.EndTurn;
import games.monopoly.actions.Mortgage;
import games.monopoly.actions.RollDice;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

/**
 * ROLL and MANAGE, movement, the GO salary, doubles, and passing the turn. Rolls avoid landing on Chance and
 * Community Chest squares, where a card would be drawn.
 */
public class MonopolyTurnTest {

    MonopolyParameters params;
    MonopolyGameState state;
    MonopolyForwardModel fm;

    @Before
    public void setup() {
        params = new MonopolyParameters();
        state = newState(3, 7, params);
        fm = new MonopolyForwardModel();
        startTurn(state, 0, sq("GO"));
    }

    private void manage(int player, boolean anotherRoll, int nDoubles) {
        state.setTurnOwner(player);
        state.setGamePhase(MonopolyGamePhase.MANAGE);
        state.setAnotherRoll(anotherRoll);
        state.setNDoubles(nDoubles);
    }

    @Test
    public void rollPhaseOffersOnlyRollDiceOutOfJail() {
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
    }

    @Test
    public void rollMovesTheTokenByTheTotalAndRecordsTheDice() {
        roll(state, fm, 2, 4);
        // GO (0) + 2 + 4 = 6, The Angel Islington: unowned, so the roller decides whether to buy
        assertEquals(sq("The Angel Islington"), state.getPosition(0));
        assertArrayEquals(new int[]{2, 4}, state.getDice());
        assertEquals(1500, state.getCash(0)); // GO not passed
        assertEquals(new BuyDecision(0, sq("The Angel Islington")), state.currentActionInProgress());
    }

    @Test
    public void passingGoCollectsTheSalary() {
        give(state, 0, sq("Whitechapel Road"));
        state.setPosition(0, sq("Super Tax"));
        roll(state, fm, 2, 3);
        // 38 + 5 = 43 -> 3, Whitechapel Road (own property: nothing more)
        assertEquals(sq("Whitechapel Road"), state.getPosition(0));
        assertEquals(1500 + 200 /* GO salary */, state.getCash(0));
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn(), new Mortgage(sq("Whitechapel Road"))), actionSet(fm, state));
    }

    @Test
    public void landingOnGoExactlyCollectsTheSalaryOnce() {
        state.setPosition(0, sq("Liverpool Street Station"));
        roll(state, fm, 2, 3);
        // 35 + 5 = 40 -> 0, GO
        assertEquals(sq("GO"), state.getPosition(0));
        assertEquals(1500 + 200, state.getCash(0));
    }

    @Test
    public void afterAnOrdinaryRollManageOffersOnlyEndTurnWhichPassesTheTurn() {
        roll(state, fm, 4, 6);
        // GO + 10 = Jail, Just Visiting
        assertEquals(sq("Jail"), state.getPosition(0));
        assertFalse(state.isInJail(0));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));

        fm.next(state, new EndTurn());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
    }

    @Test
    public void doublesGiveTheSamePlayerAnotherRollAfterManage() {
        roll(state, fm, 5, 5);
        // GO + 10 = Jail, Just Visiting
        assertEquals(sq("Jail"), state.getPosition(0));
        assertEquals(1, state.getNDoubles());
        assertTrue(state.hasAnotherRoll());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));

        roll(state, fm, 4, 6);
        // 10 + 10 = Free Parking; not a double, so no further roll
        assertEquals(sq("Free Parking"), state.getPosition(0));
        assertFalse(state.hasAnotherRoll());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));

        fm.next(state, new EndTurn());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getNDoubles());
        assertFalse(state.hasAnotherRoll());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
    }

    @Test
    public void theThirdDoubleSendsThePlayerToJailWithoutMovingOrCollectingTheSalary() {
        state.setPosition(0, sq("Park Lane"));
        manage(0, true, 2);
        roll(state, fm, 3, 3);
        // 37 + 6 would pass GO; the third double goes straight to Jail instead
        assertEquals(sq("Jail"), state.getPosition(0));
        assertTrue(state.isInJail(0));
        assertEquals(0, state.getJailRolls(0));
        assertEquals(1500, state.getCash(0));
        // the turn ends at once
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
        assertEquals(0, state.getNDoubles());
        assertFalse(state.hasAnotherRoll());
    }

    @Test
    public void aSecondDoubleMovesWithTheDefaultMaxDoubles() {
        state.setPosition(0, sq("Jail"));
        manage(0, true, 1);
        roll(state, fm, 2, 2);
        // 10 + 4 = Northumberland Avenue
        assertEquals(sq("Northumberland Avenue"), state.getPosition(0));
        assertFalse(state.isInJail(0));
        assertEquals(2, state.getNDoubles());
    }

    @Test
    public void aSecondDoubleSendsThePlayerToJailWhenMaxDoublesIsTwo() {
        params.setParameterValue("maxDoubles", 2);
        state.setPosition(0, sq("Jail"));
        manage(0, true, 1);
        roll(state, fm, 2, 2);
        assertEquals(sq("Jail"), state.getPosition(0));
        assertTrue(state.isInJail(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void doublesRolledInOneTurnDoNotCountInTheNext() {
        // player 0 rolled two doubles and then a non-double
        manage(0, false, 2);
        fm.next(state, new EndTurn());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getNDoubles());

        roll(state, fm, 5, 5);
        // player 1's first double: GO + 10 = Jail, Just Visiting, with another roll to come
        assertEquals(sq("Jail"), state.getPosition(1));
        assertFalse(state.isInJail(1));
        assertEquals(1, state.getNDoubles());
        assertTrue(state.hasAnotherRoll());
    }

    @Test
    public void aRoundEndsWhenTheTurnPassesBackRoundTheTable() {
        int round = state.getRoundCounter();
        manage(2, false, 0);
        fm.next(state, new EndTurn());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(round + 1, state.getRoundCounter());

        manage(0, false, 0);
        fm.next(state, new EndTurn());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(round + 1, state.getRoundCounter());
    }

    @Test
    public void bankruptPlayersAreSkipped() {
        MonopolyGameState state4 = newState(4, 7, null);
        state = state4;
        eliminate(state, 1, 4);
        eliminate(state, 3, 3);
        int round = state.getRoundCounter();

        manage(0, false, 0);
        fm.next(state, new EndTurn());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(round, state.getRoundCounter());

        manage(2, false, 0);
        fm.next(state, new EndTurn());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(round + 1, state.getRoundCounter());
    }
}
