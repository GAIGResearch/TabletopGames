package games.powergrid.components;


import core.components.Card;
import games.powergrid.PowerGridParameters.Resource;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

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
        return new PowerGridCard(Type.STEP3, -1, 0, new PlantInput(Map.of()));
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
}
