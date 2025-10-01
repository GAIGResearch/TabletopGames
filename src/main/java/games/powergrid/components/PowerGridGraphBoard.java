package games.powergrid.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import core.CoreConstants;
import core.components.Component;

public final class PowerGridGraphBoard extends Component {

    private final Map<Integer, PowerGridCity> citiesById;
    private final Map<Integer, List<Edge>> adj;
    public static final Map<Integer, Set<Integer>> REGION_ADJ_NA = Map.of(
    	    1, Set.of(2),      
    	    2, Set.of(1,3, 4),
    	    3, Set.of(2, 4, 7),
    	    4, Set.of(2, 3, 5, 6, 7),
    	    5, Set.of(6,4),
    	    6, Set.of(4, 5, 7),
    	    7, Set.of(3,4, 6)
    	);

    // inner static class for clarity
    public static final class Edge {
        public final int from;
        public final int to;
        public final int cost;

        public Edge(int from, int to, int cost) {
            this.from = from;
            this.to = to;
            this.cost = cost;
        }
    }

    public PowerGridGraphBoard(String name,Map<Integer, PowerGridCity> cities,List<Edge> edges) {
        super(CoreConstants.ComponentType.BOARD, name);
        this.citiesById = Map.copyOf(cities);
        Map<Integer, List<Edge>> tmp = new HashMap<>();
        for (Edge e : edges) {
            tmp.computeIfAbsent(e.from, k -> new ArrayList<>()).add(e);
            // add reverse edge since Power Grid is undirected
            tmp.computeIfAbsent(e.to, k -> new ArrayList<>()).add(new Edge(e.to, e.from, e.cost));
        }
        this.adj = tmp.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                                                      e -> List.copyOf(e.getValue())));
    }

    public PowerGridCity city(int id) { return citiesById.get(id); }
    public Collection<PowerGridCity> cities() { return citiesById.values(); }
    public List<Edge> edgesFrom(int cityId) { return adj.getOrDefault(cityId, List.of()); }

    @Override
    public PowerGridGraphBoard copy() { return this; } // immutable
    
    /*
     * TODO 
     * Incomplete Map of Europe
     */
    public static PowerGridGraphBoard europe() {
        Map<Integer, PowerGridCity> cities = new HashMap<>();
        cities.put(1, new PowerGridCity(1, "Aachen",   1,  false));
        cities.put(2, new PowerGridCity(2, "Bremen",   1,  false));
        cities.put(3, new PowerGridCity(3, "Berlin",   2,  true));  // deluxe/double
        cities.put(4, new PowerGridCity(4, "Dresden",  2, false));
        cities.put(5, new PowerGridCity(5, "Essen",    1,  false));
        cities.put(6, new PowerGridCity(6, "Freiburg", 3,  false));

        List<Edge> edges = List.of(
            new Edge(1, 2, 3), new Edge(2, 3, 4), new Edge(3, 4, 5),
            new Edge(4, 5, 6), new Edge(5, 6, 7), new Edge(6, 1, 8),
            new Edge(2, 5, 2), new Edge(3, 6, 9)
        );

        return new PowerGridGraphBoard("SixCityDemo", cities, edges);
    }
    /*
     * Complete Map of North America, creates a hash map mapping Cities to ID numbers then building a list of edges that contain all 
     * the edges between cities. Note that this is an undirected graph but that is taken care of in the PowerGridGraphBoard constructor to establish 
     * both directions. 
     */
    public static PowerGridGraphBoard northAmerica() {
    	//going to need logic to build the board such that it follows the rules on which areas are allowed 
    	 Map<Integer, PowerGridCity> cities = new HashMap<>();

    	// Region 1: North East
    	    cities.put(0,  new PowerGridCity(0, "Quebec",       1, false));
    	    cities.put(1,  new PowerGridCity(1, "Montreal",     1, false));
    	    cities.put(2,  new PowerGridCity(2, "Boston",       1, false));
    	    cities.put(3,  new PowerGridCity(3, "New_York",     1, true));
    	    cities.put(4,  new PowerGridCity(4, "Philadelphia", 1, false));
    	    cities.put(5,  new PowerGridCity(5, "Ottawa",       1, false));

    	    // Region 2: Mid Atlantic
    	    cities.put(6,  new PowerGridCity(6,  "Toronto",       2, false));
    	    cities.put(7,  new PowerGridCity(7,  "Detroit",       2, false));
    	    cities.put(8,  new PowerGridCity(8,  "Pittsburgh",    2, false));
    	    cities.put(9,  new PowerGridCity(9,  "Columbus",      2, false));
    	    cities.put(10, new PowerGridCity(10, "Washington",    2, false));
    	    cities.put(11, new PowerGridCity(11, "Charlotte",     2, false));
    	    cities.put(12, new PowerGridCity(12, "Nashville",     2, false));

    	    // Region 3: South US
    	    cities.put(13, new PowerGridCity(13, "Atlanta",          3, false));
    	    cities.put(14, new PowerGridCity(14, "Jacksonville",     3, false));
    	    cities.put(15, new PowerGridCity(15, "Miami",            3, false));
    	    cities.put(16, new PowerGridCity(16, "New_Orleans",      3, false));
    	    cities.put(17, new PowerGridCity(17, "Houston",          3, false));
    	    cities.put(18, new PowerGridCity(18, "DallasFort_Worth", 3, false));
    	    cities.put(19, new PowerGridCity(19, "San_Antonio",      3, false));

    	    // Region 4: Central US
    	    cities.put(20, new PowerGridCity(20, "Memphis",        4, false));
    	    cities.put(21, new PowerGridCity(21, "Oklahoma_City",  4, false));
    	    cities.put(22, new PowerGridCity(22, "StLouis",        4, false));
    	    cities.put(23, new PowerGridCity(23, "Indianapolis",   4, false));
    	    cities.put(24, new PowerGridCity(24, "Chicago",        4, false));
    	    cities.put(25, new PowerGridCity(25, "Milwaukee",      4, false));
    	    cities.put(26, new PowerGridCity(26, "Kansas_City",    4, false));

    	    // Region 5: Northwest
    	    cities.put(27, new PowerGridCity(27, "Minneapolis", 5, false));
    	    cities.put(28, new PowerGridCity(28, "Winnipeg",    5, false));
    	    cities.put(29, new PowerGridCity(29, "Regina",      5, false));
    	    cities.put(30, new PowerGridCity(30, "Edmonton",    5, false));
    	    cities.put(31, new PowerGridCity(31, "Calgary",     5, false));
    	    cities.put(32, new PowerGridCity(32, "Vancouver",   5, false));
    	    cities.put(33, new PowerGridCity(33, "Seattle",     5, false));

    	    // Region 6: West
    	    cities.put(34, new PowerGridCity(34, "Portland",       6, false));
    	    cities.put(35, new PowerGridCity(35, "Salt_Lake_City", 6, false));
    	    cities.put(36, new PowerGridCity(36, "Denver",         6, false));
    	    cities.put(37, new PowerGridCity(37, "Las_Vegas",      6, false));
    	    cities.put(38, new PowerGridCity(38, "San_Francisco",  6, false));
    	    cities.put(39, new PowerGridCity(39, "Los_Angeles",    6, false));
    	    cities.put(40, new PowerGridCity(40, "San_Diego",      6, false));

    	    // Region 7: Mexico
    	    cities.put(41, new PowerGridCity(41, "Albuquerque",  7, false));
    	    cities.put(42, new PowerGridCity(42, "Juarez",       7, false));
    	    cities.put(43, new PowerGridCity(43, "Chihuahua",    7, false));
    	    cities.put(44, new PowerGridCity(44, "Monterrey",    7, false));
    	    cities.put(45, new PowerGridCity(45, "Guadalajara",  7, false));
    	    cities.put(46, new PowerGridCity(46, "Mexico_City",  7, true));

    	    List<Edge> edges = List.of(
    	    	    new Edge(0, 1, 5),   // Quebec - Montreal
    	    	    new Edge(1, 2, 9),   // Montreal - Boston
    	    	    new Edge(1, 5, 3),   // Montreal - Ottawa
    	    	    new Edge(2, 3, 7),   // Boston - New York
    	    	    new Edge(3, 1, 12),  // New York - Montreal
    	    	    new Edge(3, 5, 13),  // New York - Ottawa
    	    	    new Edge(3, 6, 14),  // New York - Toronto
    	    	    new Edge(3, 8, 11),  // New York - Pittsburgh
    	    	    new Edge(3, 4, 5),   // New York - Philadelphia
    	    	    new Edge(4, 8, 9),   // Philadelphia - Pittsburgh
    	    	    new Edge(4, 10, 5),  // Philadelphia - Washington
    	    	    new Edge(10, 8, 7),  // Washington - Pittsburgh
    	    	    new Edge(10, 11, 12),// Washington - Charlotte
    	    	    new Edge(10, 9, 12), // Washington - Columbus
    	    	    new Edge(10, 12, 22),// Washington - Nashville
    	    	    new Edge(11, 12, 12),// Charlotte - Nashville
    	    	    new Edge(12, 9, 15), // Nashville - Columbus
    	    	    new Edge(9, 8, 6),   // Columbus - Pittsburgh
    	    	    new Edge(9, 7, 7),   // Columbus - Detroit
    	    	    new Edge(7, 6, 8),   // Detroit - Toronto
    	    	    new Edge(6, 8, 11),  // Toronto - Pittsburgh
    	    	    new Edge(6, 5, 8),   // Toronto - Ottawa

    	    	    new Edge(13, 11, 8), // Atlanta - Charlotte
    	    	    new Edge(13, 12, 8), // Atlanta - Nashville
    	    	    new Edge(13, 14, 10),// Atlanta - Jacksonville
    	    	    new Edge(13, 16, 16),// Atlanta - New Orleans
    	    	    new Edge(14, 11, 12),// Jacksonville - Charlotte
    	    	    new Edge(14, 16, 20),// Jacksonville - New Orleans
    	    	    new Edge(14, 15, 12),// Jacksonville - Miami
    	    	    new Edge(17, 16, 12),// New Orleans - Houston
    	    	    new Edge(16, 18, 16),// New Orleans - DallasFort_Worth
    	    	    new Edge(17, 18, 8), // Houston - DallasFort_Worth
    	    	    new Edge(17, 19, 6), // Houston - San Antonio
    	    	    new Edge(19, 18, 9), // San Antonio - DallasFort_Worth

    	    	    new Edge(20, 13, 12),// Memphis - Atlanta
    	    	    new Edge(20, 16, 14),// Memphis - New Orleans
    	    	    new Edge(20, 12, 7), // Memphis - Nashville
    	    	    new Edge(20, 22, 9), // Memphis - St Louis
    	    	    new Edge(20, 26, 14),// Memphis - Kansas City
    	    	    new Edge(20, 21, 15),// Memphis - Oklahoma City
    	    	    new Edge(20, 18, 15),// Memphis - DallasFort_Worth
    	    	    new Edge(22, 16, 9), // St Louis - Nashville
    	    	    new Edge(22, 23, 8), // St Louis - Indianapolis
    	    	    new Edge(22, 24, 9), // St Louis - Chicago
    	    	    new Edge(22, 26, 8), // St Louis - Kansas City
    	    	    new Edge(23, 12, 9), // Indianapolis - Nashville
    	    	    new Edge(23, 9, 7),  // Indianapolis - Columbus
    	    	    new Edge(23, 24, 6), // Indianapolis - Chicago
    	    	    new Edge(24, 9, 10), // Chicago - Columbus
    	    	    new Edge(24, 7, 10), // Chicago - Detroit
    	    	    new Edge(24, 25, 3), // Chicago - Milwaukee
    	    	    new Edge(24, 26, 16),// Chicago - Kansas City
    	    	    new Edge(26, 21, 10),// Kansas City - Oklahoma City
    	    	    new Edge(21, 18, 7), // Oklahoma City - DallasFort_Worth

    	    	    new Edge(27, 26, 14),// Minneapolis - Kansas City
    	    	    new Edge(27, 25, 10),// Minneapolis - Milwaukee
    	    	    new Edge(27, 28, 13),// Minneapolis - Winnipeg
    	    	    new Edge(28, 29, 11),// Winnipeg - Regina
    	    	    new Edge(29, 30, 15),// Regina - Edmonton
    	    	    new Edge(29, 31, 14),// Regina - Calgary
    	    	    new Edge(29, 32, 28),// Regina - Vancouver
    	    	    new Edge(30, 31, 5), // Edmonton - Calgary
    	    	    new Edge(31, 32, 16),// Calgary - Vancouver
    	    	    new Edge(32, 33, 4), // Vancouver - Seattle

    	    	    new Edge(36, 21, 17),// Denver - Oklahoma City
    	    	    new Edge(36, 26, 19),// Denver - Kansas City
    	    	    new Edge(36, 27, 23),// Denver - Minneapolis
    	    	    new Edge(36, 28, 26),// Denver - Winnipeg
    	    	    new Edge(36, 29, 24),// Denver - Regina
    	    	    new Edge(36, 35, 14),// Denver - Salt Lake City
    	    	    new Edge(36, 37, 25),// Denver - Las Vegas
    	    	    new Edge(35, 34, 22),// Salt Lake City - Portland
    	    	    new Edge(35, 38, 22),// Salt Lake City - San Francisco
    	    	    new Edge(34, 33, 5), // Portland - Seattle
    	    	    new Edge(34, 38, 20),// Portland - San Francisco
    	    	    new Edge(38, 37, 16),// San Francisco - Las Vegas
    	    	    new Edge(38, 39, 12),// San Francisco - Los Angeles
    	    	    new Edge(39, 37, 8), // Los Angeles - Las Vegas
    	    	    new Edge(39, 40, 3), // Los Angeles - San Diego

    	    	    new Edge(41, 21, 18),// Albuquerque - Oklahoma City
    	    	    new Edge(41, 36, 12),// Albuquerque - Denver
    	    	    new Edge(41, 37, 18),// Albuquerque - Las Vegas
    	    	    new Edge(41, 39, 25),// Albuquerque - Los Angeles
    	    	    new Edge(41, 42, 9), // Albuquerque - Juarez
    	    	    new Edge(42, 40, 22),// Juarez - San Diego
    	    	    new Edge(42, 18, 19),// Juarez - DallasFort_Worth
    	    	    new Edge(42, 19, 18),// Juarez - San Antonio
    	    	    new Edge(42, 43, 8), // Juarez - Chihuahua
    	    	    new Edge(43, 19, 16),// Chihuahua - San Antonio
    	    	    new Edge(43, 44, 14),// Chihuahua - Monterrey
    	    	    new Edge(43, 45, 23),// Chihuahua - Guadalajara
    	    	    new Edge(44, 19, 10),// Monterrey - San Antonio
    	    	    new Edge(44, 45, 16),// Monterrey - Guadalajara
    	    	    new Edge(44, 46, 17),// Monterrey - Mexico City
    	    	    new Edge(45, 46, 10) // Guadalajara - Mexico City
    	    	);


        return new PowerGridGraphBoard("NorthAmerica", cities, edges);
    }
    public int maxCityId() {
        // if the map is empty, return 0 (or throw an exception depending on your use case)
        return citiesById.keySet().stream()
                         .mapToInt(Integer::intValue)
                         .max()
                         .orElse(0);
    }
    
    public PowerGridGraphBoard filterRegions(Set<Integer> keepRegions) {
        Map<Integer, PowerGridCity> keptCities =
            this.citiesById.values().stream()
                .filter(c -> keepRegions.contains(c.getRegion()))
                .collect(Collectors.toMap(
                    c -> Integer.valueOf(c.getComponentID()),
                    c -> c,
                    (a, b) -> a,
                    HashMap::new
                ));

        List<Edge> keptEdges =
            this.adj.entrySet().stream()
                .flatMap(e -> e.getValue().stream())
                .filter(e -> keptCities.containsKey(e.from) && keptCities.containsKey(e.to))
                .filter(e -> e.from < e.to)
                .collect(Collectors.toList());

        return new PowerGridGraphBoard(this.getComponentName(), keptCities, keptEdges);
    }
    
    public PowerGridGraphBoard penalizeRegions(Set<Integer> keepRegions, int penaltyCost) {
        Map<Integer, PowerGridCity> sameCities = new HashMap<>(this.citiesById);

        List<Edge> penalizedEdges = this.adj.entrySet().stream()
            .flatMap(e -> e.getValue().stream())
            .filter(e -> e.from < e.to)
            .map(e -> {
                boolean bothActive =
                    keepRegions.contains(citiesById.get(e.from).getRegion()) &&
                    keepRegions.contains(citiesById.get(e.to).getRegion());
                int newCost = bothActive ? e.cost : penaltyCost;
                return new Edge(e.from, e.to, newCost);
            })
            .collect(Collectors.toList());

        return new PowerGridGraphBoard(this.getComponentName(), sameCities, penalizedEdges);
    }
    
    /**
     * Returns the IDs of all cities that belong to regions not in the given active set.
     *
     * @param activeRegions set of valid region IDs
     * @return set of invalid city IDs
     */
    public Set<Integer> invalidCities(Set<Integer> activeRegions) {
        return citiesById.values().stream()
            .filter(city -> !activeRegions.contains(city.getRegion()))
            .map(PowerGridCity::getComponentID)
            .collect(Collectors.toSet());
    }

    /**
     * Returns the IDs of all cities that belong to the given active regions.
     *
     * @param activeRegions set of valid region IDs
     * @return set of valid city IDs
     */
    public Set<Integer> validCities(Set<Integer> activeRegions) {
        return citiesById.values().stream()
            .filter(city -> activeRegions.contains(city.getRegion()))
            .map(PowerGridCity::getComponentID)
            .collect(Collectors.toSet());
    }
    
 // --- Add inside PowerGridGraphBoard ---

    /** Number of cities on this board. */
    public int numCities() {
        return citiesById.size();
    }

    /** True if this id exists on the map. */
    public boolean hasCity(int id) {
        return citiesById.containsKey(id);
    }
    
    public String cityName(int id) {
        PowerGridCity c = citiesById.get(id);
        if (c == null) throw new IllegalArgumentException("Unknown city id: " + id);
        return c.getComponentName(); // or c.getName() if your class has that
    }

   
    /** Cheapest cost from ANY source to target.
     *  Empty sources ⇒ 0 (first city rule).
     *  Throws if target is unreachable (which indicates a map/data bug). */
    public int shortestPathCostOrThrow(Set<Integer> sources, int target) {
        if (!hasCity(target)) throw new IllegalArgumentException("Unknown target city: " + target);
        if (sources == null || sources.isEmpty()) return 0;

        int maxId = maxCityId();
        int[] dist = new int[maxId + 1];
        java.util.Arrays.fill(dist, Integer.MAX_VALUE);

        java.util.PriorityQueue<int[]> pq =
            new java.util.PriorityQueue<>(java.util.Comparator.comparingInt(a -> a[1]));

        for (int s : sources) {
            if (!hasCity(s)) continue;       // ignore stale ids safely
            dist[s] = 0;
            pq.add(new int[]{s, 0});
        }

        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int u = cur[0], d = cur[1];
            if (d != dist[u]) continue;
            if (u == target) return d;

            for (Edge e : edgesFrom(u)) {
                int v = e.to, nd = d + e.cost;
                if (nd < dist[v]) { dist[v] = nd; pq.add(new int[]{v, nd}); }
            }
        }

        throw new IllegalStateException("Unreachable target city " + target + " from sources " + sources);
    }

    /** Multi-target variant: returns cost per target; throws if ANY target is unreachable.
     *  Empty sources ⇒ all targets cost 0 (first city rule). */
    public Map<Integer,Integer> shortestPathCosts(java.util.Set<Integer> targets,
                                                                   java.util.Set<Integer> sources) {
        java.util.Objects.requireNonNull(targets, "targets");
        if (targets.isEmpty()) return java.util.Map.of();

        // First city: all connection costs are 0
        if (sources == null || sources.isEmpty()) {
            java.util.Map<Integer,Integer> zero = new java.util.HashMap<>();
            for (int t : targets) {
                if (!hasCity(t)) throw new IllegalArgumentException("Unknown target city: " + t);
                zero.put(t, 0);
            }
            return java.util.Collections.unmodifiableMap(zero);
        }

        int maxId = maxCityId();
        int[] dist = new int[maxId + 1];
        java.util.Arrays.fill(dist, Integer.MAX_VALUE);

        java.util.PriorityQueue<int[]> pq =
            new java.util.PriorityQueue<>(java.util.Comparator.comparingInt(a -> a[1]));

        for (int s : sources) {
            if (!hasCity(s)) continue;
            dist[s] = 0;
            pq.add(new int[]{s, 0});
        }

        java.util.Set<Integer> remaining = new java.util.HashSet<>();
        for (int t : targets) {
            if (!hasCity(t)) throw new IllegalArgumentException("Unknown target city: " + t);
            remaining.add(t);
        }

        java.util.Map<Integer,Integer> result = new java.util.HashMap<>();

        while (!pq.isEmpty() && !remaining.isEmpty()) {
            int[] cur = pq.poll();
            int u = cur[0], d = cur[1];
            if (d != dist[u]) continue;

            if (remaining.remove(u)) result.put(u, d);
            for (Edge e : edgesFrom(u)) {
                int v = e.to, nd = d + e.cost;
                if (nd < dist[v]) { dist[v] = nd; pq.add(new int[]{v, nd}); }
            }
        }

        if (!remaining.isEmpty()) {
            throw new IllegalStateException("Unreachable cities: " + remaining + " from sources " + sources);
        }
        return java.util.Collections.unmodifiableMap(result);
    }



}
