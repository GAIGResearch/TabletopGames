package games.catan;

import core.AbstractGameState;
import core.interfaces.IGamePhase;
import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;

import java.util.LinkedList;

import static utilities.Utils.GameResult.GAME_ONGOING;

public class CatanTurnOrder extends ReactiveTurnOrder {
    protected int turnStep;
    protected int turnStage; // trade stage (0), build stage (1)
    protected boolean developmentCardPlayed; // Tracks whether a player has played a development card this turn
    private IGamePhase nextGamePhase; // tracks the game phase where it should be reset after a reaction

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
        nextGamePhase = AbstractGameState.DefaultGamePhase.Main;
    }

    @Override
    protected TurnOrder _copy() {
        CatanTurnOrder copy = new CatanTurnOrder(nPlayers, nMaxRounds);
        copy.turnStep = turnStep;
        copy.turnStage = turnStage;
        copy.developmentCardPlayed = developmentCardPlayed;
        copy.reactivePlayers = new LinkedList<>(reactivePlayers);
        copy.nextGamePhase = nextGamePhase;
        return copy;
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

    public void endReaction(AbstractGameState gs){
        reactivePlayers.poll();
        if (reactionsFinished()){
            // discard only happens when a knight card has been played or a 7 has been rolled
            if (gs.getGamePhase().equals(CatanGameState.CatanGamePhase.Discard)){
                gs.setGamePhase(CatanGameState.CatanGamePhase.Steal);
            } else {
                // should only happen from a trade reaction
                gs.setGamePhase(nextGamePhase);
            }
        }
    }

    // todo check if transitions are correct in all cases
    public void endTurnStage(AbstractGameState gameState){
        /* Robber -> Discard
           Trade -> Build
        *  */
        IGamePhase gamePhase = gameState.getGamePhase();
        if (gamePhase.equals(AbstractGameState.DefaultGamePhase.Main)){
            if (((CatanGameState)gameState).getRollValue() == 7){
                gameState.setGamePhase(CatanGameState.CatanGamePhase.Robber);
            } else {
                gameState.setGamePhase(CatanGameState.CatanGamePhase.Trade);
            }
        }
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Robber)){
            nextGamePhase = gamePhase;
            gameState.setGamePhase(CatanGameState.CatanGamePhase.Discard);
            ((CatanTurnOrder)gameState.getTurnOrder()).addAllReactivePlayers(gameState);
            return;
        }
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Trade)){
            gameState.setGamePhase(CatanGameState.CatanGamePhase.Build);
            return;
        }
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Build)){
            endPlayerTurn(gameState);
            gameState.setMainGamePhase();
            return;
        }
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Discard)){
            endReaction(gameState);
            if (reactionsFinished())
                gameState.setGamePhase(CatanGameState.CatanGamePhase.Steal);
            return;
        }
        if (gamePhase.equals(CatanGameState.CatanGamePhase.TradeReaction)) {
            nextGamePhase = CatanGameState.CatanGamePhase.Build;
            endReaction(gameState);
            return;
        }
        if (gamePhase.equals(CatanGameState.CatanGamePhase.PlaceRoad)){
            endPlayerTurn(gameState);
            gameState.setGamePhase(nextGamePhase);
            return;
        }
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Steal)){
            endPlayerTurn(gameState);
            gameState.setGamePhase(CatanGameState.CatanGamePhase.Trade);
            return;
        }
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Setup)){
            endPlayerTurn(gameState);
            if (getRoundCounter() >= 2){
                // After 2 rounds of setup the main game phase starts
                gameState.setMainGamePhase();
            }
        }
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
    }
}
