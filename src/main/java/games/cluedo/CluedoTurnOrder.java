package games.cluedo;

import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;

import java.util.*;

public class CluedoTurnOrder extends ReactiveTurnOrder {

    Queue<Integer> playerQueue;

    public CluedoTurnOrder(int nPlayers) {
        super(nPlayers);
        playerQueue = new LinkedList<>();
    }

    public int getNextPlayer() {
        if (!reactivePlayers.isEmpty()) {
            return reactivePlayers.remove();
        } else {
            playerQueue.add(playerQueue.peek());
            return playerQueue.remove();
        }
    }

    public void setTurnOrder(HashMap<Integer, Integer> characterToPlayerMap) {
        for (int i=0; i<6; i++) {
            if (characterToPlayerMap.containsKey(i)) {
                playerQueue.add(characterToPlayerMap.get(i));
            }
        }
    }

    @Override
    protected TurnOrder _copy() {
        CluedoTurnOrder copy = new CluedoTurnOrder(nPlayers);

        copy.playerQueue = new LinkedList<>();
        copy.playerQueue.addAll(playerQueue);
        copy.reactivePlayers = new LinkedList<>(reactivePlayers);
        return copy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerQueue);
    }

    protected void resetReactivePlayers() {
        reactivePlayers.clear();
    }

    public void addReactivePlayer(Queue<Integer> playerQueue) {
        reactivePlayers.addAll(playerQueue);
    }

}
