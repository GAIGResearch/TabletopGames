package games.hearts;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IComponentContainer;
import games.hearts.actions.Play;
import games.hearts.actions.Pass;

import java.util.*;

/**
 * <p>The forward model contains all the game rules and logic. It is mainly responsible for declaring rules for:</p>
 * <ol>
 *     <li>Game setup</li>
 *     <li>Actions available to players in a given game state</li>
 *     <li>Game events or rules applied after a player's action</li>
 *     <li>Game end</li>
 * </ol>
 */
public class HeartsForwardModel extends StandardForwardModel {

    @Override
    public void _setup(AbstractGameState firstState) {
        HeartsGameState hgs = (HeartsGameState) firstState;
        hgs.playerPoints = new HashMap<>();
        for (int i = 0; i < hgs.getNPlayers(); i++) {
            hgs.playerPoints.put(i, 0);
        }
        hgs.playerTricksTaken = new int[hgs.getNPlayers()];
        hgs.trickDecks = new ArrayList<>();
        hgs.playerDecks = new ArrayList<>();
        hgs.pendingPasses = new ArrayList<>(hgs.getNPlayers());
        for (int i = 0; i < hgs.getNPlayers(); i++) {
            hgs.pendingPasses.add(new ArrayList<>());
            hgs.trickDecks.add(new Deck<>("Player " + i + " deck", i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
        }
        _setupRound(hgs);
    }

    public void _setupRound(HeartsGameState hgs) {
        HeartsParameters params = (HeartsParameters) hgs.getGameParameters();
        hgs.setGamePhase(HeartsGameState.Phase.PASSING);
        hgs.heartsBroken = false;

        hgs.setFirstPlayer(0);
        hgs.pendingPasses = new ArrayList<>(hgs.getNPlayers());
        for (int i = 0; i < hgs.getNPlayers(); i++) {
            hgs.pendingPasses.add(new ArrayList<>());
        }

        hgs.playerDecks = new ArrayList<>();
        hgs.drawDeck = FrenchCard.generateDeck("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        hgs.playerTricksTaken = new int[hgs.getNPlayers()];

        int numOfPlayers = hgs.getNPlayers();

        hgs.drawDeck.removeAll(params.cardsToRemove.get(numOfPlayers));
        hgs.drawDeck.shuffle(hgs.getRnd());

        for (int i = 0; i < hgs.getNPlayers(); i++) {
            Deck<FrenchCard> playerDeck = new Deck<>("Player " + i + " deck", i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
            hgs.playerDecks.add(playerDeck);
            int numberOfCards = params.numberOfCardsPerPlayer[hgs.getNPlayers()];

            for (int card = 0; card < numberOfCards; card++) {
                playerDeck.add(hgs.drawDeck.draw());
            }
        }
    }


    public void _afterAction(AbstractGameState gameState, AbstractAction action) {
        HeartsGameState hgs = (HeartsGameState) gameState;
        HeartsParameters params = (HeartsParameters) hgs.getGameParameters();

        if (hgs.getGamePhase() == HeartsGameState.Phase.PASSING) {
            if (action instanceof Pass) {

                // Check if current player has passed 3 cards
                int cardsPassed = hgs.pendingPasses.get(hgs.getCurrentPlayer()).size();
                if (cardsPassed == params.cardsPassedPerRound) {
                    // Check if all players have passed their cards
                    if (hgs.pendingPasses.stream().allMatch(passes -> passes.size() == 3)) {
                        hgs.setGamePhase(HeartsGameState.Phase.PLAYING);

                        // Determine the pass direction based on the current round
                        int passDirection;
                        switch (hgs.getRoundCounter() % 4) {
                            case 0:  // To the left
                                passDirection = 1;
                                break;
                            case 1:  // To the right
                                passDirection = hgs.getNPlayers() - 1;
                                break;
                            case 2:  // Across the table (for 4 players)
                                passDirection = hgs.getNPlayers() / 2;
                                break;
                            case 3:  // No passing
                                passDirection = 0;
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + hgs.getRoundCounter());
                        }

                        // Add pending passes to next player's deck
                        for (int i = 0; i < hgs.getNPlayers(); i++) {
                            Deck<FrenchCard> nextPlayerDeck = hgs.playerDecks.get((i + passDirection) % hgs.getNPlayers());
                            for (FrenchCard card : hgs.pendingPasses.get(i)) {
                                nextPlayerDeck.add(card);
                            }
                            hgs.pendingPasses.get(i).clear();  // Clear this player's pending passes
                        }
                        // Set the first player of the PLAYING phase to be the player who has the 2 of clubs
                        for (int i = 0; i < hgs.getNPlayers(); i++) {
                            Deck<FrenchCard> playerDeck1 = hgs.playerDecks.get(i);
                            if (playerDeck1.contains(params.startingCard)) {
                                hgs.setFirstPlayer(i);
                                return;
                            }
                        }
                    }

                    // End player's turn here after 3 passes
                    endPlayerTurn(hgs);
                }
            } else {
                throw new IllegalArgumentException("Invalid action type during PASSING phase.");
            }
        } else {
            // Check if all players have played a card in this round
            if (hgs.currentPlayedCards.size() == hgs.getNPlayers()) {
                endTrick(hgs);
                if (hgs.isNotTerminal()) {
                    startNewTrick(hgs);
                }
                // Do not endPlayerTurn here, that is done in startNewTrick
                return;
            }
            endPlayerTurn(hgs);
        }
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        HeartsGameState hgs = (HeartsGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = hgs.getCurrentPlayer();
        Deck<FrenchCard> playerHand = hgs.playerDecks.get(player);

        if (hgs.getGamePhase() == HeartsGameState.Phase.PASSING) {
            // Generate Pass action for each card in the player's hand
            List<FrenchCard> cards = playerHand.getComponents();
            for (FrenchCard card : cards) {
                actions.add(new Pass(player, card));
            }
        } else {

            if (!hgs.trickDecks.stream().flatMap(IComponentContainer::stream).findAny().isPresent()) {
                // First turn of the game, the player with 2 of clubs must play it
                for (FrenchCard card : playerHand.getComponents()) {
                    if (card.suite == FrenchCard.Suite.Clubs && card.number == 2) {
                        actions.add(new Play(player, card));
                        return actions;  // Return immediately, no other actions available
                    }
                }
            }

            if (hgs.firstCardSuit == null) {
                // this is the lead player, they can play any card (except for Hearts if they are not yet broken, or they have no choice)
                boolean onlyHasHearts = playerHand.getComponents().stream().allMatch(card -> card.suite == FrenchCard.Suite.Hearts);
                for (FrenchCard card : playerHand.getComponents()) {
                    if (onlyHasHearts || hgs.heartsBroken || card.suite != FrenchCard.Suite.Hearts) {
                        actions.add(new Play(player, card));
                    }
                }
            } else {
                // Check if player has any cards of the lead suit
                boolean hasLeadSuit = playerHand.getComponents().stream().anyMatch(card -> card.suite.equals(hgs.firstCardSuit));

                if (hasLeadSuit) {
                    // Player can only play cards of the lead suit
                    for (FrenchCard card : playerHand.getComponents()) {
                        if (card.suite.equals(hgs.firstCardSuit)) {
                            actions.add(new Play(player, card));
                        }
                    }
                } else {
                    for (FrenchCard card : playerHand.getComponents()) {
                        actions.add(new Play(player, card));
                    }
                }
            }
        }


        return actions;
    }

    public void endTrick(HeartsGameState hgs) {
        HeartsParameters params = (HeartsParameters) hgs.getGameParameters();
        int highestCardValue = -1;
        int winningPlayerID = -1;
        for (Map.Entry<Integer, FrenchCard> entry : hgs.currentPlayedCards) {
            FrenchCard card = entry.getValue();
            if (card.suite.equals(hgs.firstCardSuit) && card.number > highestCardValue) {
                highestCardValue = card.number;
                winningPlayerID = entry.getKey();
            }
        }

        // Add all cards from this round to the winner's trick deck
        if (winningPlayerID != -1) {
            for (Map.Entry<Integer, FrenchCard> entry : hgs.currentPlayedCards) {
                hgs.trickDecks.get(winningPlayerID).add(entry.getValue());
            }
            hgs.playerTricksTaken[winningPlayerID]++;

            hgs.setFirstPlayer(winningPlayerID);
        } else {
            throw new AssertionError("We must have a trick winner");
        }
        hgs.currentPlayedCards.clear();

        // Check if all cards from player hands have been played
        if (hgs.playerDecks.stream().allMatch(deck -> deck.getSize() == 0)) {
            hgs.scorePointsAtEndOfRound();
            boolean scoreAbove100 = hgs.playerPoints.values().stream().anyMatch(score -> score >= params.matchScore);

            // If any player has reached 100 points or more, end the game
            if (scoreAbove100) {
                endGame(hgs);
            } else {
                endRound(hgs);
                // If no player has reached 100 points yet, reshuffle and deal new hands
                _setupRound(hgs);
            }
        }
    }

    public void forceGameEnd(HeartsGameState state) {
        endGame(state);
    }

    private void startNewTrick(HeartsGameState hgs) {
        hgs.firstCardSuit = null;
        endPlayerTurn(hgs, hgs.getFirstPlayer());
    }

}