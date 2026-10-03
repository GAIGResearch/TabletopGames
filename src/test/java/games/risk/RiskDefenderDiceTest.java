package games.risk;

import core.actions.AbstractAction;
import games.risk.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * RiskParameters.defenderChoosesDice: the DefenderDice decision after an Attack, and the roll, capture and return to
 * the attacker after DefendWith. setNextRolls gives the attacker's dice then the defender's.
 * Base arrangement (as RiskBattleTest, with defenderChoosesDice): player 0 holds only Indonesia (6 armies), player 1
 * everything else with 1 army but Argentina (player 2's). Indonesia borders Siam, New Guinea, Western Australia.
 */
public class RiskDefenderDiceTest {

    RiskForwardModel fm = new RiskForwardModel();
    RiskGameState state;

    @Before
    public void setup() {
        state = newState(3, 7, params(2));
        arrange(state);
    }

    private static RiskParameters params(int maxDefendDice) {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("defenderChoosesDice", true);
        params.setParameterValue("maxDefendDice", maxDefendDice);
        return params;
    }

    private static void arrange(RiskGameState s) {
        fillBoard(s, 1);
        give(s, 2, 1, ARGENTINA);
        give(s, 0, 6, INDONESIA);
        startPlay(s, 0, RiskGamePhase.ATTACK, 0);
    }

    /** Player 0's ATTACK actions holding only Indonesia with the given armies (>= 2), Blitz allowed. */
    private static Set<AbstractAction> indonesiaAttacks(int armies) {
        Set<AbstractAction> expected = new HashSet<>();
        for (RiskTerritory to : List.of(SIAM, NEW_GUINEA, WESTERN_AUSTRALIA)) {
            for (int n = 1; n <= Math.min(3, armies - 1); n++)
                expected.add(new Attack(INDONESIA, to, n));
            expected.add(new Blitz(INDONESIA, to));
        }
        expected.add(new EndAttack());
        return expected;
    }

    @Test
    public void attackHandsTheDiceDecisionToTheDefenderWithoutRolling() {
        give(state, 1, 3, SIAM);
        state.setNextRolls(6, 6, 6, 1, 1); // would take Siam down to 1 if the attack rolled now
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertEquals(6, state.getArmies(INDONESIA));
        assertEquals(3, state.getArmies(SIAM));
        assertEquals(new DefenderDice(1, INDONESIA, SIAM, 3), state.currentActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getTurnOwner());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        // Siam 3: 1 .. min(2, 3)
        assertEquals(Set.of(new DefendWith(INDONESIA, SIAM, 3, 1), new DefendWith(INDONESIA, SIAM, 3, 2)),
                actionSet(fm, state));
    }

    @Test
    public void aDefenderWithOneArmyCanRollOnlyOneDie() {
        // Siam 1 army: 1 .. min(2, 1)
        fm.next(state, new Attack(INDONESIA, SIAM, 2));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new DefendWith(INDONESIA, SIAM, 2, 1)), actionSet(fm, state));
    }

    @Test
    public void maxDefendDiceSetsTheDefendersMostDice() {
        state = newState(3, 7, params(3));
        arrange(state);
        give(state, 1, 5, SIAM);
        fm.next(state, new Attack(INDONESIA, SIAM, 1));
        // Siam 5: 1 .. min(3, 5)
        assertEquals(Set.of(new DefendWith(INDONESIA, SIAM, 1, 1), new DefendWith(INDONESIA, SIAM, 1, 2),
                new DefendWith(INDONESIA, SIAM, 1, 3)), actionSet(fm, state));

        state = newState(3, 7, params(3));
        arrange(state);
        give(state, 1, 2, SIAM);
        fm.next(state, new Attack(INDONESIA, SIAM, 1));
        // Siam 2: 1 .. min(3, 2)
        assertEquals(Set.of(new DefendWith(INDONESIA, SIAM, 1, 1), new DefendWith(INDONESIA, SIAM, 1, 2)),
                actionSet(fm, state));
    }

    @Test
    public void defendingWithOneDieRollsOneDefenceDie() {
        give(state, 1, 3, SIAM);
        fm.next(state, new Attack(INDONESIA, SIAM, 2));
        // attacker 6 5, defender 4: 6v4 -> Siam loses 1 (the queued 6 is not used: a second die would make it
        // 6v6 lost, 5v4 won)
        state.setNextRolls(6, 5, 4, 6);
        fm.next(state, new DefendWith(INDONESIA, SIAM, 2, 1));
        assertEquals(6, state.getArmies(INDONESIA));
        assertEquals(2, state.getArmies(SIAM));
    }

    @Test
    public void defendingWithTwoDiceRollsTwoDefenceDice() {
        give(state, 1, 3, SIAM);
        fm.next(state, new Attack(INDONESIA, SIAM, 2));
        // attacker 6 5, defender 4 6: 6v6 -> attacker loses 1, 5v4 -> Siam loses 1
        state.setNextRolls(6, 5, 4, 6);
        fm.next(state, new DefendWith(INDONESIA, SIAM, 2, 2));
        assertEquals(5, state.getArmies(INDONESIA));
        assertEquals(2, state.getArmies(SIAM));
    }

    @Test
    public void theAttackUsesTheAttackersDiceCount() {
        give(state, 1, 3, SIAM);
        fm.next(state, new Attack(INDONESIA, SIAM, 1));
        // attacker 1 die: 2; defender 6: 2v6 -> attacker loses 1 (with 3 attacking dice it would be 2 6 1 v 1: Siam 2)
        state.setNextRolls(2, 6, 1, 1);
        fm.next(state, new DefendWith(INDONESIA, SIAM, 1, 1));
        assertEquals(5, state.getArmies(INDONESIA));
        assertEquals(3, state.getArmies(SIAM));
    }

    @Test
    public void afterTheDefenceTheTurnReturnsToTheAttacker() {
        give(state, 1, 3, SIAM);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        // attacker 1 1 1, defender 6 6: 1v6, 1v6 -> attacker loses 2: Indonesia 4
        state.setNextRolls(1, 1, 1, 6, 6);
        fm.next(state, new DefendWith(INDONESIA, SIAM, 3, 2));
        assertEquals(4, state.getArmies(INDONESIA));
        assertEquals(3, state.getArmies(SIAM));
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertFalse(state.hasCapturedThisTurn());
        assertEquals(indonesiaAttacks(4), actionSet(fm, state)); // up to min(3, 4 - 1) dice
    }

    @Test
    public void emptyingTheTerritoryInTheDefenceIsACaptureByTheAttacker() {
        // Siam 1: attack with 2 dice, the defender rolls 1: 6 6 v 1 -> Siam 0
        fm.next(state, new Attack(INDONESIA, SIAM, 2));
        state.setNextRolls(6, 6, 1);
        fm.next(state, new DefendWith(INDONESIA, SIAM, 2, 1));
        assertEquals(0, state.getOwner(SIAM));
        assertEquals(0, state.getArmies(SIAM));
        assertTrue(state.hasCapturedThisTurn());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(new MoveArmiesChoice(0, INDONESIA, SIAM, 2), state.currentActionInProgress());
        // 2 attacking dice .. 6 - 1
        assertEquals(Set.of(new MoveArmies(INDONESIA, SIAM, 2), new MoveArmies(INDONESIA, SIAM, 3),
                new MoveArmies(INDONESIA, SIAM, 4), new MoveArmies(INDONESIA, SIAM, 5)), actionSet(fm, state));

        fm.next(state, new MoveArmies(INDONESIA, SIAM, 4));
        assertFalse(state.isActionInProgress()); // DefenderDice was completed by DefendWith, the move-in by MoveArmies
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(2, state.getArmies(INDONESIA)); // 6 - 4
        assertEquals(4, state.getArmies(SIAM));
    }

    @Test
    public void aCopyDuringTheMoveInAfterADefenceCarriesOnAsTheOriginal() {
        // the completed DefenderDice stays beneath the move-in until it ends; a copy taken then must not ask the
        // defender again once the move-in is made
        fm.next(state, new Attack(INDONESIA, SIAM, 2));
        state.setNextRolls(6, 6, 1); // 6 6 v 1 -> Siam 0
        fm.next(state, new DefendWith(INDONESIA, SIAM, 2, 1));
        RiskGameState copy = (RiskGameState) state.copy();
        assertEquals(state, copy);
        fm.next(copy, new MoveArmies(INDONESIA, SIAM, 3));
        assertFalse(copy.isActionInProgress());
        assertEquals(0, copy.getCurrentPlayer());
        assertEquals(RiskGamePhase.ATTACK, copy.getGamePhase());
        assertEquals(3, copy.getArmies(INDONESIA)); // 6 - 3
        assertEquals(3, copy.getArmies(SIAM));
    }

    @Test
    public void aDefenceThatLosesTheLastTerritoryEliminatesTheDefender() {
        // 4 players: player 2 only on Siam (1 army), player 3 Argentina; player 2 holds 2 cards
        state = newState(4, 7, params(2));
        fillBoard(state, 1);
        give(state, 0, 6, INDONESIA);
        give(state, 2, 1, SIAM);
        give(state, 3, 1, ARGENTINA);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        giveCards(state, 2, card(GREENLAND), card(ALBERTA));
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertEquals(2, state.getCurrentPlayer());
        state.setNextRolls(6, 6, 6, 1); // 3 v 1: 6v1 -> Siam 0
        fm.next(state, new DefendWith(INDONESIA, SIAM, 3, 1));
        assertTrue(state.isEliminated(2));
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
        assertEquals(4, state.getFinalPlace(2));
        assertEquals(2, state.getHand(0).getSize()); // 0 + 2: below 6, no trade
        assertEquals(0, state.getHand(2).getSize());
        // the move-in is on top; the completed DefenderDice beneath it is removed with it
        assertEquals(new MoveArmiesChoice(0, INDONESIA, SIAM, 3), state.currentActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void aDefenceThatLosesTheLastTerritoryOfTheMapEndsTheGame() {
        // 3 players: player 2 out, player 1 only Siam (1 army), player 0 everything else, Indonesia 4
        state = newState(3, 7, params(2));
        fillBoard(state, 0);
        give(state, 0, 4, INDONESIA);
        give(state, 1, 1, SIAM);
        eliminate(state, 2, 3);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertTrue(state.isNotTerminal());
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new DefendWith(INDONESIA, SIAM, 3, 1));
        assertFalse(state.isNotTerminal());
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        // the 3 dice move in at once: Indonesia 4 - 3, Siam 3
        assertEquals(1, state.getArmies(INDONESIA));
        assertEquals(3, state.getArmies(SIAM));
    }

    @Test
    public void withoutDefenderChoosesDiceTheAttackRollsAtOnce() {
        RiskParameters params = params(2);
        params.setParameterValue("defenderChoosesDice", false);
        state = newState(3, 7, params);
        arrange(state);
        give(state, 1, 3, SIAM);
        // attacker 6 6 6, defender rolls the maximum 2 (1 1): Siam 1
        state.setNextRolls(6, 6, 6, 1, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertEquals(1, state.getArmies(SIAM));
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void aPendingDefenderDecisionSurvivesACopy() {
        give(state, 1, 3, SIAM);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        RiskGameState copy = (RiskGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(new DefenderDice(1, INDONESIA, SIAM, 3), copy.currentActionInProgress());
        assertEquals(1, copy.getCurrentPlayer());
        assertEquals(actionSet(fm, state), actionSet(fm, copy));

        // both go on the same way
        state.setNextRolls(6, 6, 6, 1, 1);
        copy.setNextRolls(6, 6, 6, 1, 1);
        fm.next(state, new DefendWith(INDONESIA, SIAM, 3, 2));
        fm.next(copy, new DefendWith(INDONESIA, SIAM, 3, 2));
        assertEquals(state, copy);
        assertEquals(1, copy.getArmies(SIAM));
        assertEquals(0, copy.getCurrentPlayer());
    }
}
