package games.blackjack;

import core.turnorders.AlternatingTurnOrder;
import core.AbstractGameState;
import core.turnorders.TurnOrder;
import utilities.Utils;

import static utilities.Utils.GameResult.*;

public class BlackjackTurnOrder extends AlternatingTurnOrder {

    public BlackjackTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState){
        if (gameState.getGameStatus() != GAME_ONGOING) return;

        turnCounter++;
        if (turnCounter >= nPlayers) {
            // Everyone finished, game is over, assign results
            gameState.setGameStatus(GAME_END);

            BlackjackGameState bjgs = (BlackjackGameState) gameState;
            BlackjackParameters params = (BlackjackParameters) bjgs.getGameParameters();

            int[] score = new int[bjgs.getNPlayers()];
            for (int j = 0; j < bjgs.getNPlayers(); j++){
                if (bjgs.getPlayerResults()[j] != LOSE) {
                    score[j] = bjgs.calculatePoints(j);
                }
            }
            bjgs.setPlayerResult(GAME_END, bjgs.dealerPlayer);
            if (score[bjgs.dealerPlayer] > params.winScore) {
                // Dealer went bust, everyone else wins
                bjgs.setPlayerResult(Utils.GameResult.LOSE, bjgs.dealerPlayer);
            }

            for (int i = 0; i < bjgs.getNPlayers()-1; i++) {  // Check all players and compare to dealer
                if (bjgs.getPlayerResults()[i] != LOSE) {
                    if (score[bjgs.dealerPlayer] > params.winScore) {
                        // Dealer went bust, everyone else wins
                        bjgs.setPlayerResult(Utils.GameResult.WIN, i);
                    } else if (score[bjgs.dealerPlayer] > score[i]) {
                        bjgs.setPlayerResult(Utils.GameResult.LOSE, i);
                    } else if (score[bjgs.dealerPlayer] < score[i]) {
                        bjgs.setPlayerResult(Utils.GameResult.WIN, i);
                    } else if (score[bjgs.dealerPlayer] == score[i]) {
                        bjgs.setPlayerResult(Utils.GameResult.DRAW, i);
                    }
                }
            }

            for (int i = 0; i < bjgs.getNPlayers(); i++) {
                if (bjgs.getPlayerResults()[i] == GAME_ONGOING) {
                    bjgs.setPlayerResult(LOSE, i);
                }
            }
        }
        else {
            moveToNextPlayer(gameState, nextPlayer(gameState));
        }
    }

    @Override
    protected TurnOrder _copy(){
        BlackjackTurnOrder bjto = new BlackjackTurnOrder(nPlayers);
        bjto.direction = direction;
        return bjto;
    }
}
