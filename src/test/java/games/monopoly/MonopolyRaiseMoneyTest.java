package games.monopoly;

import core.interfaces.IExtendedSequence;
import games.monopoly.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Debts beyond the cash: raising money by mortgaging and by selling buildings, bankruptcy when even that cannot cover
 * the debt, a creditor receiving mortgaged property, and the buildings of a bankrupt. Three players unless stated;
 * seed 7.
 */
public class MonopolyRaiseMoneyTest {

    MonopolyParameters params;
    MonopolyGameState state;
    MonopolyForwardModel fm;
    final MonopolySquare oldKent = sq("Old Kent Road"), whitechapel = sq("Whitechapel Road"),
            parkLane = sq("Park Lane"), mayfair = sq("Mayfair");

    @Before
    public void setup() {
        params = new MonopolyParameters();
        state = newState(3, 7, params);
        fm = new MonopolyForwardModel();
        startTurn(state, 0, sq("GO"));
    }

    // ---- raising money for a debt to the Bank ----

    /** Player 0: cash 100, Old Kent Road and Mayfair; rolls 1 + 3 from GO onto Income Tax (200). */
    private void taxWithinRaisableValue() {
        state.setCash(0, 100);
        give(state, 0, oldKent, mayfair);
        // raisable: 100 + 30 + 200 = 330 >= 200
        roll(state, fm, 1, 3);
    }

    @Test
    public void aDebtBeyondTheCashButWithinTheRaisableValueStartsRaisingMoney() {
        taxWithinRaisableValue();
        assertEquals(new RaiseMoney(0, -1, 200), state.currentActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(state.isBankrupt(0));
        // nothing is paid yet
        assertEquals(100, state.getCash(0));
        // only mortgages are offered: no EndTurn or RollDice
        assertEquals(Set.of(new Mortgage(oldKent), new Mortgage(mayfair)), actionSet(fm, state));
    }

    @Test
    public void raisingMoneyGoesOnUntilTheCashCoversTheDebtWhichIsThenPaidToTheBank() {
        taxWithinRaisableValue();
        fm.next(state, new Mortgage(oldKent));
        // 100 + 30 = 130 < 200: still raising
        assertEquals(100 + 30, state.getCash(0));
        assertEquals(new RaiseMoney(0, -1, 200), state.currentActionInProgress());
        assertEquals(Set.of(new Mortgage(mayfair)), actionSet(fm, state));
        fm.next(state, new Mortgage(mayfair));
        // 130 + 200 = 330 >= 200: paid at once
        assertEquals(100 + 30 + 200 - 200, state.getCash(0));
        assertEquals(1500 * 2, state.getCash(1) + state.getCash(2));
        assertTrue(state.isMortgaged(oldKent));
        assertTrue(state.isMortgaged(mayfair));
        assertFalse(state.isActionInProgress());
        // the turn goes on in MANAGE; Old Kent Road can be bought back (33 <= 130), Mayfair not (220 > 130)
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn(), new Unmortgage(oldKent)), actionSet(fm, state));
    }

    @Test
    public void aRaisableValueExactlyEqualToTheDebtIsEnough() {
        state.setCash(0, 170);
        give(state, 0, oldKent);
        roll(state, fm, 1, 3); // Income Tax 200 = 170 + 30
        assertEquals(new RaiseMoney(0, -1, 200), state.currentActionInProgress());
        fm.next(state, new Mortgage(oldKent));
        assertEquals(0, state.getCash(0));
        assertFalse(state.isBankrupt(0));
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
    }

    @Test
    public void aDebtBeyondTheRaisableValueIsBankruptcyAtOnce() {
        state.setCash(0, 169);
        give(state, 0, oldKent, whitechapel);
        state.setMortgaged(whitechapel, true);
        roll(state, fm, 1, 3); // Income Tax 200 > 169 + 30 (Whitechapel Road is mortgaged already)
        assertTrue(state.isBankrupt(0));
        assertEquals(3, state.getFinalPlace(0));
        assertEquals(0, state.getCash(0));
        // no raising money: the properties go to the Bank and are auctioned in board order; nobody bids
        assertEquals(oldKent, passAll(state, fm));
        assertEquals(whitechapel, passAll(state, fm));
        assertFalse(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
    }

    @Test
    public void aForcedJailFineCanBeRaised() {
        putInJail(state, 0, 2);
        state.setCash(0, 30);
        give(state, 0, oldKent);
        roll(state, fm, 4, 6); // third failed roll: fine 50 > 30, raisable 30 + 30 = 60; then 10 + 10 = Free Parking
        // the fine is raised before the move: the player then moves the 4 + 6 rolled
        assertEquals(new RaiseMoney(0, -1, 50, 4 + 6), state.currentActionInProgress());
        assertEquals(sq("Jail"), state.getPosition(0));
        assertEquals(Set.of(new Mortgage(oldKent)), actionSet(fm, state));
        fm.next(state, new Mortgage(oldKent));
        assertEquals(30 + 30 - 50, state.getCash(0));
        assertFalse(state.isInJail(0));
        assertEquals(sq("Free Parking"), state.getPosition(0));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        // Old Kent Road would cost 33 to unmortgage: more than the 10 left
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void aCardPaymentCanBeRaised() {
        state.setCash(0, 100);
        give(state, 0, oldKent, whitechapel);
        onTop(state, chance("Pay school fees"));
        roll(state, fm, 3, 4); // Chance 1: pay 150 > 100; raisable 100 + 30 + 30 = 160
        assertEquals(new RaiseMoney(0, -1, 150), state.currentActionInProgress());
        play(state, fm, new Mortgage(oldKent), new Mortgage(whitechapel));
        assertEquals(100 + 30 + 30 - 150, state.getCash(0));
        assertEquals(1500 * 2, state.getCash(1) + state.getCash(2));
        assertFalse(state.isActionInProgress());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
    }

    // ---- raising money for a debt to a player ----

    @Test
    public void rentRaisedByMortgagingIsPaidToTheOwner() {
        state.setCash(0, 10);
        give(state, 0, oldKent, whitechapel);
        give(state, 1, mayfair);
        state.setPosition(0, sq("Liverpool Street Station"));
        roll(state, fm, 1, 3); // 35 + 4 = Mayfair, rent 50 > 10; raisable 10 + 30 + 30 = 70
        assertEquals(new RaiseMoney(0, 1, 50), state.currentActionInProgress());
        assertEquals(Set.of(new Mortgage(oldKent), new Mortgage(whitechapel)), actionSet(fm, state));
        fm.next(state, new Mortgage(whitechapel));
        assertEquals(10 + 30, state.getCash(0));
        assertEquals(1500, state.getCash(1));
        fm.next(state, new Mortgage(oldKent));
        assertEquals(10 + 30 + 30 - 50, state.getCash(0));
        assertEquals(1500 + 50, state.getCash(1));
        assertEquals(1500, state.getCash(2));
        assertFalse(state.isActionInProgress());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state)); // 33 to unmortgage > 20
    }

    @Test
    public void aBirthdayPayerRaisingMoneyReturnsThePlayToTheDrawer() {
        startTurn(state, 0, sq("Water Works"));
        state.setCash(1, 5);
        give(state, 1, oldKent); // raisable 5 + 30 = 35 >= 10
        onTop(state, chest("It is your birthday"));
        roll(state, fm, 2, 3); // 28 + 5 = Community Chest 3
        assertEquals(new RaiseMoney(1, 0, 10), state.currentActionInProgress());
        // player 2 has paid; player 1 decides now, though it is player 0's turn
        assertEquals(1500 - 10, state.getCash(2));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getTurnOwner());
        assertEquals(Set.of(new Mortgage(oldKent)), actionSet(fm, state));
        fm.next(state, new Mortgage(oldKent));
        assertEquals(5 + 30 - 10, state.getCash(1));
        assertEquals(1500 + 2 * 10, state.getCash(0));
        assertTrue(state.isMortgaged(oldKent));
        // back to the drawer's MANAGE
        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void twoBirthdayPayersShortOfCashEachRaiseTheirOwnDebt() {
        startTurn(state, 0, sq("Water Works"));
        state.setCash(1, 5);
        give(state, 1, oldKent); // 5 + 30 >= 10
        state.setCash(2, 0);
        give(state, 2, whitechapel); // 0 + 30 >= 10
        onTop(state, chest("It is your birthday"));
        roll(state, fm, 2, 3); // Community Chest 3
        Set<IExtendedSequence> pending = new HashSet<>(state.getActionsInProgress());
        assertEquals(Set.of(new RaiseMoney(1, 0, 10), new RaiseMoney(2, 0, 10)), pending);
        // whichever is on top decides first; each may mortgage only their own property
        for (int i = 0; i < 2; i++) {
            RaiseMoney raise = (RaiseMoney) state.currentActionInProgress();
            assertEquals(raise.player, state.getCurrentPlayer());
            MonopolySquare own = raise.player == 1 ? oldKent : whitechapel;
            assertEquals(Set.of(new Mortgage(own)), actionSet(fm, state));
            fm.next(state, new Mortgage(own));
        }
        assertFalse(state.isActionInProgress());
        assertEquals(5 + 30 - 10, state.getCash(1));
        assertEquals(30 - 10, state.getCash(2));
        assertEquals(1500 + 2 * 10, state.getCash(0));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
    }

    // ---- full bankruptcy to a player: mortgaged property and the interest ----

    @Test
    public void aCreditorReceivingMortgagedPropertiesPaysTheInterestOnEach() {
        state.setCash(0, 10);
        give(state, 0, oldKent, whitechapel, parkLane);
        state.setMortgaged(oldKent, true);
        state.setMortgaged(parkLane, true);
        give(state, 1, mayfair);
        state.setPosition(0, sq("Liverpool Street Station"));
        roll(state, fm, 1, 3); // Mayfair, rent 50 > 10 + 30 (Whitechapel Road) = 40
        assertTrue(state.isBankrupt(0));
        assertEquals(3, state.getFinalPlace(0));
        for (MonopolySquare s : List.of(oldKent, whitechapel, parkLane))
            assertEquals(1, state.getOwner(s));
        // mortgaged ones stay mortgaged; interest 3 (Old Kent Road) + 18 (Park Lane), none on Whitechapel Road
        assertTrue(state.isMortgaged(oldKent));
        assertTrue(state.isMortgaged(parkLane));
        assertFalse(state.isMortgaged(whitechapel));
        assertEquals(1500 + 10 - 3 - 18, state.getCash(1));
        assertEquals(1500, state.getCash(2));
        assertFalse(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
    }

    @Test
    public void aCreditorShortOfCashForTheInterestRaisesItThenTheTurnPasses() {
        state.setCash(0, 10);
        give(state, 0, oldKent, parkLane);
        state.setMortgaged(oldKent, true);
        state.setMortgaged(parkLane, true);
        state.setCash(1, 0);
        give(state, 1, mayfair);
        state.setPosition(0, sq("Liverpool Street Station"));
        roll(state, fm, 1, 3); // Mayfair, rent 50 > 10 (all of player 0's property is mortgaged)
        assertTrue(state.isBankrupt(0));
        // player 1 has 0 + 10 and owes the interest as one payment, 3 (Old Kent Road) + 18 (Park Lane) = 21, which
        // must be raised (raisable 10 + 200 for Mayfair)
        assertEquals(new RaiseMoney(1, -1, 3 + 18), state.currentActionInProgress());
        assertEquals(10, state.getCash(1));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new Mortgage(mayfair)), actionSet(fm, state));
        fm.next(state, new Mortgage(mayfair));
        assertEquals(10 - 3 + 200 - 18, state.getCash(1));
        assertFalse(state.isActionInProgress());
        // player 0 is out, so the turn passes to player 1
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
        assertEquals(GAME_ONGOING, state.getGameStatus());
    }

    @Test
    public void aCreditorWhoCannotRaiseTheInterestIsBankruptToTheBank() {
        params.setParameterValue("mortgageInterestPercent", 100);
        state = newState(4, 7, params);
        startTurn(state, 0, sq("GO"));
        state.setCash(0, 3);
        give(state, 0, parkLane, mayfair);
        state.setMortgaged(parkLane, true);
        state.setMortgaged(mayfair, true);
        state.setCash(1, 0);
        give(state, 1, whitechapel);
        roll(state, fm, 1, 2); // GO + 3 = Whitechapel Road, rent 4 > 3
        // player 0 bankrupt to player 1, who gets 3 and owes interest 175 (Park Lane) and 200 (Mayfair) at 100%:
        // raisable 3 + 30 (Whitechapel Road) < 175, so player 1 is bankrupt to the Bank - once
        assertTrue(state.isBankrupt(0));
        assertTrue(state.isBankrupt(1));
        assertEquals(4, state.getFinalPlace(0));
        assertEquals(3, state.getFinalPlace(1));
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(0, state.getCash(1));
        // everything goes back to the Bank unmortgaged, auctioned in board order from player 2 (after player 1)
        for (MonopolySquare s : List.of(whitechapel, parkLane, mayfair)) {
            assertEquals(-1, state.getOwner(s));
            assertFalse(state.isMortgaged(s));
        }
        assertEquals(new Auction(state, whitechapel, 2), state.currentActionInProgress());
        assertEquals(whitechapel, passAll(state, fm));
        assertEquals(parkLane, passAll(state, fm));
        assertEquals(mayfair, passAll(state, fm));
        assertFalse(state.isActionInProgress());
        assertEquals(1500, state.getCash(2));
        assertEquals(1500, state.getCash(3));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
        assertEquals(GAME_ONGOING, state.getGameStatus());
    }

    // ---- raising money by selling buildings ----

    @Test
    public void raisingMoneySellsTheBuildingsBeforeTheirGroupCanBeMortgaged() {
        state.setCash(0, 10);
        give(state, 0, parkLane, mayfair);
        state.setBuildings(mayfair, 1);
        roll(state, fm, 1, 3); // Income Tax 200 > 10; raisable 10 + 100 (the house) + 175 + 200 = 485
        assertEquals(new RaiseMoney(0, -1, 200), state.currentActionInProgress());
        // Dark Blue can't be mortgaged while Mayfair has a house
        assertEquals(Set.of(new SellBuilding(mayfair)), actionSet(fm, state));
        fm.next(state, new SellBuilding(mayfair));
        assertEquals(0, state.getBuildings(mayfair));
        assertEquals(10 + 100, state.getCash(0));
        assertEquals(new RaiseMoney(0, -1, 200), state.currentActionInProgress());
        assertEquals(Set.of(new Mortgage(parkLane), new Mortgage(mayfair)), actionSet(fm, state));
        fm.next(state, new Mortgage(parkLane));
        // 110 + 175 = 285 >= 200: paid
        assertEquals(10 + 100 + 175 - 200, state.getCash(0));
        assertEquals(1500 * 2, state.getCash(1) + state.getCash(2));
        assertFalse(state.isActionInProgress());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        // 85 left: Park Lane costs 193 to lift, and no building on a group with a mortgage
        assertEquals(Set.of(new EndTurn(), new Mortgage(mayfair)), actionSet(fm, state));
    }

    @Test
    public void raisingMoneyEndsWhenASaleCoversTheDebt() {
        state.setCash(0, 150);
        give(state, 0, parkLane, mayfair);
        state.setBuildings(parkLane, 1);
        state.setBuildings(mayfair, 1);
        roll(state, fm, 1, 3); // Income Tax 200 > 150
        assertEquals(new RaiseMoney(0, -1, 200), state.currentActionInProgress());
        assertEquals(Set.of(new SellBuilding(parkLane), new SellBuilding(mayfair)), actionSet(fm, state));
        fm.next(state, new SellBuilding(parkLane));
        // 150 + 100 = 250 >= 200: paid at once
        assertEquals(150 + 100 - 200, state.getCash(0));
        assertEquals(0, state.getBuildings(parkLane));
        assertEquals(1, state.getBuildings(mayfair));
        assertFalse(state.isActionInProgress());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        // 50 left: not enough to build again (200)
        assertEquals(Set.of(new EndTurn(), new SellBuilding(mayfair)), actionSet(fm, state));
    }

    @Test
    public void rentRaisedBySellingHotelsIsPaidToTheOwner() {
        state.setCash(0, 20);
        give(state, 0, oldKent, whitechapel);
        state.setBuildings(oldKent, MonopolyParameters.HOTEL);
        state.setBuildings(whitechapel, MonopolyParameters.HOTEL);
        give(state, 1, mayfair);
        state.setPosition(0, sq("Liverpool Street Station"));
        roll(state, fm, 1, 3); // Mayfair (bare, Park Lane with the Bank): rent 50 > 20
        assertEquals(new RaiseMoney(0, 1, 50), state.currentActionInProgress());
        assertEquals(Set.of(new SellBuilding(oldKent), new SellBuilding(whitechapel)), actionSet(fm, state));
        fm.next(state, new SellBuilding(oldKent));
        // the hotel goes back to 4 houses for 25: 45 < 50; Whitechapel Road's hotel must go next
        assertEquals(4, state.getBuildings(oldKent));
        assertEquals(20 + 25, state.getCash(0));
        assertEquals(Set.of(new SellBuilding(whitechapel)), actionSet(fm, state));
        fm.next(state, new SellBuilding(whitechapel));
        assertEquals(4, state.getBuildings(whitechapel));
        assertEquals(20 + 25 + 25 - 50, state.getCash(0));
        assertEquals(1500 + 50, state.getCash(1));
        assertFalse(state.isActionInProgress());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
    }

    // ---- bankruptcy with buildings ----

    @Test
    public void bankruptToAPlayerTheBuildingsAreSoldAndTheCreditorGetsTheMoney() {
        state.setCash(0, 10);
        give(state, 0, oldKent, whitechapel);
        state.setBuildings(oldKent, 2);
        state.setBuildings(whitechapel, 1);
        give(state, 1, parkLane, mayfair);
        state.setBuildings(parkLane, 3);
        state.setBuildings(mayfair, 3);
        state.setPosition(0, sq("Liverpool Street Station"));
        roll(state, fm, 1, 3); // Mayfair with 3 houses: rent 1400 > 10 + 3 x 25 + 30 + 30 = 145
        assertTrue(state.isBankrupt(0));
        assertEquals(0, state.getCash(0));
        // the 3 houses are sold to the Bank at 25 each, and that money goes to the creditor with the cash
        assertEquals(1500 + 10 + 3 * 25, state.getCash(1));
        assertEquals(1500, state.getCash(2));
        for (MonopolySquare s : List.of(oldKent, whitechapel)) {
            assertEquals(1, state.getOwner(s));
            assertEquals(0, state.getBuildings(s));
            assertFalse(state.isMortgaged(s));
        }
        // the creditor's own buildings stay
        assertEquals(3, state.getBuildings(mayfair));
        assertFalse(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
    }

    @Test
    public void bankruptToTheBankTheBuildingsAreRemoved() {
        state.setCash(0, 10);
        give(state, 0, oldKent, whitechapel);
        state.setBuildings(oldKent, 1);
        state.setBuildings(whitechapel, 1);
        roll(state, fm, 1, 3); // Income Tax 200 > 10 + 2 x 25 + 30 + 30 = 120
        assertTrue(state.isBankrupt(0));
        assertEquals(0, state.getCash(0));
        // nobody gets the money for the buildings
        assertEquals(1500, state.getCash(1));
        assertEquals(1500, state.getCash(2));
        for (MonopolySquare s : List.of(oldKent, whitechapel)) {
            assertEquals(-1, state.getOwner(s));
            assertEquals(0, state.getBuildings(s));
        }
        assertEquals(oldKent, passAll(state, fm));
        assertEquals(whitechapel, passAll(state, fm));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
    }

    // ---- copy and equality ----

    @Test
    public void raiseMoneyEqualsByValue() {
        RaiseMoney r = new RaiseMoney(0, -1, 200);
        assertEquals(r, new RaiseMoney(0, -1, 200));
        assertEquals(r.hashCode(), new RaiseMoney(0, -1, 200).hashCode());
        assertNotEquals(r, new RaiseMoney(1, -1, 200));
        assertNotEquals(r, new RaiseMoney(0, 2, 200));
        assertNotEquals(r, new RaiseMoney(0, -1, 199));
    }

    @Test
    public void aCopyWhileRaisingMoneyIsIndependent() {
        taxWithinRaisableValue();
        MonopolyGameState copy = (MonopolyGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertNotSame(state.currentActionInProgress(), copy.currentActionInProgress());
        play(copy, fm, new Mortgage(oldKent), new Mortgage(mayfair));
        assertFalse(copy.isActionInProgress());
        assertEquals(100 + 30 + 200 - 200, copy.getCash(0));
        // the original is untouched
        assertEquals(new RaiseMoney(0, -1, 200), state.currentActionInProgress());
        assertEquals(100, state.getCash(0));
        assertFalse(state.isMortgaged(oldKent));
        assertEquals(Set.of(new Mortgage(oldKent), new Mortgage(mayfair)), actionSet(fm, state));
        // the same moves on another copy give an equal state
        MonopolyGameState again = (MonopolyGameState) state.copy();
        play(again, fm, new Mortgage(oldKent), new Mortgage(mayfair));
        assertEquals(copy, again);
    }
}
