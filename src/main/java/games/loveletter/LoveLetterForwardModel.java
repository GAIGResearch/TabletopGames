package games.loveletter;

import core.*;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.ITreeActionSpace;
import games.GameType;
import games.loveletter.actions.PlayCard;
import games.loveletter.cards.LoveLetterCard;
import utilities.ActionTreeNode;

import java.util.*;

import static core.CoreConstants.*;


public class LoveLetterForwardModel extends StandardForwardModel implements ITreeActionSpace {

    /**
     * Creates the initial game-state of Love Letter.
     *
     * @param firstState - state to be modified
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        LoveLetterGameState llgs = (LoveLetterGameState) firstState;

        llgs.effectProtection = new boolean[llgs.getNPlayers()];
        // Set up all variables
        llgs.drawPile = new PartialObservableDeck<>("drawPile", llgs.getNPlayers());
        llgs.reserveCards = new Deck<>("reserveCards", VisibilityMode.VISIBLE_TO_ALL);
        llgs.affectionTokens = new int[llgs.getNPlayers()];
        llgs.playerHandCards = new ArrayList<>(llgs.getNPlayers());
        llgs.playerDiscardCards = new ArrayList<>(llgs.getNPlayers());

        // Set up first round
        setupRound(llgs, null);
    }

    /**
     * Sets up a round for the game, including draw pile, reserve pile and starting player hands.
     *
     * @param llgs            - current game state.
     * @param previousWinners - winners of previous round.
     */
    private void setupRound(LoveLetterGameState llgs, Set<Integer> previousWinners) {
        LoveLetterParameters llp = (LoveLetterParameters) llgs.getGameParameters();

        // No protection this round
        llgs.effectProtection = new boolean[llgs.getNPlayers()];

        // Reset player status
        for (int i = 0; i < llgs.getNPlayers(); i++) {
            llgs.setPlayerResult(GameResult.GAME_ONGOING, i);
        }

        // Add all cards to the draw pile
        llgs.drawPile.clear();
        for (HashMap.Entry<LoveLetterCard.CardType, Integer> entry : llp.cardCounts.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                LoveLetterCard card = new LoveLetterCard(entry.getKey());
                llgs.drawPile.add(card);
            }
        }

        // Remove one card from the game
        llgs.drawPile.shuffle(llgs.getRnd());
        llgs.removedCard = llgs.drawPile.draw();

        // In min-player game, N more cards are on the side, but visible to all players at all times
        llgs.reserveCards.clear();
        if (llgs.getNPlayers() == GameType.LoveLetter.getMinPlayers()) {
            for (int i = 0; i < llp.nCardsVisibleReserve; i++) {
                llgs.reserveCards.add(llgs.drawPile.draw());
            }
        }

        // Set up player hands and discards
        if (llgs.getPlayerHandCards().isEmpty()) {
            // new game set up
            for (int i = 0; i < llgs.getNPlayers(); i++) {
                boolean[] visible = new boolean[llgs.getNPlayers()];
                if (llgs.getCoreGameParameters().partialObservable) {
                    visible[i] = true;
                } else {
                    Arrays.fill(visible, true);
                }

                // add random cards to the player's hand
                PartialObservableDeck<LoveLetterCard> playerCards = new PartialObservableDeck<>("playerHand" + i, i, visible);
                for (int j = 0; j < llp.nCardsPerPlayer; j++) {
                    playerCards.add(llgs.drawPile.draw());
                }
                llgs.playerHandCards.add(playerCards);

                // create a player's discard pile, which is visible to all players
                Deck<LoveLetterCard> discardCards = new Deck<>("discardPlayer" + i, i, VisibilityMode.VISIBLE_TO_ALL);
                llgs.playerDiscardCards.add(discardCards);
            }
        } else {
            llgs.playerHandCards.forEach(PartialObservableDeck::clear);
            llgs.playerDiscardCards.forEach(Deck::clear);
            for (int i = 0; i < llgs.getNPlayers(); i++) {
                // add random cards to the player's hand
                for (int j = 0; j < llp.nCardsPerPlayer; j++) {
                    llgs.playerHandCards.get(i).add(llgs.drawPile.draw());
                }
            }
        }

        if (previousWinners != null) {
            // Next winner in turn order starts
            for (int i = 0; i < llgs.getNPlayers(); i++) {
                int p = (i + 1) % llgs.getNPlayers();
                if (previousWinners.contains(p)) {
                    llgs.setTurnOwner(p);
                    break;
                }
            }
        }

        // Game starts with drawing cards
        LoveLetterCard cardDrawn = llgs.getDrawPile().draw();
        llgs.getPlayerHandCards().get(llgs.getCurrentPlayer()).add(cardDrawn);

        // Update components in the game state
        llgs.updateComponents();
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        if (gameState.isActionInProgress()) return;

        // each turn begins with the player drawing a card after which one card will be played
        LoveLetterGameState llgs = (LoveLetterGameState) gameState;

        if (llgs.playerHandCards.get(llgs.getCurrentPlayer()).getSize() >= 2)
            throw new AssertionError("Hand should not get this big");
        if (!checkEndOfRound(llgs, action)) {
            // move turn to the next player who has not already lost the round
            int nextPlayer = gameState.getCurrentPlayer();
            do {
                nextPlayer = (nextPlayer + 1) % llgs.getNPlayers();
            } while (llgs.getPlayerResults()[nextPlayer] == GameResult.LOSE_ROUND);
            endPlayerTurn(llgs, nextPlayer);

            // Next turn starts with drawing card and removing protection
            llgs.setProtection(llgs.getCurrentPlayer(), false);
            LoveLetterCard cardDrawn = llgs.getDrawPile().draw();
            llgs.getPlayerHandCards().get(llgs.getCurrentPlayer()).add(cardDrawn);
        }
    }

    /**
     * Checks all game end conditions for the game.
     *
     * @param llgs - game state to check if terminal.
     */
    public boolean checkEndOfRound(LoveLetterGameState llgs, AbstractAction actionPlayed) {
        // Count the number of active players
        int playersAlive = 0;
        int soleWinner = -1;
        for (int i = 0; i < llgs.getNPlayers(); i++) {
            if (llgs.getPlayerResults()[i] != GameResult.LOSE_ROUND && llgs.playerHandCards.get(i).getSize() > 0) {
                playersAlive += 1;
                soleWinner = i;
            }
        }

        // Round ends when only a single player is left, or when there are no cards left in the draw pile
        if (playersAlive == 1 || llgs.getRemainingCards() == 0) {

            // End the round and add up points
            Set<Integer> winners = roundEnd(llgs, playersAlive, soleWinner);

            if (llgs.getCoreGameParameters().recordEventHistory) {
                if (playersAlive == 1) {
                    llgs.recordHistory("Winner only player left: " + soleWinner + " (" + actionPlayed.toString() + ")");
                } else if (llgs.getRemainingCards() == 0) {
                    llgs.recordHistory("No more cards remaining. Winners: " + winners.toString());
                }
            }

            GameResult result = GameResult.WIN_ROUND;
            if (winners.size() > 1) result = GameResult.DRAW_ROUND;
            for (int i = 0; i < llgs.getNPlayers(); i++) {
                if (winners.contains(i)) llgs.setPlayerResult(result, i);
                else llgs.setPlayerResult(GameResult.LOSE_ROUND, i);
            }
            endRound(llgs);

            if (checkEndOfGame(llgs)) {
                return true;  // Game is over
            }

            // Otherwise, set up the next round
            setupRound(llgs, winners);

            return true;
        }

        return false;
    }

    /**
     * Checks if the game has ended (only 1 player gets maximum over the required number of affection tokens).
     * Sets the game and player status appropriately.
     *
     * @param llgs - game state to check
     * @return - true if game has ended, false otherwise
     */
    public boolean checkEndOfGame(LoveLetterGameState llgs) {
        LoveLetterParameters llp = (LoveLetterParameters) llgs.getGameParameters();

        // Required tokens from parameters; if more players in the game, use the last value in the array
        double nRequiredTokens = (llgs.getNPlayers() == 2 ? llp.nTokensWin2 : llgs.getNPlayers() == 3 ? llp.nTokensWin3 : llp.nTokensWin4);

        // Find players with the highest number of tokens above the required number
        HashSet<Integer> bestPlayers = new HashSet<>();
        int bestValue = 0;
        for (int i = 0; i < llgs.getNPlayers(); i++) {
            if (llgs.affectionTokens[i] >= nRequiredTokens && llgs.affectionTokens[i] > bestValue) {
                bestValue = llgs.affectionTokens[i];
                bestPlayers.clear();
                bestPlayers.add(i);
            } else if (llgs.affectionTokens[i] != 0 && llgs.affectionTokens[i] == bestValue) {
                bestPlayers.add(i);
            }
        }

        // One player won, game is over
        if (bestPlayers.size() == 1) {
            endGame(llgs);
            return true;
        }
        return false;
    }

    /**
     * Ends the current round and awards affection tokens to winners.
     *
     * @param llgs          - current game state
     * @param nPlayersAlive - number of players still in the game
     * @param soleWinner    - player ID of the winner if only one (otherwise last winner ID)
     */
    private Set<Integer> roundEnd(LoveLetterGameState llgs, int nPlayersAlive, int soleWinner) {
        Set<Integer> winners = getWinners(llgs, nPlayersAlive, soleWinner);
        for (int i : winners) {
            llgs.affectionTokens[i] += 1;
        }
        return winners;
    }

    public Set<Integer> getWinners(LoveLetterGameState llgs, int nPlayersAlive, int soleWinner) {
        if (nPlayersAlive == 1) {
            // They win and get 1 affection token
            return Collections.singleton(soleWinner);
        } else {
            // Highest number in hand wins the round
            HashSet<Integer> bestPlayers = new HashSet<>();
            int bestValue = 0;
            for (int i = 0; i < llgs.getNPlayers(); i++) {
                if (llgs.getPlayerResults()[i] != GameResult.LOSE_ROUND) {
                    int points = llgs.playerHandCards.get(i).peek().cardType.getValue();
                    if (points > bestValue) {
                        bestValue = points;
                        bestPlayers.clear();
                        bestPlayers.add(i);
                    } else if (points == bestValue) {
                        bestPlayers.add(i);
                    }
                }
            }

            if (bestPlayers.size() == 1) {
                // This is the winner of the round, 1 affection token
                return bestPlayers;
            } else {
                // If tie, add numbers in discard pile, highest wins
                bestValue = 0;
                HashSet<Integer> bestPlayersByDiscardPoints = new HashSet<>();
                for (int i : bestPlayers) {
                    int points = 0;
                    for (LoveLetterCard card : llgs.playerDiscardCards.get(i).getComponents()) {
                        points += card.cardType.getValue();
                    }
                    if (points > bestValue) {
                        bestValue = points;
                        bestPlayersByDiscardPoints.clear();
                        bestPlayersByDiscardPoints.add(i);
                    } else if (points == bestValue) {
                        bestPlayersByDiscardPoints.add(i);
                    }
                }
                // Everyone tied for most points here wins the round
                return bestPlayersByDiscardPoints;
            }
        }
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return _computeAvailableActions(gameState, ActionSpace.Default);
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, ActionSpace actionSpace) {
        LoveLetterGameState llgs = (LoveLetterGameState) gameState;
        if (llgs.getPlayerResults()[llgs.getCurrentPlayer()] == CoreConstants.GameResult.LOSE_ROUND)
            throw new AssertionError("???.");

        Set<AbstractAction> actions = new LinkedHashSet<>();
        int playerID = gameState.getCurrentPlayer();
        Deck<LoveLetterCard> playerDeck = llgs.playerHandCards.get(playerID);

        // in case a player holds the countess and either the king or the prince, the countess needs to be played
        LoveLetterCard.CardType cardTypeForceCountess = llgs.needToForceCountess(playerDeck);

        // We create the respective actions for each card on the player's hand
        for (int card = 0; card < playerDeck.getSize(); card++) {
            LoveLetterCard.CardType cardType = playerDeck.getComponents().get(card).cardType;
            if (cardType != LoveLetterCard.CardType.Countess && cardTypeForceCountess != null) continue;
            int cardIdx;
            if (actionSpace.context == ActionSpace.Context.Dependent) cardIdx = card;
            else cardIdx = -1;  // Independent and default
            if (actionSpace.structure == ActionSpace.Structure.Flat || actionSpace.structure == ActionSpace.Structure.Default) {
                actions.addAll(cardType.getFlatActions(llgs, cardIdx, playerID, true));
            } else if (actionSpace.structure == ActionSpace.Structure.Deep) {
                actions.addAll(cardType.getDeepActions(llgs, cardIdx, playerID, true));
            }

        }

        return new ArrayList<>(actions);
    }

    public ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState) {
        // todo test this
        root.resetTree();

        ArrayList<AbstractAction> actions = (ArrayList<AbstractAction>) computeAvailableActions(gameState, ActionSpace.Default);
        for (AbstractAction action : actions) {
            PlayCard llAction = (PlayCard) action;

            // TODO - Probaly a better way to get the card names
            LoveLetterCard.CardType[] soloCards = new LoveLetterCard.CardType[]{
                    LoveLetterCard.CardType.Handmaid,
                    LoveLetterCard.CardType.Countess,
                    LoveLetterCard.CardType.Princess
            };

            // Actions stored in the card type layer (Layer 1)
            if (Arrays.asList(soloCards).contains(llAction.getCardType())) {
                root.findChildrenByName(llAction.getCardType().toString().toLowerCase()).setAction(action);
            }

            // Actions where you target a player (Layer 2)
            else {
                ActionTreeNode cardNode = root.findChildrenByName(llAction.getCardType().toString().toLowerCase());
                ActionTreeNode playerNode = cardNode.findChildrenByName("player" + llAction.getTargetPlayer());
                if (llAction.getCardType() == LoveLetterCard.CardType.Guard) {
                    if (llAction.getTargetCardType() == null) {
                        playerNode.getChildren().get(0).setAction(action);
                    } else {
                        playerNode.findChildrenByName(llAction.getTargetCardType().toString().toLowerCase()).setAction(action);
                    }
                } else {
                    playerNode.setAction(action);
                }
            }
        }
        return root;
    }


    /**
     * Generates the action tree for the game.
     *
     * @return - Root node of the action tree.
     */
    public ActionTreeNode initActionTree(AbstractGameState gameState) {
        // Schema
        // 0 Actions for each card type (0-7)
        // 1 Player action being used on (0 - 3)
        ActionTreeNode root = new ActionTreeNode(0, "root");

        // TODO - Probaly a better way to get the card names
        String[] cardNames = new String[]{"guard", "priest", "baron", "handmaid", "prince", "king", "countess", "princess"};
        String[] selfCards = new String[]{"handmaid", "countess", "princess"};

        for (String name : cardNames) {

            // Add each card type
            ActionTreeNode action = root.addChild(0, name);
            if (!Arrays.asList(selfCards).contains(name)) {
                // Player -1 is for when card will have no effect (e.g. opp using handmaid)
                for (int i = -1; i < 4; i++) {
                    ActionTreeNode player = action.addChild(0, "player" + i);

                    // If card is guard, add each card type as a child for each player
                    if (name.equals("guard")) {
                        player.addChild(0, "none");
                        for (String guardGuess : cardNames) {
                            player.addChild(0, guardGuess);
                        }
                    }
                }
            }
        }
        return root;
    }

}
