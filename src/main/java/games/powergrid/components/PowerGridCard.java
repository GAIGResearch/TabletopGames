package games.powergrid.components;


import core.components.Card;
import games.powergrid.PowerGridParameters.Resource;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Immutable model of a Power Grid card.
 *
 * <p>There are two kinds of cards:
 * <ul>
 *   <li>{@link Type#PLANT} – a power plant identified by a unique {@code number}, with a production
 *       {@code capacity} (how many cities it can power) and an {@link PlantInput} specifying its fuel
 *       requirement.</li>
 *   <li>{@link Type#STEP3} – the unique step marker that, when revealed, advances the game to Step 3.
 *       It has no capacity and no resource requirement.</li>
 * </ul>
 *
 * <p><b>Immutability:</b> All fields are {@code final}. {@link PlantInput} defensively copies the
 * provided requirement map, and exposes copies via {@link PlantInput#asMap()}. Because instances are
 * immutable, {@link #copy()} safely returns {@code this}.
 *
 * <p><b>Equality & hashing:</b> Structural equality over {@code type}, {@code number}, {@code capacity},
 * and a snapshot of the {@code input} requirement. See {@link #equals(Object)} and {@link #hashCode()}.
 *
 * <p>Use the factory methods {@link #plant(int, int, java.util.Map)} and {@link #step3()} to construct
 * instances.
 *
 * @see PowerGridParameters.Resource
 * @see PowerGridCard.PlantInput
 */

public class PowerGridCard extends Card {

    public enum Type { PLANT, STEP3 }
    /**
     * Helper to describe the resource requirements of a plant. Once a plant is created 
     * it is immutable 
     */
    public static final class PlantInput {
        private final EnumMap<Resource, Integer> req; //how many resources are required

        public PlantInput(Map<Resource, Integer> req) { 
            this.req = new EnumMap<>(Resource.class); //instantiates the Enum map based on teResource Type class
            this.req.putAll(req);
        }

        public int totalUnits() { return req.values().stream().mapToInt(Integer::intValue).sum(); } //sets the number of units that can be on a card 
        public int get(Resource t) { return req.getOrDefault(t, 0); }
        public EnumMap<Resource,Integer> asMap() { return new EnumMap<>(req); }
        public boolean hasMultipleTypes() { return req.size() > 1; }


        @Override
        public String toString() { return req.toString(); }
    }

    public final Type type;
    public final int number;
    public final int capacity;
    public final PlantInput input;

    public PowerGridCard(Type type, int number, int capacity, PlantInput input) {
        super(type == Type.PLANT ? "Plant-"+number : "STEP3");
        this.type = type;
        this.number = number;
        this.capacity = capacity;
        this.input = input;
    }

    // determines the step3 card which is a unique card in the game 
    public static PowerGridCard step3() {
        return new PowerGridCard(Type.STEP3, 1000, 0, new PlantInput(Map.of()));
    }


    public static PowerGridCard plant(int number, int capacity, Map<Resource,Integer> req) {
        return new PowerGridCard(Type.PLANT, number, capacity, new PlantInput(req));
    }
    public int getNumber() {
    	return this.number;
    }
    public PlantInput getInput() {
    	return this.input;
    }
    public int getCapacity() {
    	return this.capacity;
    }
    
    @Override
    public Card copy() { return this; } 

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PowerGridCard)) return false;
        PowerGridCard that = (PowerGridCard) o;
        return number == that.number && capacity == that.capacity
                && type == that.type && input.asMap().equals(that.input.asMap());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, number, capacity, input.asMap());
    }

    @Override
    public String toString() {
        return type == Type.PLANT
                ? "#" + number + " cap=" + capacity + " req=" + input.asMap()
                : "STEP3";
    }
    
    /**
     * Generates all valid fuel-spend combinations that satisfy this plant's input requirement.
     * <p>
     * Behavior:
     * <ul>
     *   <li>If the requirement involves a single resource type (e.g., {@code COAL=2}),
     *       the only valid combination is that exact map.</li>
     *   <li>If the requirement involves exactly two resource types (hybrid), and both
     *       have the same required amount {@code k} (e.g., {@code GAS=3, OIL=3}),
     *       this enumerates every split {@code (i, k - i)} for {@code i = 0..k}, where
     *       the sum equals {@code k}. For example, {@code k=3} yields:
     *       {@code (0,3), (1,2), (2,1), (3,0)}.</li>
     *   <li>If more than two resource types are present, this method throws
     *       {@link UnsupportedOperationException}.</li>
     * </ul>
     *
     * <p><b>Returns:</b> A list of {@link EnumMap} entries where keys are
     * {@code Resource} and values are units to spend for that resource. Each map
     * represents one valid way to meet the plant's input requirement for a single run.
     *
     * @return all valid spend combinations matching the input requirement
     * @throws UnsupportedOperationException if the requirement includes more than two resource types
     */
    public List<EnumMap<Resource, Integer>> generatePossibleCombos() {
        List<EnumMap<Resource, Integer>> combos = new ArrayList<>();
        EnumMap<Resource, Integer> req = input.asMap();

        if (req.size() == 1) {
            combos.add(req);
            return combos;
        }

        if (req.size() == 2) {
            Iterator<Map.Entry<Resource,Integer>> it = req.entrySet().iterator();
            Map.Entry<Resource,Integer> first = it.next();
            Map.Entry<Resource,Integer> second = it.next();

            Resource r1 = first.getKey();
            Resource r2 = second.getKey();
            int k = first.getValue();  

            for (int i = 0; i <= k; i++) {
                EnumMap<Resource,Integer> option = new EnumMap<>(Resource.class);
                option.put(r1, i);
                option.put(r2, k - i);
                combos.add(option);
            }
            return combos;
        }
        throw new UnsupportedOperationException("More than 2 resource types not handled yet");
    }
}
