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
 * Blitz and RiskParameters.allowBlitz. setNextRolls gives the attacker's dice then the defender's, roll by roll.
 * Base arrangement (as RiskBattleTest): player 0 holds only Indonesia (6 armies), player 1 everything else with
 * 1 army but Argentina (player 2's). Indonesia borders Siam, New Guinea, Western Australia.
 */
public class RiskBlitzTest {

    RiskForwardModel fm = new RiskForwardModel();
    RiskGameState state;

    @Before
    public void setup() {
        state = newState(3, 7, null);
        arrange(state);
    }

    private static void arrange(RiskGameState s) {
        fillBoard(s, 1);
        give(s, 2, 1, ARGENTINA);
        give(s, 0, 6, INDONESIA);
        startPlay(s, 0, RiskGamePhase.ATTACK, 0);
    }

    private static RiskParameters params(String name, Object value) {
        RiskParameters params = new RiskParameters();
        params.setParameterValue(name, value);
        return params;
    }

    @Test
    public void attackOffersOneBlitzPerPairAlongsideTheSingleRolls() {
        // Indonesia 6: 1..min(3, 5) dice into each of its three neighbours, a Blitz for each, EndAttack
        Set<AbstractAction> expected = new HashSet<>();
        for (RiskTerritory to : List.of(SIAM, NEW_GUINEA, WESTERN_AUSTRALIA)) {
            for (int n = 1; n <= 3; n++)
                expected.add(new Attack(INDONESIA, to, n));
            expected.add(new Blitz(INDONESIA, to));
        }
        expected.add(new EndAttack());
        assertEquals(expected, actionSet(fm, state));
    }

    @Test
    public void withoutAllowBlitzNoBlitzIsOffered() {
        state = newState(3, 7, params("allowBlitz", false));
        arrange(state);
        Set<AbstractAction> expected = new HashSet<>();
        for (RiskTerritory to : List.of(SIAM, NEW_GUINEA, WESTERN_AUSTRALIA))
            for (int n = 1; n <= 3; n++)
                expected.add(new Attack(INDONESIA, to, n));
        expected.add(new EndAttack());
        assertEquals(expected, actionSet(fm, state));
    }

    @Test
    public void noBlitzFromATerritoryWithOneArmy() {
        // Indonesia 1 army, New Guinea 2 (borders Indonesia (own), Western and Eastern Australia (player 1's))
        give(state, 0, 1, INDONESIA);
        give(state, 0, 2, NEW_GUINEA);
        assertEquals(Set.of(new Attack(NEW_GUINEA, WESTERN_AUSTRALIA, 1), new Attack(NEW_GUINEA, EASTERN_AUSTRALIA, 1),
                        new Blitz(NEW_GUINEA, WESTERN_AUSTRALIA), new Blitz(NEW_GUINEA, EASTERN_AUSTRALIA), new EndAttack()),
                actionSet(fm, state));
    }

    @Test
    public void blitzRollsUntilCaptureAndTheMoveInMinimumIsTheLastRollsDice() {
        give(state, 1, 4, SIAM);
        state.setNextRolls(
                3, 2, 1, 6, 6,  // roll 1: 3 v 2 dice (I 6, S 4): 3v6, 2v6 -> attacker loses 2: I 4, S 4
                6, 6, 1, 1, 1,  // roll 2: 3 v 2 (I 4: min(3, 3)): 6v1, 6v1 -> defender loses 2: I 4, S 2
                6, 1, 1, 5, 5,  // roll 3: 3 v 2: 6v5 win, 1v5 lose -> one each: I 3, S 1
                6, 1, 2);       // roll 4: 2 v 1 (I 3: min(3, 2); S 1: min(2, 1)): 6v2 -> S 0, captured
        fm.next(state, new Blitz(INDONESIA, SIAM));
        assertEquals(0, state.getOwner(SIAM));
        assertEquals(0, state.getArmies(SIAM));
        assertEquals(3, state.getArmies(INDONESIA));
        assertTrue(state.hasCapturedThisTurn());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(state.currentActionInProgress() instanceof MoveArmiesChoice);
        // the last roll used 2 dice: move 2 .. 3 - 1 = 2
        assertEquals(Set.of(new MoveArmies(INDONESIA, SIAM, 2)), actionSet(fm, state));

        fm.next(state, new MoveArmies(INDONESIA, SIAM, 2));
        assertEquals(1, state.getArmies(INDONESIA));
        assertEquals(2, state.getArmies(SIAM));
        assertFalse(state.isActionInProgress());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
    }

    @Test
    public void blitzCapturingOnTheFirstRollOffersTheUsualMoveIn() {
        give(state, 1, 2, SIAM);
        state.setNextRolls(6, 6, 1, 5, 4); // 3 v 2 (I 6, S 2): 6v5, 6v4 -> S 0 after one roll
        fm.next(state, new Blitz(INDONESIA, SIAM));
        assertEquals(0, state.getOwner(SIAM));
        assertEquals(6, state.getArmies(INDONESIA));
        // 3 dice .. 6 - 1
        assertEquals(Set.of(new MoveArmies(INDONESIA, SIAM, 3), new MoveArmies(INDONESIA, SIAM, 4),
                new MoveArmies(INDONESIA, SIAM, 5)), actionSet(fm, state));
    }

    @Test
    public void blitzStopsWhenTheAttackerIsDownToOneArmy() {
        give(state, 0, 4, INDONESIA);
        give(state, 0, 3, NEW_GUINEA);
        give(state, 1, 5, SIAM);
        state.setNextRolls(
                1, 1, 1, 6, 6,  // roll 1: 3 v 2 (I 4: min(3, 3); S 5): 1v6, 1v6 -> attacker loses 2: I 2, S 5
                1, 6, 6);       // roll 2: 1 v 2 (I 2: min(3, 1)): 1v6 -> attacker loses 1: I 1 - stop
        fm.next(state, new Blitz(INDONESIA, SIAM));
        assertEquals(1, state.getArmies(INDONESIA));
        assertEquals(5, state.getArmies(SIAM));
        assertEquals(1, state.getOwner(SIAM));
        assertFalse(state.hasCapturedThisTurn());
        assertFalse(state.isActionInProgress());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        // Indonesia (1 army) can no longer attack; New Guinea (3) borders Indonesia (own) and W/E Australia
        assertEquals(Set.of(
                new Attack(NEW_GUINEA, WESTERN_AUSTRALIA, 1), new Attack(NEW_GUINEA, WESTERN_AUSTRALIA, 2),
                new Attack(NEW_GUINEA, EASTERN_AUSTRALIA, 1), new Attack(NEW_GUINEA, EASTERN_AUSTRALIA, 2),
                new Blitz(NEW_GUINEA, WESTERN_AUSTRALIA), new Blitz(NEW_GUINEA, EASTERN_AUSTRALIA),
                new EndAttack()), actionSet(fm, state));
    }

    @Test
    public void blitzRespectsMaxAttackDiceAndMaxDefendDice() {
        RiskParameters params = params("maxAttackDice", 2);
        params.setParameterValue("maxDefendDice", 1);
        state = newState(3, 7, params);
        arrange(state);
        give(state, 0, 4, INDONESIA);
        give(state, 1, 2, SIAM);
        state.setNextRolls(
                1, 1, 6,  // roll 1: 2 v 1 (I 4: min(2, 3); S 2: min(1, 2)): 1v6 -> attacker loses 1: I 3, S 2
                6, 1, 2,  // roll 2: 2 v 1 (I 3: min(2, 2)): 6v2 -> S 1
                6, 6, 1); // roll 3: 2 v 1: 6v1 -> S 0, captured
        // (3 attacking or 2 defending dice would take different values from the queue and change every roll)
        fm.next(state, new Blitz(INDONESIA, SIAM));
        assertEquals(0, state.getOwner(SIAM));
        assertEquals(3, state.getArmies(INDONESIA));
        // last roll 2 dice: 2 .. 3 - 1
        assertEquals(Set.of(new MoveArmies(INDONESIA, SIAM, 2)), actionSet(fm, state));
    }

    @Test
    public void blitzEliminationPassesTheCardsOnWithTheTradeUnderTheMoveIn() {
        // 4 players: player 2 only on Siam (3 armies), player 3 Argentina; player 0 3 Infantry, player 2 3 Cavalry
        state = newState(4, 7, null);
        fillBoard(state, 1);
        give(state, 0, 6, INDONESIA);
        give(state, 2, 3, SIAM);
        give(state, 3, 1, ARGENTINA);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        giveCards(state, 2, card(GREENLAND), card(ALBERTA), card(ONTARIO));
        state.setNextRolls(
                6, 6, 1, 1, 1,  // roll 1: 3 v 2 (I 6, S 3): 6v1, 6v1 -> S 1
                6, 1, 1, 1);    // roll 2: 3 v 1 (S 1): 6v1 -> S 0, captured; player 2 has no territory left
        fm.next(state, new Blitz(INDONESIA, SIAM));
        assertEquals(0, state.getOwner(SIAM));
        assertTrue(state.isEliminated(2));
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
        assertEquals(4, state.getFinalPlace(2)); // first out of 4
        assertTrue(state.hasCapturedThisTurn());
        assertEquals(6, state.getHand(0).getSize()); // 3 + 3
        assertEquals(0, state.getHand(2).getSize());
        assertEquals(44, allCards(state).size());
        // 6 >= eliminationTradeLimit: the trade waits under the move-in (3 dice .. 5)
        assertTrue(state.isActionInProgress());
        assertTrue(state.currentActionInProgress() instanceof MoveArmiesChoice);
        assertEquals(new EliminationTrade(0), state.getQueuedAction(0));
        assertEquals(Set.of(new MoveArmies(INDONESIA, SIAM, 3), new MoveArmies(INDONESIA, SIAM, 4),
                new MoveArmies(INDONESIA, SIAM, 5)), actionSet(fm, state));
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void blitzThatTakesTheLastTerritoryWinsMovingTheLastRollsDiceIn() {
        // 3 players: player 2 already out; player 1 holds only Siam (3), player 0 everything else, Indonesia 4
        state = newState(3, 7, null);
        fillBoard(state, 0);
        give(state, 0, 4, INDONESIA);
        give(state, 1, 3, SIAM);
        eliminate(state, 2, 3);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        state.setNextRolls(
                6, 1, 1, 5, 5,  // roll 1: 3 v 2 (I 4, S 3): 6v5 win, 1v5 lose -> I 3, S 2
                6, 6, 1, 1);    // roll 2: 2 v 2 (I 3: min(3, 2)): 6v1, 6v1 -> S 0, captured
        fm.next(state, new Blitz(INDONESIA, SIAM));
        assertFalse(state.isNotTerminal());
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(42, state.getNTerritories(0));
        // the last roll's 2 dice move in at once: I 3 - 2 = 1, S 2
        assertEquals(1, state.getArmies(INDONESIA));
        assertEquals(2, state.getArmies(SIAM));
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void blitzIgnoresDefenderChoosesDiceAndRollsTheDefendersMaximum() {
        state = newState(3, 7, params("defenderChoosesDice", true));
        arrange(state);
        give(state, 1, 3, SIAM);
        state.setNextRolls(
                6, 6, 6, 1, 1,  // roll 1: 3 v 2 (S 3: min(2, 3), no choice): 6v1, 6v1 -> S 1
                6, 6, 6, 1);    // roll 2: 3 v 1: 6v1 -> S 0, captured
        fm.next(state, new Blitz(INDONESIA, SIAM));
        assertEquals(0, state.getOwner(SIAM));
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(state.currentActionInProgress() instanceof MoveArmiesChoice);
        assertEquals(new MoveArmiesChoice(0, INDONESIA, SIAM, 3), state.currentActionInProgress());
        // nothing under the move-in (no DefenderDice), and after it the attacker carries on
        assertEquals(new MoveArmiesChoice(0, INDONESIA, SIAM, 3), state.getQueuedAction(0));
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
    }
}
