package games.risk;

/**
 * The continents of the default map (data/risk/worldMap.json) by name, for the tests.
 */
class WorldContinents {

    static final RiskContinent NORTH_AMERICA = WorldMap.MAP.continents().get(0);
    static final RiskContinent SOUTH_AMERICA = WorldMap.MAP.continents().get(1);
    static final RiskContinent EUROPE = WorldMap.MAP.continents().get(2);
    static final RiskContinent AFRICA = WorldMap.MAP.continents().get(3);
    static final RiskContinent ASIA = WorldMap.MAP.continents().get(4);
    static final RiskContinent AUSTRALIA = WorldMap.MAP.continents().get(5);

    private WorldContinents() {
    }
}
