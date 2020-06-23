package games.descent;

import core.AbstractGameState;
import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;
import games.descent.components.Figure;
import games.descent.components.Monster;

import java.util.ArrayList;
import java.util.LinkedList;

import static utilities.Utils.GameResult.GAME_END;
import static utilities.Utils.GameResult.GAME_ONGOING;

public class DescentTurnOrder extends ReactiveTurnOrder {
    int monsterGroupActingNext;
    int monsterActingNext;
    int heroPlayerActingNext;

    // TODO: order is player, overlord(monster group 1), player, overlord (monster group 2) ...
    public DescentTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    @Override
    protected void _reset() {
        super._reset();
        monsterGroupActingNext = 0;
        monsterActingNext = 0;
        heroPlayerActingNext = 0;
    }

    public int getMonsterGroupActingNext() {
        return monsterGroupActingNext;
    }

    public int getHeroPlayerActingNext() {
        return heroPlayerActingNext;
    }

    public int getMonsterActingNext() {
        return monsterActingNext;
    }

    public void nextMonster(int groupSize) {
        monsterActingNext = (groupSize+monsterActingNext+1)%groupSize;
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;
        DescentGameState dgs = (DescentGameState) gameState;

        turnCounter++;
        if (turnCounter >= (nPlayers + dgs.getMonsters().size())) endRound(gameState);
        else {
            turnOwner = nextPlayer(gameState);
            int n = 0;
            while (gameState.getPlayerResults()[turnOwner] != GAME_ONGOING) {
                turnOwner = nextPlayer(gameState);
                n++;
                if (n >= nPlayers) {
                    gameState.setGameStatus(GAME_END);
                    break;
                }
            }
        }
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        if (turnOwner == 0) {
            int nMonsters = ((DescentGameState)gameState).getMonsters().size();
            monsterGroupActingNext = (nMonsters+monsterGroupActingNext+1)%nMonsters;
            return 1+heroPlayerActingNext;
        } else {
            heroPlayerActingNext = (nPlayers-1+heroPlayerActingNext+1)%(nPlayers-1);
            return 0;
        }
    }

    @Override
    public void endRound(AbstractGameState gameState) {
        super.endRound(gameState);

        // Reset figures for the next round
        DescentGameState dgs = (DescentGameState) gameState;
        for (Figure f: dgs.getHeroes()) {
            f.resetRound();
        }
        for (ArrayList<Monster> mList: dgs.getMonsters()) {
            for (Monster m: mList) {
                m.resetRound();
            }
        }
        dgs.overlord.resetRound();
    }

    @Override
    protected TurnOrder _copy() {
        DescentTurnOrder pto = new DescentTurnOrder(nPlayers);
        pto.reactivePlayers = new LinkedList<>(reactivePlayers);
        pto.monsterActingNext = monsterActingNext;
        pto.monsterGroupActingNext = monsterGroupActingNext;
        pto.heroPlayerActingNext = heroPlayerActingNext;
        return pto;
    }
}
