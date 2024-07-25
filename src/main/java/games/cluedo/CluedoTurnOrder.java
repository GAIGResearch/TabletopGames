package games.cluedo;

import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeMap;

public class CluedoTurnOrder extends ReactiveTurnOrder {

    HashMap<Integer, Integer> characterToPlayerMap;
    Queue<Integer> playerQueue;

    public CluedoTurnOrder(int nPlayers) {
        super(nPlayers);
        characterToPlayerMap = new HashMap<>();
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
        TreeMap<Integer, Integer> sortedMap = new TreeMap<>(characterToPlayerMap);
        this.characterToPlayerMap = new HashMap<>(sortedMap);
        for (int i=0; i<6; i++) {
            if (characterToPlayerMap.containsKey(i)) {
                playerQueue.add(characterToPlayerMap.get(i));
            }
        }
    }

    @Override
    protected TurnOrder _copy() {
        CluedoTurnOrder copy = new CluedoTurnOrder(nPlayers);
        copy.characterToPlayerMap = new HashMap<>(characterToPlayerMap);
        return copy;
    }

    protected void resetReactivePlayers() {
        reactivePlayers.clear();
    }

}
