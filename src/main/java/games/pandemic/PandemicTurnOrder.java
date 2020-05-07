package games.pandemic;

import core.AbstractGameState;
import core.turnorder.ReactiveTurnOrder;
import core.turnorder.TurnOrder;
import utilities.Utils;

public class PandemicTurnOrder extends ReactiveTurnOrder {
    protected int nStepsPerTurn;  // Number of steps in a turn before player's turn is finished
    protected int turnStep;  // 1 turn = n steps (by default n = 1)

    PandemicTurnOrder(int nPlayers, int nActionsPerTurn){
        super(nPlayers, -1);
        turnStep = 0;
        this.nStepsPerTurn = nActionsPerTurn;
    }

    public void endPlayerTurnStep() {
        if (reactivePlayers.size() > 0) reactivePlayers.poll();
        else turnStep++;
    }

    public int getTurnStep() {
        return turnStep;
    }


    /**
     * Method executed after a player's turn is finished.
     * By default it resets the turnStep counter to 0 and increases the turn counter.
     * Then moves to the next alive player. If this is the last player, the round ends.
     * @param gameState - current game state.
     */
    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        turnCounter++;
        if (turnCounter >= nPlayers) endRound(gameState);
        else {
            turnStep = 0;
            turnOwner = nextPlayer(gameState);
            while (gameState.getPlayerResults()[turnOwner] != Utils.GameResult.GAME_ONGOING) {
                turnOwner = nextPlayer(gameState);
            }
        }
    }


    /**
     * Method executed after all player turns.
     * By default it resets the turn counter, the turn owner to the first alive player and increases round counter.
     * If maximum number of rounds reached, game ends.
     * @param gameState - current game state.
     */
    public void endRound(AbstractGameState gameState) {
        roundCounter++;
        if (nMaxRounds != -1 && roundCounter == nMaxRounds) gameState.setGameStatus(Utils.GameResult.GAME_END);
        else {
            turnStep = 0;
            turnCounter = 0;
            turnOwner = 0;
            while (gameState.getPlayerResults()[turnOwner] != Utils.GameResult.GAME_ONGOING) {
                turnOwner = nextPlayer(gameState);
            }
        }
    }

    @Override
    public TurnOrder copy() {
        PandemicTurnOrder pto = (PandemicTurnOrder) super.copy();
        pto.turnStep = turnStep;
        pto.nStepsPerTurn = nStepsPerTurn;
        return pto;
    }
}
