package games.risk;

import games.risk.components.RiskCard;

/**
 * A territory of the map, with its continent and the symbol on its RISK card. Made when the map is loaded (RiskMap);
 * index is its position in RiskMap.territories() and in the state's arrays.
 */
public record RiskTerritory(int index, String name, RiskContinent continent, RiskCard.Symbol symbol) {

    /**
     * By index and name only (equal territories have equal continents and symbols): the record's default would hash
     * the symbol enum by identity, which differs from one run to the next.
     */
    @Override
    public int hashCode() {
        return 31 * index + name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
