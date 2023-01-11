package games.sushigo;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import utilities.Utils;

import java.util.*;
import static games.sushigo.cards.SGCard.SGCardType.*;

public class SGForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        SGGameState gs = (SGGameState) firstState;
        SGParameters parameters = (SGParameters) gs.getGameParameters();
        gs.nCardsInHand = 0;
        gs.deckRotations = 0;
        gs.playerScore = new Counter[firstState.getNPlayers()];
        gs.cardChoices = new ArrayList<>(firstState.getNPlayers());
        gs.playedCardTypes = new HashMap[firstState.getNPlayers()];
        gs.playedCards = new ArrayList<>();

        // Setup draw & discard piles
        gs.drawPile = new Deck<>("Draw pile", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        gs.discardPile = new Deck<>("Discard pile", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        setupDrawPile(gs);

        // Setup player-specific variables
        gs.playerHands = new ArrayList<>();
        gs.nCardsInHand = parameters.nCards - firstState.getNPlayers() + 2;
        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerScore[i] = new Counter(0, 0, Integer.MAX_VALUE, "Player " + i + " score");
            gs.playerHands.add(new Deck<>("Player " + i + " hand", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            gs.playedCards.add(new Deck<>("Player " + i + " played cards", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
            gs.playedCardTypes[i] = new HashMap<>();
            for (SGCard.SGCardType type: SGCard.SGCardType.values()) {
                gs.playedCardTypes[i].put(type, new Counter(0, 0, Integer.MAX_VALUE, type.name()));
            }
            gs.cardChoices.add(new ArrayList<>());

            // Draw initial hand of cards
            for (int j = 0; j < gs.nCardsInHand; j++) {
                gs.playerHands.get(i).add(gs.drawPile.draw());
            }
        }

        // Set starting player
        gs.getTurnOrder().setStartingPlayer(0);
    }

    /**
     * Adds to the draw pile all the cards in the game. How many of each type are added depends on game parameters.
     * Maki cards are special, those can have multiple Maki icons on them (1-3). Hardcoded and ugly, but no easy way around it.
     * @param gs - game state to add cards to
     */
    private void setupDrawPile(SGGameState gs) {
        SGParameters parameters = (SGParameters) gs.getGameParameters();
        for (int i = 0; i < parameters.nMaki_3Cards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Maki, 3));
        }
        for (int i = 0; i < parameters.nMaki_2Cards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Maki, 2));
        }
        for (int i = 0; i < parameters.nMaki_1Cards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Maki, 1));
        }
        for (int i = 0; i < parameters.nChopstickCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Chopsticks));
        }
        for (int i = 0; i < parameters.nTempuraCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Tempura));
        }
        for (int i = 0; i < parameters.nSashimiCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Sashimi));
        }
        for (int i = 0; i < parameters.nDumplingCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Dumpling));
        }
        for (int i = 0; i < parameters.nSquidNigiriCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.SquidNigiri));
        }
        for (int i = 0; i < parameters.nSalmonNigiriCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.SalmonNigiri));
        }
        for (int i = 0; i < parameters.nEggNigiriCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.EggNigiri));
        }
        for (int i = 0; i < parameters.nWasabiCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Wasabi));
        }
        for (int i = 0; i < parameters.nPuddingCards; i++) {
            gs.drawPile.add(new SGCard(SGCard.SGCardType.Pudding));
        }
        gs.drawPile.shuffle(new Random(parameters.getRandomSeed()));
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        SGGameState gs = (SGGameState) currentState;

        // Check if all players made their choice
        int turn = gs.getTurnOrder().getTurnCounter();
        if ((turn + 1) % gs.getNPlayers() == 0) {
            // They did! Reveal all cards at once. Process card reveal rules.
            revealCards(gs);

            // Check if the round is over
            if (isRoundOver(gs)) {
                // It is! Process end of round rules.
                gs.getTurnOrder().endRound(currentState);

                // Clear card choices from this turn, ready for the next simultaneous choice.
                gs.clearCardChoices();

                // Check if the game is over
                if (gs.getTurnOrder().getRoundCounter() >= ((SGParameters)gs.getGameParameters()).nRounds) {
                    // It is! Process end of game rules.
                    for (SGCard.SGCardType type: values()) {
                        type.onGameEnd(gs);
                    }
                    // Decide winner
                    gs.endGame();
                    return;
                }
                return;
            } else {
                // Round is not over, keep going. Rotate hands for next player turns.
                rotatePlayerHands(gs);

                // Clear card choices from this turn, ready for the next simultaneous choice.
                gs.clearCardChoices();
            }
        }

        // End player turn
        if (currentState.getGameStatus() == Utils.GameResult.GAME_ONGOING) {
            currentState.getTurnOrder().endPlayerTurn(currentState);
        }
    }

    /**
     * Reveals all player cards simultaneously and applies on reveal rules for each card type
     * @param gs - game state
     */
    void revealCards(SGGameState gs) {
        for (int i = 0; i < gs.getNPlayers(); i++) {
            Deck<SGCard> hand = gs.getPlayerHands().get(i);
            for (ChooseCard cc: gs.cardChoices.get(i)) {
                SGCard cardToReveal = hand.get(cc.cardIdx);

                hand.remove(cardToReveal);
                gs.playedCards.get(i).add(cardToReveal);
                gs.playedCardTypes[i].get(cardToReveal.type).increment(cardToReveal.count);

                //Add points to player
                cardToReveal.type.onReveal(gs, i);

                if (cc.useChopsticks) {
                    removeUsedChopsticks(gs, i);
                }
            }
        }
    }

    /**
     * Puts chopsticks card back in the player's hand if used.
     * @param gs - game state
     * @param playerId - player Id
     */
    private void removeUsedChopsticks(SGGameState gs, int playerId) {
        gs.playedCardTypes[playerId].get(SGCard.SGCardType.Chopsticks).decrement(1);
        SGCard chopsticks = null;
        for (SGCard card: gs.playedCards.get(playerId).getComponents()) {
            if (card.type == Chopsticks) {
                chopsticks = card;
                break;
            }
        }
        if (chopsticks == null)
            throw new IllegalStateException("Used Chopsticks when none were available");
        gs.playedCards.get(playerId).remove(chopsticks);
        gs.getPlayerHands().get(playerId).add(chopsticks);
    }

    /**
     * Checks if a round is over (all cards in hand played)
     * @param gs - game state
     * @return - true if round over, false otherwise
     */
    boolean isRoundOver(SGGameState gs) {
        for (int i = 0; i < gs.getPlayerHands().size(); i++) {
            if (gs.getPlayerHands().get(i).getSize() > 0) return false;
        }
        return true;
    }

    /**
     * Passes on player hands to the next player between turns, using a temporary deck for the swap.
     * @param gs - game state
     */
    void rotatePlayerHands(SGGameState gs) {
        gs.deckRotations++;
        Deck<SGCard> tempDeck;
        tempDeck = gs.getPlayerHands().get(0).copy();
        for (int i = 1; i < gs.getNPlayers(); i++) {
            gs.getPlayerHands().set(i - 1, gs.getPlayerHands().get(i).copy());
        }
        gs.getPlayerHands().set(gs.getNPlayers() - 1, tempDeck.copy());
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        SGGameState sggs = (SGGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();

        int currentPlayer = sggs.getCurrentPlayer();
        Deck<SGCard> currentPlayerHand = sggs.getPlayerHands().get(currentPlayer);
        for (int i = 0; i < currentPlayerHand.getSize(); i++) {
            // All players can do is choose a card in hand to play.
            actions.add(new ChooseCard(currentPlayer, i, false));
            if (sggs.playedCardTypes[currentPlayer].get(Chopsticks).getValue() > 0 && currentPlayerHand.getSize() > 1) {
                // If the player played chopsticks in a previous round, then they can choose to use the chopsticks now (and will choose one extra card in hand)
                actions.add(new ChooseCard(currentPlayer, i, true));
            }
        }
        return actions;
    }
}