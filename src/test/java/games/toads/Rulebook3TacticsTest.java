package games.toads;

import games.toads.ToadConstants.ToadCardType;
import games.toads.abilities.*;
import games.toads.components.ToadCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static games.toads.ToadConstants.ToadCardType.*;
import static games.toads.ToadConstants.ToadGamePhase.PLAY;
import static games.toads.ToadTestUtils.*;
import static org.junit.Assert.*;

/**
 * The Rulebook 3 Tactics and the Rulebook 3 deck.
 * Only the hidden (Flank) cards' Tactics activate. The Rulebook 3 cards come from ToadTestUtils with their abilities
 * given explicitly, so these tests do not depend on ToadCardType.defaultAbility (tested separately below).
 * Unit tests build a BattleResult with player 0 as the Attacker, so calculate() returns
 * {attacker lanes won, defender lanes won}; argument order is aField, dField, aFlank, dFlank.
 * Integration tests play through fm.next (playCards: player 0 attacks the first Battle).
 */
public class Rulebook3TacticsTest {

    private static final double EPS = 1e-9;

    ToadParameters params;
    ToadForwardModel fm;
    ToadGameState state;

    @Before
    public void setUp() {
        params = tacticsParams(933);
        fm = new ToadForwardModel();
        state = newState(params, fm);
    }

    private void assertLane(BattleResult br, boolean isFlank, double attackerValue, double defenderValue) {
        String lane = isFlank ? "Flank" : "Field";
        assertEquals("attacker " + lane, attackerValue, br.getCurrentValue(true, isFlank), EPS);
        assertEquals("defender " + lane, defenderValue, br.getCurrentValue(false, isFlank), EPS);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Assassin: +2.5 to the lower of Ally and Foe (snapshot at the start of DURING)
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void assassinGivesItsLowerAllyTwoAndAHalfAndTheAllyWins() {
        // Ally 4 v Foe 6: Ally is lower -> 4 + 2.5 = 6.5 v 6, attacker wins the Field.
        // Flank: Assassin 1 v 3 -> defender.
        BattleResult br = battle(state, plain(4), plain(6), assassinII(), plain(3));
        int[] result = br.calculate();
        assertLane(br, false, 6.5, 6);
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void assassinGivesTheLowerFoeTwoAndAHalfAndTheFoeWins() {
        // Ally 6 v Foe 4: the Foe (opponent's card) is lower -> 4 + 2.5 = 6.5 v 6, defender wins the Field.
        // Flank: Assassin 1 v 3 -> defender.
        BattleResult br = battle(state, plain(6), plain(4), assassinII(), plain(3));
        int[] result = br.calculate();
        assertLane(br, false, 6, 6.5);
        assertArrayEquals(new int[]{0, 2}, result);
    }

    @Test
    public void assassinDoesNothingWhenAllyAndFoeAreEqual() {
        // Ally 5 v Foe 5: equal -> no bonus, the Field ties. Flank: Assassin 1 v 3 -> defender.
        BattleResult br = battle(state, plain(5), plain(5), assassinII(), plain(3));
        int[] result = br.calculate();
        assertLane(br, false, 5, 5);
        assertArrayEquals(new int[]{0, 1}, result);
    }

    @Test
    public void twoOpposingAssassinsBothBoostTheSameLowerCard() {
        // Field 3 v 5. Attacker's Assassin: Ally 3 < Foe 5 -> +2.5 to the attacker Field.
        // Defender's Assassin: Foe 3 < Ally 5 -> +2.5 to the attacker Field. 3 + 2.5 + 2.5 = 8 v 5, attacker wins.
        // Flank: Assassin 1 v Assassin 1 -> tie.
        BattleResult br = battle(state, plain(3), plain(5), assassinII(), assassinII());
        int[] result = br.calculate();
        assertLane(br, false, 8, 5);
        assertArrayEquals(new int[]{1, 0}, result);
    }

    @Test
    public void assassinReadsStrengthsAfterTheStartStageBonuses() {
        // Field 5 v 4. The defender's Scout (START) gives its Ally +1 first: 5 v 5. The Assassin (DURING) then sees
        // Ally 5 = Foe 5 and does nothing: Field ties 5 v 5. (Reading printed values, or running before the Scout,
        // it would boost the Foe 4 -> 6.5, and 6.5 + 1 = 7.5 wins for the defender.)
        // Flank: Assassin 1 v Scout 2 -> defender.
        BattleResult br = battle(state, plain(5), plain(4), assassinII(), scout());
        int[] result = br.calculate();
        assertLane(br, false, 5, 5);
        assertArrayEquals(new int[]{0, 1}, result);
    }

    @Test
    public void assassinDoesNothingWhenItsAllyIsASiegeCannon() {
        // Ally Siege Cannon 0 v Foe 4: 0 < 4, but a Siege Cannon lane ignores Strength, so no bonus (Cannon stays 0).
        // The attacking Cannon wins the Field (not facing a Saboteur); Flank: Assassin 1 v 3 -> defender.
        BattleResult br = battle(state, siegeCannon(), plain(4), assassinII(), plain(3));
        int[] result = br.calculate();
        assertLane(br, false, 0, 4);
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void assassinDoesNothingWhenTheFoeIsASiegeCannon() {
        // Ally 4 v Foe Siege Cannon 0 (defending): no bonus to the Cannon (stays 0). A defending Cannon loses: the
        // attacker wins the Field. Flank: Assassin 1 v 3 -> defender.
        BattleResult br = battle(state, plain(4), siegeCannon(), assassinII(), plain(3));
        int[] result = br.calculate();
        assertLane(br, false, 4, 0);
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void assassinHasNoStrengthModifierAgainstAGeneral() {
        // The Assassin beats a 7 by the lane rule, not by a +20 modifier: its Strength stays 1.
        // Field 5 v 5 (no Assassin bonus when equal) -> tie. Flank: Assassin v General -> attacker.
        BattleResult br = battle(state, plain(5), plain(5), assassinII(), plain("General", 7, GENERAL_ONE));
        int[] result = br.calculate();
        assertLane(br, true, 1, 7);
        assertArrayEquals(new int[]{1, 0}, result);
    }

    @Test
    public void assassinIgnoresAnOpposingSaboteurTieBreakAndTheSaboteurSideWins() {
        // Field 5 v 5. The attacker's Assassin sees Ally 5 = Foe 5 and does nothing (the defender's tie-break flag
        // does not make the Foe "higher"). The defender's Saboteur gives its Ally (defender Field) the tie-break:
        // 5 v 5 tie -> defender. Flank: Assassin 1 v Saboteur 3 -> defender.
        BattleResult br = battle(state, plain(5), plain(5), assassinII(), saboteurIII());
        int[] result = br.calculate();
        assertLane(br, false, 5, 5);
        assertArrayEquals(new int[]{0, 2}, result);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Saboteur: tie-break flag on its Ally's lane only
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void saboteurBreaksATieInItsAllysLane() {
        // Field 5 v 5, attacker's Ally has the tie-break -> attacker. Flank: Saboteur 3 v 4 -> defender.
        BattleResult br = battle(state, plain(5), plain(5), saboteurIII(), plain(4));
        int[] result = br.calculate();
        assertTrue(br.hasTieBreak(true, false));
        assertLane(br, false, 5, 5); // a flag, not +1 Strength
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void saboteurDoesNotBreakATieInItsOwnLane() {
        // Field 6 v 4 -> attacker. Flank: Saboteur 3 v 3 stays tied (the Saboteur's own lane).
        // (Legacy SaboteurII would give its own lane +1 and win both.)
        BattleResult br = battle(state, plain(6), plain(4), saboteurIII(), plain(3));
        int[] result = br.calculate();
        assertFalse(br.hasTieBreak(true, true));
        assertArrayEquals(new int[]{1, 0}, result);
    }

    @Test
    public void twoOpposingSaboteursLeaveATiedLaneTied() {
        // Field 5 v 5: both Allies have the tie-break -> stays tied. Flank: Saboteur 3 v 3 -> tied (no flags there).
        BattleResult br = battle(state, plain(5), plain(5), saboteurIII(), saboteurIII());
        int[] result = br.calculate();
        assertTrue(br.hasTieBreak(true, false));
        assertTrue(br.hasTieBreak(false, false));
        assertArrayEquals(new int[]{0, 0}, result);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Trickster: swap with the Ally, no bonus, cannot be blocked
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void tricksterSwapsLanesWithItsAllyWithNoBonus() {
        // Attacker plays Field 5, Flank Trickster 4; defender Field 4, Flank 5.
        // After the swap: Field Trickster 4 v 4 -> tie; Flank 5 v 5 -> tie. (Legacy: Trickster +5/2 = +2 -> 6 wins
        // the Field; without the swap: Field 5 v 4 attacker, Flank 4 v 5 defender.)
        ToadCard trickster = tricksterII();
        BattleResult br = battle(state, plain(5), plain(4), trickster, plain(5));
        int[] result = br.calculate();
        assertSame(trickster, br.getCard(true, false));
        assertLane(br, false, 4, 4);
        assertLane(br, true, 5, 5);
        assertArrayEquals(new int[]{0, 0}, result);
    }

    @Test
    public void tricksterMovingItsAllyDoesNotActivateTheAllysTactic() {
        // Attacker Field Scout 2, Flank Trickster 4. The swap puts the Scout in the Flank, but its Tactic does not
        // activate: no +1 to the Trickster (Field 4 v 4 tie) and no ShowCards reveal.
        // Flank: Scout 2 v 1 -> attacker.
        BattleResult br = battle(state, scout(), plain(4), tricksterII(), plain(1));
        int[] result = br.calculate();
        assertLane(br, false, 4, 4);
        assertTrue(br.getPostBattleActions().isEmpty());
        assertArrayEquals(new int[]{1, 0}, result);
    }

    @Test
    public void anOpposingBodyguardDoesNotStopTheTricksterSwap() {
        // Attacker Field 5, Flank Trickster 4; defender Field 4, Flank Bodyguard 6.
        // The Block has no effect on the Trickster: Field Trickster 4 v 4 -> tie; Flank 5 v Bodyguard 6 -> defender.
        // (If blocked: Field 5 v 4 attacker, Flank 4 v 6 defender -> {1, 1}.)
        ToadCard trickster = tricksterII();
        BattleResult br = battle(state, plain(5), plain(4), trickster, bodyguard());
        int[] result = br.calculate();
        assertSame(trickster, br.getCard(true, false));
        assertArrayEquals(new int[]{0, 1}, result);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Bodyguard: blocks the opponent's hidden card
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void bodyguardBlocksAScoutsBonusAndReveal() {
        // Attacker Field 5, Flank Bodyguard 6; defender Field 4, Flank Scout 2.
        // Scout blocked: no +1 (Field 5 v 4 -> attacker) and no ShowCards. Flank 6 v 2 -> attacker.
        // (Unblocked: Field 5 v 5 tie and a ShowCards post-battle decision.)
        BattleResult br = battle(state, plain(5), plain(4), bodyguard(), scout());
        int[] result = br.calculate();
        assertLane(br, false, 5, 4);
        assertTrue(br.getPostBattleActions().isEmpty());
        assertArrayEquals(new int[]{2, 0}, result);
    }

    @Test
    public void bodyguardBlocksGeneralHostages() {
        // The defender (player 1) has 2 Hostages this War. Attacker Field 4, Flank General (Hostages) 7;
        // defender Field 5, Flank Bodyguard 6. Blocked: Field 4 v 5 -> defender (unblocked 4 + 2 = 6 would win).
        // Flank 7 v 6 -> attacker.
        state.battlesWon[0][1] = 2;
        BattleResult br = battle(state, plain(4), plain(5), generalHostages(), bodyguard());
        int[] result = br.calculate();
        assertLane(br, false, 4, 5);
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void bodyguardBlocksGeneralFlags() {
        // Each player has 2 Flags this War. Attacker Field 4, Flank General (Flags) 7; defender Field 5, Flank
        // Bodyguard 6. Blocked: Field 4 v 5 -> defender (unblocked 4 + 2 = 6 would win). Flank 7 v 6 -> attacker.
        state.shrineFlags[0][0] = 2;
        state.shrineFlags[0][1] = 2;
        BattleResult br = battle(state, plain(4), plain(5), generalFlags(), bodyguard());
        int[] result = br.calculate();
        assertLane(br, false, 4, 5);
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void bodyguardBlocksAnAssassin() {
        // Attacker Field 4, Flank Assassin 1; defender Field 6, Flank Bodyguard 6.
        // Blocked: Field 4 v 6 -> defender (unblocked 6.5 would win). Flank 1 v 6 -> defender.
        BattleResult br = battle(state, plain(4), plain(6), assassinII(), bodyguard());
        int[] result = br.calculate();
        assertLane(br, false, 4, 6);
        assertArrayEquals(new int[]{0, 2}, result);
    }

    @Test
    public void bodyguardBlocksASaboteur() {
        // Attacker Field 5, Flank Saboteur 3; defender Field 5, Flank Bodyguard 6.
        // Blocked: no tie-break, Field 5 v 5 tie (unblocked: attacker). Flank 3 v 6 -> defender.
        BattleResult br = battle(state, plain(5), plain(5), saboteurIII(), bodyguard());
        int[] result = br.calculate();
        assertFalse(br.hasTieBreak(true, false));
        assertArrayEquals(new int[]{0, 1}, result);
    }

    @Test
    public void bodyguardBlocksABerserker() {
        // Attacker (player 0) Flank Berserker; defender Flank Bodyguard. Blocked: player 0 is not Angry.
        BattleResult br = battle(state, plain(5), plain(4), berserkerII(), bodyguard());
        br.calculate();
        assertFalse(br.getFrogOverride(0));
    }

    @Test
    public void twoBodyguardsBlockEachOther() {
        // Both Blocks happen (they are simultaneous), so both hidden cards end up deactivated.
        BattleResult br = battle(state, plain(5), plain(4), bodyguard(), bodyguard());
        br.calculate();
        assertFalse("attacker Bodyguard blocked", br.isActivated(true, true));
        assertFalse("defender Bodyguard blocked", br.isActivated(false, true));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Berserker: the owner is Angry for this Battle, no Strength change
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void berserkerMakesItsOwnerAngryWithNoStrengthChange() {
        // Attacker (player 0) Flank Berserker 5 v 3: its Strength stays 5; frogOverride set for player 0 only.
        BattleResult br = battle(state, plain(4), plain(4), berserkerII(), plain(3));
        br.calculate();
        assertTrue(br.getFrogOverride(0));
        assertFalse(br.getFrogOverride(1));
        assertLane(br, true, 5, 3);
    }

    @Test
    public void calmDefenderWithABerserkerCapturesBothHostagesWithNoFlags() {
        // 0 v 0 Hostages: both Calm. Player 0 attacks: Field 2, Flank 2; player 1: Field 6, Flank Berserker 5.
        // Player 1 wins both lanes (6 v 2, 5 v 2). The Berserker makes player 1 Angry: 2 Hostages, no Shrine stack,
        // no Flags. (Without it: Calm, 1 Hostage and +1 Flag each.)
        playCards(state, fm, plain(2), plain(2), plain(6), berserkerII());
        assertEquals(0, state.battlesWon[0][0]);
        assertEquals(2, state.battlesWon[0][1]);
        assertEquals(0, state.getShrineFlags(0, 0));
        assertEquals(0, state.getShrineFlags(0, 1));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Generals
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void generalHostagesGivesItsAllyTheOpponentsHostagesThisWar() {
        // Player 0 (attacker) has the General. Opponent (player 1) Hostages this War: 2. Distractors: player 0's own
        // 3 Hostages and player 1's 5 in War 2 must not count.
        // Field 4 + 2 = 6 v 7 -> defender (own Hostages: 7 tie; War 2's: 9 wins; none: 4 loses the same way, so
        // the Strength is asserted). Flank General 7 v 6 -> attacker.
        state.battlesWon[0][0] = 3;
        state.battlesWon[0][1] = 2;
        state.battlesWon[1][1] = 5;
        BattleResult br = battle(state, plain(4), plain(7), generalHostages(), plain(6));
        int[] result = br.calculate();
        assertLane(br, false, 6, 7);
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void generalHostagesAsDefenderCountsTheAttackersHostages() {
        // Player 1 (defender) has the General; its opponent is player 0 with 1 Hostage (player 1 has 3).
        // Field 4 v 4 + 1 = 5 -> defender. Flank 6 v General 7 -> defender.
        state.battlesWon[0][0] = 1;
        state.battlesWon[0][1] = 3;
        BattleResult br = battle(state, plain(4), plain(4), plain(6), generalHostages());
        int[] result = br.calculate();
        assertLane(br, false, 4, 5);
        assertArrayEquals(new int[]{0, 2}, result);
    }

    @Test
    public void generalFlagsGivesItsAllyTheOwnersFlagsThisWar() {
        // Player 0 (attacker) has the General with 2 Flags this War. Distractors: player 1's 4 and player 0's 5 in
        // War 2 (in play the two counts are equal; they differ here only to pin down which is read).
        // Field 3 + 2 = 5 v 5 -> tie. Flank General 7 v 6 -> attacker.
        state.shrineFlags[0][0] = 2;
        state.shrineFlags[0][1] = 4;
        state.shrineFlags[1][0] = 5;
        BattleResult br = battle(state, plain(3), plain(5), generalFlags(), plain(6));
        int[] result = br.calculate();
        assertLane(br, false, 5, 5);
        assertArrayEquals(new int[]{1, 0}, result);
    }

    @Test
    public void generalFlagsAsDefenderCountsItsOwnersFlags() {
        // Player 1 (defender) has the General with 1 Flag (player 0 has 3). Field 5 v 4 + 1 = 5 -> tie.
        // Flank 6 v General 7 -> defender.
        state.shrineFlags[0][0] = 3;
        state.shrineFlags[0][1] = 1;
        BattleResult br = battle(state, plain(5), plain(4), plain(6), generalFlags());
        int[] result = br.calculate();
        assertLane(br, false, 5, 5);
        assertArrayEquals(new int[]{0, 1}, result);
    }

    @Test
    public void generalFlagsOfAnAttackingPlayerOneCountsPlayerOnesFlags() {
        // Player 1 attacks with the General; it has 1 Flag (player 0 has 3). Reading the Attacker's side as player 0
        // would give +3. Field 4 + 1 = 5 v 5 -> tie. Flank General 7 v 6 -> attacker (player 1).
        state.shrineFlags[0][0] = 3;
        state.shrineFlags[0][1] = 1;
        BattleResult br = new BattleResult(state, 1, plain(4), plain(5), generalFlags(), plain(6));
        int[] result = br.calculate();
        assertLane(br, false, 5, 5);
        assertArrayEquals(new int[]{0, 1}, result);
    }

    @Test
    public void generalHostagesInWarTwoCountsWarTwoHostages() {
        // In War 2 player 1 has 1 Hostage (4 in War 1). Field 4 + 1 = 5 v 6 -> defender (War 1's 4 would make 8 and
        // win). Flank General 7 v 6 -> attacker.
        fm.endRound(state, 0);
        state.battlesWon[0][1] = 4;
        state.battlesWon[1][1] = 1;
        BattleResult br = battle(state, plain(4), plain(6), generalHostages(), plain(6));
        int[] result = br.calculate();
        assertLane(br, false, 5, 6);
        assertArrayEquals(new int[]{1, 1}, result);
    }

    @Test
    public void generalFlagsCountsTiedLanesAndCalmDoubleWinsEarlierInTheWar() {
        // Battle 1 (player 0 attacks): Field 5 v 5 tied (+1 Flag each), Flank 3 v 4 -> player 1. Flags 1, Hostages 0:1.
        playCards(state, fm, plain(5), plain(3), plain(5), plain(4));
        assertEquals(1, state.getShrineFlags(0, 0));
        // Battle 2 (player 1 attacks): 6 v 2 and 6 v 2 -> player 1 wins both while ahead (1 >= 0): Calm, keeps 1
        // Hostage and sends a stack to the Shrine: Flags 2 each; Hostages 0:2.
        assertEquals(1, state.getCurrentPlayer());
        playCards(state, fm, plain(6), plain(6), plain(2), plain(2));
        assertEquals(2, state.getShrineFlags(0, 0));
        assertEquals(2, state.battlesWon[0][1]);
        // Battle 3 (player 0 attacks): Field 3 + 2 Flags = 5 v 4 -> player 0; Flank General 7 v 8 -> player 1.
        // (Without the bonus: Field 3 v 4 and player 1 would win both.)
        assertEquals(0, state.getCurrentPlayer());
        playCards(state, fm, plain(3), generalFlags(), plain(4), plain(8));
        assertEquals(1, state.battlesWon[0][0]);
        assertEquals(3, state.battlesWon[0][1]);
    }

    @Test
    public void generalFlagsGivesOneInTheFirstBattleOfWarTwoFromTheCasualty() {
        // War 1 played out with Tactics off; its Flags (at least the 3 arranged) must not count in War 2.
        params.setParameterValue("useTactics", false);
        state = newState(params, fm);
        state.shrineFlags[0][0] = 3;
        state.shrineFlags[0][1] = 3;
        int steps = 0;
        while (state.getRoundCounter() == 0 && state.isNotTerminal() && steps < 200) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
            steps++;
        }
        assertEquals("War 1 did not end within the step cap", 1, state.getRoundCounter());
        assertEquals(PLAY, state.getGamePhase());
        params.setParameterValue("useTactics", true);
        int attacker = state.getCurrentPlayer();
        // War 2, Battle 1: the attacker's General (Flags) has 1 Flag (the Casualty).
        // Field 3 + 1 = 4 v 4 -> tie (War 1's Flags would make it win, none would lose). Flank 7 v 8 -> defender.
        playCards(state, fm, plain(3), generalFlags(), plain(4), plain(8));
        assertEquals(1, state.getBattlesTied(1));
        assertEquals(0, state.battlesWon[1][attacker]);
        assertEquals(1, state.battlesWon[1][1 - attacker]);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Scout in the START stage, and a full Bodyguard battle through the forward model
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void bodyguardBlockedScoutGivesNoRevealDecisionAfterTheBattle() {
        // Player 0 attacks: Field 4, Flank Scout 2; player 1: Field 5, Flank Bodyguard 6.
        // Scout blocked: Field 4 v 5 -> player 1, Flank 2 v 6 -> player 1. A Calm double win: 1 Hostage, 1 Flag each.
        // No ShowCards decision: play continues with player 1 attacking.
        // (Unblocked: Field 5 v 5 tie, and player 1 must choose cards to show.)
        playCards(state, fm, plain(4), scout(), plain(5), bodyguard());
        assertFalse(state.isActionInProgress());
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(1, state.battlesWon[0][1]);
        assertEquals(0, state.getBattlesTied(0));
    }

    // ---------------------------------------------------------------------------------------------------------
    // Card types and the deck files
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void cardTypesDefaultToTheRulebook3Abilities() {
        assertEquals(AssassinII.class, ASSASSIN.defaultAbility.getClass());
        assertEquals(Scout.class, SCOUT.defaultAbility.getClass());
        assertEquals(SaboteurIII.class, SABOTEUR.defaultAbility.getClass());
        assertEquals(TricksterII.class, TRICKSTER.defaultAbility.getClass());
        assertEquals(BerserkerII.class, BERSERKER.defaultAbility.getClass());
        assertEquals(Bodyguard.class, BODYGUARD.defaultAbility.getClass());
        assertEquals(GeneralHostages.class, GENERAL_ONE.defaultAbility.getClass());
        assertEquals(GeneralFlags.class, GENERAL_TWO.defaultAbility.getClass());
        assertEquals(SiegeCannon.class, SIEGE_CANNON.defaultAbility.getClass());
        // legacy-only types keep their legacy abilities
        assertEquals(IconBearer.class, ICON_BEARER.defaultAbility.getClass());
        assertEquals(AssaultCannon.class, ASSAULT_CANNON.defaultAbility.getClass());
        assertEquals(Bomb.class, BOMB.defaultAbility.getClass());
    }

    /** "name|value|type|ability class|tactics class" for each card in the deck. */
    private static Set<String> describe(List<ToadCard> deck) {
        return deck.stream().map(c -> c.getComponentName() + "|" + c.value + "|" + c.type + "|"
                        + c.ability.getClass().getSimpleName() + "|" + c.tactics.getClass().getSimpleName())
                .collect(Collectors.toSet());
    }

    @Test
    public void defaultDeckIsTheNineRulebook3Cards() {
        List<ToadCard> deck = new ToadParameters().getCardDeck();
        assertEquals(9, deck.size());
        assertEquals(Set.of(
                "Assassin|1|ASSASSIN|AssassinII|AssassinII",
                "Scout|2|SCOUT|Scout|Scout",
                "Saboteur|3|SABOTEUR|SaboteurIII|SaboteurIII",
                "Trickster|4|TRICKSTER|TricksterII|TricksterII",
                "Berserker|5|BERSERKER|BerserkerII|BerserkerII",
                "Bodyguard|6|BODYGUARD|Bodyguard|Bodyguard",
                "General One|7|GENERAL_ONE|GeneralHostages|GeneralHostages",
                "General Two|7|GENERAL_TWO|GeneralFlags|GeneralFlags",
                "Siege Cannon|0|SIEGE_CANNON|SiegeCannon|SiegeCannon"
        ), describe(deck));
    }

    @Test
    public void cards005IsTheLegacyDeck() {
        ToadParameters legacy = new ToadParameters();
        legacy.setParameterValue("cardFile", "cards_005.json");
        List<ToadCard> deck = legacy.getCardDeck();
        assertEquals(9, deck.size());
        assertEquals(Set.of(
                "Assassin|1|ASSASSIN|Assassin|Assassin",
                "Scout|2|SCOUT|Scout|Scout",
                "Saboteur|3|SABOTEUR|SaboteurII|SaboteurII",
                "Trickster|4|TRICKSTER|Trickster|Trickster",
                "Berserker|5|BERSERKER|Berserker|Berserker",
                "Icon Bearer|6|ICON_BEARER|IconBearer|IconBearer",
                "General One|7|GENERAL_ONE|GeneralOne|GeneralOne",
                "General Two|7|GENERAL_TWO|GeneralTwo|GeneralTwo",
                "Assault Cannon|0|ASSAULT_CANNON|AssaultCannon|AssaultCannon"
        ), describe(deck));
    }
}
