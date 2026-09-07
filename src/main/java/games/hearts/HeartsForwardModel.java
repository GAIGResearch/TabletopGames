package games.hearts;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
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
        hgs.heartsBroken = false;

        hgs.pendingPasses = new ArrayList<>(hgs.getNPlayers());
        for (int i = 0; i < hgs.getNPlayers(); i++) {
            hgs.pendingPasses.add(new ArrayList<>());
        }

        hgs.playerDecks = new ArrayList<>();
        hgs.drawDeck = FrenchCard.generateDeck("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        hgs.playerTricksTaken = new int[hgs.getNPlayers()];

        // known voids are only valid for the current round, as hands are re-dealt each round
        hgs.knownVoids = new ArrayList<>();
        for (int i = 0; i < hgs.getNPlayers(); i++) {
            hgs.knownVoids.add(EnumSet.noneOf(FrenchCard.Suite.class));
        }

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

        if (passDirection(hgs) == 0) {
            // No passing this round, so nobody has a decision to make until the first trick
            startPlayingPhase(hgs);
        } else {
            hgs.setGamePhase(HeartsGameState.Phase.PASSING);
            hgs.setFirstPlayer(0);
        }
    }

    /**
     * How many seats to the left the passed cards travel this round: left, right, across, and then
     * a round with no passing at all.
     */
    private int passDirection(HeartsGameState hgs) {
        switch (hgs.getRoundCounter() % 4) {
            case 0:
                return 1;
            case 1:
                return hgs.getNPlayers() - 1;
            case 2:
                return hgs.getNPlayers() / 2;
            case 3:
                return 0;
            default:
                throw new IllegalStateException("Unexpected value: " + hgs.getRoundCounter());
        }
    }

    /**
     * Everyone has committed all their cards: hand them on, and start trick play with the player
     * who now holds the starting card.
     */
    private void resolvePasses(HeartsGameState hgs) {
        int passDirection = passDirection(hgs);
        for (int i = 0; i < hgs.getNPlayers(); i++) {
            Deck<FrenchCard> nextPlayerDeck = hgs.playerDecks.get((i + passDirection) % hgs.getNPlayers());
            for (FrenchCard card : hgs.pendingPasses.get(i)) {
                nextPlayerDeck.add(card);
            }
            hgs.pendingPasses.get(i).clear();
        }
        startPlayingPhase(hgs);
    }

    private void startPlayingPhase(HeartsGameState hgs) {
        HeartsParameters params = (HeartsParameters) hgs.getGameParameters();
        hgs.setGamePhase(HeartsGameState.Phase.PLAYING);
        for (int i = 0; i < hgs.getNPlayers(); i++) {
            if (hgs.playerDecks.get(i).contains(params.startingCard)) {
                hgs.setFirstPlayer(i);
                return;
            }
        }
        throw new AssertionError("No player holds the starting card " + params.startingCard);
    }

    /**
     * The next player, cycling round from the current one, who has not yet committed a card in the
     * current passing sub-turn. At the end of a sub-turn that is simply the next player round.
     */
    private int nextPlayerToPass(HeartsGameState hgs) {
        List<Integer> stillToPass = hgs.getPlayersStillToPass();
        int current = hgs.getCurrentPlayer();
        for (int i = 1; i <= hgs.getNPlayers(); i++) {
            int p = (current + i) % hgs.getNPlayers();
            if (stillToPass.contains(p))
                return p;
        }
        throw new AssertionError("No player is still to pass");
    }

    public void _afterAction(AbstractGameState gameState, AbstractAction action) {
        HeartsGameState hgs = (HeartsGameState) gameState;
        HeartsParameters params = (HeartsParameters) hgs.getGameParameters();

        if (hgs.getGamePhase() == HeartsGameState.Phase.PASSING) {
            if (!(action instanceof Pass) && !(action instanceof SimultaneousAction)) {
                throw new IllegalArgumentException("Invalid action type during PASSING phase.");
            }
            // Passing is simultaneous, one card at a time: everyone commits a card, then everyone
            // commits their next one. The choices may arrive one player at a time, in which case the
            // turn is handed round the players still to commit, or all together as one
            // SimultaneousAction; either way the passes resolve once everyone has committed all of
            // theirs, and trick play starts with the holder of the starting card.
            int fewest = hgs.fewestPendingPasses();
            int most = hgs.pendingPasses.stream().mapToInt(List::size).max().orElse(0);
            if (most > fewest + 1) {
                throw new AssertionError("A player has committed a card before everyone had committed the previous one");
            }
            if (fewest == params.cardsPassedPerRound) {
                resolvePasses(hgs);
                return;
            }
            endPlayerTurn(hgs, nextPlayerToPass(hgs));
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
        return _computeAvailableActions(gameState, gameState.getCurrentPlayer());
    }

    /**
     * The actions open to the given player. While passing, any player still to commit a card this
     * sub-turn may pass any card in their hand. During trick play only the player whose turn it is
     * has any actions at all.
     */
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, int player) {
        HeartsGameState hgs = (HeartsGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Deck<FrenchCard> playerHand = hgs.playerDecks.get(player);

        if (hgs.getGamePhase() == HeartsGameState.Phase.PASSING) {
            if (hgs.getPlayersStillToPass().contains(player)) {
                for (FrenchCard card : playerHand.getComponents()) {
                    actions.add(new Pass(player, card));
                }
            }
        } else if (player == hgs.getCurrentPlayer()) {

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