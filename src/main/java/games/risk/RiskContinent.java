package games.risk;

/**
 * A continent of the map, with the armies a player receives each turn for holding all of it. Made when the map is
 * loaded (RiskMap); index is its position in RiskMap.continents().
 */
public record RiskContinent(int index, String name, int bonus) {

    @Override
    public String toString() {
        return name;
    }
}
