package games.monopoly;

import games.monopoly.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

/**
 * The choice on Income Tax (square 4, flat tax 200) between the flat tax and a percentage of the player's total worth,
 * offered when MonopolyParameters.incomeTaxPercent > 0. Three players, seed 7, incomeTaxPercent 10 unless stated;
 * player 0 starts the turn on GO and rolls 1 + 3 onto Income Tax.
 */
public class MonopolyIncomeTaxTest {

    MonopolyParameters params;
    MonopolyGameState state;
    MonopolyForwardModel fm;
    final MonopolySquare incomeTax = sq("Income Tax"), oldKent = sq("Old Kent Road"),
            whitechapel = sq("Whitechapel Road"), mayfair = sq("Mayfair");

    @Before
    public void setup() {
        params = new MonopolyParameters();
        params.setParameterValue("incomeTaxPercent", 10);
        state = newState(3, 7, params);
        fm = new MonopolyForwardModel();
        startTurn(state, 0, sq("GO"));
    }

    /** Player 0 rolls 1 + 3 from GO onto Income Tax and is asked to choose. */
    private void landOnIncomeTax() {
        roll(state, fm, 1, 3);
        assertEquals(incomeTax, state.getPosition(0));
        assertEquals(new IncomeTaxChoice(0, incomeTax), state.currentActionInProgress());
    }

    // ---- the choice ----

    @Test
    public void landingOnIncomeTaxOffersTheFlatTaxOrThePercentage() {
        landOnIncomeTax();
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(new PayFlatTax(incomeTax), new PayPercentTax()), actionSet(fm, state));
        // nothing is paid until the choice is made
        assertEquals(1500, state.getCash(0));
        assertEquals(3 * 1500, totalCash(state));
    }

    @Test
    public void bothOptionsAreOfferedWhateverTheCash() {
        state.setCash(0, 50);
        landOnIncomeTax();
        // 50 < 200 flat, and 50 >= 5 (10% of 50): both offered all the same
        assertEquals(Set.of(new PayFlatTax(incomeTax), new PayPercentTax()), actionSet(fm, state));
        assertEquals(50, state.getCash(0));
    }

    @Test
    public void payingTheFlatTaxPaysTheSquaresTaxToTheBank() {
        landOnIncomeTax();
        fm.next(state, new PayFlatTax(incomeTax));
        assertEquals(1500 - 200, state.getCash(0));
        assertEquals(1500 * 2, state.getCash(1) + state.getCash(2));
        assertFalse(state.isActionInProgress());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void thePercentageIsOfTheTotalWorthRoundedDownAndCanBeLessThanTheFlatTax() {
        state.setCash(0, 1234);
        give(state, 0, oldKent);
        landOnIncomeTax();
        fm.next(state, new PayPercentTax());
        // worth 1234 + 60 = 1294; 10% = 129.4 -> 129 (< 200)
        assertEquals(1234 - 129, state.getCash(0));
        assertEquals(1500 * 2, state.getCash(1) + state.getCash(2));
        assertEquals(0, state.getOwner(oldKent));
        assertFalse(state.isActionInProgress());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn(), new Mortgage(oldKent)), actionSet(fm, state));
    }

    @Test
    public void thePercentageCountsPropertyMortgagesAndBuildingsAndCanBeMoreThanTheFlatTax() {
        state.setCash(0, 2003);
        give(state, 0, oldKent, whitechapel, mayfair);
        state.setBuildings(oldKent, 1);
        state.setBuildings(whitechapel, 1);
        state.setMortgaged(mayfair, true);
        landOnIncomeTax();
        fm.next(state, new PayPercentTax());
        // worth 2003 + 60 + 60 (Old Kent Road, Whitechapel Road) + 2 x 50 (houses) + 200 (Mayfair's mortgage value)
        // = 2423; 10% = 242.3 -> 242 (> 200)
        assertEquals(2003 - 242, state.getCash(0));
        assertEquals(1, state.getBuildings(oldKent));
        assertTrue(state.isMortgaged(mayfair));
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
    }

    @Test
    public void thePercentageComesFromTheParameterAt10() {
        state.setCash(0, 1501);
        landOnIncomeTax();
        fm.next(state, new PayPercentTax());
        // 10% of 1501 = 150.1 -> 150
        assertEquals(1501 - 150, state.getCash(0));
    }

    @Test
    public void thePercentageComesFromTheParameterAt15() {
        params.setParameterValue("incomeTaxPercent", 15);
        state.setCash(0, 1501);
        landOnIncomeTax();
        fm.next(state, new PayPercentTax());
        // 15% of 1501 = 225.15 -> 225
        assertEquals(1501 - 225, state.getCash(0));
    }

    // ---- no choice ----

    @Test
    public void withTheParameterAt0IncomeTaxIsFlatWithNoChoice() {
        params.setParameterValue("incomeTaxPercent", 0);
        roll(state, fm, 1, 3);
        assertEquals(incomeTax, state.getPosition(0));
        assertEquals(1500 - 200, state.getCash(0));
        assertFalse(state.isActionInProgress());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void superTaxNeverGivesAChoice() {
        state.setPosition(0, sq("Liverpool Street Station"));
        roll(state, fm, 1, 2);
        // 35 + 3 = Super Tax, 100 flat whatever incomeTaxPercent
        assertEquals(sq("Super Tax"), state.getPosition(0));
        assertEquals(1500 - 100, state.getCash(0));
        assertFalse(state.isActionInProgress());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    // ---- the turn after the choice ----

    @Test
    public void afterADoubleOntoIncomeTaxTheChoiceIsFollowedByAnotherRoll() {
        roll(state, fm, 2, 2);
        // GO + 4 = Income Tax, a double
        assertEquals(new IncomeTaxChoice(0, incomeTax), state.currentActionInProgress());
        assertEquals(Set.of(new PayFlatTax(incomeTax), new PayPercentTax()), actionSet(fm, state));
        fm.next(state, new PayPercentTax());
        // 10% of 1500 = 150
        assertEquals(1500 - 150, state.getCash(0));
        assertTrue(state.hasAnotherRoll());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
        roll(state, fm, 2, 4);
        // 4 + 6 = Jail, just visiting
        assertEquals(sq("Jail"), state.getPosition(0));
        assertFalse(state.isInJail(0));
        assertEquals(1500 - 150, state.getCash(0));
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    // ---- shortfalls ----

    @Test
    public void aFlatTaxBeyondTheCashRaisesMoney() {
        state.setCash(0, 100);
        give(state, 0, oldKent, mayfair);
        // raisable 100 + 30 + 200 = 330 >= 200
        landOnIncomeTax();
        fm.next(state, new PayFlatTax(incomeTax));
        assertEquals(new RaiseMoney(0, -1, 200), state.currentActionInProgress());
        assertEquals(100, state.getCash(0));
        assertEquals(Set.of(new Mortgage(oldKent), new Mortgage(mayfair)), actionSet(fm, state));
        fm.next(state, new Mortgage(mayfair));
        // 100 + 200 = 300 >= 200: paid
        assertEquals(100 + 200 - 200, state.getCash(0));
        assertFalse(state.isActionInProgress());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
    }

    @Test
    public void aPercentageBeyondTheCashIsFixedWhenChosenAndRaisesMoney() {
        state.setCash(0, 10);
        give(state, 0, mayfair);
        landOnIncomeTax();
        fm.next(state, new PayPercentTax());
        // worth 10 + 400 = 410; 10% = 41 > 10 cash; raisable 10 + 200 = 210 >= 41
        assertEquals(new RaiseMoney(0, -1, 41), state.currentActionInProgress());
        assertEquals(10, state.getCash(0));
        assertEquals(Set.of(new Mortgage(mayfair)), actionSet(fm, state));
        fm.next(state, new Mortgage(mayfair));
        // the debt stays at 41, as worked out when chosen; the worth is still 410 (cash 10 + 200, and Mayfair's
        // mortgage value 200)
        assertEquals(10 + 200 - 41, state.getCash(0));
        assertEquals(1500 * 2, state.getCash(1) + state.getCash(2));
        assertFalse(state.isActionInProgress());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void aFlatTaxBeyondEverythingIsBankruptcyToTheBank() {
        state.setCash(0, 50);
        landOnIncomeTax();
        fm.next(state, new PayFlatTax(incomeTax));
        // 50 < 200 with nothing to raise
        assertTrue(state.isBankrupt(0));
        assertEquals(3, state.getFinalPlace(0));
        assertEquals(0, state.getCash(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
    }

    // ---- by a card ----

    @Test
    public void goBackThreeSpacesOntoIncomeTaxGivesTheSameChoice() {
        onTop(state, chance("Go back three spaces"));
        roll(state, fm, 3, 4);
        // GO + 7 = Chance 1, back 3 to Income Tax
        assertEquals(incomeTax, state.getPosition(0));
        assertEquals(new IncomeTaxChoice(0, incomeTax), state.currentActionInProgress());
        assertEquals(Set.of(new PayFlatTax(incomeTax), new PayPercentTax()), actionSet(fm, state));
        assertEquals(1500, state.getCash(0));
        fm.next(state, new PayPercentTax());
        assertEquals(1500 - 150, state.getCash(0));
        assertEquals(chance("Go back three spaces"), bottom(state.getChanceDeck()));
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    // ---- equality and copy ----

    @Test
    public void incomeTaxChoicesAreEqualByPlayerAndSquareAndCopy() {
        IncomeTaxChoice choice = new IncomeTaxChoice(0, incomeTax);
        assertEquals(new IncomeTaxChoice(0, sq("Income Tax")), choice);
        assertEquals(new IncomeTaxChoice(0, incomeTax).hashCode(), choice.hashCode());
        assertNotEquals(new IncomeTaxChoice(1, incomeTax), choice);
        assertEquals(choice, choice.copy());
        assertEquals(new PayFlatTax(sq("Income Tax")), new PayFlatTax(incomeTax));
        assertNotEquals(new PayFlatTax(sq("Super Tax")), new PayFlatTax(incomeTax));
        assertEquals(new PayPercentTax(), new PayPercentTax());
    }

    @Test
    public void aCopyWithAPendingChoiceIsEqualAndIndependent() {
        landOnIncomeTax();
        MonopolyGameState copy = (MonopolyGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(new IncomeTaxChoice(0, incomeTax), copy.currentActionInProgress());
        assertNotSame(state.currentActionInProgress(), copy.currentActionInProgress());
        fm.next(copy, new PayFlatTax(incomeTax));
        assertEquals(1500 - 200, copy.getCash(0));
        assertEquals(1500, state.getCash(0));
        assertEquals(new IncomeTaxChoice(0, incomeTax), state.currentActionInProgress());
        assertEquals(Set.of(new PayFlatTax(incomeTax), new PayPercentTax()), actionSet(fm, state));
    }
}
