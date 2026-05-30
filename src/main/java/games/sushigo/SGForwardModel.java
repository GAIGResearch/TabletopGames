package games.sushigo;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.ITreeActionSpace;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import utilities.ActionTreeNode;
import utilities.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static games.sushigo.cards.SGCard.SGCardType.*;

@SuppressWarnings("unchecked")
public class SGForwardModel extends StandardForwardModel implements ITreeActionSpace {

    @Override
    protected void _setup(AbstractGameState firstState) {
        SGGameState gs = (SGGameState) firstState;
        SGParameters parameters = (SGParameters) gs.getGameParameters();
        gs.nCardsInHand = 0;
        gs.deckRotations = 0;
        gs.playerScore = new Counter[firstState.getNPlayers()];
        gs.cardChoices = new ArrayList<>(firstState.getNPlayers());
        gs.playedCardTypes = new HashMap[firstState.getNPlayers()];
        gs.playedCardTypesAllGame = new HashMap[firstState.getNPlayers()];
        gs.pointsPerCardType = new HashMap[firstState.getNPlayers()];
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
            gs.playedCardTypesAllGame[i] = new HashMap<>();
            gs.pointsPerCardType[i] = new HashMap<>();
            for (SGCard.SGCardType type: SGCard.SGCardType.values()) {
                gs.playedCardTypes[i].put(type, new Counter(0, 0, Integer.MAX_VALUE, "Played cards " + type.name()));
                gs.playedCardTypesAllGame[i].put(type, new Counter(0, 0, Integer.MAX_VALUE, "Played cards (all) " + type.name()));
                gs.pointsPerCardType[i].put(type, new Counter(0, 0, Integer.MAX_VALUE, "Points per " + type.name()));
            }
            gs.cardChoices.add(new ArrayList<>());

            // Draw initial hand of cards
            for (int j = 0; j < gs.nCardsInHand; j++) {
                gs.playerHands.get(i).add(gs.drawPile.draw());
            }
        }

        // Set starting player
        gs.setFirstPlayer(0);
    }

    /**
     * Adds to the draw pile all the cards in the game. How many of each type are added depends on game parameters.
     * Maki cards are special, those can have multiple Maki icons on them (1-3). Hardcoded and ugly, but no easy way around it.
     * @param gs - game state to add cards to
     */
    private void setupDrawPile(SGGameState gs) {
        SGParameters parameters = (SGParameters) gs.getGameParameters();
        for (Pair<SGCard.SGCardType, Integer> p: parameters.nCardsPerType.keySet()) {
            int count = parameters.nCardsPerType.get(p);
            for (int i = 0; i < count; i++) {
                gs.drawPile.add(new SGCard(p.a, p.b));
            }
        }
        gs.drawPile.shuffle(gs.getRnd());
    }


    /*

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        if (currentState.isActionInProgress())
            return; // we only want to trigger this processing if an extended action sequence (i.e. Chopsticks) has been terminated

        SGGameState gs = (SGGameState) currentState;

        // Check if all players made their choice
        int nextPlayer = gs.getCurrentPlayer();
        do {
            nextPlayer = (nextPlayer + 1) % gs.getNPlayers();
        } while (nextPlayer != gs.getCurrentPlayer() && !gs.cardChoices.get(nextPlayer).isEmpty());

        if (nextPlayer == gs.getCurrentPlayer()) {
            // They did! Reveal all cards at once. Process card reveal rules.
            revealCards(gs);

            // Check if the round is over
            if (isRoundOver(gs)) {
                // It is! Process end of round rules.
                endRound(gs);
                _endRound(gs);

                // Clear card choices from this turn, ready for the next simultaneous choice.
                gs.clearCardChoices();

                // Check if the game is over
                if (gs.getRoundCounter() >= ((SGParameters)gs.getGameParameters()).nRounds) {
                    // It is! Process end of game rules.
                    for (SGCard.SGCardType type: values()) {
                        type.onGameEnd(gs);
                    }
                    // Decide winner
                    endGame(gs);
                    return;
                }

                _startRound(gs);
                return;
            } else {
                // Round is not over, keep going. Rotate hands for next player turns.
                rotatePlayerHands(gs);

                // Clear card choices from this turn, ready for the next simultaneous choice.
                gs.clearCardChoices();
            }
        }

        // End player turn
        if (gs.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING) {
            endPlayerTurn(gs, nextPlayer);
        }
    }



    */
/*
    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        if (currentState.isActionInProgress())
            return;

        SGGameState gs = (SGGameState) currentState;

        // Only proceed when all players have made their choice
        boolean allChosen = gs.cardChoices.stream().allMatch(choices -> !choices.isEmpty());
        if (!allChosen) return;

        System.out.println("_afterAction fired, allChosen=" + allChosen +
                " roundOver=" + isRoundOver(gs) +
                " currentPlayer=" + gs.getCurrentPlayer() +
                " cardChoices=" + gs.cardChoices.stream().map(List::size).collect(Collectors.toList()) +
                " handSizes=" + gs.getPlayerHands().stream().map(d -> d.getSize()).collect(Collectors.toList()));


        System.out.println("Before reveal - handSizes: " +
                gs.getPlayerHands().stream().map(d -> d.getSize()).collect(Collectors.toList()) +
                " cardChoices: " + gs.cardChoices.stream().map(List::size).collect(Collectors.toList()));



        // All players have chosen, reveal cards
        revealCards(gs);

        // Check if the round is over
        if (isRoundOver(gs)) {
            endRound(gs);
            _endRound(gs);
            gs.clearCardChoices();

            if (gs.getRoundCounter() >= ((SGParameters) gs.getGameParameters()).nRounds) {
                for (SGCard.SGCardType type : values()) {
                    type.onGameEnd(gs);
                }
                endGame(gs);
                return;
            }

            _startRound(gs);
            return;
        } else {
            rotatePlayerHands(gs);
            gs.clearCardChoices();
        }

        if (gs.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING) {
            endPlayerTurn(gs, gs.getCurrentPlayer());
        }
    }

    */



    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {

        // Ignore intermediate actions inside extended sequences
        if (currentState.isActionInProgress())
            return;

        SGGameState gs = (SGGameState) currentState;

        int nextPlayer = gs.getCurrentPlayer();
        do {
            nextPlayer = (nextPlayer + 1) % gs.getNPlayers();
        } while (nextPlayer != gs.getCurrentPlayer()
                && (!gs.cardChoices.get(nextPlayer).isEmpty()
                || gs.getPlayerHands().get(nextPlayer).getSize() == 0));

        boolean allChosen = nextPlayer == gs.getCurrentPlayer();
        if (!allChosen) {
            endPlayerTurn(gs, nextPlayer);
            return;
        }

        // Reveal all selected cards
        revealCards(gs);

        // Check if round is over
        if (isRoundOver(gs)) {

            endRound(gs);
            _endRound(gs);

            gs.clearCardChoices();

            // Game finished?
            if (gs.getRoundCounter() >= ((SGParameters) gs.getGameParameters()).nRounds) {

                for (SGCard.SGCardType type : values()) {
                    type.onGameEnd(gs);
                }

                endGame(gs);
                return;
            }

            // Start next round
            _startRound(gs);
            return;
        }

        // Continue current round
        rotatePlayerHands(gs);

        // Clear submitted choices for next simultaneous turn
        gs.clearCardChoices();

        // Advance turn
        if (gs.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING) {
            endPlayerTurn(gs, gs.getCurrentPlayer());
        }
    }

    public void _endRound(SGGameState gs) {


        // Apply card end of round rules
        for (SGCard.SGCardType type: SGCard.SGCardType.values()) {
            type.onRoundEnd(gs);
        }

        // Clear played hands if they get discarded between rounds, they go in the discard pile
        for (int i = 0; i < gs.getNPlayers(); i++) {
            // We think this copy may be for the properties
            Deck<SGCard> cardsToKeep = gs.playedCards.get(i).copy();
            cardsToKeep.clear();
            for (SGCard card : gs.playedCards.get(i).getComponents()) {
                if (card.type.isDiscardedBetweenRounds()) {
                    gs.discardPile.add(card);
                    gs.playedCardTypes[i].get(card.type).setValue(0);
                } else {
                    cardsToKeep.add(card);
                }
            }
            gs.playedCards.get(i).clear();
            gs.playedCards.get(i).add(cardsToKeep);
        }
    }

    public void _startRound(SGGameState gs) {
        //Draw new hands for players
        for (int i = 0; i < gs.getNPlayers(); i++){
            for (int j = 0; j < gs.nCardsInHand; j++)
            {
                if (gs.drawPile.getSize() == 0) {
                    // Reshuffle discard into draw pile
                    gs.drawPile.add(gs.discardPile);
                    gs.discardPile.clear();
                    gs.drawPile.shuffle(gs.getRnd());
                }
                gs.playerHands.get(i).add(gs.drawPile.draw());
            }
            gs.deckRotations = 0;
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
                gs.playedCardTypesAllGame[i].get(cardToReveal.type).increment(cardToReveal.count);

                //Add points to player
                cardToReveal.type.onReveal(gs, i);

                if (cc.useChopsticks) {
                    removeUsedChopsticks(gs, i);
                }
            }
        }
//       int expectedPlayerCards = gs.getPlayerHands().get(0).getSize();
//       for (int i = 1; i < gs.getNPlayers(); i++) {
//           if (gs.getPlayerHands().get(i).getSize() != expectedPlayerCards) {
//               throw new AssertionError("Player " + i + " has " + gs.getPlayerHands().get(i).getSize() + " cards, expected " + expectedPlayerCards);
//           }
//       }
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
        if (currentPlayerHand.getSize() == 0) {
            actions.add(new DoNothing());
            return actions;
        }
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

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, int activePlayer) {
        SGGameState sggs = (SGGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();

        Deck<SGCard> currentPlayerHand = sggs.getPlayerHands().get(activePlayer);
        if (currentPlayerHand.getSize() == 0) {
            actions.add(new DoNothing());
            return actions;
        }
        for (int i = 0; i < currentPlayerHand.getSize(); i++) {
            actions.add(new ChooseCard(activePlayer, i, false));
            if (sggs.playedCardTypes[activePlayer].get(Chopsticks).getValue() > 0 && currentPlayerHand.getSize() > 1) {
                actions.add(new ChooseCard(activePlayer, i, true));
            }
        }
        return actions;
    }


    @Override
    public ActionTreeNode initActionTree(AbstractGameState gameState) {
        /* action tree contains 2 branches: play and chopstick and subactions represent the card ids in hand */
        int nCards = ((SGParameters) gameState.getGameParameters()).nCards;
        ActionTreeNode root = new ActionTreeNode(0, "root");
        ActionTreeNode playNode = root.addChild(0, "play");
        ActionTreeNode chopsticksNode = root.addChild(0, "chopsticks");
        for (int i = 0; i < nCards; i++){
            playNode.addChild(0, String.valueOf(i));
            chopsticksNode.addChild(0, String.valueOf(i));
        }
        return root;
    }

    @Override
    public ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState) {
        root.resetTree();
        ActionTreeNode playNode = root.findChildrenByName("play");
        ActionTreeNode chopsticksNode = root.findChildrenByName("chopsticks");
        SGGameState sggs = (SGGameState) gameState;
        int currentPlayer = sggs.getCurrentPlayer();
        Deck<SGCard> currentPlayerHand = sggs.getPlayerHands().get(currentPlayer);
        // handle extended actions
        if (gameState.isActionInProgress()){
            // only happens with chopstick
            for (AbstractAction action: gameState.getActionsInProgress().peek()._computeAvailableActions(gameState)) {
                chopsticksNode.findChildrenByName(String.valueOf(((ChooseCard)action).cardIdx), true).setAction(action);
            }
            return root;
        }
        // normal action selection
        for (int i = 0; i < currentPlayerHand.getSize(); i++) {
            // All players can do is choose a card in hand to play.
            playNode.findChildrenByName(String.valueOf(i)).setAction(new ChooseCard(currentPlayer, i, false));
            if (sggs.playedCardTypes[currentPlayer].get(Chopsticks).getValue() > 0 && currentPlayerHand.getSize() > 1) {
                // If the player played chopsticks in a previous round, then they can choose to use the chopsticks now (and will choose one extra card in hand)
                chopsticksNode.findChildrenByName(String.valueOf(i)).setAction(new ChooseCard(currentPlayer, i, true));
            }
        }
        return root;
    }
}
