package games.monopoly;

import org.junit.Before;
import org.junit.Test;

import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.assertEquals;

/**
 * MonopolyGameState.getRent: the rent charged on landing, as a pure query. Rents from data/monopoly/ukBoard.json.
 */
public class MonopolyRentTest {

    MonopolyGameState state;
    final MonopolySquare oldKent = sq("Old Kent Road"), whitechapel = sq("Whitechapel Road");
    final MonopolySquare pallMall = sq("Pall Mall"), whitehall = sq("Whitehall"),
            northumberland = sq("Northumberland Avenue");
    final MonopolySquare kingsCross = sq("King's Cross Station"), marylebone = sq("Marylebone Station"),
            fenchurch = sq("Fenchurch Street Station"), liverpool = sq("Liverpool Street Station");
    final MonopolySquare electric = sq("Electric Company"), water = sq("Water Works");

    @Before
    public void setup() {
        state = newState(3, 7, null);
    }

    @Test
    public void aStreetChargesItsBaseRentAndTheBankChargesNothing() {
        assertEquals(0, state.getRent(oldKent)); // Bank-owned
        give(state, 1, oldKent);
        assertEquals(2, state.getRent(oldKent));
        give(state, 2, whitechapel);
        // the group is split between two players: no doubling
        assertEquals(2, state.getRent(oldKent));
        assertEquals(4, state.getRent(whitechapel));
    }

    @Test
    public void theUnimprovedRentDoublesWhenTheOwnerHoldsTheWholeGroup() {
        give(state, 1, oldKent, whitechapel);
        assertEquals(2 * 2, state.getRent(oldKent));
        assertEquals(2 * 4, state.getRent(whitechapel));

        give(state, 1, pallMall, whitehall);
        // two of the three Pink streets: not doubled
        assertEquals(10, state.getRent(pallMall));
        give(state, 1, northumberland);
        assertEquals(2 * 10, state.getRent(pallMall));
        assertEquals(2 * 12, state.getRent(northumberland));
    }

    @Test
    public void theGroupStillDoublesTheRentWhenAnotherStreetInItIsMortgaged() {
        // rulebook p.4
        give(state, 1, oldKent, whitechapel);
        state.setMortgaged(whitechapel, true);
        assertEquals(2 * 2, state.getRent(oldKent));
    }

    @Test
    public void aMortgagedPropertyChargesNoRent() {
        give(state, 1, oldKent, whitechapel, kingsCross, electric);
        setDice(state, 3, 4);
        state.setMortgaged(whitechapel, true);
        state.setMortgaged(kingsCross, true);
        state.setMortgaged(electric, true);
        assertEquals(0, state.getRent(whitechapel));
        assertEquals(0, state.getRent(kingsCross));
        assertEquals(0, state.getRent(electric));
    }

    @Test
    public void stationRentDependsOnTheNumberOfStationsOwned() {
        give(state, 2, kingsCross);
        assertEquals(25, state.getRent(kingsCross));
        give(state, 2, marylebone);
        assertEquals(50, state.getRent(kingsCross));
        assertEquals(50, state.getRent(marylebone));
        give(state, 2, fenchurch);
        assertEquals(100, state.getRent(fenchurch));
        give(state, 2, liverpool);
        assertEquals(200, state.getRent(kingsCross));
        // another player's station does not count
        give(state, 1, liverpool);
        assertEquals(100, state.getRent(kingsCross));
        assertEquals(25, state.getRent(liverpool));
    }

    @Test
    public void mortgagedStationsStillCountForTheOwnersOtherStations() {
        // the deed counts the stations owned
        give(state, 2, kingsCross, marylebone, fenchurch, liverpool);
        state.setMortgaged(liverpool, true);
        assertEquals(200, state.getRent(kingsCross));
    }

    @Test
    public void utilityRentIsAMultipleOfTheDice() {
        setDice(state, 3, 4);
        give(state, 1, electric);
        assertEquals(4 * (3 + 4), state.getRent(electric));
        give(state, 1, water);
        assertEquals(10 * (3 + 4), state.getRent(electric));
        assertEquals(10 * (3 + 4), state.getRent(water));
        setDice(state, 6, 6);
        assertEquals(10 * (6 + 6), state.getRent(water));
        // a mortgaged utility still counts for the other
        state.setMortgaged(water, true);
        assertEquals(10 * (6 + 6), state.getRent(electric));
    }

    // ---- rent with houses and hotels ----

    @Test
    public void rentWithOneToFourHousesAndAHotel() {
        MonopolySquare parkLane = sq("Park Lane"), mayfair = sq("Mayfair");
        give(state, 1, parkLane, mayfair);
        int[] expected = {200, 600, 1400, 1700, 2000}; // Mayfair: 1-4 houses, then a hotel
        for (int n = 1; n <= MonopolyParameters.HOTEL; n++) {
            state.setBuildings(parkLane, n);
            state.setBuildings(mayfair, n);
            assertEquals(n + " buildings", expected[n - 1], state.getRent(mayfair));
        }
        // Park Lane 4 houses, Mayfair a hotel
        state.setBuildings(parkLane, 4);
        assertEquals(1300, state.getRent(parkLane));
        assertEquals(2000, state.getRent(mayfair));
    }

    @Test
    public void aStreetWithHousesIsNotDoubled() {
        give(state, 1, oldKent, whitechapel);
        state.setBuildings(oldKent, 1);
        state.setBuildings(whitechapel, 1);
        assertEquals(10, state.getRent(oldKent));
        assertEquals(20, state.getRent(whitechapel));
    }

    @Test
    public void aBareStreetOfAWholeGroupIsStillDoubledWhenAnotherHasBuildings() {
        // rulebook p.4: double rent on the unimproved streets of a complete colour group
        give(state, 1, pallMall, whitehall, northumberland);
        state.setBuildings(whitehall, 1);
        assertEquals(2 * 10, state.getRent(pallMall));
        assertEquals(50, state.getRent(whitehall));
        assertEquals(2 * 12, state.getRent(northumberland));
    }
}
