package games.toads;

import games.toads.abilities.BattleResult;
import games.toads.components.ToadCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static games.toads.ToadConstants.ToadCardType.*;
import static games.toads.ToadTestUtils.*;
import static games.toads.abilities.AbilityConstants.*;
import static org.junit.Assert.*;

/**
 * The Rulebook 3 battle engine: Tactics processed in groups of equal priority with a
 * snapshot of the lane values, deferred Blocks, fractional values, tie-break flags, and lane resolution
 * (Siege Cannon, Assassin v General, Strength, tie-break).
 * Unit tests build a BattleResult directly with player 0 as the Attacker, so calculate() returns
 * {attacker lanes won, defender lanes won}. The integration tests drive a battle through fm.next.
 */
public class BattleEngineTest {

    private static final double EPS = 1e-9;

    ToadParameters params;
    ToadGameState state;
    ToadForwardModel fm;

    @Before
    public void setUp() {
        params = tacticsParams(933);
        fm = new ToadForwardModel();
        state = newState(params, fm);
    }

    private void tacticsOff() {
        params.setParameterValue("useTactics", false);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Group processing and the snapshot
    // ---------------------------------------------------------------------------------------------------------

    /** +2.5 to whichever of (Ally, Foe) is lower, reading the snapshot (the Rulebook 3 Assassin's rule). */
    private static ToadCard boostLowerOfAllyAndFoe(String name, int value) {
        return withTactics(name, value, null, tactic(DURING, (isAttacker, isFlank, br) -> {
            double ally = br.getSnapshotValue(isAttacker, !isFlank);
            double foe = br.getSnapshotValue(!isAttacker, !isFlank);
            if (ally < foe) br.addValue(isAttacker, !isFlank, 2.5);
            else if (foe < ally) br.addValue(!isAttacker, !isFlank, 2.5);
        }));
    }

    @Test
    public void twoTacticsInOneGroupBothReadThePreGroupSnapshot() {
        // Field: attacker 3 v defender 5. Both hidden cards (DURING) boost the lower of the Field pair.
        // Attacker's: Ally 3 < Foe 5 -> +2.5 to the attacker Field. Defender's: Foe 3 < Ally 5 (snapshot) -> +2.5 to
        // the attacker Field. Field = 3 + 2.5 + 2.5 = 8 v 5 -> attacker wins.
        // (Reading current values instead: 3 -> 5.5, then the defender sees Ally 5 < Foe 5.5 and boosts its own
        // Field to 7.5, which wins.)
        // Flank: 4 v 6 -> defender wins.
        BattleResult br = battle(state, plain(3), plain(5),
                boostLowerOfAllyAndFoe("A", 4), boostLowerOfAllyAndFoe("D", 6));
        int[] result = br.calculate();
        assertEquals(8.0, br.getCurrentValue(true, false), EPS);
        assertEquals(5.0, br.getCurrentValue(false, false), EPS);
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void snapshotIsRetakenBeforeEachGroup() {
        // Attacker hidden card: START +1 to its Ally (Field 4 -> 5); DURING records the Ally's snapshot value.
        // The DURING group starts after the START group has committed, so its snapshot is 5, not the printed 4.
        double[] seen = new double[1];
        ToadCard aFlank = withTactics("A", 2, null,
                tactic(START, addToAlly(1)),
                tactic(DURING, (isAttacker, isFlank, br) -> seen[0] = br.getSnapshotValue(isAttacker, !isFlank)));
        BattleResult br = battle(state, plain(4), plain(3), aFlank, plain(6));
        int[] result = br.calculate();
        assertEquals(5.0, seen[0], EPS);
        // Field 5 v 3 attacker; Flank 2 v 6 defender
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void tacticsInAGroupRunAttackerFirstThenFlankBeforeField() {
        // BLOCK: each hidden card activates its own Field card, queuing that Field card's DURING tactic.
        // The DURING group then holds all four cards' tactics (queued Flanks first, Fields later).
        // Order: attacker before defender, and within a side the hidden (Flank) card before the Field.
        List<String> order = new ArrayList<>();
        ToadCard aField = withTactics("AField", 3, null, tactic(DURING, (a, f, br) -> order.add("AField")));
        ToadCard dField = withTactics("DField", 3, null, tactic(DURING, (a, f, br) -> order.add("DField")));
        ToadCard aFlank = withTactics("AFlank", 3, null,
                tactic(BLOCK, (a, f, br) -> br.setActivation(a, !f, true)),
                tactic(DURING, (a, f, br) -> order.add("AFlank")));
        ToadCard dFlank = withTactics("DFlank", 3, null,
                tactic(BLOCK, (a, f, br) -> br.setActivation(a, !f, true)),
                tactic(DURING, (a, f, br) -> order.add("DFlank")));
        battle(state, aField, dField, aFlank, dFlank).calculate();
        assertEquals(List.of("AFlank", "AField", "DFlank", "DField"), order);
    }

    @Test
    public void tacticQueuedByALaterGroupAtAnEarlierPriorityStillRuns() {
        // The attacker's hidden card activates its Field card during DURING; the Field card's only tactic is at
        // START (already passed). It must still run: Field 4 + 3 = 7 v 6 -> attacker wins. Flank 2 v 5 defender.
        ToadCard aField = withTactics("AField", 4, null, tactic(START, addToSelf(3)));
        ToadCard aFlank = withTactics("AFlank", 2, null,
                tactic(DURING, (a, f, br) -> br.setActivation(a, !f, true)));
        BattleResult br = battle(state, aField, plain(6), aFlank, plain(5));
        int[] result = br.calculate();
        assertEquals(7.0, br.getCurrentValue(true, false), EPS);
        assertArrayEquals(new int[]{1, 1}, result);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Blocks
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void blockStopsTheOpposingHiddenCardsLaterTactics() {
        // Attacker's hidden card BLOCKs the defender's hidden card, whose START/DURING/AFTER tactics would give
        // itself +3, its Ally +3 and itself +3.
        // Blocked: Field 4 v 4 tie; Flank 5 v 3 attacker. (Unblocked: Field 4 v 7, Flank 5 v 9 - defender both.)
        ToadCard dFlank = withTactics("D", 3, null,
                tactic(START, addToSelf(3)), tactic(DURING, addToAlly(3)), tactic(AFTER, addToSelf(3)));
        ToadCard aFlank = withTactics("A", 5, null, tactic(BLOCK, blockOpposingFlank()));
        BattleResult br = battle(state, plain(4), plain(4), aFlank, dFlank);
        int[] result = br.calculate();
        assertEquals(3.0, br.getCurrentValue(false, true), EPS);
        assertEquals(4.0, br.getCurrentValue(false, false), EPS);
        assertFalse(br.isActivated(false, true));
        assertArrayEquals(new int[]{1, 0}, result);
    }

    @Test
    public void twoBlocksInTheSameGroupBothHappen() {
        // Each hidden card BLOCKs the other and has a START +3 to itself; both BLOCK tactics run (count 2) and
        // both cards end deactivated, so neither +3 happens: Flank 4 v 5 defender; Field 5 v 5 tie.
        // (If the attacker's block took effect at once, the defender's block would never run and the attacker's
        // +3 would make its Flank 7 v 5.)
        int[] blocksRun = new int[1];
        ToadCard aFlank = withTactics("A", 4, null,
                tactic(BLOCK, (a, f, br) -> { blocksRun[0]++; br.block(!a, true); }),
                tactic(START, addToSelf(3)));
        ToadCard dFlank = withTactics("D", 5, null,
                tactic(BLOCK, (a, f, br) -> { blocksRun[0]++; br.block(!a, true); }),
                tactic(START, addToSelf(3)));
        BattleResult br = battle(state, plain(5), plain(5), aFlank, dFlank);
        int[] result = br.calculate();
        assertEquals(2, blocksRun[0]);
        assertEquals(4.0, br.getCurrentValue(true, true), EPS);
        assertEquals(5.0, br.getCurrentValue(false, true), EPS);
        assertFalse(br.isActivated(true, true));
        assertFalse(br.isActivated(false, true));
        assertArrayEquals(new int[]{0, 1}, result);
    }

    @Test
    public void blockHasNoEffectOnAnUnblockableCard() {
        // The defender's hidden card cannot be blocked; its START +3 still runs: Flank 5 v 3 + 3 = 6 defender.
        // Field 4 v 4 tie.
        ToadCard dFlank = unblockable("D", 3, null, tactic(START, addToSelf(3)));
        ToadCard aFlank = withTactics("A", 5, null, tactic(BLOCK, blockOpposingFlank()));
        BattleResult br = battle(state, plain(4), plain(4), aFlank, dFlank);
        int[] result = br.calculate();
        assertEquals(6.0, br.getCurrentValue(false, true), EPS);
        assertTrue(br.isActivated(false, true));
        assertArrayEquals(new int[]{0, 1}, result);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Fractional values
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void fractionalBonusTurnsALossIntoAWin() {
        // Attacker's hidden card gives its Ally +2.5: Field 4 + 2.5 = 6.5 v 6 -> attacker. Flank 2 v 5 defender.
        ToadCard aFlank = withTactics("A", 2, null, tactic(DURING, addToAlly(2.5)));
        BattleResult br = battle(state, plain(4), plain(6), aFlank, plain(5));
        int[] result = br.calculate();
        assertEquals(6.5, br.getCurrentValue(true, false), EPS);
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void equalFractionalValuesTie() {
        // Both hidden cards give their Ally +2.5: Field 1 + 2.5 = 3.5 v 1 + 2.5 = 3.5 -> tie. Flank 2 v 5 defender.
        ToadCard aFlank = withTactics("A", 2, null, tactic(DURING, addToAlly(2.5)));
        ToadCard dFlank = withTactics("D", 5, null, tactic(DURING, addToAlly(2.5)));
        BattleResult br = battle(state, plain(1), plain(1), aFlank, dFlank);
        int[] result = br.calculate();
        assertEquals(3.5, br.getCurrentValue(true, false), EPS);
        assertEquals(3.5, br.getCurrentValue(false, false), EPS);
        assertArrayEquals(new int[]{0, 1}, result);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Tie-break flags
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void tieBreakFlagWinsATiedLane() {
        // Attacker's hidden card flags its Ally: Field 5 v 5 tied -> attacker (flag). Flank 3 v 4 defender.
        ToadCard aFlank = withTactics("A", 3, null, tactic(DURING, tieBreakAlly()));
        BattleResult br = battle(state, plain(5), plain(5), aFlank, plain(4));
        int[] result = br.calculate();
        assertTrue(br.hasTieBreak(true, false));
        assertFalse(br.hasTieBreak(false, false));
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void tieBreakFlagOnBothSidesLeavesTheLaneTied() {
        // Both hidden cards flag their Ally: Field 5 v 5, both flagged -> tie. Flank 3 v 4 defender.
        ToadCard aFlank = withTactics("A", 3, null, tactic(DURING, tieBreakAlly()));
        ToadCard dFlank = withTactics("D", 4, null, tactic(DURING, tieBreakAlly()));
        BattleResult br = battle(state, plain(5), plain(5), aFlank, dFlank);
        int[] result = br.calculate();
        assertTrue(br.hasTieBreak(true, false));
        assertTrue(br.hasTieBreak(false, false));
        assertArrayEquals(new int[]{0, 1}, result);
    }

    @Test
    public void tieBreakFlagDoesNotAffectAnUntiedLane() {
        // Attacker's Field is flagged but 4 v 5 is not a tie -> defender. Flank 6 v 2 attacker.
        ToadCard aFlank = withTactics("A", 6, null, tactic(DURING, tieBreakAlly()));
        BattleResult br = battle(state, plain(4), plain(5), aFlank, plain(2));
        int[] result = br.calculate();
        assertTrue(br.hasTieBreak(true, false));
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void swapFieldAndFlankMovesTheTieBreakFlagWithTheCard() {
        // Attacker hidden card (3): BLOCK flags its Ally (the Field 5); START swaps the attacker's two cards.
        // After the swap: Field = the 3 v defender 4 -> defender; Flank = the flagged 5 v 5 -> attacker (flag).
        // (Flag left behind on the Field lane: Field 3 v 4 defender, Flank 5 v 5 tie -> {0, 1}.)
        ToadCard aFlank = withTactics("A", 3, null,
                tactic(BLOCK, tieBreakAlly()),
                tactic(START, (a, f, br) -> br.swapFieldAndFlank(a ? 0 : 1)));
        BattleResult br = battle(state, plain(5), plain(4), aFlank, plain(5));
        int[] result = br.calculate();
        assertTrue(br.hasTieBreak(true, true));
        assertFalse(br.hasTieBreak(true, false));
        assertArrayEquals(new int[]{1, 1}, result);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Lane resolution: Siege Cannon
    // ---------------------------------------------------------------------------------------------------------

    private static ToadCard cannon() {
        return plain("Siege Cannon", 0, SIEGE_CANNON);
    }

    @Test
    public void attackingSiegeCannonBeatsASevenEvenWithBonusesOnTheSeven() {
        // Field: attacking Cannon v Seven 7 + 3 (defender's hidden card) = 10 -> Cannon wins regardless.
        // Flank: 2 v 4 defender.
        ToadCard dFlank = withTactics("D", 4, null, tactic(DURING, addToAlly(3)));
        BattleResult br = battle(state, cannon(), plain("Seven", 7, null), plain(2), dFlank);
        int[] result = br.calculate();
        assertEquals(10.0, br.getCurrentValue(false, false), EPS); // the bonus was applied, and ignored
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void attackingSiegeCannonLosesToASaboteurEvenWithBonusesOnTheCannon() {
        // Field: attacking Cannon 0 + 10 (attacker's hidden card) v Saboteur 3 -> Saboteur wins regardless.
        // Flank: 3 v 5 defender.
        ToadCard aFlank = withTactics("A", 3, null, tactic(DURING, addToAlly(10)));
        BattleResult br = battle(state, cannon(), plain("Saboteur", 3, SABOTEUR), aFlank, plain(5));
        int[] result = br.calculate();
        assertEquals(10.0, br.getCurrentValue(true, false), EPS);
        assertArrayEquals(new int[]{0, 2}, result);
    }

    @Test
    public void defendingSiegeCannonLosesEvenToAnAssassinWithBonusesOnTheCannon() {
        // Field: Assassin 1 v defending Cannon 0 + 10 (defender's hidden card) -> Assassin (Cannon loses in Defence).
        // Flank: 4 v 3 attacker.
        ToadCard dFlank = withTactics("D", 3, null, tactic(DURING, addToAlly(10)));
        BattleResult br = battle(state, plain("Assassin", 1, ASSASSIN), cannon(), plain(4), dFlank);
        int[] result = br.calculate();
        assertArrayEquals(new int[]{2, 0}, result);
    }

    @Test
    public void siegeCannonVersusSiegeCannonTheAttackerWins() {
        // Field: Cannon v Cannon -> attacker. Flank: 2 v 5 defender.
        tacticsOff();
        int[] result = battle(state, cannon(), cannon(), plain(2), plain(5)).calculate();
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void siegeCannonRuleAppliesInTheFlankAndWithoutTactics() {
        // useTactics off. Flank: attacking Cannon v 7 -> Cannon. Field: 6 v 2 attacker.
        tacticsOff();
        int[] result = battle(state, plain(6), plain(2), cannon(), plain("Seven", 7, null)).calculate();
        assertArrayEquals(new int[]{2, 0}, result);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Lane resolution: Assassin v General
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void assassinBeatsAGeneralWhateverTheBonuses() {
        // Field: Assassin 1 (no CardModifiers) v General 7 + 4 (defender's hidden card) = 11 -> Assassin.
        // Flank: 5 v 3 attacker.
        ToadCard dFlank = withTactics("D", 3, null, tactic(DURING, addToAlly(4)));
        BattleResult br = battle(state, plain("Assassin", 1, ASSASSIN), plain("General", 7, GENERAL_ONE), plain(5), dFlank);
        int[] result = br.calculate();
        assertEquals(11.0, br.getCurrentValue(false, false), EPS);
        assertArrayEquals(new int[]{2, 0}, result);
    }

    @Test
    public void defendingAssassinBeatsAnUntypedSevenWithoutTactics() {
        // useTactics off, and any value-7 card counts as a General.
        // Field: untyped Seven v defending Assassin 1 -> Assassin. Flank: 2 v 4 defender.
        tacticsOff();
        int[] result = battle(state, plain("Seven", 7, null), plain("Assassin", 1, ASSASSIN), plain(2), plain(4)).calculate();
        assertArrayEquals(new int[]{0, 2}, result);
    }

    @Test
    public void assassinRuleNeedsBothAnAssassinAndASeven() {
        // Field: Assassin 1 v 6 -> 6 wins on Strength. Flank: a non-Assassin 1 (Scout type) v Seven -> Seven wins.
        tacticsOff();
        int[] result = battle(state, plain("Assassin", 1, ASSASSIN), plain(6), plain("Scout", 1, SCOUT),
                plain("Seven", 7, null)).calculate();
        assertArrayEquals(new int[]{0, 2}, result);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Integration: a real battle through fm.next
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void siegeCannonAttackThroughTheForwardModel() {
        assertEquals("player 0 attacks first", 0, state.getCurrentPlayer());
        // P0 (Attacker): Field Siege Cannon, Flank 3. P1: Field Seven, Flank 5.
        // Field: attacking Cannon beats the 7 -> P0. Flank: 3 v 5 -> P1. One lane each, no tie.
        // (Without the Cannon rule: 0 v 7 and 3 v 5 -> P1 both, reduced to 1 by the overcommit rule -> {0, 1}.)
        playCards(state, fm, cannon(), plain(3), plain("Seven", 7, null), plain(5));
        assertEquals(1, state.nextBattle);
        assertArrayEquals(new int[]{1, 1}, state.roundWinners[0]);
        assertEquals(1, state.getBattlesWon(0, 0));
        assertEquals(1, state.getBattlesWon(0, 1));
        assertEquals(0, state.getBattlesTied(0));
    }

    @Test
    public void tieBreakFlagThroughTheForwardModel() {
        assertEquals("player 0 attacks first", 0, state.getCurrentPlayer());
        // P0 (Attacker): Field 5, Flank 4. P1: Field 5, Flank 3 whose DURING tactic flags its Ally (the Field 5).
        // Field: 5 v 5 tied -> P1 (flag). Flank: 4 v 3 -> P0. One lane each, no tie.
        // (Without the flag: Field tied, Flank P0 -> {1, 0} and one tie.)
        ToadCard p1Flank = withTactics("Flagger", 3, null, tactic(DURING, tieBreakAlly()));
        playCards(state, fm, plain(5), plain(4), plain(5), p1Flank);
        assertEquals(1, state.nextBattle);
        assertArrayEquals(new int[]{1, 1}, state.roundWinners[0]);
        assertEquals(0, state.getBattlesTied(0));
    }
}
