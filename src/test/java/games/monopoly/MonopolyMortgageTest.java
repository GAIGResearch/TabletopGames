package games.monopoly;

import games.monopoly.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Mortgaging and unmortgaging, the interest on lifting a mortgage, canMortgage with buildings in the group (buildings
 * set directly), and getRaisableValue. Three players; player 0 is in MANAGE at Free Parking with no further roll.
 */
public class MonopolyMortgageTest {

    MonopolyParameters params;
    MonopolyGameState state;
    MonopolyForwardModel fm;
    final MonopolySquare oldKent = sq("Old Kent Road"), whitechapel = sq("Whitechapel Road"),
            parkLane = sq("Park Lane"), mayfair = sq("Mayfair"), kingsCross = sq("King's Cross Station"),
            electric = sq("Electric Company"), pallMall = sq("Pall Mall"), whitehall = sq("Whitehall"),
            northumberland = sq("Northumberland Avenue");

    @Before
    public void setup() {
        params = new MonopolyParameters();
        state = newState(3, 7, params);
        fm = new MonopolyForwardModel();
        manage(false);
    }

    /** Player 0's turn, in MANAGE after a roll to Free Parking. */
    private void manage(boolean anotherRoll) {
        startTurn(state, 0, sq("Free Parking"));
        state.setGamePhase(MonopolyGamePhase.MANAGE);
        state.setAnotherRoll(anotherRoll);
    }

    // ---- the interest and the cost of lifting a mortgage ----

    @Test
    public void theInterestIsTenPercentOfTheMortgageRoundedUpToThePound() {
        assertEquals(18, params.mortgageInterest(parkLane)); // 10% of 175 = 17.5 -> 18
        assertEquals(8, params.mortgageInterest(electric)); // 10% of 75 = 7.5 -> 8
        assertEquals(3, params.mortgageInterest(oldKent)); // 10% of 30 = 3 exactly
        assertEquals(20, params.mortgageInterest(mayfair)); // 10% of 200
    }

    @Test
    public void unmortgagingCostsTheMortgageValuePlusTheInterest() {
        assertEquals(175 + 18, params.unmortgageCost(parkLane)); // 193
        assertEquals(75 + 8, params.unmortgageCost(electric));
        assertEquals(30 + 3, params.unmortgageCost(oldKent));
    }

    @Test
    public void theInterestComesFromMortgageInterestPercent() {
        params.setParameterValue("mortgageInterestPercent", 20);
        assertEquals(35, params.mortgageInterest(parkLane)); // 20% of 175 = 35
        assertEquals(15, params.mortgageInterest(electric)); // 20% of 75 = 15
        assertEquals(175 + 35, params.unmortgageCost(parkLane));
        params.setParameterValue("mortgageInterestPercent", 0);
        assertEquals(0, params.mortgageInterest(parkLane));
        assertEquals(175, params.unmortgageCost(parkLane));
    }

    // ---- the actions offered ----

    @Test
    public void manageOffersMortgageForUnmortgagedPropertiesAndUnmortgageForMortgagedOnes() {
        give(state, 0, oldKent, whitechapel, mayfair, kingsCross);
        state.setMortgaged(whitechapel, true);
        // another player's properties are not offered
        give(state, 1, parkLane, electric);
        state.setMortgaged(electric, true);
        assertEquals(Set.of(new EndTurn(), new Mortgage(oldKent), new Mortgage(mayfair), new Mortgage(kingsCross),
                new Unmortgage(whitechapel)), actionSet(fm, state));
    }

    @Test
    public void withAnotherRollDueManageOffersRollDiceAlongsideTheMortgages() {
        manage(true);
        give(state, 0, oldKent);
        assertEquals(Set.of(new RollDice(), new Mortgage(oldKent)), actionSet(fm, state));
    }

    @Test
    public void unmortgageIsOfferedOnlyWithItsCostInCash() {
        give(state, 0, whitechapel);
        state.setMortgaged(whitechapel, true);
        state.setCash(0, 30 + 3 - 1);
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
        state.setCash(0, 30 + 3);
        assertEquals(Set.of(new EndTurn(), new Unmortgage(whitechapel)), actionSet(fm, state));
    }

    @Test
    public void noMortgagingInTheRollPhase() {
        startTurn(state, 0, sq("GO"));
        give(state, 0, oldKent, whitechapel);
        state.setMortgaged(whitechapel, true);
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
        putInJail(state, 0, 0);
        assertEquals(Set.of(new PayJailFine(), new RollDice()), actionSet(fm, state));
    }

    // ---- buildings in the group ----

    @Test
    public void noStreetOfAGroupWithBuildingsCanBeMortgaged() {
        give(state, 0, pallMall, whitehall, northumberland, oldKent, kingsCross);
        state.setBuildings(whitehall, 1);
        assertFalse(state.canMortgage(pallMall));
        assertFalse(state.canMortgage(whitehall));
        assertFalse(state.canMortgage(northumberland));
        // another group, and a station, are not affected
        assertTrue(state.canMortgage(oldKent));
        assertTrue(state.canMortgage(kingsCross));
        // the group is whole: build evenly on the bare streets, or sell Whitehall's house
        assertEquals(Set.of(new EndTurn(), new Mortgage(oldKent), new Mortgage(kingsCross), new BuildHouse(pallMall),
                new BuildHouse(northumberland), new SellBuilding(whitehall)), actionSet(fm, state));
        // once the buildings are gone, the group can be mortgaged
        state.setBuildings(whitehall, 0);
        assertTrue(state.canMortgage(pallMall));
        assertTrue(state.canMortgage(whitehall));
    }

    @Test
    public void aHotelInTheGroupAlsoPreventsMortgaging() {
        give(state, 0, parkLane, mayfair);
        state.setBuildings(parkLane, 4);
        state.setBuildings(mayfair, MonopolyParameters.HOTEL);
        assertFalse(state.canMortgage(parkLane));
        assertFalse(state.canMortgage(mayfair));
        // no Mortgage: only building and selling
        assertEquals(Set.of(new EndTurn(), new BuildHouse(parkLane), new SellBuilding(mayfair)), actionSet(fm, state));
    }

    @Test
    public void aMortgagedPropertyCannotBeMortgagedAgain() {
        give(state, 0, kingsCross);
        state.setMortgaged(kingsCross, true);
        assertFalse(state.canMortgage(kingsCross));
        state.setMortgaged(kingsCross, false);
        assertTrue(state.canMortgage(kingsCross));
    }

    // ---- mortgaging and unmortgaging ----

    @Test
    public void mortgagingPaysTheMortgageValueAndKeepsTheTurnInManage() {
        give(state, 0, parkLane);
        fm.next(state, new Mortgage(parkLane));
        assertTrue(state.isMortgaged(parkLane));
        assertEquals(0, state.getOwner(parkLane));
        assertEquals(1500 + 175, state.getCash(0));
        assertEquals(1500, state.getCash(1));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn(), new Unmortgage(parkLane)), actionSet(fm, state));
    }

    @Test
    public void unmortgagingParkLaneCosts193() {
        give(state, 0, parkLane);
        state.setMortgaged(parkLane, true);
        fm.next(state, new Unmortgage(parkLane));
        assertFalse(state.isMortgaged(parkLane));
        assertEquals(0, state.getOwner(parkLane));
        assertEquals(1500 - (175 + 18), state.getCash(0));
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn(), new Mortgage(parkLane)), actionSet(fm, state));
    }

    @Test
    public void mortgagingAndUnmortgagingCostTheInterest() {
        give(state, 0, electric);
        play(state, fm, new Mortgage(electric), new Unmortgage(electric));
        assertEquals(1500 + 75 - (75 + 8), state.getCash(0));
        assertFalse(state.isMortgaged(electric));
        // and the turn can still end
        play(state, fm, new EndTurn());
        assertEquals(1, state.getCurrentPlayer());
    }

    // ---- raisable value ----

    /**
     * Player 0: cash 100; Brown with a house on each street; Dark Blue with 4 houses on Park Lane and a hotel on
     * Mayfair; Electric Company mortgaged; King's Cross Station. Player 1 holds Bond Street, which is not counted.
     */
    private void arrangeRaisable() {
        state.setCash(0, 100);
        give(state, 0, oldKent, whitechapel, parkLane, mayfair, electric, kingsCross);
        state.setBuildings(oldKent, 1);
        state.setBuildings(whitechapel, 1);
        state.setBuildings(parkLane, 4);
        state.setBuildings(mayfair, MonopolyParameters.HOTEL);
        state.setMortgaged(electric, true);
        give(state, 1, sq("Bond Street"));
    }

    @Test
    public void theRaisableValueIsCashPlusBuildingsAtHalfCostPlusMortgageValues() {
        arrangeRaisable();
        int buildings = 1 * 50 / 2 /* Old Kent Road */ + 1 * 50 / 2 /* Whitechapel Road */
                + 4 * 200 / 2 /* Park Lane */ + 5 * 200 / 2 /* Mayfair hotel = 5 houses */; // 25 + 25 + 400 + 500
        int mortgages = 30 + 30 + 175 + 200 + 100; // Electric Company is already mortgaged: nothing
        assertEquals(100 + buildings + mortgages, state.getRaisableValue(0)); // 100 + 950 + 535 = 1585
        assertEquals(1500 + 160, state.getRaisableValue(1)); // Bond Street's mortgage
    }

    @Test
    public void buildingValuesComeFromBuildingSalePercentRoundedDownPerBuilding() {
        params.setParameterValue("buildingSalePercent", 33);
        arrangeRaisable();
        // per building: Brown 50 x 33 / 100 = 16 (16.5 down) each; Park Lane 4 x 66; Mayfair 5 x 66 (200 x 33 / 100)
        // (rounding the total, 1900 x 33 / 100, would give 627, not 626)
        int buildings = 16 + 16 + 4 * 66 + 5 * 66;
        assertEquals(100 + buildings + (30 + 30 + 175 + 200 + 100), state.getRaisableValue(0)); // 1261
    }

    @Test
    public void eachBuildingIsValuedAtItsOwnRoundedSaleValue() {
        params.setParameterValue("buildingSalePercent", 33);
        state.setCash(0, 0);
        give(state, 0, oldKent, whitechapel);
        state.setBuildings(oldKent, 2);
        state.setBuildings(whitechapel, 2);
        // each house sells for 16 (16.5 down), so 2 x 16 = 32 a street - not 2 x 50 x 33 / 100 = 33
        assertEquals(4 * 16 + 30 + 30, state.getRaisableValue(0));
    }

    @Test
    public void withNoPropertiesTheRaisableValueIsTheCash() {
        state.setCash(2, 42);
        assertEquals(42, state.getRaisableValue(2));
    }
}
