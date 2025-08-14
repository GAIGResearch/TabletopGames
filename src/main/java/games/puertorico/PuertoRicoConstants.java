package games.puertorico;

import games.puertorico.components.Building;
import games.puertorico.roles.*;

import java.util.*;

import static games.puertorico.PuertoRicoConstants.Crop.INDIGO;


public class PuertoRicoConstants {

    public enum ActionRotation {SINGLE_ACTION, MULTIPLE_ACTIONS, MULTIPLE_ROUNDS}

    public enum Role {
        CAPTAIN(true, "All players load goods on the cargo ships (public or private), starting with the player who chose the role.\n" +
                "Each cargo ship carries good of a single kind, and no 2 ships may carry goods of the same type.\n" +
                "Players load only one kind of goods, and they must load as many as possible, if any valid goods are available.\n" +
                "The phase continues as long as at least 1 player has goods they can load. If they can, they must load goods.\n" +
                "Players earn 1VP per good loaded.\n" +
                "When the phase ends, leftover goods must be stored. Each player can store for free 1 type of good. Good types may also be stored in warehouses.\n" +
                "If goods cannot be stored, they are lost. Finally, the full cargo ships get unloaded.", ActionRotation.MULTIPLE_ROUNDS),
        DISCARD(true, "All players discard goods if leftover from Captain phase.", ActionRotation.MULTIPLE_ACTIONS, true),
        BUILDER(true, "All players may buy a building and place it in their town.\n" +
                "The cost of the building is reduced by 1 for the player who chose the Builder role.\n" +
                "The cost is further reduced by 1 for each quarry the player owns.\n" +
                "The player may buy a building of any size, as long as it fits in their town. "),
        MAYOR(true, "All players earn colonists and place them on either plantations, quarries or buildings.\n" +
                "All players pick 1 colonist from the colonist ship in turn order, starting with the player who chose the role, until no more colonists remain on the ship.\n" +
                "The player who chose the role earns 1 extra colonist.\n" +
                "Colonists may be moved from one plantation, quarry or building to another. Only occupied tiles function.\n" +
                "When the phase ends, the ship gets filled with colonists again: 1 for every unoccupied space in the buildings in all players' towns.", ActionRotation.MULTIPLE_ACTIONS),
        TRADER(true, "All players sell goods. Each player may sell a single good.\n" +
                "The player earns doubloons equal to the value of the good sold, as indicated in the market area.\n" +
                "The player who chose the role earns 1 extra doubloon for the good sold.\n" +
                "Players can't sell any more goods if the trading house is full. The trading house buys only different kinds of goods (exception: office building)."),
        SETTLER(true, "Take a plantation tile from the supply and place it in your island.\n" +
                "The player who chose the role may take a quarry instead. "),
        CRAFTSMAN(false, "All players produce goods from all occupied plantations with a matching occupied factory (exception: corn does not need a factory).\n" +
                "The player who chose the role produces 1 extra good of their choice."),
        PROSPECTOR(false, "Take 1 doubloon from the bank. All other players get nothing.");
        public final String description;
        public final boolean allPlayers;
        public final ActionRotation rotationType;
        public final boolean hidden; // for implementation - not provided as an option to the player

        Role(boolean allPlayers, String description) {
            this(allPlayers, description, ActionRotation.SINGLE_ACTION, false);
        }

        Role(boolean allPlayers, String description, ActionRotation actionType) {
            this(allPlayers, description, actionType, false);
        }

        Role(boolean allPlayers, String description, ActionRotation actionType, boolean hidden) {
            this.allPlayers = allPlayers;
            this.rotationType = actionType;
            this.hidden = hidden;
            this.description = description;
        }

        public PuertoRicoRole<?> getAction(PuertoRicoGameState state) {
            switch (this) {
                case SETTLER:
                    return new Settler(state);
                case BUILDER:
                    return new Builder(state);
                case CRAFTSMAN:
                    return new Craftsman(state);
                case PROSPECTOR:
                    return new Prospector(state);
                case MAYOR:
                    return new Mayor(state);
                case TRADER:
                    return new Trader(state);
                case CAPTAIN:
                    return new Captain(state);
                case DISCARD:
                    // This should only be instantiated from Captain
                default:
                    throw new IllegalArgumentException("Unknown role: " + this);
            }
        }
    }

    public enum Crop {
        CORN(0), INDIGO(1), SUGAR(2), TOBACCO(3), COFFEE(4), QUARRY(0);

        public static List<Crop> getTradeableCrops() {
            return Arrays.asList(CORN, INDIGO, SUGAR, TOBACCO, COFFEE);
        }

        public final int price;

        Crop(int price) {
            this.price = price;
        }
    }

    public enum BuildingType {  // TODO: replace numbers in tooltips with parameters/variables
        SMALL_INDIGO_PLANT(1, 1, 1, 1, 1, "Small Indigo Plant, cost 1, VP 1, capacity 1", INDIGO),
        INDIGO_PLANT(3, 2, 3, 2, 1, "Indigo Plant, cost 3, VP 2, capacity 3", INDIGO),
        SMALL_SUGAR_MILL(2, 1, 1, 1, 1, "Small Sugar Mill, cost 2, VP 1, capacity 1", Crop.SUGAR),
        SUGAR_MILL(4, 2, 3, 2, 1, "Sugar Mill, cost 4, VP 2, capacity 3", Crop.SUGAR),
        TOBACCO_STORAGE(5, 3, 3, 3, 1, "Tobacco Storage, cost 5, VP 3, capacity 3", Crop.TOBACCO),
        COFFEE_ROASTER(6, 3, 2, 3, 1, "Coffee Roaster, cost 6, VP 3, capacity 2", Crop.COFFEE),
        SMALL_MARKET(2, 1, 1, "Small Market, cost 2, VP 1, capacity 1. +1 doubloon with sale (trader phase)."),
        HACIENDA(2, 1, 1, "Hacienda, cost 2, VP 1, capacity 1. +1 plantation from supply (settler phase)."),
        CONSTRUCTION_HUT(2, 1, 1, "Construction Hut, cost 2, VP 1, capacity 1. quarry instead of plantation (settler phase)."),
        SMALL_WAREHOUSE(2, 1, 1, "Small Warehouse, cost 2, VP 1, capacity 1. store 1 kind of goods (captain phase)."),
        HOSPICE(4, 2, 2, "Hospice, cost 4, VP 2, capacity 1. +1 colonist for settling (settler phase)."),
        OFFICE(5, 2, 2, "Office, cost 5, VP 2, capacity 1. sell same kind of goods (trader phase)."),
        LARGE_MARKET(5, 2, 2, "Large Market, cost 5, VP 2, capacity 1. +2 doubloons with sale (trader phase)."),
        LARGE_WAREHOUSE(6, 2, 2, "Large Warehouse, cost 6, VP 2, capacity 1. store 2 kinds of goods (captain phase)."),
        UNIVERSITY(8, 3, 3, "University, cost 8, VP 3, capacity 1. +1 colonist for building (builder phase)."),
        FACTORY(7, 3, 3, "Factory, cost 7, VP 3, capacity 1. +0/1/2/3/5 doubloons with production (craftsman phase)."),
        HARBOUR(8, 3, 3, "Harbour, cost 8, VP 3, capacity 1. +1 VP per delivery (captain phase)."),
        WHARF(9, 3, 3, "Wharf, cost 9, VP 3, capacity 1. Your own ship (captain phase)."),
        GUILD_HALL(10, 4, 1, 4, 2, "Guild Hall, cost 10, VP 4, +2 VP per large and +1 VP per small building."),
        RESIDENCE(10, 4, 1, 4, 2, "Residence, cost 10, VP 4, +4/5/6/7 VP for <10/10/11/12 occupied island spaces."),
        CITY_HALL(10, 4, 1, 4, 2, "City Hall, cost 10, VP 4, capacity 1. +1 VP per building (builder phase)."),
        FORTRESS(10, 4, 1, 4, 2, "Fortress, cost 10, VP 4, capacity 1. +1 VP per ship (captain phase)."),
        CUSTOMS_HOUSE(10, 4, 1, 4, 2, "Customs House, cost 10, VP 4, capacity 1. +1 VP per doubloon (trader phase).");

        public final int cost;
        public final int capacity;
        public final int vp;
        public final int nMaxQuarryDiscount;
        public final String tooltip;
        public final int size;
        public final Crop crop; // null if not a production building

        BuildingType(int cost, int vp, int capacity, int nMaxQuarryDiscount, int size, String tooltip) {
            this(cost, vp, capacity, nMaxQuarryDiscount, size, tooltip, null);
        }

        BuildingType(int cost, int vp, int capacity, int nMaxQuarryDiscount, int size, String tooltip, Crop production) {
            this.cost = cost;
            this.vp = vp;
            this.capacity = capacity;
            this.nMaxQuarryDiscount = nMaxQuarryDiscount;
            this.tooltip = tooltip;
            this.size = size;
            this.crop = production;
        }

        BuildingType(int cost, int vp, int nMaxQuarryDiscount, String tooltip) {
            this(cost, vp, 1, nMaxQuarryDiscount, 1, tooltip);
        }

        public static Set<Integer> getQuarryDiscounts() {
            Set<Integer> discounts = new HashSet<>();
            for (BuildingType type : BuildingType.values()) {
                discounts.add(type.nMaxQuarryDiscount);
            }
            return discounts;
        }

        public static List<BuildingType> getBuildingTypesWithQuarryDiscount(int n) {
            List<BuildingType> types = new ArrayList<>();
            for (BuildingType type : BuildingType.values()) {
                if (type.nMaxQuarryDiscount == n) {
                    types.add(type);
                }
            }
            return types;
        }

        public Building instantiate() {
            return Building.instantiate(this);
        }
    }

}
