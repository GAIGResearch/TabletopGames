package games.catan;

import core.AbstractGameState;
import core.interfaces.IGamePhase;
import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;

import java.util.LinkedList;

import static utilities.Utils.GameResult.GAME_ONGOING;

public class CatanTurnOrder extends ReactiveTurnOrder {
    protected int turnStep;
    protected int actionsTakenInCurrentStage = 0;
    protected boolean developmentCardPlayed; // Tracks whether a player has played a development card this turn
    private IGamePhase nextGamePhase; // tracks the game phase where it should be reset after a reaction

    CatanTurnOrder(int nPlayers, int nMaxRounds) {
        super(nPlayers, nMaxRounds);
        turnStep = 0;
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
        copy.developmentCardPlayed = developmentCardPlayed;
        copy.reactivePlayers = new LinkedList<>(reactivePlayers);
        copy.nextGamePhase = nextGamePhase;
        copy.actionsTakenInCurrentStage = actionsTakenInCurrentStage;
        return copy;
    }

    /**
     * Method executed after a player's turn is finished.
     * By default it resets the turnStep counter to 0 and increases the turn counter.
     * Then moves to the next alive player. If this is the last player, the round ends.
     * If the game has ended, turn owner is not changed. If there are no players still playing, game ends and method returns.
     *
     * @param gameState - current game state.
     */
    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;

        setDevelopmentCardPlayed(false);
        turnCounter++;
        if (turnCounter >= nPlayers) endRound(gameState);
        else {
            turnStep = 0;
            moveToNextPlayer(gameState, nextPlayer(gameState));
        }
    }

    public void endReaction(AbstractGameState gs) {
        reactivePlayers.poll();
        if (reactionsFinished()) {
            // discard only happens when a knight card has been played or a 7 has been rolled
            if (gs.getGamePhase().equals(CatanGameState.CatanGamePhase.Discard)) {
                setGamePhase(CatanGameState.CatanGamePhase.Steal, gs);
            } else {
                // should only happen from a trade reaction
                setGamePhase(nextGamePhase, gs);
            }
        }
    }

    // todo check if transitions are correct in all cases
    public void endTurnStage(AbstractGameState gameState) {
        /* Robber -> Discard
           Trade -> Build
        *  */
        IGamePhase gamePhase = gameState.getGamePhase();
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Robber)) {
            nextGamePhase = gamePhase;
            setGamePhase(CatanGameState.CatanGamePhase.Discard, gameState);
            ((CatanTurnOrder) gameState.getTurnOrder()).addAllReactivePlayers(gameState);
            return;
        }
        // We finish the overall Trade/TradeReaction pair once we run out of actions (and a Trade has been terminated either
        // with an EndNegotiation or an AcceptTrade action. If we still have actions left, then we
        // can initiate another Trade
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Trade) || gamePhase.equals(CatanGameState.CatanGamePhase.TradeReaction)) {
            if (actionsTakenInCurrentStage++ >= ((CatanParameters) gameState.getGameParameters()).max_trade_actions_allowed) {
                setGamePhase(CatanGameState.CatanGamePhase.Build, gameState);
            } else {
                gameState.setGamePhase(CatanGameState.CatanGamePhase.Trade);
                // this deliberately does not use the local setGamePhase method to avoid resetting actionsTakenInCurrentStage
            }
            return;
        }
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Build)) {
            if (actionsTakenInCurrentStage++ == ((CatanParameters) gameState.getGameParameters()).max_build_actions_allowed) {
                endPlayerTurn(gameState);
                gameState.setMainGamePhase();
            }
            return;
        }
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Discard)) {
            endReaction(gameState);
            if (reactionsFinished())
                setGamePhase(CatanGameState.CatanGamePhase.Steal, gameState);
            return;
        }

        if (gamePhase.equals(CatanGameState.CatanGamePhase.PlaceRoad)) {
            endPlayerTurn(gameState);
            setGamePhase(nextGamePhase, gameState);
            return;
        }
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Steal)) {
            endPlayerTurn(gameState);
            setGamePhase(CatanGameState.CatanGamePhase.Trade, gameState);
            return;
        }
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Setup)) {
            endPlayerTurn(gameState);
            if (getRoundCounter() >= 2) {
                // After 2 rounds of setup the main game phase starts
                gameState.setMainGamePhase();
            }
        }
    }

    protected void setGamePhase(IGamePhase phase, AbstractGameState state) {
        state.setGamePhase(phase);
        actionsTakenInCurrentStage = 0;
    }

    public void handleTradeOffer(CatanGameState gameState, int player) {
        gameState.setGamePhase(CatanGameState.CatanGamePhase.TradeReaction);
        addReactivePlayer(player);
    }

    // Skips remaining actions in turn stage, called for DoNothing actions in multi-action turn stages
    public void skipTurnStage(CatanGameState gameState) {
        if (gameState.getGamePhase() == CatanGameState.CatanGamePhase.Trade) {
            actionsTakenInCurrentStage = ((CatanParameters) gameState.getGameParameters()).max_trade_actions_allowed;
        } else {
            actionsTakenInCurrentStage = ((CatanParameters) gameState.getGameParameters()).max_build_actions_allowed;
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
     *
     * @param gameState - current game state.
     */
    public void endRound(AbstractGameState gameState) {
        roundCounter++;
        turnStep = 0;
        turnCounter = 0;
        moveToNextPlayer(gameState, nextPlayer(gameState));
    }
}
