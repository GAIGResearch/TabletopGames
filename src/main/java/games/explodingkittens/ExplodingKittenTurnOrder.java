package games.explodingkittens;

import core.AbstractGameState;
import core.turnorder.ReactiveTurnOrder;
import core.turnorder.TurnOrder;

import static games.explodingkittens.ExplodingKittensGameState.GamePhase.NopePhase;

public class ExplodingKittenTurnOrder extends ReactiveTurnOrder {
    int requiredDraws = 1;

    public ExplodingKittenTurnOrder(int nPlayers){
        super(nPlayers);
    }

    @Override
    public void endPlayerTurnStep(AbstractGameState gameState) {
        if (reactivePlayers.size() > 0) reactivePlayers.poll();
        else {
            requiredDraws -= 1;
            if (requiredDraws == 0 || !gameState.isPlayerAlive(turnOwner)) {
                requiredDraws = 1;
                turnOwner = (turnOwner + 1) % nPlayers;
                while (!gameState.isPlayerAlive(turnOwner))
                    turnOwner = (turnOwner + 1) % nPlayers;
            }
        }
    }

    public void registerNopeableActionByPlayer(ExplodingKittensGameState gameState){
        addAllReactivePlayersButCurrent(gameState);
        gameState.setGamePhase(NopePhase);
    }

    public void registerFavorAction(int player){
        reactivePlayers.clear();
        addReactivePlayer(player);
    }

    public void registerAttackAction(int attackTarget){
        requiredDraws = 2;
        turnOwner = attackTarget;
    }

    @Override
    public TurnOrder copy() {
        ExplodingKittenTurnOrder to = (ExplodingKittenTurnOrder) super.copy();
        to.requiredDraws = requiredDraws;
        return copyTo(to);
    }
}
