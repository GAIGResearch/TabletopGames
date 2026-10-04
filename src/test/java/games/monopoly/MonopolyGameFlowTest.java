package games.monopoly;

import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.monopoly.actions.*;
import games.monopoly.components.MonopolyCard;
import org.junit.Test;

import java.util.*;

import static core.CoreConstants.GameResult.*;
import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Scripted games (the opening rounds with an auction, turns with cards, mortgages and raising money, building and
 * selling houses), and random games played to the end, checked at each step against invariants and an oracle for the
 * players' cash, and at the end for the results.
 */
public class MonopolyGameFlowTest {

    // BuildHouse / SellBuilding steps taken in the random games of one test
    int builds, sells;
    // PayFlatTax / PayPercentTax steps taken in the random games of one test
    int flatTaxes, percentTaxes;

    @Test
    public void scriptedOpeningRounds() {
        Game game = newGame(3, 11);
        MonopolyGameState state = (MonopolyGameState) game.getGameState();
        MonopolyForwardModel fm = (MonopolyForwardModel) game.getForwardModel();
        startTurn(state, 0, sq("GO"));
        int round = state.getRoundCounter();
        MonopolySquare angel = sq("The Angel Islington"), marlborough = sq("Marlborough Street");

        // player 0: GO + 6 = The Angel Islington, bought for 100
        roll(state, fm, 2, 4);
        fm.next(state, new BuyProperty(angel));
        assertEquals(1500 - 100, state.getCash(0));
        fm.next(state, new EndTurn());

        // player 1: GO + 6 = The Angel Islington, rent 6 to player 0
        assertEquals(1, state.getCurrentPlayer());
        roll(state, fm, 2, 4);
        assertEquals(1500 - 6, state.getCash(1));
        assertEquals(1500 - 100 + 6, state.getCash(0));
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
        fm.next(state, new EndTurn());

        // player 2: a double to Jail (Just Visiting), then 10 + 6 = Bow Street, declined
        assertEquals(2, state.getCurrentPlayer());
        roll(state, fm, 5, 5);
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
        roll(state, fm, 1, 5);
        assertEquals(sq("Bow Street"), state.getPosition(2));
        decline(state, fm, sq("Bow Street"));
        assertEquals(-1, state.getOwner(sq("Bow Street")));
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
        fm.next(state, new EndTurn());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(round + 1, state.getRoundCounter());

        // player 0: 6 + 12 = Marlborough Street, bought for 180; then 18 + 12 = Go To Jail
        roll(state, fm, 6, 6);
        fm.next(state, new BuyProperty(marlborough));
        assertEquals(1500 - 100 + 6 - 180, state.getCash(0));
        assertEquals(Set.of(new RollDice(), new Mortgage(angel), new Mortgage(marlborough)), actionSet(fm, state));
        roll(state, fm, 6, 6);
        assertTrue(state.isInJail(0));
        assertEquals(sq("Jail"), state.getPosition(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.ROLL, state.getGamePhase());

        // player 1: 6 + 7 = Whitehall, declined and auctioned: 2 opens at 10, 0 (in Jail) raises by 50, 1 and 2
        // pass - player 0 buys it for 60
        roll(state, fm, 3, 4);
        assertEquals(sq("Whitehall"), state.getPosition(1));
        fm.next(state, new DeclineProperty(sq("Whitehall")));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(Set.of(new Bid(10), new PassBid()), actionSet(fm, state));
        fm.next(state, new Bid(10));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Bid(10 + 50));
        fm.next(state, new PassBid());
        fm.next(state, new PassBid());
        assertEquals(0, state.getOwner(sq("Whitehall")));
        assertEquals(1500 - 100 + 6 - 180 - 60, state.getCash(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
        fm.next(state, new EndTurn());

        // player 2: 16 + 2 = Marlborough Street, rent 14 to player 0 in Jail; another roll: 18 + 5 = Fleet Street
        roll(state, fm, 1, 1);
        assertEquals(1500 - 14, state.getCash(2));
        assertEquals(1500 - 100 + 6 - 180 - 60 + 14, state.getCash(0));
        roll(state, fm, 2, 3);
        assertEquals(sq("Fleet Street"), state.getPosition(2));
        decline(state, fm, sq("Fleet Street"));
        fm.next(state, new EndTurn());
        assertEquals(round + 2, state.getRoundCounter());

        // player 0 in Jail: fails to roll a double, and the turn passes
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(new PayJailFine(), new RollDice()), actionSet(fm, state));
        roll(state, fm, 1, 2);
        assertTrue(state.isInJail(0));
        assertEquals(1, state.getJailRolls(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(GAME_ONGOING, state.getGameStatus());
    }

    @Test
    public void scriptedCardTurns() {
        Game game = newGame(3, 11);
        MonopolyGameState state = (MonopolyGameState) game.getGameState();
        MonopolyForwardModel fm = (MonopolyForwardModel) game.getForwardModel();
        startTurn(state, 0, sq("GO"));
        int round = state.getRoundCounter();
        onTop(state, chest("Get out of Jail"), chest("It is your birthday"), chest("Pay a "));
        onTop(state, chance("Go to Jail"), chance("Go back three spaces"), chance("Advance to GO"),
                chance("Advance to Trafalgar Square"));
        MonopolyCard jailCard = chest("Get out of Jail");

        // player 0: a double onto Community Chest 1 - Get Out of Jail Free, kept; then 2 + 5 = Chance 1 - Go to Jail
        roll(state, fm, 1, 1);
        assertEquals(List.of(jailCard), state.getJailCards(0).getComponents());
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
        roll(state, fm, 2, 3);
        assertTrue(state.isInJail(0));
        assertEquals(1, state.getCurrentPlayer());

        // player 1: Chance 1 - Go back three spaces to Income Tax
        roll(state, fm, 3, 4);
        assertEquals(sq("Income Tax"), state.getPosition(1));
        assertEquals(1500 - 200, state.getCash(1));
        fm.next(state, new EndTurn());

        // player 2: a double onto Community Chest 1 - birthday, 10 from each (player 0 in Jail too); then Chance 1 -
        // Advance to GO
        roll(state, fm, 1, 1);
        assertEquals(1500 + 2 * 10, state.getCash(2));
        assertEquals(1500 - 10, state.getCash(0));
        assertEquals(1500 - 200 - 10, state.getCash(1));
        roll(state, fm, 2, 3);
        assertEquals(sq("GO"), state.getPosition(2));
        assertEquals(1500 + 2 * 10 + 200, state.getCash(2));
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
        fm.next(state, new EndTurn());
        assertEquals(round + 1, state.getRoundCounter());

        // player 0 in Jail uses the card, which goes to the bottom of Community Chest, and rolls 10 + 7 = Community
        // Chest 2: pay 10 or take a Chance - takes a Chance: Advance to Trafalgar Square (no GO), bought for 240
        assertEquals(Set.of(new PayJailFine(), new UseJailCard(jailCard), new RollDice()), actionSet(fm, state));
        fm.next(state, new UseJailCard(jailCard));
        assertFalse(state.isInJail(0));
        assertEquals(jailCard, bottom(state.getCommunityChestDeck()));
        roll(state, fm, 3, 4);
        assertEquals(Set.of(new PayFine(10), new TakeChance()), actionSet(fm, state));
        fm.next(state, new TakeChance());
        assertEquals(sq("Trafalgar Square"), state.getPosition(0));
        fm.next(state, new BuyProperty(sq("Trafalgar Square")));
        assertEquals(1500 - 10 - 240, state.getCash(0));
        assertEquals(chest("Pay a "), bottom(state.getCommunityChestDeck()));
        assertEquals(chance("Advance to Trafalgar Square"), bottom(state.getChanceDeck()));
        assertEquals(Set.of(new EndTurn(), new Mortgage(sq("Trafalgar Square"))), actionSet(fm, state));
        assertCardsConserved(state);
    }

    @Test
    public void scriptedMortgagesAndRaisingMoney() {
        Game game = newGame(3, 11);
        MonopolyGameState state = (MonopolyGameState) game.getGameState();
        MonopolyForwardModel fm = (MonopolyForwardModel) game.getForwardModel();
        startTurn(state, 0, sq("GO"));
        int round = state.getRoundCounter();
        MonopolySquare angel = sq("The Angel Islington"), whitechapel = sq("Whitechapel Road");

        // player 0: GO + 6 = The Angel Islington, bought for 100 and mortgaged for 50
        roll(state, fm, 2, 4);
        fm.next(state, new BuyProperty(angel));
        assertEquals(Set.of(new EndTurn(), new Mortgage(angel)), actionSet(fm, state));
        fm.next(state, new Mortgage(angel));
        assertEquals(1500 - 100 + 50, state.getCash(0));
        assertEquals(Set.of(new EndTurn(), new Unmortgage(angel)), actionSet(fm, state));
        fm.next(state, new EndTurn());

        // player 1: GO + 6 = The Angel Islington, mortgaged - no rent
        roll(state, fm, 2, 4);
        assertEquals(1500, state.getCash(1));
        assertEquals(1500 - 100 + 50, state.getCash(0));
        fm.next(state, new EndTurn());

        // player 2: GO + 3 = Whitechapel Road, bought for 60
        roll(state, fm, 1, 2);
        fm.next(state, new BuyProperty(whitechapel));
        assertEquals(Set.of(new EndTurn(), new Mortgage(whitechapel)), actionSet(fm, state));
        fm.next(state, new EndTurn());
        assertEquals(round + 1, state.getRoundCounter());

        // player 0: 6 + 4 = Jail (Just Visiting); lifts the mortgage for 50 + 5
        roll(state, fm, 1, 3);
        assertEquals(Set.of(new EndTurn(), new Unmortgage(angel)), actionSet(fm, state));
        fm.next(state, new Unmortgage(angel));
        assertEquals(1500 - 100 + 50 - 55, state.getCash(0));
        assertFalse(state.isMortgaged(angel));
        assertEquals(Set.of(new EndTurn(), new Mortgage(angel)), actionSet(fm, state));
        fm.next(state, new EndTurn());

        // player 1: 6 + 4 = Jail (Just Visiting)
        roll(state, fm, 3, 1);
        fm.next(state, new EndTurn());

        // player 2, down to 4: 3 + 3 = The Angel Islington, rent 6 - raised by mortgaging Whitechapel Road
        state.setCash(2, 4);
        roll(state, fm, 1, 2);
        assertEquals(new RaiseMoney(2, 0, 6), state.currentActionInProgress());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(Set.of(new Mortgage(whitechapel)), actionSet(fm, state));
        fm.next(state, new Mortgage(whitechapel));
        assertEquals(4 + 30 - 6, state.getCash(2));
        assertEquals(1500 - 100 + 50 - 55 + 6, state.getCash(0));
        assertFalse(state.isActionInProgress());
        // 28 left: not enough to lift Whitechapel Road's mortgage (33)
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
        fm.next(state, new EndTurn());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(round + 2, state.getRoundCounter());
        assertEquals(GAME_ONGOING, state.getGameStatus());
    }

    @Test
    public void scriptedBuildingRentRepairsAndSelling() {
        Game game = newGame(3, 13);
        MonopolyGameState state = (MonopolyGameState) game.getGameState();
        MonopolyForwardModel fm = (MonopolyForwardModel) game.getForwardModel();
        MonopolySquare parkLane = sq("Park Lane"), mayfair = sq("Mayfair");
        startTurn(state, 0, sq("Water Works"));
        give(state, 0, parkLane, mayfair);
        state.setPosition(1, sq("Liverpool Street Station"));
        state.setPosition(2, sq("Bond Street"));
        onTop(state, chance("Make general repairs"));

        // player 0: 28 + 9 = Park Lane (own); builds a house on Park Lane and two on Mayfair, evenly
        roll(state, fm, 4, 5);
        assertEquals(parkLane, state.getPosition(0));
        assertEquals(Set.of(new EndTurn(), new Mortgage(parkLane), new Mortgage(mayfair), new BuildHouse(parkLane),
                new BuildHouse(mayfair)), actionSet(fm, state));
        play(state, fm, new BuildHouse(parkLane), new BuildHouse(mayfair), new BuildHouse(mayfair));
        assertEquals(1500 - 3 * 200, state.getCash(0));
        assertEquals(Set.of(new EndTurn(), new BuildHouse(parkLane), new SellBuilding(mayfair)), actionSet(fm, state));
        fm.next(state, new EndTurn());

        // player 1: 35 + 4 = Mayfair with 2 houses, rent 600
        roll(state, fm, 1, 3);
        assertEquals(1500 - 600, state.getCash(1));
        assertEquals(1500 - 600 + 600, state.getCash(0));
        fm.next(state, new EndTurn());

        // player 2: 34 + 3 = Park Lane with 1 house, rent 175
        roll(state, fm, 1, 2);
        assertEquals(1500 - 175, state.getCash(2));
        assertEquals(1500 + 175, state.getCash(0));
        fm.next(state, new EndTurn());

        // player 0: 37 + 10 = Chance 1, passing GO; general repairs on the 3 houses built, 3 x 25
        roll(state, fm, 4, 6);
        assertEquals(1500 + 175 + 200 - 3 * 25, state.getCash(0));
        assertEquals(chance("Make general repairs"), bottom(state.getChanceDeck()));
        // sells all three back, evenly, at 100 each
        assertEquals(Set.of(new EndTurn(), new BuildHouse(parkLane), new SellBuilding(mayfair)), actionSet(fm, state));
        play(state, fm, new SellBuilding(mayfair), new SellBuilding(parkLane), new SellBuilding(mayfair));
        assertEquals(0, state.getBuildings(parkLane));
        assertEquals(0, state.getBuildings(mayfair));
        assertEquals(1500 - 3 * 200 + 600 + 175 + 200 - 3 * 25 + 3 * 100, state.getCash(0)); // 2100
        assertEquals(Set.of(new EndTurn(), new Mortgage(parkLane), new Mortgage(mayfair), new BuildHouse(parkLane),
                new BuildHouse(mayfair)), actionSet(fm, state));
        fm.next(state, new EndTurn());
        assertEquals(1, state.getCurrentPlayer());
        assertCardsConserved(state);
    }

    @Test
    public void scriptedIncomeTaxChoices() {
        MonopolyParameters params = new MonopolyParameters();
        params.setParameterValue("incomeTaxPercent", 10);
        Game game = newGame(3, 11, params);
        MonopolyGameState state = (MonopolyGameState) game.getGameState();
        MonopolyForwardModel fm = (MonopolyForwardModel) game.getForwardModel();
        MonopolySquare incomeTax = sq("Income Tax"), mayfair = sq("Mayfair");
        startTurn(state, 0, sq("GO"));
        onTop(state, chance("Go back three spaces"));
        state.setCash(2, 10);
        give(state, 2, mayfair);

        // player 0: a double onto Income Tax - pays 10% of 1500 = 150, then rolls again: 4 + 6 = Jail, just visiting
        roll(state, fm, 2, 2);
        assertEquals(Set.of(new PayFlatTax(incomeTax), new PayPercentTax()), actionSet(fm, state));
        fm.next(state, new PayPercentTax());
        assertEquals(1500 - 150, state.getCash(0));
        assertEquals(Set.of(new RollDice()), actionSet(fm, state));
        roll(state, fm, 2, 4);
        assertEquals(sq("Jail"), state.getPosition(0));
        fm.next(state, new EndTurn());

        // player 1: Chance 1 - Go back three spaces to Income Tax: the same choice; pays the flat 200
        assertEquals(1, state.getCurrentPlayer());
        roll(state, fm, 3, 4);
        assertEquals(new IncomeTaxChoice(1, incomeTax), state.currentActionInProgress());
        fm.next(state, new PayFlatTax(incomeTax));
        assertEquals(1500 - 200, state.getCash(1));
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
        fm.next(state, new EndTurn());

        // player 2 (cash 10, Mayfair): onto Income Tax, 10% of 10 + 400 = 41 > 10 - raised by mortgaging Mayfair
        assertEquals(2, state.getCurrentPlayer());
        roll(state, fm, 1, 3);
        fm.next(state, new PayPercentTax());
        assertEquals(new RaiseMoney(2, -1, 41), state.currentActionInProgress());
        fm.next(state, new Mortgage(mayfair));
        assertEquals(10 + 200 - 41, state.getCash(2));
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn()), actionSet(fm, state));
        assertEquals(1500 - 150 + 1500 - 200 + 10 + 200 - 41, totalCash(state));
    }

    @Test
    public void randomGamesWithAPercentageIncomeTaxRunToTheEnd() {
        for (long seed = 1; seed <= 3; seed++) {
            MonopolyParameters params = new MonopolyParameters();
            params.setParameterValue("incomeTaxPercent", 10);
            playRandomGame(4, seed, params);
        }
        // the oracle must have seen both choices
        assertTrue(flatTaxes + " flat taxes", flatTaxes > 0);
        assertTrue(percentTaxes + " percentage taxes", percentTaxes > 0);
    }

    @Test
    public void thirdDoubleInARealTurn() {
        Game game = newGame(2, 5);
        MonopolyGameState state = (MonopolyGameState) game.getGameState();
        MonopolyForwardModel fm = (MonopolyForwardModel) game.getForwardModel();
        startTurn(state, 1, sq("GO"));
        roll(state, fm, 5, 5); // Jail, Just Visiting
        roll(state, fm, 5, 5); // Free Parking
        assertEquals(sq("Free Parking"), state.getPosition(1));
        roll(state, fm, 3, 3); // third double: to Jail, not on to 20 + 6 = Leicester Square
        assertEquals(sq("Jail"), state.getPosition(1));
        assertTrue(state.isInJail(1));
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void randomTwoPlayerGamesRunToTheEnd() {
        for (long seed = 1; seed <= 3; seed++)
            playRandomGame(2, seed);
        // with two players whole groups are common: the oracle must have seen building and selling
        assertTrue(builds + " builds", builds > 0);
        assertTrue(sells + " sales", sells > 0);
    }

    @Test
    public void randomFourPlayerGamesRunToTheEnd() {
        for (long seed = 1; seed <= 3; seed++)
            playRandomGame(4, seed);
    }

    @Test
    public void randomEightPlayerGameRunsToTheEnd() {
        playRandomGame(8, 1);
    }

    /**
     * Plays random actions to the end of the game, checking the invariants and the cash oracle at each step and the
     * results at the end.
     */
    private void playRandomGame(int nPlayers, long seed) {
        playRandomGame(nPlayers, seed, null);
    }

    private void playRandomGame(int nPlayers, long seed, MonopolyParameters params) {
        Game game = newGame(nPlayers, seed, params);
        MonopolyGameState state = (MonopolyGameState) game.getGameState();
        MonopolyForwardModel fm = (MonopolyForwardModel) game.getForwardModel();
        Random rnd = new Random(seed);
        int steps = 0, rolls = 0, exactChecks = 0;
        while (state.isNotTerminal() && steps++ < 200000) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse("no actions at step " + steps, actions.isEmpty());
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            if (action instanceof RollDice) rolls++;
            if (step(state, fm, action)) exactChecks++;
            checkInvariants(state, fm);
        }
        assertFalse(nPlayers + " players, seed " + seed + ": not over in 200000 steps", state.isNotTerminal());
        // random players buy whatever they can, so a two-player game can end in a bankruptcy within a few laps
        assertTrue(nPlayers + " players, seed " + seed + ": " + rolls + " rolls", rolls > 20);
        assertTrue(nPlayers + " players, seed " + seed + ": " + exactChecks + " exact checks", exactChecks > 20);
        checkResults(state);
    }

    /**
     * Takes the action and checks the change in the players' total cash. Returns whether that exact check was made.
     */
    private boolean step(MonopolyGameState state, MonopolyForwardModel fm, AbstractAction action) {
        MonopolyParameters params = (MonopolyParameters) state.getGameParameters();
        int actor = state.getCurrentPlayer();
        int before = totalCash(state);
        int nInBefore = state.getNPlayersIn();
        int oldPos = state.getPosition(actor).index();
        boolean wasInJail = state.isInJail(actor);
        int jailRolls = state.getJailRolls(actor);
        int nDoubles = state.getNDoubles();
        List<MonopolyCard> chanceBefore = new ArrayList<>(state.getChanceDeck().getComponents());
        List<MonopolyCard> chestBefore = new ArrayList<>(state.getCommunityChestDeck().getComponents());
        Auction auctionBefore = state.currentActionInProgress() instanceof Auction a ? a.copy() : null;
        int actorCash = state.getCash(actor);
        int actorWorth = state.getNetWorth(actor);
        IncomeTaxChoice taxChoiceBefore = state.currentActionInProgress() instanceof IncomeTaxChoice c ? c : null;
        Set<IExtendedSequence> raisingBefore = raising(state);
        MonopolyBoard board = state.getBoard();
        int[] buildingsBefore = new int[board.nSquares()], ownerBefore = new int[board.nSquares()];
        for (MonopolySquare s : board.squares()) {
            buildingsBefore[s.index()] = state.getBuildings(s);
            ownerBefore[s.index()] = state.getOwner(s);
        }

        fm.next(state, action);

        // a step in which a player goes bankrupt is checked only for the bankrupt's cash, properties and jail cards
        if (state.getNPlayersIn() < nInBefore) {
            for (int p = 0; p < state.getNPlayers(); p++)
                if (state.isBankrupt(p)) {
                    assertEquals(0, state.getCash(p));
                    assertTrue(state.getProperties(p).isEmpty());
                    assertEquals(0, state.getJailCards(p).getSize());
                }
            // a bankrupt's buildings are sold (to a player creditor) or removed (to the Bank)
            for (MonopolySquare s : board.squares())
                if (ownerBefore[s.index()] >= 0 && state.isBankrupt(ownerBefore[s.index()]))
                    assertEquals(s + " of a bankrupt", 0, state.getBuildings(s));
            return false;
        }
        // buildings change only by building or selling, one at a time on the street named
        MonopolySquare builtOn = action instanceof BuildHouse b ? b.square
                : action instanceof SellBuilding sb ? sb.square : null;
        for (MonopolySquare s : board.squares()) {
            int change = !s.equals(builtOn) ? 0 : action instanceof BuildHouse ? 1 : -1;
            assertEquals("buildings on " + s + " after " + action, buildingsBefore[s.index()] + change,
                    state.getBuildings(s));
        }
        // drawn[0] is the Chance card drawn in this step, and drawn[1] the Community Chest card (null if none). They
        // are found from the piles before and after the step, and the route of the token is followed through them.
        MonopolyCard[] drawn = {
                drawnCard(state, actor, action, chanceBefore, state.getChanceDeck().getComponents()),
                drawnCard(state, actor, action, chestBefore, state.getCommunityChestDeck().getComponents())};
        int[] visits = new int[2];
        // Money only comes from the Bank as the GO salary, a card, a mortgage or the sale of a building. It only goes
        // to the Bank as a tax, a purchase, the jail fine, a card, a fine, the winning bid of an auction, the cost of
        // lifting a mortgage, or a debt to the Bank once raised. Rent and birthdays move money between players.
        int expected = 0;
        if (action instanceof BuyProperty buy) {
            expected = -buy.square.price();
            assertEquals(actor, state.getOwner(buy.square));
        } else if (action instanceof DeclineProperty decline) {
            assertTrue("no auction after " + action, state.currentActionInProgress() instanceof Auction a
                    && a.square.equals(decline.square));
        } else if (action instanceof Bid || action instanceof PassBid) {
            assertNotNull(action + " outside an auction", auctionBefore);
            MonopolySquare square = auctionBefore.square;
            boolean ended = !(state.currentActionInProgress() instanceof Auction a && a.square.equals(square));
            // the auction is won by a bid only when everybody else is out; a pass leaves the earlier high bidder
            int winner = action instanceof Bid ? actor : auctionBefore.getHighBidder();
            int price = action instanceof Bid bid ? bid.amount : auctionBefore.getHighBid();
            if (action instanceof Bid bid)
                assertTrue(bid.amount + " bid with cash " + actorCash, bid.amount <= actorCash);
            if (ended) {
                assertEquals("owner of " + square + " after the auction", winner, state.getOwner(square));
                expected = winner == -1 ? 0 : -price;
            } else
                assertEquals(-1, state.getOwner(square));
        } else if (action instanceof BuildHouse b) {
            builds++;
            // the whole group, unmortgaged, and this street had the fewest buildings, short of a hotel
            int fewest = Integer.MAX_VALUE;
            for (MonopolySquare s : board.streets(b.square.group())) {
                assertEquals(actor, state.getOwner(s));
                assertFalse(s + " mortgaged", state.isMortgaged(s));
                fewest = Math.min(fewest, buildingsBefore[s.index()]);
            }
            assertEquals("build on " + b.square + " with " + fewest + " the fewest", fewest,
                    buildingsBefore[b.square.index()]);
            assertTrue(buildingsBefore[b.square.index()] < MonopolyParameters.HOTEL);
            assertTrue(b.square.houseCost() + " to build with cash " + actorCash, b.square.houseCost() <= actorCash);
            expected = -b.square.houseCost();
        } else if (action instanceof Mortgage || action instanceof SellBuilding) {
            if (action instanceof Mortgage m) {
                assertEquals(actor, state.getOwner(m.square));
                assertTrue(state.isMortgaged(m.square));
                expected = m.square.mortgage();
            } else {
                sells++;
                MonopolySquare square = ((SellBuilding) action).square;
                assertEquals(actor, state.getOwner(square));
                int most = 0;
                for (MonopolySquare s : board.streets(square.group()))
                    most = Math.max(most, buildingsBefore[s.index()]);
                assertTrue(buildingsBefore[square.index()] > 0);
                assertEquals("sell on " + square + " with " + most + " the most", most,
                        buildingsBefore[square.index()]);
                expected = square.houseCost() * params.buildingSalePercent / 100;
            }
            // once the forced Jail fine has been raised, the token moves by the roll
            Set<IExtendedSequence> raisingAfter = raising(state);
            for (IExtendedSequence seq : raisingBefore)
                if (!raisingAfter.contains(seq) && ((RaiseMoney) seq).thenMove > 0) {
                    int n = state.getBoard().nSquares();
                    int to = oldPos + ((RaiseMoney) seq).thenMove;
                    if (to >= n)
                        expected += state.getBoard().goSalary();
                    expected += resolve(state, actor, to % n, drawn, visits);
                }
        } else if (action instanceof Unmortgage u) {
            assertEquals(actor, state.getOwner(u.square));
            assertFalse(state.isMortgaged(u.square));
            assertTrue(params.unmortgageCost(u.square) + " to unmortgage with cash " + actorCash,
                    params.unmortgageCost(u.square) <= actorCash);
            expected = -params.unmortgageCost(u.square);
        } else if (action instanceof PayJailFine) {
            expected = -params.jailFine;
        } else if (action instanceof PayFlatTax flat) {
            flatTaxes++;
            assertNotNull(action + " without an IncomeTaxChoice", taxChoiceBefore);
            assertEquals(taxChoiceBefore.square, flat.square);
            expected = -flat.square.tax();
        } else if (action instanceof PayPercentTax) {
            percentTaxes++;
            assertNotNull(action + " without an IncomeTaxChoice", taxChoiceBefore);
            // the percentage of the worth before paying, rounded down
            expected = -(actorWorth * params.incomeTaxPercent / 100);
        } else if (action instanceof PayFine fine) {
            expected = -fine.amount;
        } else if (action instanceof TakeChance) {
            visits[0]++;
            expected = applyCard(state, actor, oldPos, drawn[0], drawn, visits);
        } else if (action instanceof RollDice) {
            int[] dice = state.getDice();
            boolean doubles = dice[0] == dice[1];
            boolean forcedFine = wasInJail && !doubles && jailRolls == params.maxJailRolls - 1;
            // a forced fine that has to be raised is paid before the move, which then comes when the raising ends
            boolean fineRaised = forcedFine && state.currentActionInProgress() instanceof RaiseMoney r && r.thenMove > 0;
            boolean moves = wasInJail ? doubles || forcedFine && !fineRaised
                    : !(doubles && nDoubles + 1 >= params.maxDoubles);
            if (forcedFine)
                expected -= params.jailFine;
            if (moves) {
                int total = dice[0] + dice[1];
                int n = state.getBoard().nSquares();
                if (oldPos + total >= n)
                    expected += state.getBoard().goSalary();
                expected += resolve(state, actor, (oldPos + total) % n, drawn, visits);
            }
        }
        // a debt to the Bank that has to be raised is paid only when the raising ends
        Set<IExtendedSequence> raisingAfter = raising(state);
        for (IExtendedSequence seq : raisingAfter)
            if (!raisingBefore.contains(seq) && ((RaiseMoney) seq).creditor == -1)
                expected += ((RaiseMoney) seq).amount;
        for (IExtendedSequence seq : raisingBefore)
            if (!raisingAfter.contains(seq) && ((RaiseMoney) seq).creditor == -1)
                expected -= ((RaiseMoney) seq).amount;
        assertEquals("Chance card drawn after " + action, visits[0] == 1, drawn[0] != null);
        assertEquals("Community Chest card drawn after " + action, visits[1] == 1, drawn[1] != null);
        assertEquals("cash change after " + action + " by " + actor + ", cards " + Arrays.toString(drawn),
                expected, totalCash(state) - before);
        return true;
    }

    /** The card drawn from the pile in this step, or null if none was drawn, checking how the pile changed. */
    private MonopolyCard drawnCard(MonopolyGameState state, int actor, AbstractAction action,
                                   List<MonopolyCard> before, List<MonopolyCard> after) {
        // a card used to leave Jail goes back to the bottom of its pile
        if (action instanceof UseJailCard use && !after.isEmpty() && use.card.pile == after.get(0).pile) {
            List<MonopolyCard> expected = new ArrayList<>(before);
            expected.add(use.card);
            assertEquals(expected, after);
            assertFalse(state.getJailCards(actor).contains(use.card));
            return null;
        }
        if (after.equals(before))
            return null;
        // the top card is drawn and goes to the bottom, unless it is a Get Out of Jail Free card, which the actor keeps
        MonopolyCard top = before.get(0);
        List<MonopolyCard> expected = new ArrayList<>(before.subList(1, before.size()));
        if (top.effect == MonopolyCard.Effect.GET_OUT_OF_JAIL)
            assertTrue(state.getJailCards(actor).contains(top));
        else
            expected.add(top);
        assertEquals("pile after drawing " + top, expected, after);
        return top;
    }

    /** The RaiseMoney sequences on the action stack that are not yet complete, by identity. */
    private Set<IExtendedSequence> raising(MonopolyGameState state) {
        Set<IExtendedSequence> set = Collections.newSetFromMap(new IdentityHashMap<>());
        // a completed one stays on the stack while a decision pushed above it, such as buying the square the player
        // then moved to, is open
        for (IExtendedSequence seq : state.getActionsInProgress())
            if (seq instanceof RaiseMoney && !seq.executionComplete(state))
                set.add(seq);
        return set;
    }

    /** The change in the players' total cash from the actor stopping on the square at index pos. */
    private int resolve(MonopolyGameState state, int actor, int pos, MonopolyCard[] drawn, int[] visits) {
        MonopolySquare square = state.getBoard().square(pos);
        return switch (square.type()) {
            case TAX -> -square.tax();
            case INCOME_TAX -> {
                if (((MonopolyParameters) state.getGameParameters()).incomeTaxPercent == 0)
                    yield -square.tax();
                // the choice is pushed; nothing is paid until PayFlatTax / PayPercentTax
                assertEquals(new IncomeTaxChoice(actor, square), state.currentActionInProgress());
                yield 0;
            }
            case CHANCE -> {
                visits[0]++;
                yield applyCard(state, actor, pos, drawn[0], drawn, visits);
            }
            case COMMUNITY_CHEST -> {
                visits[1]++;
                yield applyCard(state, actor, pos, drawn[1], drawn, visits);
            }
            // rent moves money between players; a purchase is a separate action
            default -> 0;
        };
    }

    /** The change in the players' total cash from the actor, on the square at index pos, carrying out the card. */
    private int applyCard(MonopolyGameState state, int actor, int pos, MonopolyCard card, MonopolyCard[] drawn,
                          int[] visits) {
        MonopolyBoard board = state.getBoard();
        assertNotNull("no card drawn on " + board.square(pos), card);
        return switch (card.effect) {
            case ADVANCE -> {
                int target = board.square(card.target).index();
                yield (target <= pos ? board.goSalary() : 0) + resolve(state, actor, target, drawn, visits);
            }
            case GO_BACK_TO -> resolve(state, actor, board.square(card.target).index(), drawn, visits);
            case GO_BACK -> resolve(state, actor, (pos - card.amount + board.nSquares()) % board.nSquares(), drawn,
                    visits);
            case COLLECT -> card.amount;
            case PAY -> -card.amount;
            case REPAIRS -> {
                int cost = 0;
                for (MonopolySquare s : state.getProperties(actor)) {
                    int b = state.getBuildings(s);
                    cost += b == MonopolyParameters.HOTEL ? card.hotelAmount : b * card.amount;
                }
                yield -cost;
            }
            case PAY_OR_CHANCE -> {
                assertEquals(new PayOrChance(actor, card.amount), state.currentActionInProgress());
                yield 0;
            }
            // GO_TO_JAIL and GET_OUT_OF_JAIL cost nothing, and COLLECT_FROM_EACH moves money between players
            default -> 0;
        };
    }

    private void checkInvariants(MonopolyGameState state, MonopolyForwardModel fm) {
        MonopolyBoard board = state.getBoard();
        for (int p = 0; p < state.getNPlayers(); p++) {
            assertTrue(state.getCash(p) >= 0);
            int pos = state.getPosition(p).index();
            assertTrue(pos >= 0 && pos < board.nSquares());
            if (state.isInJail(p))
                assertEquals(board.jail(), state.getPosition(p));
        }
        for (MonopolySquare s : board.squares()) {
            int owner = state.getOwner(s);
            if (!s.type().isProperty())
                assertEquals(-1, owner);
            else if (owner >= 0)
                assertFalse(s + " owned by bankrupt " + owner, state.isBankrupt(owner));
            else
                assertFalse(s + " mortgaged by the Bank", state.isMortgaged(s));
        }
        if (state.isNotTerminal())
            assertFalse(state.isBankrupt(state.getCurrentPlayer()));
        if (state.currentActionInProgress() instanceof Auction auction) {
            assertEquals(-1, state.getOwner(auction.square));
            assertFalse(auction.isOut(state.getCurrentPlayer()));
            if (auction.getHighBidder() >= 0)
                assertTrue(auction.getHighBid() <= state.getCash(auction.getHighBidder()));
        }
        if (state.currentActionInProgress() instanceof IncomeTaxChoice choice) {
            assertTrue(((MonopolyParameters) state.getGameParameters()).incomeTaxPercent > 0);
            int chooser = choice.getCurrentPlayer(state);
            assertEquals(chooser, state.getCurrentPlayer());
            assertEquals(SquareType.INCOME_TAX, choice.square.type());
            assertEquals(choice.square, state.getPosition(chooser));
            assertEquals(Set.of(new PayFlatTax(choice.square), new PayPercentTax()), actionSet(fm, state));
        }
        if (state.currentActionInProgress() instanceof RaiseMoney raise) {
            // the debtor decides, still short of the debt, and may only mortgage or sell buildings
            assertEquals(raise.player, state.getCurrentPlayer());
            assertFalse(state.isBankrupt(raise.player));
            assertTrue(state.getCash(raise.player) < raise.amount);
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse("nothing to mortgage or sell while raising " + raise, actions.isEmpty());
            for (AbstractAction a : actions)
                assertTrue(a + " while raising money", a instanceof Mortgage || a instanceof SellBuilding);
        }
        // buildings only on the streets of a group wholly owned by one player with none mortgaged, evenly
        for (MonopolyGroup group : board.groups()) {
            List<MonopolySquare> streets = board.streets(group);
            int most = 0, fewest = Integer.MAX_VALUE;
            for (MonopolySquare s : streets) {
                most = Math.max(most, state.getBuildings(s));
                fewest = Math.min(fewest, state.getBuildings(s));
            }
            assertTrue(group + ": " + fewest + " buildings", fewest >= 0);
            assertTrue(group + ": " + most + " buildings", most <= MonopolyParameters.HOTEL);
            assertTrue(group + " built unevenly: " + fewest + " to " + most, most - fewest <= 1);
            if (most > 0) {
                int owner = state.getOwner(streets.get(0));
                assertTrue(group + " built on by the Bank", owner >= 0);
                for (MonopolySquare s : streets) {
                    assertEquals(group + " built on, not wholly owned", owner, state.getOwner(s));
                    assertFalse(s + " mortgaged in a built group", state.isMortgaged(s));
                }
            }
        }
        assertCardsConserved(state);
    }

    private void checkResults(MonopolyGameState state) {
        int n = state.getNPlayers();
        GameResult[] results = state.getPlayerResults();
        Set<Integer> places = new HashSet<>();
        int nOut = 0;
        for (int p = 0; p < n; p++)
            if (state.isBankrupt(p)) {
                nOut++;
                places.add(state.getFinalPlace(p));
                assertEquals(LOSE_GAME, results[p]);
            }
        // bankrupt players took the places from the bottom up
        Set<Integer> expectedPlaces = new HashSet<>();
        for (int k = 0; k < nOut; k++)
            expectedPlaces.add(n - k);
        assertEquals(expectedPlaces, places);

        // the last player standing wins
        if (n - nOut == 1) {
            for (int p = 0; p < n; p++)
                assertEquals(state.isBankrupt(p) ? LOSE_GAME : WIN_GAME, results[p]);
            return;
        }
        // otherwise the game ended at MonopolyParameters.maxRounds: the players with the highest net worth win, or
        // draw if there are more than one
        MonopolyParameters params = (MonopolyParameters) state.getGameParameters();
        assertEquals(params.getMaxRounds(), state.getRoundCounter());
        int[] worth = new int[n];
        int best = Integer.MIN_VALUE;
        for (int p = 0; p < n; p++) {
            if (state.isBankrupt(p)) continue;
            worth[p] = state.getCash(p);
            for (MonopolySquare s : state.getBoard().squares())
                if (state.getOwner(s) == p)
                    worth[p] += (state.isMortgaged(s) ? s.mortgage() : s.price()) + state.getBuildings(s) * s.houseCost();
            best = Math.max(best, worth[p]);
        }
        int nBest = 0;
        for (int p = 0; p < n; p++)
            if (!state.isBankrupt(p) && worth[p] == best) nBest++;
        for (int p = 0; p < n; p++) {
            GameResult expected = state.isBankrupt(p) || worth[p] < best ? LOSE_GAME : nBest > 1 ? DRAW_GAME : WIN_GAME;
            assertEquals("player " + p, expected, results[p]);
        }
    }
}
