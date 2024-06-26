package games.poker.components;

import core.components.Counter;

import java.util.HashMap;
import java.util.Objects;

public class MoneyPot extends Counter {

    HashMap<Integer, Integer> playerContribution;  // Mapping from player ID to sum contributed to this pot
    int limit;

    public MoneyPot() {
        super(0, 0, Integer.MAX_VALUE, "");
        playerContribution = new HashMap<>();
        limit = -1;
    }

    private MoneyPot(int valueIdx, int minimum, int maximum, String name, HashMap<Integer, Integer> playerContribution,
                     int limit, int ID) {
        super(null, valueIdx, minimum, maximum, name, ID);
        this.playerContribution = new HashMap<>(playerContribution);
        this.limit = limit;
    }

    public MoneyPot copy() {
        MoneyPot copy = new MoneyPot(valueIdx, minimum, maximum, componentName, playerContribution,
                limit, componentID);
        copyComponentTo(copy);
        return copy;
    }

    public void increment(int value, int player) {
        this.valueIdx += value;

        int contribution = value;
        if (playerContribution.containsKey(player)) {
            contribution += playerContribution.get(player);
            playerContribution.put(player, contribution);
        } else {
            playerContribution.put(player, value);
        }
    }

    public HashMap<Integer, Integer> getPlayerContribution() {
        return playerContribution;
    }

    public int getPlayerContribution(int player) {
        if (playerContribution.containsKey(player)) return playerContribution.get(player);
        return 0;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean isNoLimit() {
        return this.limit == -1;
    }

    public int getLowestContribution() {
        int lowest = Integer.MAX_VALUE;
        for (int contributor: playerContribution.keySet()) {
            if (playerContribution.get(contributor) < lowest) lowest = playerContribution.get(contributor);
        }
        return lowest;
    }

    public int getHighestContribution() {
        int highest = 0;
        for (int contributor: playerContribution.keySet()) {
            if (playerContribution.get(contributor) > highest) highest = playerContribution.get(contributor);
        }
        return highest;
    }

    public boolean isBalanced() {
        return getHighestContribution() == getHighestContribution();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MoneyPot)) return false;
        if (!super.equals(o)) return false;
        MoneyPot moneyPot = (MoneyPot) o;
        return limit == moneyPot.limit && Objects.equals(playerContribution, moneyPot.playerContribution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerContribution, limit);
    }
}
