package games.uno;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.uno.actions.NoCards;
import games.uno.actions.PlayCard;
import games.uno.cards.UnoCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.VisibilityMode.*;

public class UnoForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        UnoGameState ugs = (UnoGameState) firstState;

        // Set up scores for all players, initially 0
        ugs.playerScore = new int[firstState.getNPlayers()];
        ugs.playerDecks = new ArrayList<>();
        for (int i = 0; i < ugs.getNPlayers(); i++) {
            ugs.playerDecks.add(new Deck<>("Player " + i + " deck", i, VISIBLE_TO_OWNER));
        }

        // Create the draw deck with all the cards
        ugs.drawDeck = new Deck<>("DrawDeck", HIDDEN_TO_ALL);
        createCards(ugs);

        // Create the discard deck, at the beginning it is empty
        ugs.discardDeck = new Deck<>("DiscardDeck", VISIBLE_TO_ALL);

        // Player 0 starts the game
        ugs.getTurnOrder().setStartingPlayer(0);

        // Set up first round
        setupRound(ugs);
    }

    /**
     * Create all the cards and include them into the drawPile.
     * @param ugs - current game state.
     */
    private void createCards(UnoGameState ugs) {
        UnoGameParameters ugp = (UnoGameParameters)ugs.getGameParameters();
        for (String color : ugp.colors) {
            if (!color.equals("Wild")) {

                // Create the number cards
                for (int number = 0; number < ugp.nNumberCards; number++) {
                    ugs.drawDeck.add(new UnoCard(UnoCard.UnoCardType.Number, color, number));
                    if (number > 0)
                        ugs.drawDeck.add(new UnoCard(UnoCard.UnoCardType.Number, color, number));
                }

                // Create the DrawTwo, Reverse and Skip cards for each color
                for (int i = 0; i < ugp.nSkipCards; i++) {
                    ugs.drawDeck.add(new UnoCard(UnoCard.UnoCardType.Skip, color));
                }
                for (int i = 0; i < ugp.nReverseCards; i++) {
                    ugs.drawDeck.add(new UnoCard(UnoCard.UnoCardType.Reverse, color));
                }
                for (int i = 0; i < ugp.nDrawCards; i++) {
                    for (int n : ugp.specialDrawCards) {
                        ugs.drawDeck.add(new UnoCard(UnoCard.UnoCardType.Draw, color, n));
                    }
                }
            }
        }

        // Create the wild cards, N of each type
        for (int i = 0; i < ugp.nWildCards; i++) {
            for (int n : ugp.specialWildDrawCards) {
                ugs.drawDeck.add(new UnoCard(UnoCard.UnoCardType.Wild, "Wild", n));
            }
        }
    }

    private void drawCardsToPlayers(UnoGameState ugs) {
        for (int player = 0; player < ugs.getNPlayers(); player++) {
            for (int card = 0; card < ((UnoGameParameters)ugs.getGameParameters()).nCardsPerPlayer; card++) {
                ugs.playerDecks.get(player).add(ugs.drawDeck.draw());
            }
        }
    }

    /**
     * Sets up a round for the game, including draw pile, discard deck and player decks, all reset.
     * @param ugs - current game state.
     */
    private void setupRound(UnoGameState ugs) {
        Random r = new Random(ugs.getGameParameters().getRandomSeed() + ugs.getTurnOrder().getRoundCounter());

        // Refresh player decks
        for (int i = 0; i < ugs.getNPlayers(); i++) {
            ugs.drawDeck.add(ugs.playerDecks.get(i));
            ugs.playerDecks.get(i).clear();
        }

        // Refresh draw deck and shuffle
        ugs.drawDeck.add(ugs.discardDeck);
        ugs.discardDeck.clear();
        ugs.drawDeck.shuffle(r);

        // Draw new cards for players
        drawCardsToPlayers(ugs);

        // Get current card and set the current card and color
        ugs.currentCard  = ugs.drawDeck.draw();
        ugs.currentColor = ugs.currentCard.color;

        // The first card cannot be a wild.
        // In case, add to draw deck and shuffle again
        while (ugs.isWildCard(ugs.currentCard))
        {
            if (ugs.getCoreGameParameters().verbose) {
                System.out.println("First card wild");
            }
            ugs.drawDeck.add(ugs.currentCard);
            ugs.drawDeck.shuffle(r);
            ugs.currentCard = ugs.drawDeck.draw();
            ugs.currentColor = ugs.currentCard.color;
        }

        // If the first card is Skip, Reverse or DrawTwo, play the card
        if (!ugs.isNumberCard(ugs.currentCard)) {
            if (ugs.getCoreGameParameters().verbose) {
                System.out.println("First card no number " + ugs.currentColor);
            }
            if (ugs.currentCard.type == UnoCard.UnoCardType.Reverse) {
                ((UnoTurnOrder) ugs.getTurnOrder()).reverse();
            }
            else if (ugs.currentCard.type == UnoCard.UnoCardType.Draw) {
                int player = ugs.getCurrentPlayer();
                for (int i = 0; i < ugs.currentCard.drawN; i++) {
                    ugs.playerDecks.get(player).add(ugs.drawDeck.draw());
                }
            }
            ugs.getTurnOrder().endPlayerTurn(ugs);
        }

        // add current card to discard deck
        ugs.discardDeck.add(ugs.currentCard);
    }

    @Override
    protected void _next(AbstractGameState gameState, AbstractAction action) {
        action.execute(gameState);
        if (checkRoundEnd((UnoGameState)gameState)) {
            return;
        }
//        if (checkRunningTotal((UnoGameState)gameState)) {
//            return;
//        }
        if (gameState.getGameStatus() == Utils.GameResult.GAME_ONGOING) {
            gameState.getTurnOrder().endPlayerTurn(gameState);
        }
    }

    /**
     * Checks if the round ended (when one player runs out of cards). On round end, points for all players are added up
     * and next round is set up.
     * @param ugs - current game state
     * @return true if round ended, false otherwise
     */
    private boolean checkRoundEnd(UnoGameState ugs) {
        // Did any player run out of cards?
        boolean roundEnd = false;
        for (int playerID = 0; playerID < ugs.getNPlayers(); playerID++) {
            if (ugs.getPlayerResults()[playerID] == Utils.GameResult.GAME_ONGOING) {
                if (ugs.playerDecks.get(playerID).getComponents().size() == 0) {
                    roundEnd = true;
                    break;
                }
            }
        }

        if (roundEnd) {
            // Add up points
            for (int i = 0; i < ugs.getNPlayers(); i++) {
                ugs.playerScore[i] += ugs.calculatePlayerPoints(i);
            }
//            System.out.println("Round end " + Arrays.toString(ugs.playerScore));
            ugs.getTurnOrder().endRound(ugs);

            // Did this player just hit N points to win? Win condition check!
            if (checkGameEnd(ugs, ugs.playerScore)) return true;

            // Reset cards for the new round
            setupRound(ugs);

            return false;
        }
        return false;
    }

    /**
     * Alternative rules check running total of points for the players.
     * @param ugs - current game state, calculates new total based on this state and checks game end.
     * @return - true if game has ended, false otherwise.
     */
    private boolean checkRunningTotal(UnoGameState ugs) {
        UnoGameParameters ugp = (UnoGameParameters) ugs.getGameParameters();

        int[] playerScores = new int[ugs.getNPlayers()];
        for (int playerID = 0; playerID < ugs.getNPlayers(); playerID++) {
            if (ugs.getPlayerResults()[playerID] == Utils.GameResult.GAME_ONGOING) {
                int nPoints = ugs.playerScore[playerID] + ugs.calculatePlayerPoints(playerID);
                playerScores[playerID] = nPoints;
            }
        }

        return checkGameEnd(ugs, playerScores);
    }

    /**
     * The game is ended when a player reaches N points (as total of points from cards of all other players)
     * @param playerScores - player scores to use for checking game end
     * @param ugs - current game state
     */
    private boolean checkGameEnd(UnoGameState ugs, int[] playerScores) {
        UnoGameParameters ugp = (UnoGameParameters) ugs.getGameParameters();

        int[] nCardsPlayer = new int[ugs.getNPlayers()];
        boolean aWinner = false;
        boolean noCardsWinner = false;
        boolean tie = false;
        int maxScore = ugp.nWinPoints;
        int idMaxScore = -1;

        for (int playerID = 0; playerID < ugs.getNPlayers(); playerID++) {
            nCardsPlayer[playerID] = ugs.playerDecks.get(playerID).getComponents().size();

            // A winner!
            if (playerScores[playerID] >= maxScore) {
                if (playerScores[playerID] == maxScore) {
                    tie = aWinner;
                } else {
                    tie = false;
                }
                aWinner = true;
                maxScore = playerScores[playerID];
                idMaxScore = playerID;
                if (nCardsPlayer[playerID] == 0) {
                    noCardsWinner = true;
                }
            }
        }

        if (aWinner) {
            // Game is over
            HashSet<Integer> winners = new HashSet<>();

            if (noCardsWinner) {
                if (tie) {
                    // Check all points
                    for (int i = 0; i < ugs.getNPlayers(); i++) {
                        if (playerScores[i] == maxScore) {
                            winners.add(i);
                        }
                    }
                } else {
                    winners.add(idMaxScore);
                }
            } else {
                // The one with the lowest number of points wins
                int minScore = maxScore;
                for (int i = 0; i < ugs.getNPlayers(); i++) {
                    if (playerScores[i] < minScore) {
                        minScore = playerScores[i];
                    }
                }
                for (int i = 0; i < ugs.getNPlayers(); i++) {
                    if (playerScores[i] == minScore) {
                        winners.add(i);
                    }
                }
            }

            for (int i = 0; i < ugs.getNPlayers(); i++) {
                if (winners.contains(i)) {
                    ugs.setPlayerResult(Utils.GameResult.WIN, i);
                } else {
                    ugs.setPlayerResult(Utils.GameResult.LOSE, i);
                }
            }
            ugs.setGameStatus(Utils.GameResult.GAME_END);
            return true;
        }
        return false;
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        UnoGameState ugs = (UnoGameState)gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = ugs.getCurrentPlayer();

        Deck<UnoCard> playerHand = ugs.playerDecks.get(player);
        for (UnoCard card : playerHand.getComponents()) {
            int cardIdx = playerHand.getComponents().indexOf(card);
            if (card.isPlayable(ugs)) {
                if (ugs.isWildCard(card)) {
                    for (String color : ((UnoGameParameters)ugs.getGameParameters()).colors) {
                        actions.add(new PlayCard(playerHand.getComponentID(), ugs.discardDeck.getComponentID(), cardIdx, color));
                    }
                }
                else {
                    actions.add(new PlayCard(playerHand.getComponentID(), ugs.discardDeck.getComponentID(), cardIdx));
                }
            }
        }

        if (actions.isEmpty()) {
            actions.add(new NoCards());
        }

        return actions;
    }

    @Override
    protected void endGame(AbstractGameState gameState) {
        if (gameState.getCoreGameParameters().verbose) {
            System.out.println("Game Results:");
            for (int playerID = 0; playerID < gameState.getNPlayers(); playerID++) {
                if (gameState.getPlayerResults()[playerID] == Utils.GameResult.WIN) {
                    System.out.println("The winner is the player : " + playerID);
                    break;
                }
            }
        }
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new UnoForwardModel();
    }
}

