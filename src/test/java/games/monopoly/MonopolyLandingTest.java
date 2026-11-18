package games.monopoly;

import games.monopoly.actions.EndTurn;
import games.monopoly.actions.RollDice;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

/**
 * What happens on landing (through RollDice): rent paid to the owner, taxes, Go To Jail.
 */
public class MonopolyLandingTest {

    MonopolyGameState state;
    MonopolyForwardModel fm;
    final MonopolySquare pallMall = sq("Pall Mall"), whitehall = sq("Whitehall"),
            northumberland = sq("Northumberland Avenue");

    @Before
    public void setup() {
        state = newState(3, 7, null);
        fm = new MonopolyForwardModel();
        startTurn(state, 0, sq("GO"));
    }

    @Test
    public void landingOnAnotherPlayersStreetPaysTheRentToTheOwner() {
        give(state, 2, pallMall, whitehall, northumberland);
        state.setPosition(0, sq("Jail")); // Just Visiting
        roll(state, fm, 1, 2);
        // 10 + 3 = Whitehall, rent 10 doubled for the whole Pink group
        assertEquals(whitehall, state.getPosition(0));
        assertEquals(1500 - 2 * 10, state.getCash(0));
        assertEquals(1500 + 2 * 10, state.getCash(2));
        assertEquals(1500, state.getCash(1));
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void anOwnerInJailStillCollectsRent() {
        give(state, 2, whitehall);
        putInJail(state, 2, 1);
        state.setPosition(0, sq("Jail"));
        roll(state, fm, 1, 2);
        assertEquals(1500 - 10, state.getCash(0));
        assertEquals(1500 + 10, state.getCash(2));
    }

    @Test
    public void landingOnAMortgagedPropertyPaysNothing() {
        give(state, 2, whitehall);
        state.setMortgaged(whitehall, true);
        state.setPosition(0, sq("Jail"));
        roll(state, fm, 1, 2);
        assertEquals(whitehall, state.getPosition(0));
        assertEquals(1500, state.getCash(0));
        assertEquals(1500, state.getCash(2));
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void aUtilityChargesAMultipleOfTheDiceJustRolled() {
        give(state, 2, sq("Electric Company"));
        setDice(state, 1, 1); // an earlier roll
        state.setPosition(0, sq("King's Cross Station"));
        roll(state, fm, 3, 4);
        // 5 + 7 = Electric Company, one utility owned: 4 x the dice
        assertEquals(sq("Electric Company"), state.getPosition(0));
        assertEquals(1500 - 4 * (3 + 4), state.getCash(0));
        assertEquals(1500 + 4 * (3 + 4), state.getCash(2));
    }

    @Test
    public void incomeTaxIsPaidToTheBank() {
        roll(state, fm, 1, 3);
        // GO + 4 = Income Tax, 200
        assertEquals(sq("Income Tax"), state.getPosition(0));
        assertEquals(1500 - 200, state.getCash(0));
        assertEquals(1500, state.getCash(1));
        assertEquals(1500, state.getCash(2));
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
    }

    @Test
    public void superTaxIsPaidToTheBank() {
        state.setPosition(0, sq("Liverpool Street Station"));
        roll(state, fm, 1, 2);
        // 35 + 3 = Super Tax, 100
        assertEquals(sq("Super Tax"), state.getPosition(0));
        assertEquals(1500 - 100, state.getCash(0));
        assertEquals(1500 * 2, state.getCash(1) + state.getCash(2));
    }

    @Test
    public void goToJailSendsThePlayerToJailWithoutTheSalaryAndEndsTheTurn() {
        state.setPosition(0, sq("Fenchurch Street Station"));
        roll(state, fm, 2, 3);
        // 25 + 5 = Go To Jail: straight to Jail (10), not forward past GO
        assertEquals(sq("Jail"), state.getPosition(0));
        assertTrue(state.isInJail(0));
        assertEquals(0, state.getJailRolls(0));
        assertEquals(1500, state.getCash(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
    }

    @Test
    public void goToJailEndsTheTurnEvenAfterADouble() {
        state.setPosition(0, sq("Leicester Square"));
        roll(state, fm, 2, 2);
        // 26 + 4 = Go To Jail
        assertTrue(state.isInJail(0));
        assertEquals(sq("Jail"), state.getPosition(0));
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.hasAnotherRoll());
        assertEquals(0, state.getNDoubles());
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
    }
}
