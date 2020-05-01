package games.explodingkittens;

import core.AbstractGameState;
import core.turnorder.TurnOrder;

import java.util.*;

public class ExplodingKittenTurnOrder extends TurnOrder {
    public int currentPlayer;
    int requiredDraws = 1;

    Queue<Integer> requiresReactionByPlayer = new LinkedList<>();
    int direction = 1;

    public ExplodingKittenTurnOrder(int nPlayers){
        super(nPlayers);
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
                    currentPlayer = (currentPlayer + direction) % nPlayers;
                    while (!ekgs.isPlayerAlive[currentPlayer])
                        currentPlayer = (currentPlayer + direction) % nPlayers;
                }
                break;
            case NopePhase:
            case FavorPhase:
                requiresReactionByPlayer.poll();
                break;
        }
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
