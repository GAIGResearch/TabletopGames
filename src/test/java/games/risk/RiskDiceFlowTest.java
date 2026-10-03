package games.risk;

import games.risk.actions.*;
import org.junit.Test;

import java.util.Set;

import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * Scripted walk-through of a turn with defenderChoosesDice and Blitz, each attack chosen (ChooseAttack) before its
 * dice: a single roll where the defender picks the dice, a blitz capture with the move-in, a blitz that stops at 1 army, then the card and the next player's turn.
 */
public class RiskDiceFlowTest {

    RiskForwardModel fm = new RiskForwardModel();

    @Test
    public void defenderChoosesDiceThenBlitzCaptureThenBlitzStopsThenTheTurnPasses() {
        // 3 players: player 1 holds everything with 1 army but Argentina (player 2), Indonesia (player 0, 6) and
        // Siam (player 1, 3); player 0 to reinforce with 3
        RiskParameters params = new RiskParameters();
        params.setParameterValue("defenderChoosesDice", true);
        RiskGameState state = newState(3, 7, params);
        fillBoard(state, 1);
        give(state, 2, 1, ARGENTINA);
        give(state, 0, 6, INDONESIA);
        give(state, 1, 3, SIAM);
        startPlay(state, 0, RiskGamePhase.REINFORCE, 3);
        for (int i = 0; i < 3; i++)
            fm.next(state, new PlaceArmy(INDONESIA));
        assertEquals(9, state.getArmies(INDONESIA)); // 6 + 3
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertTrue(fm.computeAvailableActions(state).contains(new ChooseAttack(INDONESIA, SIAM)));

        // the attack is chosen first, then the dice
        fm.next(state, new ChooseAttack(INDONESIA, SIAM));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(new AttackDiceChoice(0, INDONESIA, SIAM), state.currentActionInProgress());
        assertEquals(Set.of(new Attack(INDONESIA, SIAM, 1), new Attack(INDONESIA, SIAM, 2),
                new Attack(INDONESIA, SIAM, 3), new Blitz(INDONESIA, SIAM)), actionSet(fm, state));

        // a single roll: player 1 chooses 2 dice; 6 2 1 v 5 3: 6v5 won, 2v3 lost -> Indonesia 8, Siam 2
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new DefendWith(INDONESIA, SIAM, 3, 1), new DefendWith(INDONESIA, SIAM, 3, 2)),
                actionSet(fm, state));
        state.setNextRolls(6, 2, 1, 5, 3);
        fm.next(state, new DefendWith(INDONESIA, SIAM, 3, 2));
        assertEquals(8, state.getArmies(INDONESIA));
        assertEquals(2, state.getArmies(SIAM));
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());

        // blitz: 3 v 2 (no defender choice), 6 6 6 v 1 1 -> Siam 0 after one roll; move 3 .. 8 - 1
        state.setNextRolls(6, 6, 6, 1, 1);
        fm.next(state, new ChooseAttack(INDONESIA, SIAM));
        fm.next(state, new Blitz(INDONESIA, SIAM));
        assertEquals(0, state.getOwner(SIAM));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(new MoveArmiesChoice(0, INDONESIA, SIAM, 3), state.currentActionInProgress());
        assertEquals(5, actionSet(fm, state).size()); // 3, 4, 5, 6, 7
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 5));
        assertEquals(3, state.getArmies(INDONESIA));
        assertEquals(5, state.getArmies(SIAM));
        assertFalse(state.isActionInProgress());

        // blitz from Siam (5) on China (player 1, 1 army), each roll against 1 defending die, attacker dice:
        // Siam 5: min(3, 4) = 3; Siam 4: min(3, 3) = 3; Siam 3: min(3, 2) = 2; Siam 2: min(3, 1) = 1
        state.setNextRolls(
                1, 1, 1, 6,  // 5 -> 4
                1, 1, 1, 6,  // 4 -> 3
                1, 1, 6,     // 3 -> 2
                1, 6);       // 2 -> 1: stop
        fm.next(state, new ChooseAttack(SIAM, CHINA));
        fm.next(state, new Blitz(SIAM, CHINA));
        assertEquals(1, state.getArmies(SIAM));
        assertEquals(1, state.getArmies(CHINA));
        assertEquals(1, state.getOwner(CHINA));
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());

        // end attacking: Siam was captured, so a card
        fm.next(state, new EndAttack());
        assertEquals(1, state.getHand(0).getSize());
        assertEquals(RiskGamePhase.FORTIFY, state.getGamePhase());
        fm.next(state, new EndTurn());

        // player 1: 39 territories (all but Argentina, Indonesia, Siam) -> 13, + North America 5, Europe 5, Africa 3
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(26, state.getArmiesToPlace(1));
    }
}
