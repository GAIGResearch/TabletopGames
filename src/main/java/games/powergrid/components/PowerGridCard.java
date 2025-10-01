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
 * Power Plant card definition for Power Grid.
 * Includes the ResourceType enum and PlantInput helper class
 * so everything related to plants is in one file.
 */
public class PowerGridCard extends Card {

    public enum Type { PLANT, STEP3 }
    /**
     * Helper to describe the resource requirements of a plant. Once a plant is created 
     * it is immutable 
     */
    public static final class PlantInput {
        private final EnumMap<Resource, Integer> req; //how many resources are required

        public PlantInput(Map<Resource, Integer> req) { //constructor
            this.req = new EnumMap<>(Resource.class); //instantiates the Enum map based on teResource Type class
            this.req.putAll(req);//Copies all entries from the provided map into its own internal req
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
    public Card copy() { return this; } // immutable

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
    
    public List<EnumMap<Resource, Integer>> generatePossibleCombos() {
        List<EnumMap<Resource, Integer>> combos = new ArrayList<>();
        EnumMap<Resource, Integer> req = input.asMap();

        if (req.size() == 1) {
            // Only one resource → just return the requirement
            combos.add(req);
            return combos;
        }

        if (req.size() == 2) {
            // Hybrid: both values should be the same (e.g. GAS=2, OIL=2)
            Iterator<Map.Entry<Resource,Integer>> it = req.entrySet().iterator();
            Map.Entry<Resource,Integer> first = it.next();
            Map.Entry<Resource,Integer> second = it.next();

            Resource r1 = first.getKey();
            Resource r2 = second.getKey();
            int k = first.getValue();  // assume both values equal

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
