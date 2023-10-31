package games.hearts;

import core.AbstractParameters;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Deck;

import java.util.List;

import games.hearts.actions.Play;
import games.hearts.HeartsForwardModel;
import games.hearts.HeartsGameState;
import games.hearts.HeartsParameters;
import games.hearts.actions.Pass;
import org.junit.*;
import core.components.FrenchCard;
import java.util.AbstractMap;


import static core.CoreConstants.GameResult.*;
import static org.junit.Assert.*;

public class TestHearts {

    private HeartsForwardModel forwardModel;
    private HeartsGameState gameState;

    AbstractParameters gameParameters = new HeartsParameters();

    @Before
    public void setUp() {
        forwardModel = new HeartsForwardModel();
        // Assuming the constructor HeartsGameState(int nPlayers) exists.
        gameState = new HeartsGameState(gameParameters, 3);
        forwardModel.setup(gameState);
    }

    @Test
    public void testSetup() {
        // Assert that game phase is set to PASSING
        assertEquals(HeartsGameState.Phase.PASSING, gameState.getGamePhase());
        // Assert that all players have a deck with 17 cards (assuming a 3 player game)
        gameState.getPlayerDecks().forEach(deck -> assertEquals(17, deck.getSize()));
        // Add more assertions based on your game logic
    }

    @Test
    public void testAfterAction() {
        // Setup a Pass action
        FrenchCard cardToPass = gameState.getPlayerDecks().get(0).get(0); // Get first card of first player
        Pass passAction = new Pass(0, cardToPass);
        forwardModel.next(gameState, passAction);

        // Assert that the card has been removed from the player's deck
        assertFalse(gameState.getPlayerDecks().get(0).contains(cardToPass));

        // Assert that the card has been added to the pending passes of the player
        assertTrue(gameState.pendingPasses.get(0).contains(cardToPass));


    }

    @Test
    public void testSetupPhaseIsPassing() {
        assertEquals(HeartsGameState.Phase.PASSING, gameState.getGamePhase());
    }


    @Test
    public void testPlayerDecksSize() {
        assertEquals(3, gameState.getPlayerDecks().size());
        for (Deck<FrenchCard> deck : gameState.getPlayerDecks()) {
            assertEquals(17, deck.getSize());
        }
    }

    @Test
    public void testDrawDeckSize() {
        assertEquals(52 - (17 * 3) - 1, gameState.getDrawDeck().getSize());
    }

    @Test
    public void testRemovedCard() {
        boolean containsRemovedCard = gameState.getDrawDeck().getComponents().stream().anyMatch(c ->
                c.suite == FrenchCard.Suite.Diamonds &&
                        c.number == 2 &&
                        c.type == FrenchCard.FrenchCardType.Number
        );
        assertFalse(containsRemovedCard);
    }

    @Test
    public void testAfterActionPass() {
        int initialDeckSize = gameState.getPlayerDecks().get(0).getSize();
        FrenchCard card = gameState.getPlayerDecks().get(0).get(0);
        Pass passAction = new Pass(0, card);
        forwardModel.next(gameState, passAction);

        // Check that the card has been removed from the player's deck
        assertEquals(initialDeckSize - 1, gameState.getPlayerDecks().get(0).getSize());
        assertFalse(gameState.getPlayerDecks().get(0).getComponents().contains(card));

        // Check that the card has been added to the pendingPasses
        assertTrue(gameState.pendingPasses.get(0).contains(card));
    }


    @Test
    public void testComputeAvailableActionsPassingPhase() {
        gameState.setGamePhase(HeartsGameState.Phase.PASSING);
        // Assuming player 0 has 5 cards
        Deck<FrenchCard> playerDeck = gameState.getPlayerDecks().get(0);

        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);
        // All 5 cards should be able to be passed
        assertEquals(17, actions.size());
    }

    @Test
    public void testComputeAvailableActionsFirstTurn() {

        gameState.setGamePhase(HeartsGameState.Phase.PLAYING);
        // Assuming player 0 has 2 of Clubs
        Deck<FrenchCard> playerDeck = gameState.getPlayerDecks().get(0);
        playerDeck.add(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 2));

        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);
        // Only one action should be available: playing 2 of Clubs
        assertEquals(1, actions.size());
        assertTrue(actions.get(0) instanceof Play);
        assertEquals(FrenchCard.Suite.Clubs, ((Play)actions.get(0)).card.suite);
        assertEquals(2, ((Play)actions.get(0)).card.number);
    }

    @Test
    public void testComputeAvailableActionsWithoutLeadSuit() {
        // Set the game phase to PLAYING
        gameState.setGamePhase(HeartsGameState.Phase.PLAYING);
        gameState.heartsBroken = true;

        // Set the leading suit to Clubs, for instance
        gameState.firstCardSuit = FrenchCard.Suite.Clubs;

        // Assume that player 0 doesn't have any clubs
        Deck<FrenchCard> playerDeck = gameState.getPlayerDecks().get(0);
        playerDeck.clear(); // Ensure the deck is empty before adding cards
        playerDeck.add(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Hearts, 2));
        playerDeck.add(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 3));
        playerDeck.add(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Spades, 4));

        // The available actions should be all the cards in the player's deck
        List<AbstractAction> actions = forwardModel._computeAvailableActions(gameState);

        // Check that all cards in the player's deck are among the available actions
        assertEquals(playerDeck.getSize(), actions.size());
        for (AbstractAction action : actions) {
            assertTrue(action instanceof Play);
            FrenchCard card = ((Play) action).card;
            assertTrue(playerDeck.contains(card));
        }
    }



    @Test
    public void testEndGameSingleWinner() {
        // Let's assume that player 0 has the lowest score
        gameState.setPlayerPoints(0, 0);
        gameState.setPlayerPoints(1, 100);
        gameState.setPlayerPoints(2, 50);

        // Execute end game process
        forwardModel.forceGameEnd(gameState);

        // Verify the state of the game
        assertEquals(GAME_END, gameState.getGameStatus());

        // Verify results of the game
        assertEquals(WIN_GAME, gameState.getPlayerResult(0));
        assertEquals(LOSE_GAME, gameState.getPlayerResult(1));
        assertEquals(LOSE_GAME, gameState.getPlayerResult(2));
    }

    @Test
    public void testEndGameMultipleWinners() {
        // Let's assume that player 0 and 1 have the same lowest score
        gameState.setPlayerPoints(0, 50);
        gameState.setPlayerPoints(1, 50);
        gameState.setPlayerPoints(2, 100);

        // Execute end game process
        forwardModel.forceGameEnd(gameState);

        // Verify the state of the game
        assertEquals(GAME_END, gameState.getGameStatus());

        // Verify results of the game
        assertEquals(DRAW_GAME, gameState.getPlayerResult(0));
        assertEquals(DRAW_GAME, gameState.getPlayerResult(1));
        assertEquals(LOSE_GAME, gameState.getPlayerResult(2));
    }


    @Test
    public void testEndTurnWithRoundWinner() {
        // Set the game phase to PLAYING
        gameState.setGamePhase(HeartsGameState.Phase.PLAYING);

        // Let's set up the game state to a hypothetical end-turn scenario.
        // Assume that three players have played the following cards:
        // Player 0 played Club 3
        Play play0 = new Play(0, new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 3));
        gameState.currentPlayedCards.add(new AbstractMap.SimpleEntry<>(play0.playerID, play0.card));

        // Player 1 played Club 2
        Play play1 = new Play(1, new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 2));
        gameState.currentPlayedCards.add(new AbstractMap.SimpleEntry<>(play1.playerID, play1.card));

        // Player 2 played Club 4 (highest in the trick)
        Play play2 = new Play(2, new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 4));
        gameState.currentPlayedCards.add(new AbstractMap.SimpleEntry<>(play2.playerID, play2.card));

        // Set the firstCardSuit as Club which was set when first card was played
        gameState.firstCardSuit = FrenchCard.Suite.Clubs;

        // Set the player's deck
        gameState.getPlayerDecks().get(play0.playerID).add(play0.card);
        gameState.getPlayerDecks().get(play1.playerID).add(play1.card);
        gameState.getPlayerDecks().get(play2.playerID).add(play2.card);

        // Call _endTurn
        forwardModel.endTrick(gameState);

        // Verify that the player with the highest card of the leading suit has won
        assertEquals(2, gameState.getFirstPlayer());

        // Verify that the winning player has the trick cards
        assertTrue(gameState.trickDecks.get(2).contains(play0.card));
        assertTrue(gameState.trickDecks.get(2).contains(play1.card));
        assertTrue(gameState.trickDecks.get(2).contains(play2.card));
    }

    @Test
    public void testEndTurnTrickDeck() {
        // Set the game phase to PLAYING
        gameState.setGamePhase(HeartsGameState.Phase.PLAYING);

        // Let's set up the game state to a hypothetical end-turn scenario.
        // Assume that three players have played the following cards:
        // Player 0 played Club 3
        Play play0 = new Play(0, new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 3));
        gameState.currentPlayedCards.add(new AbstractMap.SimpleEntry<>(play0.playerID, play0.card));

        // Player 1 played Club 2
        Play play1 = new Play(1, new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 2));
        gameState.currentPlayedCards.add(new AbstractMap.SimpleEntry<>(play1.playerID, play1.card));

        // Player 2 played Club 4 (highest in the trick)
        Play play2 = new Play(2, new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 4));
        gameState.currentPlayedCards.add(new AbstractMap.SimpleEntry<>(play2.playerID, play2.card));

        // Set the firstCardSuit as Club which was set when first card was played
        gameState.firstCardSuit = FrenchCard.Suite.Clubs;

        // Set the player's deck
        gameState.getPlayerDecks().get(play0.playerID).add(play0.card);
        gameState.getPlayerDecks().get(play1.playerID).add(play1.card);
        gameState.getPlayerDecks().get(play2.playerID).add(play2.card);

        // Call endTurn
        forwardModel.endTrick(gameState);

        // Verify that the trick deck is cleared
        assertTrue(gameState.currentPlayedCards.isEmpty());

        // Verify that the winning player has the trick cards
        assertTrue(gameState.trickDecks.get(2).contains(play0.card));
        assertTrue(gameState.trickDecks.get(2).contains(play1.card));
        assertTrue(gameState.trickDecks.get(2).contains(play2.card));

        // If cards go to winning player, check their deck size.
        assertEquals(3, gameState.trickDecks.get(2).getSize());
    }

    @Test
    public void testEndTurnNewRound() {
        // Set the game phase to PLAYING
        gameState.setGamePhase(HeartsGameState.Phase.PLAYING);

        // Let's set up the game state to a hypothetical end-turn scenario.
        // Assume that three players have played the following cards:
        // Player 0 played Club 3
        Play play0 = new Play(0, new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 3));
        gameState.currentPlayedCards.add(new AbstractMap.SimpleEntry<>(play0.playerID, play0.card));

        // Player 1 played Club 2
        Play play1 = new Play(1, new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 2));
        gameState.currentPlayedCards.add(new AbstractMap.SimpleEntry<>(play1.playerID, play1.card));

        // Player 2 played Club 4 (highest in the trick)
        Play play2 = new Play(2, new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 4));
        gameState.currentPlayedCards.add(new AbstractMap.SimpleEntry<>(play2.playerID, play2.card));

        // Set the firstCardSuit as Club which was set when first card was played
        gameState.firstCardSuit = FrenchCard.Suite.Clubs;

        // Set the player's deck
        gameState.getPlayerDecks().get(play0.playerID).add(play0.card);
        gameState.getPlayerDecks().get(play1.playerID).add(play1.card);
        gameState.getPlayerDecks().get(play2.playerID).add(play2.card);

        // Clear all the cards from player's deck
        gameState.getPlayerDecks().clear();

        // Call endTurn
        forwardModel.endTrick(gameState);


        // Verify that a new round has started
        assertEquals(1, gameState.getRoundCounter());
    }

    @Test
    public void testGetCurrentPlayer() {
        // Set the game phase to PLAYING
        gameState.setGamePhase(HeartsGameState.Phase.PLAYING);

        // Test that the correct player's turn is returned
        assertEquals(0, gameState.getCurrentPlayer());
    }

    @Test
    public void testGetDrawDeck() {
        // Add a card to the draw deck
        FrenchCard card = new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 2);
        gameState.getDrawDeck().add(card);

        // Check that the draw deck contains the added card
        assertTrue(gameState.getDrawDeck().contains(card));
    }


    @Test
    public void testGetPlayerDecks() {
        // Clear all the old cards from player's deck
        gameState.getPlayerDecks().clear();

        // Create a deck for a player
        Deck<FrenchCard> deck = new Deck<>(("Player " + 0 + " deck"), 0, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        deck.add(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 2));

        // Add the deck to the player decks
        gameState.getPlayerDecks().add(deck);

        // Test that the correct deck is returned
        assertEquals(deck, gameState.getPlayerDecks().get(0));
    }

    @Test
    public void testCalculatePoints() {
        // Create a trick deck for a player with some cards
        Deck<FrenchCard> deck = new Deck<>(("Player " + 0 + " deck"), 0, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        deck.add(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Hearts, 2)); // 1 point
        deck.add(new FrenchCard(FrenchCard.FrenchCardType.Queen, FrenchCard.Suite.Spades)); // 13 points

        // Set the trick deck in the game state
        gameState.trickDecks.add(0, deck);

        // Calculate points
        gameState.scorePointsAtEndOfRound();

        // Test that the correct number of points was calculated
        assertEquals(14, gameState.getPlayerPoints(0));
    }

    @Test
    public void testGetPlayerPoints() {
        // Set some points for a player
        gameState.playerPoints.put(0, 10);

        // Test that the correct number of points is returned
        assertEquals(10, gameState.getPlayerPoints(0));
    }

    @Test
    public void testGetGameScore() {
        // Set the game state
        gameState.setGamePhase(HeartsGameState.Phase.PLAYING);

        // Assign points to players
        gameState.scorePointsAtEndOfRound();  // Assuming that player 0 has some cards in their trick deck

        // Test getGameScore
        int playerScore = (int) gameState.getGameScore(0);
        assertEquals(playerScore, gameState.getPlayerPoints(0));
    }

    @Test
    public void testGetOrdinalPosition() {
        // Set the game state
        gameState.setGamePhase(HeartsGameState.Phase.PLAYING);

        // Set some points for players
        gameState.playerPoints.put(0, 10);
        gameState.playerPoints.put(1, 30);
        gameState.playerPoints.put(2, 15);


        // Test getOrdinalPosition
        int ordinal = gameState.getOrdinalPosition(0);
        assertEquals(1, ordinal);  // Assuming that player 0 has more points than player 1
    }










}







