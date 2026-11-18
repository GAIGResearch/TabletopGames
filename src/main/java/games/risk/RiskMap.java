package games.risk;

import core.components.BoardNode;
import core.components.GraphBoard;
import core.properties.PropertyString;
import core.properties.PropertyStringArray;
import games.risk.components.RiskCard;
import games.risk.components.RiskMission;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.JSONUtils;

import java.util.*;

import static core.CoreConstants.nameHash;

/**
 * The board: its territories, continents and connections, loaded from a JSON file (RiskParameters.mapFile). The file
 * is a GraphBoard in the usual format, with each node's "continent" and card "symbol", plus a "continents" list
 * giving each continent's name and bonus. For Secret Mission, it has a "missions" list and "backupTerritories". See
 * data/risk/worldMap.json. Immutable, and shared by all states.
 */
public class RiskMap {

    public final String fileName;
    private final GraphBoard board;
    private final List<RiskContinent> continents = new ArrayList<>();
    private final List<RiskTerritory> territories = new ArrayList<>();
    private final Map<String, RiskTerritory> byName = new HashMap<>();
    private final List<List<RiskTerritory>> neighbours = new ArrayList<>();
    private final List<RiskMission> missions = new ArrayList<>();
    private final int backupTerritories;
    // an SVG drawing of the board with a path for each territory, or null (see svgFile)
    private final String svgFile;
    // the credit the drawing's licence asks for, shown with it, or null
    private final String svgCredit;

    public RiskMap(String fileName) {
        this.fileName = fileName;
        JSONObject json = JSONUtils.loadJSONFile(fileName);
        board = new GraphBoard();
        board.loadBoard(json);

        Map<String, RiskContinent> continentByName = new HashMap<>();
        for (Object o : (JSONArray) json.get("continents")) {
            JSONObject c = (JSONObject) o;
            RiskContinent continent = new RiskContinent(continents.size(), (String) c.get("name"),
                    ((Long) c.get("bonus")).intValue());
            continents.add(continent);
            continentByName.put(continent.name(), continent);
        }

        // the conquest and occupy Secret Mission cards, and the territories of the backup mission (optional)
        JSONArray missionList = (JSONArray) json.get("missions");
        Long backup = (Long) json.get("backupTerritories");
        if (missionList != null && backup == null)
            throw new IllegalArgumentException(fileName + ": a map with missions needs backupTerritories");
        backupTerritories = backup == null ? 0 : backup.intValue();
        svgFile = (String) json.get("svg");
        svgCredit = (String) json.get("svgCredit");
        if (missionList != null)
            for (Object o : missionList) {
                JSONObject m = (JSONObject) o;
                if (m.containsKey("continents")) {
                    List<RiskContinent> named = new ArrayList<>();
                    for (Object c : (JSONArray) m.get("continents")) {
                        RiskContinent continent = continentByName.get((String) c);
                        if (continent == null)
                            throw new IllegalArgumentException(fileName + ": a mission names unknown continent " + c);
                        named.add(continent);
                    }
                    missions.add(RiskMission.conquer(named, Boolean.TRUE.equals(m.get("anotherContinent"))));
                } else {
                    Long minArmies = (Long) m.get("armies");
                    missions.add(RiskMission.occupy(((Long) m.get("territories")).intValue(),
                            minArmies == null ? 1 : minArmies.intValue()));
                }
            }

        // territories in the order of the file's nodes
        List<BoardNode> nodes = new ArrayList<>();
        for (Object o : (JSONArray) json.get("nodes")) {
            String name = (String) ((JSONArray) ((JSONObject) o).get("name")).get(1);
            BoardNode node = board.getNodeByStringProperty(nameHash, name);
            String continentName = ((PropertyString) node.getProperty("continent")).value;
            RiskContinent continent = continentByName.get(continentName);
            if (continent == null)
                throw new IllegalArgumentException(fileName + ": " + name + " is in unknown continent "
                        + continentName);
            RiskCard.Symbol symbol = RiskCard.Symbol.valueOf(
                    ((PropertyString) node.getProperty("symbol")).value.toUpperCase());
            RiskTerritory territory = new RiskTerritory(territories.size(), name, continent, symbol);
            territories.add(territory);
            byName.put(name, territory);
            nodes.add(node);
        }

        for (BoardNode node : nodes) {
            // GraphBoard.loadBoard skips a neighbour it cannot find, so check the names here
            for (String n : ((PropertyStringArray) node.getProperty("neighbours")).getValues())
                if (!byName.containsKey(n))
                    throw new IllegalArgumentException(fileName + ": " + node.getComponentName()
                            + " has unknown neighbour " + n);
            List<RiskTerritory> list = new ArrayList<>();
            for (BoardNode n : node.getNeighbours().keySet())
                list.add(byName.get(n.getComponentName()));
            list.sort(Comparator.comparingInt(RiskTerritory::index));
            neighbours.add(Collections.unmodifiableList(list));
        }
    }

    /**
     * All the territories, in index order.
     */
    public List<RiskTerritory> territories() {
        return Collections.unmodifiableList(territories);
    }

    public int nTerritories() {
        return territories.size();
    }

    public RiskTerritory territory(String name) {
        RiskTerritory t = byName.get(name);
        if (t == null)
            throw new IllegalArgumentException("No territory " + name + " in " + fileName);
        return t;
    }

    public List<RiskContinent> continents() {
        return Collections.unmodifiableList(continents);
    }

    /**
     * The territories of the continent, in index order.
     */
    public List<RiskTerritory> territories(RiskContinent continent) {
        return territories.stream().filter(t -> t.continent().equals(continent)).toList();
    }

    /**
     * The territories adjacent to the given one (sharing a border, or joined by a dashed line), in index order.
     */
    public List<RiskTerritory> neighbours(RiskTerritory territory) {
        return neighbours.get(territory.index());
    }

    public boolean adjacent(RiskTerritory a, RiskTerritory b) {
        return neighbours.get(a.index()).contains(b);
    }

    /**
     * The conquest and occupy Secret Mission cards for this map (its "missions" list), in the order of the file.
     * The destroy missions are not on the map; they are made for each colour by RiskMission.destroy.
     */
    public List<RiskMission> missions() {
        return Collections.unmodifiableList(missions);
    }

    /**
     * The number of territories to occupy for a destroy mission that cannot be completed (RiskMission.isBackup).
     */
    public int backupTerritories() {
        return backupTerritories;
    }

    /**
     * The map file's "svg": an SVG drawing of the board, with a path for each territory whose id is its name in
     * lower case with underscores for spaces ("North Africa" is north_africa), for the GUI; or null if it has none.
     */
    public String svgFile() {
        return svgFile;
    }

    /**
     * The map file's "svgCredit": the attribution the SVG drawing's licence asks for (lines separated by \n), which
     * the GUI shows with the drawing; or null if it has none.
     */
    public String svgCredit() {
        return svgCredit;
    }

    /**
     * The board as a graph: one node per territory, named by the territory's name.
     */
    public GraphBoard getBoard() {
        return board;
    }
}
