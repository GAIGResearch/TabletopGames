package games.powergrid.actions;

import java.util.Objects;


import core.AbstractGameState;
import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;
import games.powergrid.components.PowerGridCity;
import games.powergrid.components.PowerGridGraphBoard;

/**
 * Action that builds a generator in a given city at a specified cost.
 * <p>
 * This action validates city bounds and slot availability for the current game step,
 * checks that the acting player has sufficient funds, then:
 * <ol>
 *   <li>Claims the first open slot allowed by the current step,</li>
 *   <li>Deducts the build cost from the player,</li>
 *   <li>Increments the player's owned-city count.</li>
 * </ol>
 * <p>
 * Slot availability follows Power Grid rules (e.g., only slot 0 in Step 1; slots 0–1 in Step 2; etc.).
 * Exceptions are thrown if preconditions are violated (invalid city, no open slot, or insufficient funds).
 *
 * <p><b>Side effects:</b> Mutates {@link PowerGridGameState} by updating city slots, player money,
 * and city counts.
 */

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
        PowerGridGameState s = (PowerGridGameState) gameState;
        PowerGridGraphBoard board = s.getGameMap();

        PowerGridCity c = (board != null) ? board.city(cityId) : null;
        String cityName = (c != null) ? c.getComponentName() : String.valueOf(cityId);
        cityName = cityName.replace('_', ' ');

        String slotStr = "none";
        int[][] slots = s.getCitySlotsById();
        if (slots != null && cityId >= 0 && cityId < slots.length) {
            int slot = getFirstOpenSlot(slots[cityId], s.getStep());
            if (slot >= 0) slotStr = String.valueOf(slot + 1);
        }

        return String.format("BuildGenerator(city=%s, slot=%s, cost=%d)", cityName, slotStr, cost);
    }

    
    public String getSimpleString() {
    	return "Build " + this.cityId; 
    }

    /**
     * Returns the index of the first available (empty) city slot for building in the current step.
     * <p>
     * A slot is considered open if its value is {@code -1}. The search is limited by the
     * current game step (e.g., in Step 1 only slot 0 can be used, in Step 2 up to slot 1, etc.).
     *
     * @param citySlot an array of player IDs occupying each slot in the city; {@code -1} indicates empty.
     * @param step     the current game step, which determines how many slots are available for use.
     * @return the index of the first open slot within the allowed range, or {@code -1} if none are available.
     */
    
    private int getFirstOpenSlot(int[] citySlot, int step) {
        int limit = Math.min(step, citySlot.length);
        for (int i = 0; i < limit; i++) {
            if (citySlot[i] == -1) return i;
        }
        return -1;
    }
}