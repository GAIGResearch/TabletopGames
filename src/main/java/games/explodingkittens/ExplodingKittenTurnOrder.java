package games.explodingkittens;

import core.AbstractGameState;
import players.AbstractPlayer;
import turnorder.TurnOrder;

import java.util.*;

public class ExplodingKittenTurnOrder extends TurnOrder {
    public int currentPlayer;
    int requiredDraws = 1;

    Queue<Integer> requiresReactionByPlayer = new LinkedList<>();
    int direction = 1;

    public ExplodingKittenTurnOrder(List<AbstractPlayer> players){
        this.players = players;
        this.currentPlayer = 0;
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gameState;
        switch (ekgs.gamePhase) {
            case PlayerMove:
            case DefusePhase:
                requiredDraws -= 1;
                if (requiredDraws == 0 || !ekgs.isPlayerAlive[currentPlayer]) {
                    requiredDraws = 1;
                    currentPlayer = (currentPlayer + direction) % players.size();
                    while (!ekgs.isPlayerAlive[currentPlayer])
                        currentPlayer = (currentPlayer + direction) % players.size();
                }
                break;
            case NopePhase:
            case FavorPhase:
                requiresReactionByPlayer.poll();
                break;
        }
    }

    public AbstractPlayer getCurrentPlayer(AbstractGameState gameState){
        return players.get(getCurrentPlayerIndex(gameState));

    }

    public int getCurrentPlayerIndex(AbstractGameState gameState){
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gameState;
        switch (ekgs.gamePhase){
            case NopePhase:
            case FavorPhase:
                return requiresReactionByPlayer.peek();
            case PlayerMove:
            case DefusePhase:
            default:
                return currentPlayer;
        }
    }

    public void registerNopeableActionByPlayer(ExplodingKittensGameState gameState){
        int nextPlayer = getCurrentPlayerIndex(gameState);

        requiresReactionByPlayer.clear();
        for (int i = 0; i < gameState.getNPlayers()-1; i++)
        {
            nextPlayer = (nextPlayer + 1) % gameState.getNPlayers();
            if (gameState.isPlayerAlive[nextPlayer])
                requiresReactionByPlayer.add(nextPlayer);
        }

        gameState.gamePhase = ExplodingKittensGamePhase.NopePhase;
    }

    public boolean reactionsRemaining(){
        return requiresReactionByPlayer.size() > 0;
    }

    public void registerFavorAction(int player){
        requiresReactionByPlayer.clear();
        requiresReactionByPlayer.add(player);
    }

    public void registerAttackAction(int attackTarget){
        if (requiredDraws == 1)
        {
            requiredDraws = 2;
        } else{
            requiredDraws += 2;
        }
        currentPlayer = attackTarget;
    }
}
