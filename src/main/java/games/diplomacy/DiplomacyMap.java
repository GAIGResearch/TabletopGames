package games.diplomacy;

import core.components.BoardNode;
import core.components.GraphBoard;
import core.properties.Property;
import core.properties.PropertyBoolean;
import core.properties.PropertyString;
import core.properties.PropertyStringArray;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.JSONUtils;

import java.util.*;

import static core.CoreConstants.nameHash;

/**
 * The board: its provinces, which of them are supply centres and whose home centres, the moves armies and fleets
 * can make, and the powers with their starting units. Loaded from a JSON file (DiplomacyParameters.mapFile) in the
 * GraphBoard format, whose "adjacent" lists give every province sharing a border. Each node also has a "type"
 * (Land, Coast or Sea), optional "supplyCentre" and "home", and the fleet moves: a "fleet" list, or for a province
 * with separate coasts, its "coasts" and a "fleet/&lt;coast&gt;" list for each. A fleet move to such a province names
 * the coast ("Spa/nc"). The file also gives the "startYear", the "victoryCentres" needed to win, and the "powers"
 * in player order, each with its starting "units" ("A Vie", "F StP/sc"). A "home" on a province that is not a supply
 * centre marks the rest of a power's home country, which it starts the game controlling. For the GUI, the file may give
 * the map's "image", and its "regions" image, whose red channel is the index + 1 of the province at each pixel (0 for
 * none); each node's "position" (and "position/&lt;coast&gt;") is then where a unit there is drawn on the image. See
 * data/diplomacy/standardMap.json.
 * Immutable, and shared by all states.
 */
public class DiplomacyMap {

    public final String fileName;
    private final GraphBoard board;
    private final List<DiplomacyProvince> provinces = new ArrayList<>();
    private final Map<String, DiplomacyProvince> byName = new HashMap<>();
    private final List<List<DiplomacyProvince>> adjacent = new ArrayList<>();
    private final Map<DiplomacyLocation, List<DiplomacyLocation>> fleetMoves = new HashMap<>();
    private final List<String> powers = new ArrayList<>();
    private final List<Map<DiplomacyLocation, DiplomacyUnit.Type>> startingUnits = new ArrayList<>();
    private final int startYear;
    private final int victoryCentres;
    private final String imageFile, regionsFile;

    public DiplomacyMap(String fileName) {
        this.fileName = fileName;
        JSONObject json = JSONUtils.loadJSONFile(fileName);
        board = new GraphBoard();
        board.loadBoard(json);
        startYear = ((Long) json.get("startYear")).intValue();
        victoryCentres = ((Long) json.get("victoryCentres")).intValue();
        imageFile = (String) json.get("image");
        regionsFile = (String) json.get("regions");

        JSONArray powerList = (JSONArray) json.get("powers");
        for (Object o : powerList)
            powers.add((String) ((JSONObject) o).get("name"));

        // provinces in the order of the file's nodes
        List<BoardNode> nodes = new ArrayList<>();
        for (Object o : (JSONArray) json.get("nodes")) {
            String name = (String) ((JSONArray) ((JSONObject) o).get("name")).get(1);
            BoardNode node = board.getNodeByStringProperty(nameHash, name);
            DiplomacyProvince.Type type = DiplomacyProvince.Type.valueOf(
                    ((PropertyString) node.getProperty("type")).value.toUpperCase());
            Property sc = node.getProperty("supplyCentre");
            boolean supplyCentre = sc != null && ((PropertyBoolean) sc).value;
            Property homeProperty = node.getProperty("home");
            int home = -1;
            if (homeProperty != null) {
                home = powers.indexOf(((PropertyString) homeProperty).value);
                if (home < 0)
                    throw new IllegalArgumentException(fileName + ": " + name + " is home to an unknown power");
            }
            Property coastProperty = node.getProperty("coasts");
            List<String> coasts = coastProperty == null ? List.of()
                    : List.of(((PropertyStringArray) coastProperty).getValues());
            DiplomacyProvince province = new DiplomacyProvince(provinces.size(), name,
                    ((PropertyString) node.getProperty("fullName")).value, type, supplyCentre, home, coasts);
            provinces.add(province);
            byName.put(name, province);
            nodes.add(node);
        }

        for (int i = 0; i < nodes.size(); i++) {
            BoardNode node = nodes.get(i);
            DiplomacyProvince province = provinces.get(i);
            // GraphBoard.loadBoard skips a neighbour it cannot find, so check the names here
            for (String n : ((PropertyStringArray) node.getProperty("adjacent")).getValues())
                if (!byName.containsKey(n))
                    throw new IllegalArgumentException(fileName + ": " + province + " has unknown neighbour " + n);
            List<DiplomacyProvince> list = new ArrayList<>();
            for (BoardNode n : node.getNeighbours().keySet())
                list.add(byName.get(n.getComponentName()));
            list.sort(Comparator.comparingInt(DiplomacyProvince::index));
            adjacent.add(Collections.unmodifiableList(list));

            if (province.hasCoasts()) {
                for (String coast : province.coasts())
                    fleetMoves.put(new DiplomacyLocation(province, coast),
                            locations((PropertyStringArray) node.getProperty("fleet/" + coast)));
            } else if (province.type() != DiplomacyProvince.Type.LAND) {
                fleetMoves.put(new DiplomacyLocation(province),
                        locations((PropertyStringArray) node.getProperty("fleet")));
            }
        }

        for (Object o : powerList) {
            Map<DiplomacyLocation, DiplomacyUnit.Type> units = new LinkedHashMap<>();
            for (Object u : (JSONArray) ((JSONObject) o).get("units")) {
                String[] parts = ((String) u).split(" ");
                units.put(location(parts[1]), parts[0].equals("F") ? DiplomacyUnit.Type.FLEET : DiplomacyUnit.Type.ARMY);
            }
            startingUnits.add(Collections.unmodifiableMap(units));
        }
    }

    private List<DiplomacyLocation> locations(PropertyStringArray names) {
        List<DiplomacyLocation> list = new ArrayList<>();
        for (String n : names.getValues())
            list.add(location(n));
        return Collections.unmodifiableList(list);
    }

    /**
     * All the provinces, in index order.
     */
    public List<DiplomacyProvince> provinces() {
        return Collections.unmodifiableList(provinces);
    }

    public int nProvinces() {
        return provinces.size();
    }

    public DiplomacyProvince province(String name) {
        DiplomacyProvince p = byName.get(name);
        if (p == null)
            throw new IllegalArgumentException("No province " + name + " in " + fileName);
        return p;
    }

    /**
     * The location named by a province and optional coast, as in the map file: "Par", "Spa/nc".
     */
    public DiplomacyLocation location(String name) {
        String[] parts = name.split("/");
        DiplomacyProvince province = province(parts[0]);
        String coast = parts.length > 1 ? parts[1] : "";
        if (!coast.isEmpty() && !province.coasts().contains(coast))
            throw new IllegalArgumentException(province + " has no coast " + coast + " in " + fileName);
        return new DiplomacyLocation(province, coast);
    }

    /**
     * The provinces sharing a border with the given one (by land or sea), in index order.
     */
    public List<DiplomacyProvince> adjacent(DiplomacyProvince province) {
        return adjacent.get(province.index());
    }

    /**
     * The provinces an army in the given province can move to.
     */
    public List<DiplomacyProvince> armyMoves(DiplomacyProvince province) {
        if (province.type() == DiplomacyProvince.Type.SEA)
            return List.of();
        return adjacent(province).stream().filter(p -> p.type() != DiplomacyProvince.Type.SEA).toList();
    }

    /**
     * The locations a fleet at the given location can move to (along a coast or across water); empty for a land
     * province, or for a province with separate coasts when no coast is given.
     */
    public List<DiplomacyLocation> fleetMoves(DiplomacyLocation location) {
        return fleetMoves.getOrDefault(location, List.of());
    }

    public List<String> powers() {
        return Collections.unmodifiableList(powers);
    }

    public int nPowers() {
        return powers.size();
    }

    /**
     * The units the power starts with, by location, in the order of the file.
     */
    public Map<DiplomacyLocation, DiplomacyUnit.Type> startingUnits(int power) {
        return startingUnits.get(power);
    }

    /**
     * The home supply centres of the power, in index order.
     */
    public List<DiplomacyProvince> homeCentres(int power) {
        return provinces.stream().filter(p -> p.supplyCentre() && p.home() == power).toList();
    }

    public List<DiplomacyProvince> supplyCentres() {
        return provinces.stream().filter(DiplomacyProvince::supplyCentre).toList();
    }

    public int startYear() {
        return startYear;
    }

    /**
     * The supply centres a power must control after a Fall turn to win.
     */
    public int victoryCentres() {
        return victoryCentres;
    }

    /**
     * The file of the map's image, or null if it has none.
     */
    public String imageFile() {
        return imageFile;
    }

    /**
     * The file of the image locating each province on the map image (see the class comment), or null if none.
     */
    public String regionsFile() {
        return regionsFile;
    }

    /**
     * The board as a graph: one node per province, named by its abbreviation, joined to every adjacent province.
     */
    public GraphBoard getBoard() {
        return board;
    }
}
