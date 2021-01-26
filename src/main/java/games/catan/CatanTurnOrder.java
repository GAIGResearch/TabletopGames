package games.catan;

import core.AbstractGameState;
import core.turnorders.AlternatingTurnOrder;
import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;
import utilities.Utils;

import static utilities.Utils.GameResult.GAME_ONGOING;

public class CatanTurnOrder extends ReactiveTurnOrder {
    protected int turnStep;
    protected int turnStage; // trade stage (0), build stage (1)
    protected boolean developmentCardPlayed; // Tracks whether a player has played a development card this turn

    CatanTurnOrder(int nPlayers, int nMaxRounds) {
        super(nPlayers, nMaxRounds);
        turnStep = 0;
        turnStage = 0;
        developmentCardPlayed = false;
    }

    @Override
    protected void _reset() {
        super._reset();
        turnStep = 0;
    }

    @Override
    protected TurnOrder _copy() {
        CatanTurnOrder to = new CatanTurnOrder(nPlayers, nMaxRounds);
        to.turnStep = turnStep;
        to.turnStage = turnStage;
        to.developmentCardPlayed = developmentCardPlayed;
        return to;
    }

    /**
     * Method executed after a player's turn is finished.
     * By default it resets the turnStep counter to 0 and increases the turn counter.
     * Then moves to the next alive player. If this is the last player, the round ends.
     * If the game has ended, turn owner is not changed. If there are no players still playing, game ends and method returns.
     * @param gameState - current game state.
     */
    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;

        turnStage = 0;
        setDevelopmentCardPlayed(false);
        turnCounter++;
        if (turnCounter >= nPlayers) endRound(gameState);
        else {
            turnStep = 0;
            moveToNextPlayer(gameState, nextPlayer(gameState));
        }
    }

    public void endTurnStage(AbstractGameState gameState){
        turnStage++;
        if (turnStage==2){
            endPlayerTurn(gameState);
        }
    }

    public int getTurnStage() {
        return turnStage;
    }

    public boolean isDevelopmentCardPlayed() {
        return developmentCardPlayed;
    }

    public void setDevelopmentCardPlayed(boolean developmentCardPlayed) {
        this.developmentCardPlayed = developmentCardPlayed;
    }

    /**
     * Method executed after all player turns.
     * By default it resets the turn counter, the turn owner to the first alive player and increases round counter.
     * If maximum number of rounds reached, game ends.
     * @param gameState - current game state.
     */
    public void endRound(AbstractGameState gameState) {
        roundCounter++;
        turnStep = 0;
        turnCounter = 0;
        moveToNextPlayer(gameState, nextPlayer(gameState));

        // todo (mb) Catan has no max turns, but logic below could be useful
//        if (nMaxRounds != -1 && roundCounter == nMaxRounds) gameState.setGameStatus(Utils.GameResult.GAME_END);
//        else {
//            turnStep = 0;
//            turnCounter = 0;
//            moveToNextPlayer(gameState, nextPlayer(gameState));
//        }
    }
}
