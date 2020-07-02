package games.explodingkittens;

import core.AbstractGameState;
import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;
import utilities.Utils;

import java.util.ArrayList;
import java.util.LinkedList;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.Nope;
import static utilities.Utils.GameResult.GAME_ONGOING;

public class ExplodingKittenTurnOrder extends ReactiveTurnOrder {
    int requiredDraws;

    public ExplodingKittenTurnOrder(int nPlayers){
        super(nPlayers);
        requiredDraws = 1;
    }

    @Override
    protected void _reset() {
        super._reset();
        requiredDraws = 1;
    }

    public void endPlayerTurnStep(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;

        ArrayList<Integer> deadPlayers = new ArrayList<>();
        for (int i: reactivePlayers) {
            if (gameState.getPlayerResults()[i] != Utils.GameResult.GAME_ONGOING) {
                deadPlayers.add(i);
            }
        }
        reactivePlayers.removeAll(deadPlayers);
        if (reactivePlayers.size() > 0) reactivePlayers.poll();
        else {
            requiredDraws -= 1;
            if (requiredDraws == 0 || gameState.getPlayerResults()[turnOwner] != Utils.GameResult.GAME_ONGOING) {
                requiredDraws = 1;
                endPlayerTurn(gameState);
            }
        }
    }

    public void registerNopeableActionByPlayer(ExplodingKittensGameState gameState){
        addAllReactivePlayersButCurrent(gameState);
        gameState.setGamePhase(Nope);
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
    protected TurnOrder _copy() {
        ExplodingKittenTurnOrder to = new ExplodingKittenTurnOrder(nPlayers);
        to.reactivePlayers = new LinkedList<>(reactivePlayers);
        to.requiredDraws = requiredDraws;
        return to;
    }
}
