package games.monopoly;

import games.monopoly.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static games.monopoly.MonopolyParameters.HOTEL;
import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Building and selling houses and hotels. Three players; player 0 is in MANAGE at Free Parking with no further roll,
 * and has cash 1500 unless stated.
 */
public class MonopolyBuildingTest {

    MonopolyParameters params;
    MonopolyGameState state;
    MonopolyForwardModel fm;
    final MonopolySquare oldKent = sq("Old Kent Road"), whitechapel = sq("Whitechapel Road"),
            angel = sq("The Angel Islington"), euston = sq("Euston Road"), pentonville = sq("Pentonville Road"),
            pallMall = sq("Pall Mall"), whitehall = sq("Whitehall"), northumberland = sq("Northumberland Avenue"),
            strand = sq("Strand"), parkLane = sq("Park Lane"), mayfair = sq("Mayfair"),
            kingsCross = sq("King's Cross Station");

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

    /** Sets the buildings on each street, in the order given. */
    private void build(MonopolySquare[] streets, int... n) {
        for (int i = 0; i < streets.length; i++)
            state.setBuildings(streets[i], n[i]);
    }

    private MonopolySquare[] lightBlue() {
        return new MonopolySquare[]{angel, euston, pentonville};
    }

    // ---- the sale value ----

    @Test
    public void aBuildingSellsForHalfItsHouseCost() {
        assertEquals(50 / 2, params.buildingSaleValue(oldKent));
        assertEquals(100 / 2, params.buildingSaleValue(pallMall));
        assertEquals(150 / 2, params.buildingSaleValue(strand));
        assertEquals(200 / 2, params.buildingSaleValue(mayfair));
    }

    @Test
    public void theSaleValueComesFromBuildingSalePercentRoundedDown() {
        params.setParameterValue("buildingSalePercent", 33);
        assertEquals(16, params.buildingSaleValue(oldKent)); // 50 x 33 / 100 = 16.5 -> 16
        assertEquals(49, params.buildingSaleValue(strand)); // 150 x 33 / 100 = 49.5 -> 49
        assertEquals(66, params.buildingSaleValue(mayfair)); // 200 x 33 / 100 = 66
        params.setParameterValue("buildingSalePercent", 100);
        assertEquals(200, params.buildingSaleValue(mayfair));
    }

    // ---- when building is allowed ----

    @Test
    public void manageOffersBuildHouseOnEachStreetOfAWholeGroupOnly() {
        // Brown is whole; Pink is not (Northumberland Avenue is player 1's); a station is never built on
        give(state, 0, oldKent, whitechapel, pallMall, whitehall, kingsCross);
        give(state, 1, northumberland, parkLane, mayfair);
        state.setBuildings(mayfair, 1); // player 1's houses are not player 0's to sell
        assertTrue(state.canBuild(oldKent));
        assertTrue(state.canBuild(whitechapel));
        assertFalse(state.canBuild(pallMall));
        assertFalse(state.canBuild(whitehall));
        assertFalse(state.canBuild(kingsCross));
        assertEquals(Set.of(new EndTurn(), new Mortgage(oldKent), new Mortgage(whitechapel), new Mortgage(pallMall),
                new Mortgage(whitehall), new Mortgage(kingsCross), new BuildHouse(oldKent), new BuildHouse(whitechapel)),
                actionSet(fm, state));
    }

    @Test
    public void noBuildingOnAGroupWithAStreetStillWithTheBank() {
        give(state, 0, pallMall, whitehall);
        assertFalse(state.canBuild(pallMall));
        assertEquals(Set.of(new EndTurn(), new Mortgage(pallMall), new Mortgage(whitehall)), actionSet(fm, state));
    }

    @Test
    public void noBuildingWhileAStreetOfTheGroupIsMortgaged() {
        give(state, 0, pallMall, whitehall, northumberland);
        state.setMortgaged(northumberland, true);
        assertFalse(state.canBuild(pallMall));
        assertFalse(state.canBuild(whitehall));
        assertFalse(state.canBuild(northumberland));
        assertEquals(Set.of(new EndTurn(), new Mortgage(pallMall), new Mortgage(whitehall),
                new Unmortgage(northumberland)), actionSet(fm, state));
        // lifting the mortgage (80 + 8) allows building on all three
        fm.next(state, new Unmortgage(northumberland));
        assertEquals(1500 - 88, state.getCash(0));
        assertEquals(Set.of(new EndTurn(), new Mortgage(pallMall), new Mortgage(whitehall),
                new Mortgage(northumberland), new BuildHouse(pallMall), new BuildHouse(whitehall),
                new BuildHouse(northumberland)), actionSet(fm, state));
    }

    @Test
    public void buildingNeedsTheHouseCostInCash() {
        give(state, 0, parkLane, mayfair);
        state.setCash(0, 200 - 1);
        assertFalse(state.canBuild(parkLane));
        assertFalse(state.canBuild(mayfair));
        assertEquals(Set.of(new EndTurn(), new Mortgage(parkLane), new Mortgage(mayfair)), actionSet(fm, state));
        state.setCash(0, 200);
        assertTrue(state.canBuild(parkLane));
        assertTrue(state.canBuild(mayfair));
    }

    @Test
    public void withAnotherRollDueManageOffersRollDiceAlongsideBuilding() {
        manage(true);
        give(state, 0, oldKent, whitechapel);
        build(new MonopolySquare[]{oldKent, whitechapel}, 1, 0);
        assertEquals(Set.of(new RollDice(), new BuildHouse(whitechapel), new SellBuilding(oldKent)),
                actionSet(fm, state));
    }

    @Test
    public void noBuildingOrSellingInTheRollPhase() {
        give(state, 0, oldKent, whitechapel, parkLane, mayfair);
        build(new MonopolySquare[]{parkLane, mayfair}, 1, 1);
        startTurn(state, 0, sq("GO"));
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
    }

    // ---- building ----

    @Test
    public void buildingAHousePaysTheHouseCostToTheBank() {
        give(state, 0, parkLane, mayfair);
        fm.next(state, new BuildHouse(mayfair));
        assertEquals(1, state.getBuildings(mayfair));
        assertEquals(0, state.getBuildings(parkLane));
        assertEquals(1500 - 200, state.getCash(0));
        assertEquals(1500, state.getCash(1));
        assertEquals(1500, state.getCash(2));
        // the turn goes on in MANAGE; Mayfair now has more than Park Lane, and the group can't be mortgaged
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertFalse(state.canBuild(mayfair));
        assertEquals(Set.of(new EndTurn(), new BuildHouse(parkLane), new SellBuilding(mayfair)), actionSet(fm, state));
    }

    @Test
    public void housesAreBuiltEvenlyAroundTheGroup() {
        give(state, 0, lightBlue());
        play(state, fm, new BuildHouse(angel));
        // a second house on The Angel Islington must wait for one on each of the others
        assertFalse(state.canBuild(angel));
        play(state, fm, new BuildHouse(euston));
        assertFalse(state.canBuild(angel));
        assertFalse(state.canBuild(euston));
        play(state, fm, new BuildHouse(pentonville), new BuildHouse(angel));
        assertEquals(2, state.getBuildings(angel));
        assertEquals(1, state.getBuildings(euston));
        assertEquals(1, state.getBuildings(pentonville));
        assertEquals(1500 - 4 * 50, state.getCash(0));
    }

    @Test
    public void noThirdHouseOnAStreetWhileAnotherOfTheGroupHasOne() {
        // rulebook p.6: you cannot put three houses on one street while another of its group has only one
        give(state, 0, lightBlue());
        build(lightBlue(), 2, 1, 2);
        assertFalse(state.canBuild(angel));
        assertTrue(state.canBuild(euston));
        assertFalse(state.canBuild(pentonville));
        assertEquals(Set.of(new EndTurn(), new BuildHouse(euston), new SellBuilding(angel),
                new SellBuilding(pentonville)), actionSet(fm, state));
    }

    @Test
    public void aHotelOnlyWhenEveryStreetOfTheGroupHasFourHouses() {
        give(state, 0, parkLane, mayfair);
        build(new MonopolySquare[]{parkLane, mayfair}, 4, 3);
        state.setCash(0, 1000);
        assertFalse(state.canBuild(parkLane));
        play(state, fm, new BuildHouse(mayfair));
        assertEquals(4, state.getBuildings(mayfair));
        // now both have 4 houses: either may have a hotel
        assertTrue(state.canBuild(parkLane));
        assertTrue(state.canBuild(mayfair));
        play(state, fm, new BuildHouse(parkLane));
        assertEquals(HOTEL, state.getBuildings(parkLane));
        assertEquals(1000 - 200 - 200, state.getCash(0));
        // no more on Park Lane; Mayfair can have its hotel
        assertFalse(state.canBuild(parkLane));
        assertEquals(Set.of(new EndTurn(), new BuildHouse(mayfair), new SellBuilding(parkLane)), actionSet(fm, state));
        play(state, fm, new BuildHouse(mayfair));
        assertEquals(HOTEL, state.getBuildings(mayfair));
        assertEquals(1000 - 3 * 200, state.getCash(0));
        // nothing beyond a hotel
        assertEquals(Set.of(new EndTurn(), new SellBuilding(parkLane), new SellBuilding(mayfair)), actionSet(fm, state));
    }

    @Test
    public void buildingIsNotLimitedByTheRulebooksThirtyTwoHousesAndTwelveHotels() {
        List<MonopolySquare> streets = state.getBoard().squares(SquareType.STREET);
        assertEquals(22, streets.size());
        give(state, 0, streets.toArray(new MonopolySquare[0]));
        // 22 x 4 = 88 houses in play, beyond the rulebook's 32
        for (MonopolySquare s : streets)
            state.setBuildings(s, 4);
        state.setCash(0, 10000);
        for (MonopolySquare s : streets)
            assertTrue(s.name(), state.canBuild(s));
        for (MonopolySquare s : streets)
            play(state, fm, new BuildHouse(s));
        // 22 hotels, beyond the rulebook's 12
        for (MonopolySquare s : streets)
            assertEquals(s.name(), HOTEL, state.getBuildings(s));
        // house costs: Brown 2 x 50, Light Blue 3 x 50, Pink 3 x 100, Orange 3 x 100, Red 3 x 150, Yellow 3 x 150,
        // Green 3 x 200, Dark Blue 2 x 200 = 2750
        assertEquals(10000 - (100 + 150 + 300 + 300 + 450 + 450 + 600 + 400), state.getCash(0));
    }

    // ---- selling ----

    @Test
    public void sellingAHousePaysHalfItsCostAndKeepsTheTurnInManage() {
        give(state, 0, oldKent, whitechapel);
        build(new MonopolySquare[]{oldKent, whitechapel}, 2, 2);
        fm.next(state, new SellBuilding(oldKent));
        assertEquals(1, state.getBuildings(oldKent));
        assertEquals(2, state.getBuildings(whitechapel));
        assertEquals(1500 + 25, state.getCash(0));
        assertEquals(1500, state.getCash(1));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        // Whitechapel Road must lose one before Old Kent Road can lose another
        assertFalse(state.canSellBuilding(oldKent));
        assertTrue(state.canSellBuilding(whitechapel));
        assertEquals(Set.of(new EndTurn(), new BuildHouse(oldKent), new SellBuilding(whitechapel)),
                actionSet(fm, state));
    }

    @Test
    public void buildingsAreSoldEvenlyInReverse() {
        give(state, 0, lightBlue());
        build(lightBlue(), 3, 3, 2);
        assertTrue(state.canSellBuilding(angel));
        assertTrue(state.canSellBuilding(euston));
        assertFalse(state.canSellBuilding(pentonville));
        assertEquals(Set.of(new EndTurn(), new BuildHouse(pentonville), new SellBuilding(angel),
                new SellBuilding(euston)), actionSet(fm, state));
        // sell all 8, always from a street with the most: (3,3,2) -> (3,2,2) -> (2,2,2) -> (1,2,2) -> (1,1,2)
        // -> (1,1,1) -> (1,0,1)
        play(state, fm, new SellBuilding(euston), new SellBuilding(angel), new SellBuilding(angel),
                new SellBuilding(euston), new SellBuilding(pentonville), new SellBuilding(euston));
        assertFalse(state.canSellBuilding(euston));
        // -> (1,0,0) -> (0,0,0)
        play(state, fm, new SellBuilding(pentonville), new SellBuilding(angel));
        assertEquals(1500 + 8 * 25, state.getCash(0));
        for (MonopolySquare s : lightBlue())
            assertEquals(0, state.getBuildings(s));
        // with the buildings gone the group may be mortgaged again
        assertEquals(Set.of(new EndTurn(), new Mortgage(angel), new Mortgage(euston), new Mortgage(pentonville),
                new BuildHouse(angel), new BuildHouse(euston), new BuildHouse(pentonville)), actionSet(fm, state));
    }

    @Test
    public void sellingAHotelLeavesFourHouses() {
        give(state, 0, parkLane, mayfair);
        build(new MonopolySquare[]{parkLane, mayfair}, HOTEL, HOTEL);
        fm.next(state, new SellBuilding(mayfair));
        assertEquals(4, state.getBuildings(mayfair));
        assertEquals(HOTEL, state.getBuildings(parkLane));
        assertEquals(1500 + 100, state.getCash(0)); // one building at half of 200
        // the hotel on Park Lane must go next; Mayfair may have its hotel back
        assertFalse(state.canSellBuilding(mayfair));
        assertTrue(state.canSellBuilding(parkLane));
        assertEquals(Set.of(new EndTurn(), new BuildHouse(mayfair), new SellBuilding(parkLane)), actionSet(fm, state));
    }

    @Test
    public void theSalePriceComesFromBuildingSalePercent() {
        params.setParameterValue("buildingSalePercent", 100);
        give(state, 0, parkLane, mayfair);
        build(new MonopolySquare[]{parkLane, mayfair}, 1, 1);
        fm.next(state, new SellBuilding(mayfair));
        assertEquals(1500 + 200, state.getCash(0));
    }

    @Test
    public void theSalePriceIsRoundedDownPerBuilding() {
        params.setParameterValue("buildingSalePercent", 33);
        give(state, 0, oldKent, whitechapel);
        build(new MonopolySquare[]{oldKent, whitechapel}, 1, 1);
        play(state, fm, new SellBuilding(oldKent), new SellBuilding(whitechapel));
        assertEquals(1500 + 16 + 16, state.getCash(0)); // 50 x 33 / 100 = 16.5 -> 16 each
    }

    @Test
    public void buildingThenSellingAHouseCostsHalfItsPrice() {
        give(state, 0, pallMall, whitehall, northumberland);
        play(state, fm, new BuildHouse(whitehall), new SellBuilding(whitehall));
        assertEquals(1500 - 100 + 50, state.getCash(0));
        assertEquals(0, state.getBuildings(whitehall));
    }
}
