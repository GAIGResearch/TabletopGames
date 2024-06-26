package core.turnorders;

import core.AbstractGameState;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import static core.CoreConstants.GameResult.GAME_ONGOING;

public class ReactiveTurnOrder extends TurnOrder {

    protected Queue<Integer> reactivePlayers;

    public ReactiveTurnOrder(int nPlayers){
        super(nPlayers);
        reactivePlayers = new LinkedList<>();
    }

    protected ReactiveTurnOrder() {}

    public ReactiveTurnOrder(int nPlayers, int nMaxRounds) {
        super(nPlayers, nMaxRounds);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState gameState) {
        if (reactivePlayers.size() > 0) {
            return reactivePlayers.peek();
        }
        else return turnOwner;
    }

    @Override
    protected TurnOrder _copy() {
        ReactiveTurnOrder to = new ReactiveTurnOrder();
        to.reactivePlayers = new LinkedList<>(reactivePlayers);
        return to;
    }

    @Override
    public void _endRound(AbstractGameState gameState) {
    }

    @Override
    public void _startRound(AbstractGameState gameState) {
    }

    @Override
    protected void _reset() {
        reactivePlayers = new LinkedList<>();
    }

    public boolean reactionsFinished(){
        return reactivePlayers.size() <= 0;
    }

    public Queue<Integer> getReactivePlayers() {
        return reactivePlayers;
    }

    public void addReactivePlayer(int player){
        reactivePlayers.add(player);
    }

    public void addCurrentPlayerReaction(AbstractGameState gameState) {
        reactivePlayers.add(getCurrentPlayer(gameState));
    }

    public void addAllReactivePlayers(AbstractGameState gameState) {
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            if (gameState.getPlayerResults()[i] == GAME_ONGOING) {
                reactivePlayers.add(i);
            }
        }
    }

    public void addAllReactivePlayersButCurrent(AbstractGameState gameState) {
        int currentPlayer = getCurrentPlayer(gameState);
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            if (i != currentPlayer && gameState.getPlayerResults()[i] == GAME_ONGOING) {
                reactivePlayers.add(i);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReactiveTurnOrder)) return false;
        if (!super.equals(o)) return false;
        ReactiveTurnOrder that = (ReactiveTurnOrder) o;
        return Objects.equals(reactivePlayers, that.reactivePlayers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), reactivePlayers);
    }
}
