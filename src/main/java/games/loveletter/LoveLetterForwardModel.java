package games.loveletter;

import core.AbstractGameState;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.interfaces.IGamePhase;
import games.loveletter.actions.*;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static games.loveletter.LoveLetterGameState.LoveLetterGamePhase.Draw;
import static core.CoreConstants.PARTIAL_OBSERVABLE;
import static core.CoreConstants.VERBOSE;


public class LoveLetterForwardModel extends AbstractForwardModel {

    /**
     * Creates the initial game-state of Love Letter.
     * @param firstState - state to be modified
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        LoveLetterGameState llgs = (LoveLetterGameState)firstState;
        LoveLetterParameters llp = (LoveLetterParameters)firstState.getGameParameters();

        llgs.drawPile = new PartialObservableDeck<>("drawPile", llgs.getNPlayers());
        llgs.effectProtection = new boolean[llgs.getNPlayers()];

        // Add all cards to the draw pile
        for (HashMap.Entry<LoveLetterCard.CardType, Integer> entry : llp.cardCounts.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                LoveLetterCard card = new LoveLetterCard(entry.getKey());
                llgs.drawPile.add(card);
            }
        }

        // Put one card to the side, such that player's won't know all cards in the game
        llgs.reserveCards = new PartialObservableDeck<>("reserveCards", llgs.getNPlayers());
        llgs.drawPile.shuffle();
        llgs.reserveCards.add(llgs.drawPile.draw());

        // Give each player a single card
        llgs.playerHandCards = new ArrayList<>(llgs.getNPlayers());
        llgs.playerDiscardCards = new ArrayList<>(llgs.getNPlayers());
        for (int i = 0; i < llgs.getNPlayers(); i++) {
            // Setup player deck to be fully/partial observable
            boolean[] visibility = new boolean[llgs.getNPlayers()];
            Arrays.fill(visibility, !PARTIAL_OBSERVABLE);
            visibility[i] = true;

            // add a single random card to the player's hand
            PartialObservableDeck<LoveLetterCard> playerCards = new PartialObservableDeck<>("playerHand" + i,
                    visibility.clone());
            for (int j = 0; j < llp.nCardsPerPlayer; j++) {
                playerCards.add(llgs.drawPile.draw());
            }
            llgs.playerHandCards.add(playerCards);

            // create a player's discard pile, which is visible to all players
            Arrays.fill(visibility, true);
            Deck<LoveLetterCard> discardCards = new Deck<>("discardPlayer" + i, i);
            llgs.playerDiscardCards.add(discardCards);
        }

        llgs.setGamePhase(Draw);
    }

    @Override
    protected void _next(AbstractGameState gameState, AbstractAction action) {
        if (VERBOSE) {
            System.out.println(action.toString());
        }

        // each turn begins with the player drawing a card after which one card will be played
        // switch the phase after each executed action
        LoveLetterGameState llgs = (LoveLetterGameState) gameState;
        action.execute(gameState);

        IGamePhase gamePhase = llgs.getGamePhase();
        if (gamePhase == Draw)
            llgs.setGamePhase(AbstractGameState.DefaultGamePhase.Main);
        else if (gamePhase == AbstractGameState.DefaultGamePhase.Main){
            llgs.setGamePhase(Draw);
            checkEndOfGame(llgs);
            if (llgs.getGameStatus() != Utils.GameResult.GAME_END)
                llgs.getTurnOrder().endPlayerTurn(gameState);
        } else
            throw new IllegalArgumentException("The gamestate " + llgs.getGamePhase() +
                    " is not know by LoveLetterForwardModel");
    }

    /**
     * Checks all game end conditions for the game.
     * @param llgs - game state to check if terminal.
     */
    private void checkEndOfGame(LoveLetterGameState llgs) {
        // count the number of active players
        int playersAlive = 0;
        for (Utils.GameResult result : llgs.getPlayerResults())
            if (result != Utils.GameResult.LOSE)
                playersAlive += 1;

        // game ends because only a single player is left
        if (playersAlive == 1) {
            llgs.setGameStatus(Utils.GameResult.GAME_END);
        }
        else if (llgs.getRemainingCards() == 0){
            // game needs to end because their are no cards left
            llgs.setGameStatus(Utils.GameResult.GAME_END);
        }
    }


    /**
     * Sets the game-state to be terminal and determines the result of each player.
     */
    @Override
    protected void endGame(AbstractGameState gameState) {
        LoveLetterGameState llgs = (LoveLetterGameState) gameState;
        gameState.setGameStatus(Utils.GameResult.GAME_END);

        // determine which player has the card with the highest value
        List<Integer> bestPlayers = new ArrayList<>();
        int bestValue = 0;
        int points;
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            if (gameState.getPlayerResults()[i] != Utils.GameResult.LOSE)
                points = llgs.playerHandCards.get(i).peek().cardType.getValue();
            else
                points = 0;

            if (points > bestValue){
                bestValue = points;
                bestPlayers.clear();
                bestPlayers.add(i);
            } else if (points == bestValue) {
                bestPlayers.add(i);
            }
        }

        // if just a single player is alive, the player immediately wins the game
        if (bestPlayers.size() == 1){
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                gameState.setPlayerResult(Utils.GameResult.LOSE, i);
            }
            gameState.setPlayerResult(Utils.GameResult.WIN, bestPlayers.get(0));
        } else {
            // else, the player with the higher sum of card values in its discard pile wins
            // in case two or more players have the same value, they all win
            bestValue = 0;
            for (int i = 0; i < gameState.getNPlayers(); i++) {
                points = 0;
                if (gameState.getPlayerResults()[i] == Utils.GameResult.WIN)
                    for (LoveLetterCard card : llgs.playerDiscardCards.get(i).getComponents())
                        points += card.cardType.getValue();
                if (points > bestValue) {
                    bestValue = points;
                    bestPlayers.clear();
                    bestPlayers.add(i);
                } else if (points == bestValue) {
                    bestPlayers.add(i);
                }
            }

            for (int i = 0; i < gameState.getNPlayers(); i++) {
                gameState.setPlayerResult(Utils.GameResult.LOSE, i);
            }
            for (Integer playerID : bestPlayers)
                gameState.setPlayerResult(Utils.GameResult.WIN, playerID);
        }

        // Print game result
        if (VERBOSE) {
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
            actions.add(new DrawCard(llgs.drawPile.getComponentID(), llgs.playerHandCards.get(player).getComponentID(), 0));
        } else {
            throw new IllegalArgumentException(gameState.getGamePhase().toString() + " is unknown to LoveLetterGameState");
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
                                actions.add(new GuardAction(playerDeck.getComponentID(),
                                        playerDiscardPile.getComponentID(), card, targetPlayer, type));
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
                            if (targetPlayer == playerID || llgs.getPlayerResults()[targetPlayer] == Utils.GameResult.LOSE)
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
