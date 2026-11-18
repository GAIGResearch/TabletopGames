package games.monopoly;

import core.CoreConstants;
import core.components.Deck;
import games.monopoly.actions.*;
import games.monopoly.components.MonopolyCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Landing on Chance and Community Chest: drawing the top card, and each kind of card's effect (the UK cards of
 * data/monopoly/ukBoard.json; GO salary 200). Get Out of Jail Free cards are in MonopolyJailCardTest.
 * Chance squares: 7, 22, 36; Community Chest: 2, 17, 33.
 */
public class MonopolyCardTest {

    MonopolyParameters params;
    MonopolyGameState state;
    MonopolyForwardModel fm;

    @Before
    public void setup() {
        params = new MonopolyParameters();
        state = newState(3, 7, params);
        fm = new MonopolyForwardModel();
    }

    // ---- drawing ----

    @Test
    public void drawingTakesTheTopCardAndPutsItAtTheBottom() {
        onTop(state, chance("Speeding fine"), chance("Bank pays you dividend"));
        assertEquals(chance("Speeding fine"), state.drawCard(MonopolyCard.Pile.CHANCE));
        Deck<MonopolyCard> deck = state.getChanceDeck();
        assertEquals(16, deck.getSize());
        assertEquals(chance("Bank pays you dividend"), deck.peek());
        assertEquals(chance("Speeding fine"), bottom(deck));
    }

    @Test
    public void drawingAGetOutOfJailFreeCardTakesItOutOfThePile() {
        onTop(state, chest("Get out of Jail"));
        assertEquals(chest("Get out of Jail"), state.drawCard(MonopolyCard.Pile.COMMUNITY_CHEST));
        assertEquals(15, state.getCommunityChestDeck().getSize());
        assertFalse(state.getCommunityChestDeck().contains(chest("Get out of Jail")));
    }

    @Test
    public void landingOnChanceDrawsAndCarriesOutTheTopCard() {
        startTurn(state, 0, sq("GO"));
        onTop(state, chance("Bank pays you dividend"), chance("Speeding fine"));
        List<MonopolyCard> chestBefore = new ArrayList<>(state.getCommunityChestDeck().getComponents());
        roll(state, fm, 3, 4); // GO + 7 = Chance 1
        assertEquals(1500 + 50, state.getCash(0));
        assertEquals(chance("Speeding fine"), state.getChanceDeck().peek());
        assertEquals(chance("Bank pays you dividend"), bottom(state.getChanceDeck()));
        assertEquals(16, state.getChanceDeck().getSize());
        assertEquals(chestBefore, state.getCommunityChestDeck().getComponents());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void landingOnCommunityChestDrawsFromItsOwnPile() {
        startTurn(state, 0, sq("Water Works"));
        onTop(state, chest("Bank error"));
        List<MonopolyCard> chanceBefore = new ArrayList<>(state.getChanceDeck().getComponents());
        roll(state, fm, 2, 3); // 28 + 5 = Community Chest 3
        assertEquals(1500 + 200, state.getCash(0));
        assertEquals(chest("Bank error"), bottom(state.getCommunityChestDeck()));
        assertEquals(chanceBefore, state.getChanceDeck().getComponents());
    }

    // ---- COLLECT / PAY ----

    @Test
    public void aPayCardPaysTheBank() {
        startTurn(state, 0, sq("GO"));
        onTop(state, chance("Pay school fees"));
        roll(state, fm, 3, 4); // Chance 1
        assertEquals(1500 - 150, state.getCash(0));
        assertEquals(3 * 1500 - 150, totalCash(state));
    }

    @Test
    public void aCardPaymentBeyondTheCashIsBankruptcyToTheBank() {
        startTurn(state, 0, sq("GO"));
        state.setCash(0, 100);
        // cash 100 + Old Kent Road's mortgage 30 = 130 < 150, so no raising money could cover it
        give(state, 0, sq("Old Kent Road"));
        onTop(state, chance("Pay school fees"));
        roll(state, fm, 3, 4); // Chance 1
        assertTrue(state.isBankrupt(0));
        assertEquals(3, state.getFinalPlace(0));
        assertEquals(0, state.getCash(0));
        assertEquals(-1, state.getOwner(sq("Old Kent Road")));
        // Old Kent Road is auctioned (MonopolyAuctionTest); nobody bids
        assertEquals(sq("Old Kent Road"), passAll(state, fm));
        assertEquals(-1, state.getOwner(sq("Old Kent Road")));
        assertEquals(1500, state.getCash(1));
        assertEquals(1500, state.getCash(2));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
    }

    // ---- ADVANCE ----

    @Test
    public void advanceToTrafalgarSquareFromChance3PassesGoAndTheSquareCanBeBought() {
        startTurn(state, 0, sq("Regent Street"));
        onTop(state, chance("Advance to Trafalgar Square"));
        roll(state, fm, 1, 4); // 31 + 5 = Chance 3 (36), forward to 24 past GO
        assertEquals(sq("Trafalgar Square"), state.getPosition(0));
        assertEquals(1500 + 200, state.getCash(0));
        assertEquals(new BuyDecision(0, sq("Trafalgar Square")), state.currentActionInProgress());
        assertEquals(Set.of(new BuyProperty(sq("Trafalgar Square")), new DeclineProperty(sq("Trafalgar Square"))),
                actionSet(fm, state));
        fm.next(state, new BuyProperty(sq("Trafalgar Square")));
        assertEquals(0, state.getOwner(sq("Trafalgar Square")));
        assertEquals(1500 + 200 - 240, state.getCash(0));
        assertEquals(Set.of(new EndTurn(), new Mortgage(sq("Trafalgar Square"))), actionSet(fm, state));
    }

    @Test
    public void advanceToTrafalgarSquareFromChance1DoesNotPassGo() {
        startTurn(state, 0, sq("GO"));
        onTop(state, chance("Advance to Trafalgar Square"));
        roll(state, fm, 3, 4); // Chance 1 (7), forward to 24
        assertEquals(sq("Trafalgar Square"), state.getPosition(0));
        assertEquals(1500, state.getCash(0));
        assertEquals(new BuyDecision(0, sq("Trafalgar Square")), state.currentActionInProgress());
    }

    @Test
    public void advanceToGoPaysTheSalaryOnce() {
        startTurn(state, 0, sq("GO"));
        onTop(state, chance("Advance to GO"));
        roll(state, fm, 3, 4); // Chance 1
        assertEquals(sq("GO"), state.getPosition(0));
        assertEquals(1500 + 200, state.getCash(0));
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void passingGoAndThenAdvancingToGoPaysTheSalaryTwice() {
        // rulebook p.4: the roll passes GO onto Community Chest 1, which says Advance to GO
        startTurn(state, 0, sq("Mayfair"));
        onTop(state, chest("Advance to GO"));
        roll(state, fm, 1, 2); // 39 + 3 = 42 - 40 = Community Chest 1 (2)
        assertEquals(sq("GO"), state.getPosition(0));
        assertEquals(1500 + 200 + 200, state.getCash(0));
    }

    @Test
    public void advanceToMayfairChargesTheRentThere() {
        startTurn(state, 0, sq("Community Chest 2"));
        give(state, 1, sq("Mayfair"));
        onTop(state, chance("Advance to Mayfair"));
        roll(state, fm, 2, 3); // 17 + 5 = Chance 2 (22), forward to 39 without passing GO
        assertEquals(sq("Mayfair"), state.getPosition(0));
        // Mayfair alone (Park Lane with the Bank): rent 50
        assertEquals(1500 - 50, state.getCash(0));
        assertEquals(1500 + 50, state.getCash(1));
    }

    @Test
    public void aTripToMaryleboneFromChance3PassesGoAndPaysStationRent() {
        startTurn(state, 0, sq("Regent Street"));
        give(state, 2, sq("Marylebone Station"), sq("King's Cross Station"));
        onTop(state, chance("Take a trip to Marylebone"));
        roll(state, fm, 1, 4); // Chance 3 (36), forward to 15 past GO
        assertEquals(sq("Marylebone Station"), state.getPosition(0));
        // two stations: rent 50
        assertEquals(1500 + 200 - 50, state.getCash(0));
        assertEquals(1500 + 50, state.getCash(2));
    }

    // ---- GO_BACK / GO_BACK_TO ----

    @Test
    public void goBackThreeSpacesFromChance1LandsOnIncomeTax() {
        startTurn(state, 0, sq("GO"));
        onTop(state, chance("Go back three spaces"));
        roll(state, fm, 3, 4); // Chance 1 (7) - 3 = Income Tax (4)
        assertEquals(sq("Income Tax"), state.getPosition(0));
        assertEquals(1500 - 200, state.getCash(0));
    }

    @Test
    public void goBackThreeSpacesFromChance3LandsOnCommunityChest3AndDrawsAgain() {
        startTurn(state, 0, sq("Regent Street"));
        onTop(state, chance("Go back three spaces"));
        onTop(state, chest("You inherit"));
        roll(state, fm, 1, 4); // Chance 3 (36) - 3 = Community Chest 3 (33)
        assertEquals(sq("Community Chest 3"), state.getPosition(0));
        assertEquals(1500 + 100, state.getCash(0));
        assertEquals(chance("Go back three spaces"), bottom(state.getChanceDeck()));
        assertEquals(chest("You inherit"), bottom(state.getCommunityChestDeck()));
    }

    @Test
    public void goBackToOldKentRoadCollectsNoSalaryAndChargesTheRent() {
        startTurn(state, 0, sq("Water Works"));
        give(state, 1, sq("Old Kent Road"));
        onTop(state, chest("Go back to Old Kent Road"));
        roll(state, fm, 2, 3); // Community Chest 3 (33), back to 1
        assertEquals(sq("Old Kent Road"), state.getPosition(0));
        // no salary; rent 2 (Whitechapel Road with the Bank)
        assertEquals(1500 - 2, state.getCash(0));
        assertEquals(1500 + 2, state.getCash(1));
    }

    // ---- GO_TO_JAIL and doubles ----

    @Test
    public void theGoToJailCardSendsThePlayerToJailAndEndsTheTurn() {
        startTurn(state, 0, sq("GO"));
        onTop(state, chance("Go to Jail"));
        roll(state, fm, 3, 4); // Chance 1
        assertEquals(sq("Jail"), state.getPosition(0));
        assertTrue(state.isInJail(0));
        assertEquals(1500, state.getCash(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());
    }

    @Test
    public void theGoToJailCardAfterADoubleGivesNoFurtherRoll() {
        startTurn(state, 0, sq("GO"));
        onTop(state, chest("Go to Jail"));
        roll(state, fm, 1, 1); // Community Chest 1
        assertTrue(state.isInJail(0));
        assertEquals(sq("Jail"), state.getPosition(0));
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.hasAnotherRoll());
        assertEquals(0, state.getNDoubles());
    }

    @Test
    public void afterACardMovesThePlayerADoubleStillGivesAnotherRoll() {
        startTurn(state, 0, sq("Whitechapel Road"));
        onTop(state, chance("Advance to Pall Mall"));
        roll(state, fm, 2, 2); // 3 + 4 = Chance 1, forward to Pall Mall (11)
        assertEquals(sq("Pall Mall"), state.getPosition(0));
        decline(state, fm, sq("Pall Mall"));
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
        assertEquals(0, state.getCurrentPlayer());
    }

    // ---- REPAIRS ----

    @Test
    public void generalRepairsChargePerHouseAndPerHotelOnThePlayersStreets() {
        startTurn(state, 0, sq("GO"));
        give(state, 0, sq("Old Kent Road"), sq("Whitechapel Road"));
        state.setBuildings(sq("Old Kent Road"), 2);
        state.setBuildings(sq("Whitechapel Road"), MonopolyParameters.HOTEL);
        // another player's houses are not counted
        give(state, 1, sq("Mayfair"));
        state.setBuildings(sq("Mayfair"), 3);
        onTop(state, chance("Make general repairs"));
        roll(state, fm, 3, 4); // Chance 1
        assertEquals(1500 - (2 * 25 + 1 * 100), state.getCash(0));
        assertEquals(1500, state.getCash(1));
    }

    @Test
    public void streetRepairsHaveTheirOwnRates() {
        startTurn(state, 0, sq("GO"));
        give(state, 0, sq("Park Lane"), sq("Mayfair"), sq("Pall Mall"));
        state.setBuildings(sq("Park Lane"), 3);
        state.setBuildings(sq("Mayfair"), 4);
        state.setBuildings(sq("Pall Mall"), MonopolyParameters.HOTEL);
        onTop(state, chance("You are assessed for street repairs"));
        roll(state, fm, 3, 4); // Chance 1
        assertEquals(1500 - ((3 + 4) * 40 + 1 * 115), state.getCash(0));
    }

    // ---- COLLECT_FROM_EACH ----

    @Test
    public void onTheirBirthdayThePlayerCollectsFromEachOtherPlayer() {
        startTurn(state, 0, sq("Water Works"));
        onTop(state, chest("It is your birthday"));
        roll(state, fm, 2, 3); // Community Chest 3
        assertEquals(1500 + 2 * 10, state.getCash(0));
        assertEquals(1500 - 10, state.getCash(1));
        assertEquals(1500 - 10, state.getCash(2));
    }

    @Test
    public void aBankruptPlayerPaysNothingOnABirthday() {
        startTurn(state, 0, sq("Water Works"));
        eliminate(state, 2, 3);
        state.setCash(2, 0);
        onTop(state, chest("It is your birthday"));
        roll(state, fm, 2, 3); // Community Chest 3
        assertEquals(1500 + 10, state.getCash(0));
        assertEquals(1500 - 10, state.getCash(1));
        assertEquals(0, state.getCash(2));
        assertEquals(3, state.getFinalPlace(2));
    }

    @Test
    public void aBirthdayPayerShortOfCashIsBankruptToTheDrawer() {
        startTurn(state, 0, sq("Water Works"));
        state.setCash(1, 5);
        onTop(state, chest("It is your birthday"));
        roll(state, fm, 2, 3); // Community Chest 3
        assertTrue(state.isBankrupt(1));
        assertEquals(3, state.getFinalPlace(1));
        assertEquals(0, state.getCash(1));
        // player 1's 5 (all they had) and player 2's 10
        assertEquals(1500 + 5 + 10, state.getCash(0));
        assertEquals(1500 - 10, state.getCash(2));
        assertEquals(CoreConstants.GameResult.GAME_ONGOING, state.getGameStatus());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void aBirthdayBankruptcyLeavingOnePlayerEndsTheGame() {
        state = newState(2, 7, params);
        startTurn(state, 0, sq("Water Works"));
        state.setCash(1, 5);
        onTop(state, chest("It is your birthday"));
        roll(state, fm, 2, 3); // Community Chest 3
        assertTrue(state.isBankrupt(1));
        assertEquals(1500 + 5, state.getCash(0));
        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[1]);
    }

    // ---- PAY_OR_CHANCE ----

    private void drawPayOrChance() {
        startTurn(state, 0, sq("Water Works"));
        onTop(state, chest("Pay a "));
        roll(state, fm, 2, 3); // Community Chest 3 (33)
    }

    @Test
    public void payOrChanceOffersBothChoices() {
        drawPayOrChance();
        assertEquals(new PayOrChance(0, 10), state.currentActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(new PayFine(10), new TakeChance()), actionSet(fm, state));
        assertEquals(1500, state.getCash(0));
    }

    @Test
    public void withoutTheFineInCashThePlayerMustTakeAChance() {
        drawPayOrChance();
        state.setCash(0, 10 - 1);
        assertEquals(Set.of(new TakeChance()), actionSet(fm, state));
        state.setCash(0, 10);
        assertEquals(Set.of(new PayFine(10), new TakeChance()), actionSet(fm, state));
    }

    @Test
    public void payingTheFineEndsTheChoice() {
        drawPayOrChance();
        List<MonopolyCard> chanceBefore = new ArrayList<>(state.getChanceDeck().getComponents());
        fm.next(state, new PayFine(10));
        assertEquals(1500 - 10, state.getCash(0));
        assertEquals(chanceBefore, state.getChanceDeck().getComponents());
        assertFalse(state.isActionInProgress());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
    }

    @Test
    public void takingAChanceDrawsAndCarriesOutAChanceCard() {
        drawPayOrChance();
        onTop(state, chance("Advance to Trafalgar Square"));
        fm.next(state, new TakeChance());
        // from Community Chest 3 (33) forward to Trafalgar Square (24), passing GO
        assertEquals(sq("Trafalgar Square"), state.getPosition(0));
        assertEquals(1500 + 200, state.getCash(0));
        assertEquals(chance("Advance to Trafalgar Square"), bottom(state.getChanceDeck()));
        assertEquals(new BuyDecision(0, sq("Trafalgar Square")), state.currentActionInProgress());
        assertEquals(Set.of(new BuyProperty(sq("Trafalgar Square")), new DeclineProperty(sq("Trafalgar Square"))),
                actionSet(fm, state));
        fm.next(state, new BuyProperty(sq("Trafalgar Square")));
        assertEquals(1500 + 200 - 240, state.getCash(0));
        assertFalse(state.isActionInProgress());
        assertEquals(Set.of(new EndTurn(), new Mortgage(sq("Trafalgar Square"))), actionSet(fm, state));
    }
}
