package core.turnorders;

import core.AbstractGameState;

import java.util.LinkedList;
import java.util.Queue;

import static utilities.Utils.GameResult.GAME_ONGOING;

public class ReactiveTurnOrder extends TurnOrder {

    protected Queue<Integer> reactivePlayers = new LinkedList<>();

    public ReactiveTurnOrder(int nPlayers){
        super(nPlayers);
    }

    public ReactiveTurnOrder(int nPlayers, int nMaxRounds) {
        super(nPlayers, nMaxRounds);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState gameState) {
        if (reactivePlayers.size() > 0) return reactivePlayers.peek();
        else return turnOwner;
    }

    @Override
    public TurnOrder copy() {
        ReactiveTurnOrder to = new ReactiveTurnOrder(nPlayers);
        to.reactivePlayers = new LinkedList<>(reactivePlayers);
        return copyTo(to);
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
}
