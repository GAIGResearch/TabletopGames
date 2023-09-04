package games.hearts;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
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
        hgs.chosenCards = new HashMap<>();
        hgs.playerPoints = new HashMap<>();
        hgs.resetGameScores();
        hgs.playerPassCounter = new int[hgs.getNPlayers()];
        hgs.playerTricksTaken = new int[hgs.getNPlayers()];
        hgs.trickDecks = new ArrayList<>();
        hgs.playerDecks = new ArrayList<>();
        hgs.pendingPasses = new ArrayList<>(hgs.getNPlayers());
        for (int i = 0; i < hgs.getNPlayers(); i++) {
            hgs.pendingPasses.add(new ArrayList<>());
            hgs.trickDecks.add(new Deck<>("Player " + i + " deck", i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
        }
        hgs.passedCards = new ArrayList<>(hgs.getNPlayers());
        for (int i = 0; i < hgs.getNPlayers(); i++) {
            hgs.passedCards.add(new ArrayList<>());
        }
        _setupRound(hgs);
    }

    public void _setupRound(HeartsGameState hgs){
        hgs.setGamePhase(HeartsGameState.Phase.PASSING);

        hgs.playerWithTwoOfClubs = -1;
        hgs.setFirstPlayer(0);
        hgs.pendingPasses = new ArrayList<>(hgs.getNPlayers());
        for (int i = 0; i < hgs.getNPlayers(); i++) {
            hgs.pendingPasses.add(new ArrayList<>());
        }

        hgs.playerDecks = new ArrayList<>();
        hgs.drawDeck = FrenchCard.generateDeck("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        hgs.drawDeck.shuffle(new Random(System.currentTimeMillis()));

        int numOfPlayers = hgs.getNPlayers();

        Map<Integer, List<FrenchCard>> cardsToRemove = new HashMap<>();
        cardsToRemove.put(3, Arrays.asList(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 2)));
        cardsToRemove.put(5, Arrays.asList(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 2),
                new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Spades, 2)));
        cardsToRemove.put(6, Arrays.asList(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 2),
                new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 3),
                new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 3),
                new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 4)));
        cardsToRemove.put(7, Arrays.asList(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 2),
                new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 3),
                new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 3)));

        if (cardsToRemove.containsKey(numOfPlayers)) {
            for (FrenchCard removeCard : cardsToRemove.get(numOfPlayers)) {
                int removeIndex = -1;
                for (int i = 0; i < 52; i++) {
                    FrenchCard card = hgs.drawDeck.get(i);
                    if (card.suite == removeCard.suite && card.number == removeCard.number && card.type == removeCard.type) {
                        removeIndex = i;
                        break;
                    }
                }
                if (removeIndex != -1) {
                    hgs.drawDeck.remove(removeIndex);
                }
            }
        }

        for (int i = 0; i < hgs.getNPlayers(); i++){
            Deck<FrenchCard> playerDeck = new Deck<>("Player " + i + " deck", i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
            hgs.playerDecks.add(playerDeck);
            int numberOfCards = getNumberOfCards(hgs);

            for (int card = 0; card < numberOfCards; card++) {
                playerDeck.add(hgs.drawDeck.draw());
            }
        }
    }

    private static int getNumberOfCards(HeartsGameState hgs) {
        int numberOfCards;
        if (hgs.getNPlayers() == 3) {
            numberOfCards = 17;
        } else if(hgs.getNPlayers()==4){
            numberOfCards = 13;
        }
        else if (hgs.getNPlayers() == 5) {
            numberOfCards = 10;
        } else if (hgs.getNPlayers() == 6) {
            numberOfCards = 8;
        } else if (hgs.getNPlayers() == 7) {
            numberOfCards = 7;
        } else {
            numberOfCards = 0;
        }
        return numberOfCards;
    }

    public void _afterAction(AbstractGameState gameState, AbstractAction action) {
        HeartsGameState hgs = (HeartsGameState) gameState;

        if (hgs.getGamePhase() == HeartsGameState.Phase.PASSING) {
            if (action instanceof Pass) {
                Pass passAction = (Pass) action;

                Deck<FrenchCard> playerDeck = hgs.playerDecks.get(passAction.playerID);

                playerDeck.remove(passAction.card1);
                hgs.pendingPasses.get(passAction.playerID).add(passAction.card1);

                hgs.playerPassCounter[passAction.playerID]++;

                // Check if current player has passed 3 cards
                if (hgs.playerPassCounter[passAction.playerID] == 3) {
                    // Reset player's pass counter
                    hgs.playerPassCounter[passAction.playerID] = 0;

                    // Check if all players have passed their cards
                    if (passAction.playerID == hgs.getNPlayers() - 1) {
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
                            for (FrenchCard card : playerDeck1.getComponents()) {
                                if (card.suite == FrenchCard.Suite.Clubs && card.number == 2) {
                                    hgs.setFirstPlayer(i);
                                    return;
                                }
                            }
                        }
                    }

                    // End player's turn here after 3 passes
                    endPlayerTurn(hgs);
                    return;
                }
            } else {
                throw new IllegalArgumentException("Invalid action type during PASSING phase.");
            }
        } else {
            if (action instanceof Play) {
                Play play = (Play) action;
                if (hgs.currentPlayedCards.isEmpty()) {
                    hgs.firstCardSuit = play.card.suite;  // Save the suit of the first card
                }

                // Store played card and its player ID
                hgs.currentPlayedCards.add(new AbstractMap.SimpleEntry<>(play.playerID, play.card));

                // If a heart has been played, set heartsBroken to true
                if (play.card.suite == FrenchCard.Suite.Hearts) {
                    hgs.heartsBroken = true;
                }

                // Remove the card from the player's deck
                hgs.playerDecks.get(play.playerID).remove(play.card);
                hgs.calculatePoints(play.playerID);
            }

            // Check if all players have played a card in this round
            if (hgs.currentPlayedCards.size() == hgs.getNPlayers()) {
                endTrick(hgs);
                if (hgs.isNotTerminal()) {
                    startNewTrick(hgs);
                }
                // Do not endPlayerTurn here, that is done in startNewTrick
                return;
            }
        }

        // Only end player's turn here if it's not PASSING phase
        // As in PASSING phase a player decides three cards, abnd then the next player decides
        if (hgs.getGamePhase() != HeartsGameState.Phase.PASSING) {
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
        }else {

            if (hgs.getTurnCounter() == 0) {
                // First turn of the game, the player with 2 of clubs must play it
                for (FrenchCard card : playerHand.getComponents()) {
                    if (card.suite == FrenchCard.Suite.Clubs && card.number == 2) {
                        actions.add(new Play(player, card));
                        return actions;  // Return immediately, no other actions available
                    }
                }
            }

            boolean hasLeadSuit = playerHand.getComponents().stream().anyMatch(card -> card.suite.equals(hgs.firstCardSuit));

            if (hasLeadSuit) {
                // Player can only play cards of the lead suit
                for (FrenchCard card : playerHand.getComponents()) {
                    if (card.suite.equals(hgs.firstCardSuit)) {
                        actions.add(new Play(player, card));
                    }
                }
            } else {
                // Player can play cards of any other suit, but only play a heart if hearts have been broken or their hand only contains hearts
                boolean onlyHasHearts = playerHand.getComponents().stream().allMatch(card -> card.suite == FrenchCard.Suite.Hearts);
                for (FrenchCard card : playerHand.getComponents()) {
                    if (card.suite != FrenchCard.Suite.Hearts || hgs.heartsBroken || onlyHasHearts) {
                        actions.add(new Play(player, card));
                    }
                }
            }
        }


        return actions;
    }

    public void endTrick(HeartsGameState hgs) {
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
        }
        hgs.currentPlayedCards.clear();

        // Check if all cards from player hands have been played
        if (hgs.playerDecks.stream().allMatch(deck -> deck.getSize() == 0)) {
            boolean scoreAbove100 = hgs.getPlayerPointsMap().values().stream().anyMatch(score -> score >= 100);

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