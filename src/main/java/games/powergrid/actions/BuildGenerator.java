package games.powergrid.actions;

import java.util.Objects;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;

public class BuildGenerator extends AbstractAction {
    private final int cityId;
    private final int cost;

    public BuildGenerator(int cityId, int cost){
        this.cityId = cityId;
        this.cost = cost;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState pggs = (PowerGridGameState) gs;
        int playerId = pggs.getCurrentPlayer();

        int[][] citySlotsById = pggs.getCitySlotsById();
        if (cityId < 0 || cityId >= citySlotsById.length)
            throw new IllegalArgumentException("cityId out of range: " + cityId);

        int[] citySlots = citySlotsById[cityId];
        if (citySlots == null) throw new IllegalStateException("City slots not initialised for " + cityId);

        int step = pggs.getStep();
        int slotIndex = getFirstOpenSlot(citySlots, step);
        if (slotIndex == -1 || citySlots[slotIndex] != -1)
            throw new IllegalStateException("No open slots for city " + cityId);

        int money = pggs.getPlayersMoney(playerId);  
        if (money < cost)
            throw new IllegalStateException("Insufficient funds: have " + money + ", need " + cost);

        citySlots[slotIndex] = playerId;
        pggs.decreasePlayerMoney(playerId, cost);

        pggs.incrementCityCount(playerId); 

        return true;
    }


    @Override
    public AbstractAction copy() {
        return new BuildGenerator(cityId, cost);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BuildGenerator)) return false;
        BuildGenerator that = (BuildGenerator) obj;
        return this.cityId == that.cityId && this.cost == that.cost;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cityId, cost);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        try {
            PowerGridGameState s = (PowerGridGameState) gameState;
            int[][] slots = s.getCitySlotsById();
            int slot = getFirstOpenSlot(slots[cityId], s.getStep());
            String slotStr = (slot >= 0) ? String.valueOf(slot) : "none";
            return String.format("BuildGenerator(city=%d, slot=%s, cost=%d)", cityId, slotStr, cost);
        } catch (Exception e) {
            return String.format("BuildGenerator(city=%d, cost=%d)", cityId, cost);
        }
    }

    private int getFirstOpenSlot(int[] citySlot, int step) {
        int limit = Math.min(step, citySlot.length);
        for (int i = 0; i < limit; i++) {
            if (citySlot[i] == -1) return i;
        }
        return -1;
    }
}