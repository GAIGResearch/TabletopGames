package games.blackjack;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import java.util.ArrayList;
import java.util.*;

import static core.CoreConstants.GameResult.*;
import static core.CoreConstants.GameResult.LOSE_GAME;


public class BlackjackForwardModel extends StandardForwardModel {
    @Override
    protected void _setup(AbstractGameState firstState) {
        BlackjackGameState bjgs = (BlackjackGameState) firstState;
        bjgs.dealerPlayer = bjgs.getNPlayers() - 1;  // Dealer player is last

        //Create a deck
        bjgs.playerDecks = new ArrayList<>();

        //create the playing deck
        bjgs.drawDeck = FrenchCard.generateDeck("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        //shuffle the cards
        bjgs.drawDeck.shuffle(new Random((bjgs.getGameParameters().getRandomSeed())));

        bjgs.setFirstPlayer(0);

        //Create a hand for each player
        boolean[] visibility = new boolean[firstState.getNPlayers()];
        Arrays.fill(visibility, true);
        for (int i = 0; i < bjgs.getNPlayers(); i++){
            PartialObservableDeck<FrenchCard> playerDeck = new PartialObservableDeck<>("Player " + i + " deck", i, visibility);
            bjgs.playerDecks.add(playerDeck);
            for (int card = 0; card < ((BlackjackParameters)bjgs.getGameParameters()).nCardsPerPlayer; card++) {
                if (i == bjgs.dealerPlayer && i < ((BlackjackParameters)bjgs.getGameParameters()).nDealerCardsHidden) {
                    new Hit(i, false, true).execute(bjgs);
                } else {
                    new Hit(i).execute(bjgs);
                }
            }
        }
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action){
        BlackjackGameState bjgs = (BlackjackGameState) gameState;
        if (action instanceof Hit) {
            Hit hit = (Hit)action;
            // Check if bust or win score
            int points = bjgs.calculatePoints(hit.playerID);
            if (points > ((BlackjackParameters)gameState.getGameParameters()).winScore) {
                gameState.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, hit.playerID);
                if (hit.advanceTurnOrder) {
                    _endTurn((BlackjackGameState) gameState);
                }
            } else if (points == ((BlackjackParameters)gameState.getGameParameters()).winScore) {
                gameState.setPlayerResult(CoreConstants.GameResult.WIN_GAME, hit.playerID);
                if (hit.advanceTurnOrder) {
                    _endTurn((BlackjackGameState) gameState);
                }
            }
        } else {
            _endTurn(bjgs);
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        BlackjackGameState bjgs = (BlackjackGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = bjgs.getCurrentPlayer();

        // Check if current player is the dealer.
        // Dealer must hit if score is <=16 otherwise must stand
        if (bjgs.getCurrentPlayer() == bjgs.dealerPlayer){
            if (bjgs.calculatePoints(bjgs.dealerPlayer) >= ((BlackjackParameters) bjgs.getGameParameters()).dealerStand){
//                System.out.println("Stand");
                actions.add(new Stand());
            }
            else {
//                System.out.println("Hit");
                actions.add(new Hit(player, true, false));
            }
        }
        else {
            actions.add(new Hit(player, true, false));
            actions.add(new Stand());
        }
        return actions;
    }

    private void _endTurn(BlackjackGameState bjgs) {
        if (bjgs.getTurnCounter() >= bjgs.getNPlayers()) {
            // Everyone finished, game is over, assign results
            bjgs.setGameStatus(GAME_END);

            BlackjackParameters params = (BlackjackParameters) bjgs.getGameParameters();

            int[] score = new int[bjgs.getNPlayers()];
            for (int j = 0; j < bjgs.getNPlayers(); j++){
                if (bjgs.getPlayerResults()[j] != LOSE_GAME) {
                    score[j] = bjgs.calculatePoints(j);
                }
            }
            bjgs.setPlayerResult(GAME_END, bjgs.dealerPlayer);
            if (score[bjgs.dealerPlayer] > params.winScore) {
                // Dealer went bust, everyone else wins
                bjgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, bjgs.dealerPlayer);
            }

            for (int i = 0; i < bjgs.getNPlayers()-1; i++) {  // Check all players and compare to dealer
                if (bjgs.getPlayerResults()[i] != LOSE_GAME) {
                    if (score[bjgs.dealerPlayer] > params.winScore) {
                        // Dealer went bust, everyone else wins
                        bjgs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, i);
                    } else if (score[bjgs.dealerPlayer] > score[i]) {
                        bjgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, i);
                    } else if (score[bjgs.dealerPlayer] < score[i]) {
                        bjgs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, i);
                    } else if (score[bjgs.dealerPlayer] == score[i]) {
                        bjgs.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, i);
                    }
                }
            }

            for (int i = 0; i < bjgs.getNPlayers(); i++) {
                if (bjgs.getPlayerResults()[i] == GAME_ONGOING) {
                    bjgs.setPlayerResult(LOSE_GAME, i);
                }
            }
        }
        else {
            endPlayerTurn(bjgs);
        }
    }

}