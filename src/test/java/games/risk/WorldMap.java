package games.risk;

import java.util.List;

/**
 * The territories of the default map (data/risk/worldMap.json) by name, for the tests. Territories are values, so
 * these equal the ones of any state using the default map.
 */
class WorldMap {

    static final RiskMap MAP = new RiskParameters().getMap();
    static final List<RiskTerritory> ALL = MAP.territories();

    static final RiskTerritory ALASKA = MAP.territory("Alaska");
    static final RiskTerritory NORTHWEST_TERRITORY = MAP.territory("Northwest Territory");
    static final RiskTerritory GREENLAND = MAP.territory("Greenland");
    static final RiskTerritory ALBERTA = MAP.territory("Alberta");
    static final RiskTerritory ONTARIO = MAP.territory("Ontario");
    static final RiskTerritory QUEBEC = MAP.territory("Quebec");
    static final RiskTerritory WESTERN_UNITED_STATES = MAP.territory("Western United States");
    static final RiskTerritory EASTERN_UNITED_STATES = MAP.territory("Eastern United States");
    static final RiskTerritory CENTRAL_AMERICA = MAP.territory("Central America");
    static final RiskTerritory VENEZUELA = MAP.territory("Venezuela");
    static final RiskTerritory PERU = MAP.territory("Peru");
    static final RiskTerritory BRAZIL = MAP.territory("Brazil");
    static final RiskTerritory ARGENTINA = MAP.territory("Argentina");
    static final RiskTerritory ICELAND = MAP.territory("Iceland");
    static final RiskTerritory SCANDINAVIA = MAP.territory("Scandinavia");
    static final RiskTerritory UKRAINE = MAP.territory("Ukraine");
    static final RiskTerritory GREAT_BRITAIN = MAP.territory("Great Britain");
    static final RiskTerritory NORTHERN_EUROPE = MAP.territory("Northern Europe");
    static final RiskTerritory WESTERN_EUROPE = MAP.territory("Western Europe");
    static final RiskTerritory SOUTHERN_EUROPE = MAP.territory("Southern Europe");
    static final RiskTerritory NORTH_AFRICA = MAP.territory("North Africa");
    static final RiskTerritory EGYPT = MAP.territory("Egypt");
    static final RiskTerritory EAST_AFRICA = MAP.territory("East Africa");
    static final RiskTerritory CONGO = MAP.territory("Congo");
    static final RiskTerritory SOUTH_AFRICA = MAP.territory("South Africa");
    static final RiskTerritory MADAGASCAR = MAP.territory("Madagascar");
    static final RiskTerritory URAL = MAP.territory("Ural");
    static final RiskTerritory SIBERIA = MAP.territory("Siberia");
    static final RiskTerritory YAKUTSK = MAP.territory("Yakutsk");
    static final RiskTerritory KAMCHATKA = MAP.territory("Kamchatka");
    static final RiskTerritory IRKUTSK = MAP.territory("Irkutsk");
    static final RiskTerritory MONGOLIA = MAP.territory("Mongolia");
    static final RiskTerritory JAPAN = MAP.territory("Japan");
    static final RiskTerritory AFGHANISTAN = MAP.territory("Afghanistan");
    static final RiskTerritory CHINA = MAP.territory("China");
    static final RiskTerritory MIDDLE_EAST = MAP.territory("Middle East");
    static final RiskTerritory INDIA = MAP.territory("India");
    static final RiskTerritory SIAM = MAP.territory("Siam");
    static final RiskTerritory INDONESIA = MAP.territory("Indonesia");
    static final RiskTerritory NEW_GUINEA = MAP.territory("New Guinea");
    static final RiskTerritory WESTERN_AUSTRALIA = MAP.territory("Western Australia");
    static final RiskTerritory EASTERN_AUSTRALIA = MAP.territory("Eastern Australia");

    private WorldMap() {
    }
}
