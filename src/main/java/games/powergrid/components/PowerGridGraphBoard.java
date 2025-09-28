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
        cities.put(1, new PowerGridCity(1, "Aachen",   1, 100, 100, false));
        cities.put(2, new PowerGridCity(2, "Bremen",   1, 220, 100, false));
        cities.put(3, new PowerGridCity(3, "Berlin",   2, 340, 100, true));  // deluxe/double
        cities.put(4, new PowerGridCity(4, "Dresden",  2, 340, 220, false));
        cities.put(5, new PowerGridCity(5, "Essen",    1, 220, 220, false));
        cities.put(6, new PowerGridCity(6, "Freiburg", 3, 100, 220, false));

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
        //Region1 North East
        cities.put(1, new PowerGridCity(1, "Quebec",   1, 1708, 342, false));
        cities.put(2, new PowerGridCity(2, "Montreal",   1, 1678, 425, false));
        cities.put(3, new PowerGridCity(3, "Boston",   1, 1715, 518, false)); 
        cities.put(4, new PowerGridCity(4, "New_York",  1, 1562, 531, true)); //Top center row is coordiantes
        cities.put(5, new PowerGridCity(5, "Philadelphia",    1, 1543, 662, false));
        cities.put(6, new PowerGridCity(6, "Ottawa", 1, 1503, 419, false));
        
        //Region2 Mid Atlantic
        cities.put(7, new PowerGridCity(7, "Toronto",   2, 1355, 463, false));
        cities.put(8, new PowerGridCity(8, "Detroit",   2, 1291, 526, false));
        cities.put(9, new PowerGridCity(9, "Pittsburgh",   2, 1390, 594, false)); 
        cities.put(10, new PowerGridCity(10, "Columbus",  2, 1281, 655, false));
        cities.put(11, new PowerGridCity(11, "Washington",    2, 1429, 729, false));
        cities.put(12, new PowerGridCity(12, "Charlotte", 2, 1503, 419, false));
        cities.put(13, new PowerGridCity(13, "Nashville", 2, 1134, 729, false));
        
        //Region3 SOUTH US
        cities.put(14, new PowerGridCity(14, "Atlanta",   3, 1232, 896, false));
        cities.put(15, new PowerGridCity(15, "Jacksonville",   3, 1285, 1000, false));
        cities.put(16, new PowerGridCity(16, "Miami",   3, 1296, 1104, false)); 
        cities.put(17, new PowerGridCity(17, "New_Orleans",  3, 1062, 982, false));
        cities.put(18, new PowerGridCity(18, "Houston",    3, 881, 971, false));
        cities.put(19, new PowerGridCity(19, "DallasFort_Worth", 3, 820, 892, false));
        cities.put(20, new PowerGridCity(20, "San_Antonio", 3, 761, 1047, false));
        
        //Region4 Central US
        cities.put(21, new PowerGridCity(21, "Memphis",   4, 1017, 834, false));
        cities.put(22, new PowerGridCity(22, "Oklahoma_City",   4, 785, 800, false));
        cities.put(23, new PowerGridCity(23, "StLouis",   4, 978, 686, false)); 
        cities.put(24, new PowerGridCity(24, "Indianapolis",  4, 1116, 619, false));
        cities.put(25, new PowerGridCity(25, "Chicago",    4, 1074, 533, false));
        cities.put(26, new PowerGridCity(26, "Milwaukee", 4, 1052, 454, false));
        cities.put(27, new PowerGridCity(27, "Kansas City", 4, 815, 640, false));
        
      //Region5 Northwest 
        cities.put(28, new PowerGridCity(28, "Minneapolis",   5, 860, 425, false));
        cities.put(29, new PowerGridCity(29, "Winnipeg",   5, 784, 236, false));
        cities.put(30, new PowerGridCity(30, "Regina",   5, 532, 235, false)); 
        cities.put(31, new PowerGridCity(31, "Edmonton",  5, 334, 43, false));
        cities.put(32, new PowerGridCity(32, "Calgary",    5, 260, 134, false));
        cities.put(33, new PowerGridCity(33, "Vancouver", 5, 118, 223, false));
        cities.put(34, new PowerGridCity(34, "Seattle", 5, 126, 326, false));
        
        
        //Region6 West
        cities.put(35, new PowerGridCity(35, "Portland",   6, 120, 425, false));
        cities.put(36, new PowerGridCity(36, "Salt_Lake_City",   6, 394, 524, false));
        cities.put(37, new PowerGridCity(37, "Denver",   6, 532, 615, false)); 
        cities.put(38, new PowerGridCity(38, "Las_Vegas",  6, 318, 674, false));
        cities.put(39, new PowerGridCity(39, "San_Francisco",6, 100, 622, false));
        cities.put(40, new PowerGridCity(40, "Los_Angeles", 6, 160, 805, false));
        cities.put(41, new PowerGridCity(41, "San_Diego", 6, 239, 897, false));
        
        //Region7 Mexico 
        cities.put(42, new PowerGridCity(42, "Albuquerque",   7, 508, 829, false));
        cities.put(43, new PowerGridCity(43, "Juarez",   7, 506, 931, false));
        cities.put(44, new PowerGridCity(44, "Chihuahua",   7, 538, 1058, false)); 
        cities.put(45, new PowerGridCity(45, "Monterrey",  7, 734, 1135, false));
        cities.put(46, new PowerGridCity(46, "Guadalajara",7, 685, 1235, false));
        cities.put(47, new PowerGridCity(47, "Mexico_City", 7, 867, 1252, true));

        List<Edge> edges = List.of(
            new Edge(1, 2, 5), //Quebec- Montreal
            new Edge(2, 3, 9),//Montreal - Boston
            new Edge(2, 6, 3),//Montreal - Ottawa
            new Edge(3, 4, 7),//Boston - New York
            new Edge(4, 2, 12), //New York - Monreal
            new Edge(4, 6, 13), //New York - Ottawa
            new Edge(4, 7, 14), //New York - Toronto
            new Edge(4, 9, 11), //New York - Pittsburgh
            new Edge(4, 5, 5), //New York - Philadelphia
            new Edge(5, 9, 9), //Philadelphia - Pittsburgh
            new Edge(5, 11, 5), //Philadelphia - Washington
            new Edge(11, 9, 7), //Washington- Pittsburgh 
            new Edge(11, 12, 12), //Washington - Charlotte
            new Edge(11, 10, 12), //Washington - Columbus
            new Edge(11, 13, 22), //Washington - Nashville
            new Edge(12, 13, 12), //Charlotte - Nashville
            new Edge(13, 10, 15), //Nashville- Columbus
            new Edge(10, 9, 6), //Columbus- Pittsburgh       
            new Edge(10, 8, 7), //Columbus- Detroit
            new Edge(8, 7, 8), //Detroit- Toronto
            new Edge(7, 9, 11), //Toronto-Pittsburgh
            new Edge(7, 6, 8), //Toronto - Ottawa
            
            new Edge(14, 12, 8), //Atlanta- Charlotte       
            new Edge(14, 13, 8), //Atlanta- Nashville 
            new Edge(14, 15, 10), //Atlanta- Jacksonville
            new Edge(14, 17, 16), //Atlanta- New Orleans
            new Edge(15, 12, 12), //Jacksonville - Charlottte
            new Edge(15, 17, 20), //Jacksonville - New  Orleans
            new Edge(15, 16, 12),  //Jacksonville - Miami   
            new Edge(18, 17, 12), //New Orleans - Houston
            new Edge(17, 19, 16), //New Orleans - Dallas Fort WOrth 
            new Edge(18, 19, 8), //Houston - Dallas Fort WOrth 
            new Edge(18, 20, 6), //Houston - San Antonio
            new Edge(20, 19, 9), //San Antonio - Dallas Fort Worth
            
            new Edge(21, 14, 12), //Memphis- Atlanta     
            new Edge(21, 17, 14), //Memphis- New Orleans 
            new Edge(21, 13, 7), //Memphis- Nashville
            new Edge(21, 23, 9), //Memphis- St Louis
            new Edge(21, 27, 14), //Memphis- Kansas City
            new Edge(21, 22, 15), //Memphis- Oklahoma City
            new Edge(21, 19, 15),  //Memphis-Dallas For Worth        
            new Edge(23, 17, 9), //St Louis - Nashville
            new Edge(23, 24, 8), //St Louis - Indianapolis
            new Edge(23, 25, 9), //St Louis - Chicago
            new Edge(23, 27, 8), //St Louis - Kansas City          
            new Edge(24, 13, 9), //Indianapolis - Nashville
            new Edge(24, 10, 7), //Indianapolis - Columbus
            new Edge(24, 25, 6), //Indianapolis - Chicago           
            new Edge(25, 10, 10), //Chicago - Columbus
            new Edge(25, 8, 10), //Chicago - Detroit
            new Edge(25, 26, 3), //Chicago - Milwaukee
            new Edge(25, 27, 16), ///Chicago - Kansas City
            new Edge(27, 22, 10), /// Kansas City - Oklahoma City
            new Edge(22,19, 7), /// Oklahoma City-Dallas For Worth 
            
            
            new Edge(28, 27, 14), //Minneapolis - Kansas City    
            new Edge(28, 26, 10), //Minneapolis - Miwaukee  
            new Edge(28, 29, 13), //Minneapolis - Winnipeg           
            new Edge(29, 30,11), //Winnipeg - Regina      
            new Edge(30, 31, 15), // Regina - Edmonton  
            new Edge(30, 32, 14), // Regina - Calgary
            new Edge(30, 33, 28),  // Regina - Vancouver    
            new Edge(31, 32, 5), // Edmonton  - Calgary
            new Edge(32, 33, 16), // Calgary - Vancouver
            new Edge(33, 34, 4),  //  Vancouver - Seattle
            
            new Edge(37, 22, 17), //Denver - Oklahoma City
            new Edge(37, 27, 19), //Denver - Kansas City
            new Edge(37, 28, 23), //Denver - Minneapolis          
            new Edge(37, 29, 26), //Denver - Winnipeg       
            new Edge(37, 30, 24), //Denver - Regina  
            new Edge(37, 36, 14), //Denver - Salt Lake City
            new Edge(37, 38, 25),  //Denver - Las Vegas            
            new Edge(36, 35, 22), //Salt Lake City - Regina 
            new Edge(36, 39, 22), //Salt Lake City - San Francisco
            new Edge(35, 34, 5),  //Portland - Seattle
            new Edge(35, 39, 20),  //Portland - San Francisco      
            new Edge(39, 38, 16), //San Francisco - Las Vegas
            new Edge(39, 40, 12), //San Francisco - Los Angeles 
            new Edge(40, 38, 8),  //Los Angeles - Las Vegas
            new Edge(40, 41, 3),  //Los Angeles - San Diego 
            
            
            new Edge(42, 22, 18), //Albuquerque - Oklahoma City
            new Edge(42, 37, 12), //Albuquerque - Denver
            new Edge(42, 38, 18), //Albuquerque - Las Vegas        
            new Edge(42, 40, 25), //Albuquerque - Los Angeles      
            new Edge(42, 43, 9), //Albuquerque - Juarez 
            new Edge(43, 41, 22), //Juarez - San Diego
            new Edge(43, 19, 19),  //Juarez - Dallas Fort Worth        
            new Edge(43, 20, 18), //Juarez - San Antonio
            new Edge(43, 44, 8), //Juarez - Chihuahua 
            new Edge(44, 20, 16),  //Chihuahua - San Antonio
            new Edge(44, 45, 14),  //Chihuahua - Monterry  
            new Edge(44, 46, 23), //Chihuahua - Guadalajara         
            new Edge(45, 20, 10), //Monterrey - San Antonio 
            new Edge(45, 46, 16),  //Monterrey - Guadalajara
            new Edge(45, 47, 17),  //Monterrey - Mexico City    
            new Edge(46, 47, 10)  //Guadalajara - Mexico City
            
            
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
    

}
