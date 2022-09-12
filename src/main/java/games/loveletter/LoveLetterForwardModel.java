package games.loveletter;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import games.GameType;
import games.loveletter.actions.*;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.*;

import static core.CoreConstants.*;
import static games.loveletter.LoveLetterGameState.LoveLetterGamePhase.Draw;


public class LoveLetterForwardModel extends AbstractForwardModel {

    /**
     * Creates the initial game-state of Love Letter.
     * @param firstState - state to be modified
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        LoveLetterGameState llgs = (LoveLetterGameState)firstState;

        // Set up all variables
        llgs.drawPile = new PartialObservableDeck<>("drawPile", llgs.getNPlayers());
        llgs.reserveCards = new PartialObservableDeck<>("reserveCards", llgs.getNPlayers());
        llgs.affectionTokens = new int[llgs.getNPlayers()];
        llgs.playerHandCards = new ArrayList<>(llgs.getNPlayers());
        llgs.playerDiscardCards = new ArrayList<>(llgs.getNPlayers());

        // Set up first round
        setupRound(llgs, null);
    }

    /**
     * Sets up a round for the game, including draw pile, reserve pile and starting player hands.
     * @param llgs - current game state.
     * @param previousWinners - winners of previous round.
     */
    private void setupRound(LoveLetterGameState llgs, HashSet<Integer> previousWinners) {
        LoveLetterParameters llp = (LoveLetterParameters) llgs.getGameParameters();

        // No protection this round
        llgs.effectProtection = new boolean[llgs.getNPlayers()];

        // Reset player status
        for (int i = 0; i < llgs.getNPlayers(); i++) {
            llgs.setPlayerResult(Utils.GameResult.GAME_ONGOING, i);
        }

        // Add all cards to the draw pile
        llgs.drawPile.clear();
        for (HashMap.Entry<LoveLetterCard.CardType, Integer> entry : llp.cardCounts.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                LoveLetterCard card = new LoveLetterCard(entry.getKey());
                llgs.drawPile.add(card);
            }
        }

        // Put one card to the side, such that player's won't know all cards in the game
        Random r = new Random(llgs.getGameParameters().getRandomSeed() + llgs.getTurnOrder().getRoundCounter());
        llgs.drawPile.shuffle(r);
        llgs.reserveCards.clear();
        llgs.reserveCards.add(llgs.drawPile.draw());

        // In min-player game, N more cards are on the side, but visible to all players at all times
        if (llgs.getNPlayers() == GameType.LoveLetter.getMinPlayers()) {
            boolean[] fullVisibility = new boolean[llgs.getNPlayers()];
            Arrays.fill(fullVisibility, true);
            for (int i = 0; i < llp.nCardsVisibleReserve; i++) {
                llgs.reserveCards.add(llgs.drawPile.draw(), fullVisibility);
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

        // Game starts with drawing cards
        llgs.setGamePhase(Draw);

        if (previousWinners != null) {
            // Random winner starts next round
            int nextPlayer = r.nextInt(previousWinners.size());
            int n = -1;
            for (int i: previousWinners) {
                n++;
                if (n == nextPlayer) {
                    llgs.getTurnOrder().setTurnOwner(i);
                }
            }
        }

        // Update components in the game state
        llgs.updateComponents();
    }

    @Override
    protected void _next(AbstractGameState gameState, AbstractAction action) {
        // each turn begins with the player drawing a card after which one card will be played
        // switch the phase after each executed action
        LoveLetterGameState llgs = (LoveLetterGameState) gameState;
        action.execute(gameState);

        if (llgs.playerHandCards.get(gameState.getCurrentPlayer()).getSize() > 2)
            throw new AssertionError("Hand should not get this big");
        IGamePhase gamePhase = llgs.getGamePhase();
        if (gamePhase == Draw) {
            llgs.setGamePhase(AbstractGameState.DefaultGamePhase.Main);
        } else if (gamePhase == AbstractGameState.DefaultGamePhase.Main) {
            llgs.setGamePhase(Draw);
            llgs.getTurnOrder().endPlayerTurn(gameState);
            checkEndOfRound(llgs);
        } else {
            throw new IllegalArgumentException("The game phase " + llgs.getGamePhase() +
                    " is not know by LoveLetterForwardModel");
        }
    }

    /**
     * Checks all game end conditions for the game.
     * @param llgs - game state to check if terminal.
     */
    public void checkEndOfRound(LoveLetterGameState llgs) {
        // Count the number of active players
        int playersAlive = 0;
        int soleWinner = -1;
        for (int i = 0; i < llgs.getNPlayers(); i++) {
            if (llgs.getPlayerResults()[i] != Utils.GameResult.LOSE && llgs.playerHandCards.get(i).getSize() > 0) {
                playersAlive += 1;
                soleWinner = i;
            }
        }

        // Round ends when only a single player is left, or when there are no cards left in the draw pile
        if (playersAlive == 1 || llgs.getRemainingCards() == 0) {
            // End the round and add up points
            HashSet<Integer> winners = roundEnd(llgs, playersAlive, soleWinner);

            if (checkEndOfGame(llgs)) {
                return;  // Game is over
            }

            // Otherwise, end the round and set up the next
            llgs.getTurnOrder().endRound(llgs);
            setupRound(llgs, winners);
        }
    }

    /**
     * Checks if the game has ended (only 1 player gets maximum over the required number of affection tokens).
     * Sets the game and player status appropriately.
     * @param llgs - game state to check
     * @return - true if game has ended, false otherwise
     */
    public boolean checkEndOfGame(LoveLetterGameState llgs) {
        LoveLetterParameters llp = (LoveLetterParameters) llgs.getGameParameters();

        // Required tokens from parameters; if more players in the game, use the last value in the array
        double nRequiredTokens = (llgs.getNPlayers() == 2? llp.nTokensWin2 : llgs.getNPlayers() == 3? llp.nTokensWin3 : llp.nTokensWin4);

        // Find players with highest number of tokens above the required number
        HashSet<Integer> bestPlayers = new HashSet<>();
        int bestValue = 0;
        for (int i = 0; i < llgs.getNPlayers(); i++) {
            if (llgs.affectionTokens[i] >= nRequiredTokens && llgs.affectionTokens[i] > bestValue){
                bestValue = llgs.affectionTokens[i];
                bestPlayers.clear();
                bestPlayers.add(i);
            } else if (llgs.affectionTokens[i] != 0 && llgs.affectionTokens[i] == bestValue) {
                bestPlayers.add(i);
            }
        }

        // One player won, game is over
        if (bestPlayers.size() == 1) {
            llgs.setGameStatus(Utils.GameResult.GAME_END);
            for (int i = 0; i < llgs.getNPlayers(); i++) {
                if (bestPlayers.contains(i)) {
                    llgs.setPlayerResult(Utils.GameResult.WIN, i);
                } else {
                    llgs.setPlayerResult(Utils.GameResult.LOSE, i);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Ends the current round and awards affection tokens to winners.
     * @param llgs - current game state
     * @param nPlayersAlive - number of players still in the game
     * @param soleWinner - player ID of the winner if only one (otherwise last winner ID)
     */
    private HashSet<Integer> roundEnd(LoveLetterGameState llgs, int nPlayersAlive, int soleWinner) {
        HashSet<Integer> winners = getWinners(llgs, nPlayersAlive, soleWinner);
        for (int i: winners) {
            llgs.affectionTokens[i] += 1;
        }
        return winners;
    }

    public HashSet<Integer> getWinners(LoveLetterGameState llgs, int nPlayersAlive, int soleWinner) {
        if (nPlayersAlive == 1) {
            // They win and get 1 affection token
            return new HashSet<Integer>() {{
                add(soleWinner);
            }};
        } else {
            // Highest number in hand wins the round
            HashSet<Integer> bestPlayers = new HashSet<>();
            int bestValue = 0;
            for (int i = 0; i < llgs.getNPlayers(); i++) {
                if (llgs.getPlayerResults()[i] != Utils.GameResult.LOSE) {
                    int points = llgs.playerHandCards.get(i).peek().cardType.getValue();
                    if (points > bestValue){
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
                for (int i: bestPlayers) {
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

    @Override
    protected void endGame(AbstractGameState gameState) {
        // Print game result
        if (gameState.getCoreGameParameters().verbose) {
            System.out.println(Arrays.toString(gameState.getPlayerResults()));
            Utils.GameResult[] playerResults = gameState.getPlayerResults();
            for (int j = 0; j < gameState.getNPlayers(); j++) {
                if (playerResults[j] == Utils.GameResult.WIN)
                    System.out.println("Player " + j + " won");
            }
        }
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        LoveLetterGameState llgs = (LoveLetterGameState)gameState;
        ArrayList<AbstractAction> actions;
        int player = gameState.getTurnOrder().getCurrentPlayer(gameState);
        if (gameState.getGamePhase().equals(AbstractGameState.DefaultGamePhase.Main)) {
            actions = playerActions(llgs, player);
        } else if (gameState.getGamePhase().equals(LoveLetterGameState.LoveLetterGamePhase.Draw)) {
            // In draw phase, the players can only draw cards.
            actions = new ArrayList<>();
            actions.add(new DrawCard());
        } else {
            throw new IllegalArgumentException(gameState.getGamePhase() + " is unknown to LoveLetterGameState");
        }

        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new LoveLetterForwardModel();
    }

    /**
     * Computes actions available for the given player.
     * @param playerID - ID of player to calculate actions for.
     * @return - ArrayList of AbstractAction objects.
     */
    private ArrayList<AbstractAction> playerActions(LoveLetterGameState llgs, int playerID) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Deck<LoveLetterCard> playerDeck = llgs.playerHandCards.get(playerID);
        Deck<LoveLetterCard> playerDiscardPile = llgs.playerDiscardCards.get(playerID);

        // in case a player holds the countess and either the king or the prince, the countess needs to be played
        if (llgs.needToForceCountess(playerDeck)){
            for (int c = 0; c < playerDeck.getSize(); c++) {
                if (playerDeck.getComponents().get(c).cardType == LoveLetterCard.CardType.Countess)
                    actions.add(new CountessAction(playerDeck.getComponentID(), playerDiscardPile.getComponentID(), c));
            }
        }
        // else: we create the respective actions for each card on the player's hand
        else {
            for (int card = 0; card < playerDeck.getSize(); card++) {
                switch (playerDeck.getComponents().get(card).cardType) {
                    case Priest:
                        for (int targetPlayer = 0; targetPlayer < llgs.getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || llgs.getPlayerResults()[targetPlayer] == Utils.GameResult.LOSE)
                                continue;
                            actions.add(new PriestAction(playerDeck.getComponentID(),
                                    playerDiscardPile.getComponentID(), card, targetPlayer));
                        }
                        break;

                    case Guard:
                        for (int targetPlayer = 0; targetPlayer < llgs.getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || llgs.getPlayerResults()[targetPlayer] == Utils.GameResult.LOSE)
                                continue;
                            for (LoveLetterCard.CardType type : LoveLetterCard.CardType.values())
                                if (type != LoveLetterCard.CardType.Guard) {
                                    actions.add(new GuardAction(playerDeck.getComponentID(),
                                            playerDiscardPile.getComponentID(), card, targetPlayer, type));
                                }
                        }
                        break;

                    case Baron:
                        for (int targetPlayer = 0; targetPlayer < llgs.getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || llgs.getPlayerResults()[targetPlayer] == Utils.GameResult.LOSE)
                                continue;
                            actions.add(new BaronAction(playerDeck.getComponentID(),
                                    playerDiscardPile.getComponentID(), card, targetPlayer));
                        }
                        break;

                    case Handmaid:
                        actions.add(new HandmaidAction(playerDeck.getComponentID(),
                                playerDiscardPile.getComponentID(), card));
                        break;

                    case Prince:
                        for (int targetPlayer = 0; targetPlayer < llgs.getNPlayers(); targetPlayer++) {
                            if (llgs.getPlayerResults()[targetPlayer] == Utils.GameResult.LOSE)
                                continue;
                            actions.add(new PrinceAction(playerDeck.getComponentID(),
                                    playerDiscardPile.getComponentID(), card, targetPlayer));
                        }
                        break;

                    case King:
                        for (int targetPlayer = 0; targetPlayer < llgs.getNPlayers(); targetPlayer++) {
                            if (targetPlayer == playerID || llgs.getPlayerResults()[targetPlayer] == Utils.GameResult.LOSE)
                                continue;
                            actions.add(new KingAction(playerDeck.getComponentID(),
                                    playerDiscardPile.getComponentID(), card, targetPlayer));
                        }
                        break;

                    case Countess:
                        actions.add(new CountessAction(playerDeck.getComponentID(),
                                playerDiscardPile.getComponentID(), card));
                        break;

                    case Princess:
                        actions.add(new PrincessAction(playerDeck.getComponentID(),
                                playerDiscardPile.getComponentID(), card));
                        break;

                    default:
                        throw new IllegalArgumentException("No core actions known for cardtype: " +
                                playerDeck.getComponents().get(card).cardType.toString());
                }
            }
        }

        // add end turn by drawing a card
        return actions;
    }
}
