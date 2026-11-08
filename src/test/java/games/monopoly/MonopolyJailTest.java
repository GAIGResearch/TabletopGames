package games.monopoly;

import core.CoreConstants;
import games.monopoly.actions.EndTurn;
import games.monopoly.actions.PayJailFine;
import games.monopoly.actions.RollDice;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

/**
 * A player in Jail at the start of their turn: paying the fine, rolling for doubles, the forced fine.
 */
public class MonopolyJailTest {

    MonopolyParameters params;
    MonopolyGameState state;
    MonopolyForwardModel fm;

    @Before
    public void setup() {
        params = new MonopolyParameters();
        state = newState(3, 7, params);
        fm = new MonopolyForwardModel();
        startTurn(state, 0, sq("Jail"));
        putInJail(state, 0, 0);
    }

    @Test
    public void inJailThePlayerMayPayTheFineOrRoll() {
        assertEquals(Set.of(new PayJailFine(), new RollDice()), actionSet(fm, state));
    }

    @Test
    public void thePlayerCanPayTheFineOnlyWithTheCash() {
        state.setCash(0, 50 - 1);
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
        state.setCash(0, 50);
        assertEquals(Set.of(new PayJailFine(), new RollDice()), actionSet(fm, state));
    }

    @Test
    public void theFineComesFromTheParameter() {
        state.setCash(0, 60);
        assertEquals(Set.of(new PayJailFine(), new RollDice()), actionSet(fm, state));
        params.setParameterValue("jailFine", 100);
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
        state.setCash(0, 100);
        fm.next(state, new PayJailFine());
        assertEquals(0, state.getCash(0));
    }

    @Test
    public void payingTheFineFreesThePlayerWhoThenRollsAsNormal() {
        fm.next(state, new PayJailFine());
        assertEquals(1500 - 50, state.getCash(0));
        assertFalse(state.isInJail(0));
        assertEquals(sq("Jail"), state.getPosition(0));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));

        roll(state, fm, 4, 6);
        // 10 + 10 = Free Parking
        assertEquals(sq("Free Parking"), state.getPosition(0));
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void afterPayingTheFineADoubleGivesAnotherRoll() {
        fm.next(state, new PayJailFine());
        roll(state, fm, 5, 5);
        assertEquals(sq("Free Parking"), state.getPosition(0));
        assertTrue(state.hasAnotherRoll());
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
    }

    @Test
    public void aDoubleInJailFreesThePlayerWhoMovesByItWithNoFurtherRoll() {
        roll(state, fm, 5, 5);
        assertFalse(state.isInJail(0));
        assertEquals(sq("Free Parking"), state.getPosition(0));
        assertEquals(1500, state.getCash(0));
        assertFalse(state.hasAnotherRoll());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void aDoubleOnTheLastAllowedRollCostsNoFine() {
        state.setJailRolls(0, 2);
        roll(state, fm, 3, 3);
        // 10 + 6 = Bow Street
        assertFalse(state.isInJail(0));
        assertEquals(sq("Bow Street"), state.getPosition(0));
        assertEquals(1500, state.getCash(0));
    }

    @Test
    public void aFailedRollKeepsThePlayerInJailAndEndsTheTurn() {
        roll(state, fm, 1, 3);
        assertTrue(state.isInJail(0));
        assertEquals(sq("Jail"), state.getPosition(0));
        assertEquals(1, state.getJailRolls(0));
        assertEquals(1500, state.getCash(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
    }

    @Test
    public void aSecondFailedRollStillKeepsThePlayerInJail() {
        state.setJailRolls(0, 1);
        roll(state, fm, 1, 3);
        assertTrue(state.isInJail(0));
        assertEquals(2, state.getJailRolls(0));
        assertEquals(1500, state.getCash(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void theThirdFailedRollPaysTheFineAndMovesByTheRoll() {
        state.setJailRolls(0, 2);
        roll(state, fm, 4, 6);
        assertFalse(state.isInJail(0));
        assertEquals(sq("Free Parking"), state.getPosition(0));
        assertEquals(1500 - 50, state.getCash(0));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void withMaxJailRollsOneTheFirstFailedRollPaysTheFine() {
        params.setParameterValue("maxJailRolls", 1);
        roll(state, fm, 4, 6);
        assertFalse(state.isInJail(0));
        assertEquals(sq("Free Parking"), state.getPosition(0));
        assertEquals(1500 - 50, state.getCash(0));
    }

    @Test
    public void aForcedFineWithoutTheCashMakesThePlayerBankruptToTheBank() {
        state.setCash(0, 30);
        state.setJailRolls(0, 2);
        roll(state, fm, 1, 3);
        assertTrue(state.isBankrupt(0));
        assertEquals(3, state.getFinalPlace(0)); // the first of 3 players out
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(0, state.getCash(0));
        assertEquals(1500, state.getCash(1));
        assertEquals(1500, state.getCash(2));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
        assertEquals(CoreConstants.GameResult.GAME_ONGOING, state.getGameStatus());
    }
}
