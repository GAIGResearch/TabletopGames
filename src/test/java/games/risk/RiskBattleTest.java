package games.risk;

import core.actions.AbstractAction;
import games.risk.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.risk.WorldMap.*;
import static games.risk.RiskTestUtils.*;
import static org.junit.Assert.*;

/**
 * Dice resolution (RiskUtils.battleLosses) and the Attack action: losses, capture and the move in.
 * Base arrangement: player 0 holds only Indonesia (6 armies), player 1 everything else but Argentina (player 2's).
 */
public class RiskBattleTest {

    RiskForwardModel fm = new RiskForwardModel();
    RiskGameState state;

    @Before
    public void setup() {
        state = newState(3, 7, null);
        fillBoard(state, 1);
        give(state, 2, 1, ARGENTINA);
        give(state, 0, 6, INDONESIA);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
    }

    private static void assertLosses(int attackerLoses, int defenderLoses, int[] attack, int[] defend) {
        assertArrayEquals(new int[]{attackerLoses, defenderLoses}, RiskUtils.battleLosses(attack, defend));
    }

    @Test
    public void pdfBattleExamples() {
        // 1: 3 vs 1, the attacker's highest (5) beats 4
        assertLosses(0, 1, new int[]{2, 5, 1}, new int[]{4});
        // 2: 3 vs 2: 6 beats 5, 3 loses to 4
        assertLosses(1, 1, new int[]{6, 3, 2}, new int[]{5, 4});
        // 3: 2 vs 2: 5 ties 5 (defender wins), 3 loses to 4
        assertLosses(2, 0, new int[]{5, 3}, new int[]{5, 4});
        // 4: 1 vs 2: only the highest pair, 6 beats 5
        assertLosses(0, 1, new int[]{6}, new int[]{3, 5});
    }

    @Test
    public void defenderWinsTiesAndTheAttackerNeverLosesMoreThanTwo() {
        assertLosses(1, 0, new int[]{4}, new int[]{4});
        assertLosses(2, 0, new int[]{6, 6, 6}, new int[]{6, 6}); // two pairs only, though 3 dice
        assertLosses(0, 2, new int[]{6, 6, 1}, new int[]{5, 5});
        // 1 vs 2 where the defender's lower die would lose: still only one pair (3 vs 5)
        assertLosses(1, 0, new int[]{3}, new int[]{5, 1});
    }

    @Test
    public void diceAreSortedBeforePairing() {
        // sorted: 6 v 5 and 4 v 2, both to the attacker; in the order given 4 v 5 and 1 v 2 would both lose
        assertLosses(0, 2, new int[]{4, 1, 6}, new int[]{5, 2});
        // sorted: 6 v 5 and 2 v 1, both to the attacker; the defender's dice in the order given: 6 v 1, 2 v 5
        assertLosses(0, 2, new int[]{6, 2}, new int[]{1, 5});
    }

    @Test
    public void attackWithoutCaptureRemovesTheLossesAndStaysInAttack() {
        give(state, 1, 2, SIAM);
        // attacker 6 2 1, defender 5 3: 6 beats 5, 2 loses to 3
        state.setNextRolls(6, 2, 1, 5, 3);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertEquals(5, state.getArmies(INDONESIA));
        assertEquals(1, state.getArmies(SIAM));
        assertEquals(1, state.getOwner(SIAM));
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void defenderRollsOneDieWhenDefendingWithOneArmy() {
        // Siam has 1 army: defender rolls just the 6, so 1 v 6 and the attacker loses one.
        // (Rolling two dice, 6 and 2, would cost the attacker two.)
        state.setNextRolls(1, 1, 1, 6, 2);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertEquals(5, state.getArmies(INDONESIA));
        assertEquals(1, state.getArmies(SIAM));
    }

    @Test
    public void defenderRollsTwoDiceAgainstOneAttackingDie() {
        give(state, 1, 3, SIAM);
        // attacker 4; defender 3 and 5: 4 v 5, the attacker loses (with one defending die, 4 v 3 would win)
        state.setNextRolls(4, 3, 5);
        fm.next(state, new Attack(INDONESIA, SIAM, 1));
        assertEquals(5, state.getArmies(INDONESIA));
        assertEquals(3, state.getArmies(SIAM));
    }

    @Test
    public void captureOffersMovingFromTheDiceRolledToAllButOneArmy() {
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertEquals(0, state.getOwner(SIAM));
        assertEquals(0, state.getArmies(SIAM));
        assertEquals(6, state.getArmies(INDONESIA));
        assertTrue(state.currentActionInProgress() instanceof MoveArmiesChoice);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(new MoveArmies(INDONESIA, SIAM, 3), new MoveArmies(INDONESIA, SIAM, 4),
                new MoveArmies(INDONESIA, SIAM, 5)), actionSet(fm, state)); // 3 dice .. 6 - 1
    }

    @Test
    public void captureWithTwoDiceRequiresAtLeastTwoArmiesToMove() {
        give(state, 0, 5, INDONESIA);
        state.setNextRolls(6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 2));
        assertEquals(Set.of(new MoveArmies(INDONESIA, SIAM, 2), new MoveArmies(INDONESIA, SIAM, 3),
                new MoveArmies(INDONESIA, SIAM, 4)), actionSet(fm, state)); // 2 dice .. 5 - 1
    }

    @Test
    public void afterMovingInThePlayerAttacksAgainFromBothTerritories() {
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 4));
        assertEquals(2, state.getArmies(INDONESIA));
        assertEquals(4, state.getArmies(SIAM));
        assertFalse(state.isActionInProgress());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        Set<AbstractAction> expected = Set.of(
                new Attack(INDONESIA, NEW_GUINEA, 1), new Attack(INDONESIA, WESTERN_AUSTRALIA, 1), // 2 armies: 1 die
                new Attack(SIAM, CHINA, 1), new Attack(SIAM, CHINA, 2), new Attack(SIAM, CHINA, 3),
                new Attack(SIAM, INDIA, 1), new Attack(SIAM, INDIA, 2), new Attack(SIAM, INDIA, 3),
                new Blitz(INDONESIA, NEW_GUINEA), new Blitz(INDONESIA, WESTERN_AUSTRALIA),     // one Blitz per pair
                new Blitz(SIAM, CHINA), new Blitz(SIAM, INDIA),
                new EndAttack());
        assertEquals(expected, actionSet(fm, state));
    }
}
