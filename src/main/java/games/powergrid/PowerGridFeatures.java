package games.powergrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IStateFeatureJSON;
import core.interfaces.IStateFeatureVector;
import games.powergrid.components.PowerGridCard;
import games.powergrid.components.PowerGridResourceMarket;

/**
 * State feature extractor for the Power Grid game, providing both player-relative
 * and global observation vectors suitable for reinforcement learning agents.
 * <p>
 * This class implements {@link IStateFeatureVector} and produces a fixed-length
 * numerical feature representation by concatenating:
 * <ul>
 *     <li>A per-player observation block (rotated so the agent is always first)</li>
 *     <li>A global game-state observation containing regions, markets, turn order,
 *         city ownership, power plant markets, and map structure</li>
 * </ul>
 *
 * The resulting observation is normalized to [0,1], supports zero-padding for
 * player-agnostic training, and maintains a consistent feature size across maps
 * and player counts. Feature names may also be queried via {@link #names()} for
 * debugging, logging, or visualization. It is important to note that currently PyTAG
 * uses length of names to determine the observation input size. 
 *
 * Designed for use with RL frameworks such as SB3, PPO, or other learning agents
 * that require a vector-based representation of the game state.
 *
 */

public class PowerGridFeatures implements IStateFeatureVector {
	
	private int totalPlayers = 3;
	//if false doesn't zero load empty player seats makes observation size based on player amouunt
	//used for testing zero padding effectivness for RL 
	private boolean playerAgnostic = false; 


	@Override
	//concats the player observations and the global observation into a single observation vector
	public double[] doubleVector(AbstractGameState state, int playerId) {
	    // return the actual numbers without calling featureVector()
		if (state != null) {
            totalPlayers = ((PowerGridGameState) state).getNPlayers();
        }
	    return concatDoubleArrays(buildPlayersObservation(state, playerId,playerAgnostic), buildGlobalObservation(state,playerId)); 
	}
	
	//used for testing if the observation is generating a value greater than one not currently used because observation stable
	public static boolean hasValueGreaterThanOne(double[] arr) {
	    for (int i = 0; i < arr.length; i++) {
	        double v = arr[i];
	        if (v > 1.0) {
	            System.out.println("Value " + v + " at index " + i + " is greater than 1.0");
	            return true;
	        }
	    }
	    return false;
	}

    /**
     * Builds a normalized observation vector describing all players in the current Power Grid game state.
     * <p>
     * The observation is constructed as a flat double array, containing one fixed-size feature block
     * per player. Each block contains 11 normalized values representing:
     * <ol>
     *   <li>Money</li>
     *   <li>Coal</li>
     *   <li>Gas</li>
     *   <li>Oil</li>
     *   <li>Uranium</li>
     *   <li>Cities owned</li>
     *   <li>Total generation capacity</li>
     *   <li>Income level</li>
     *   <li>Plant 1 minimum resource requirement</li>
     *   <li>Plant 2 minimum resource requirement</li>
     *   <li>Plant 3 minimum resource requirement</li>
     * </ol>
     *
     * The values are normalized to [0, 1] using pre-defined maximum values. 
     *
     * <p><b>Player Ordering:</b><br>
     * Players are listed in order starting with the current agent (agentPlayerID) and then wrapping
     * around the turn order. For example, in a 4-player game, if agentPlayerID = 2, the order will be:
     * 2, 3, 0, 1.
     *
     * <p><b>Player Count Mode:</b><br>
     * If {@code playerCountAgnostic} is true, the vector always reserves space for 6 players.
     * Any unused player blocks are zero-padded. If false, only the active number of players are included this means that the enviornment evaluated must have players 
     * less than or equal to the number trained else it will thrown an error.
     *
     * @param state                the current {@link AbstractGameState}, expected to be a Power Grid game state
     * @param agentPlayerID        the player index (0-based) of the agent requesting the observation;
     *                             used to rotate player order
     * @param playerCountAgnostic  if true, always allocate space for the maximum number of players (6),
     *                             padding unused slots with zeros; if false, only include active players
     * @return a flat {@code double[]} observation array of size:
     *         <ul>
     *           <li>{@code 6 × 11 = 66} if playerCountAgnostic = true</li>
     *           <li>{@code (#players) × 11} otherwise</li>
     *         </ul>
     */


	private double[] buildPlayersObservation(AbstractGameState state, int agentPlayerID, boolean playerCountAgnostic) {
	    PowerGridGameState pggs = (PowerGridGameState) state;
	    PowerGridParameters params = (PowerGridParameters) state.getGameParameters();

	    final int MAX_PLAYERS = 6, F = 11;
	    final int PLAYERS     = pggs.getNPlayers();  

	    final double MONEY_MAX = 500.0;
	    final double COAL_MAX  = 9.0, GAS_MAX = 9.0, OIL_MAX = 9.0, UR_MAX = 6.0;
	    final double CAP_MAX   = PowerGridParameters.MAX_CAPACITY;
	    final double INC_MAX   = PowerGridParameters.MAX_INCOME;
	    final double CARD_MAX  = PowerGridParameters.MAX_CARD;
	    final double cityMax   = params.citiesToTriggerEnd[PLAYERS - 1] + 2.0;
	    
	    final int slots = playerCountAgnostic ? MAX_PLAYERS : PLAYERS;
	    double[] out = new double[F * slots];
	    	
	    int w = 0;

	    for (int b = 0; b < PLAYERS; b++) {
	        int pid = (agentPlayerID + b) % PLAYERS;

	        out[w + 0] = n(pggs.getPlayersMoney(pid), MONEY_MAX);
	        out[w + 1] = n(pggs.getFuel(pid, PowerGridParameters.Resource.COAL),     COAL_MAX);
	        out[w + 2] = n(pggs.getFuel(pid, PowerGridParameters.Resource.GAS),      GAS_MAX);
	        out[w + 3] = n(pggs.getFuel(pid, PowerGridParameters.Resource.OIL),      OIL_MAX);
	        out[w + 4] = n(pggs.getFuel(pid, PowerGridParameters.Resource.URANIUM),  UR_MAX);
	        out[w + 5] = n(pggs.getCityCountByPlayer(pid), cityMax);
	        out[w + 6] = n(pggs.getPlayerCapacity(pid),   CAP_MAX);
	        out[w + 7] = n(pggs.getIncome(pid),           INC_MAX);

	        Deck<PowerGridCard> deck = pggs.getOwnedPlantsByPlayer(pid);
	        out[w + 8]  = n(plantNo(deck, 0), CARD_MAX);
	        out[w + 9]  = n(plantNo(deck, 1), CARD_MAX);
	        out[w +10]  = n(plantNo(deck, 2), CARD_MAX);

	        w += F;
	    }

	    //Pad remaining blocks with zeros if we are doing player agnostic 
	    if (playerCountAgnostic && w < F * MAX_PLAYERS) {
	        Arrays.fill(out, w, F * MAX_PLAYERS, 0.0);
	    }
		
	    return out;
	}


	   /**
     * Builds a global,semi agent-centric observation vector for the current Power Grid game state.
     * <p>
     * This observation encodes:
     * <ul>
     *   <li><b>Regions:</b> One-hot encoding of the valid/active map regions.</li>
     *   <li><b>Resource market:</b> Normalized quantities of each resource type currently available
     *       in the {@link PowerGridResourceMarket}.</li>
     *   <li><b>Turn order:</b> Normalized ordering of players for the current turn sequence
     *       (e.g., auction turn order).</li>
     *   <li><b>Round order:</b> Normalized ordering of players for the overall round sequence.</li>
     *   <li><b>Cities owned:</b> Normalized number of powered cities per player, capped by the
     *       maximum cities owned in the current game.</li>
     *   <li><b>Plant markets:</b> Current and future power plant markets, encoded as the plant
     *       numbers of up to 4 cards in each market (missing entries represented as 0).</li>
     *   <li><b>Map structure:</b> City slot ownership and adjacency:
     *     <ul>
     *       <li>City slots encoded via {@code encodeSlotsMineOppEmpty}, typically as
     *           mine/opponent/empty indicators from the perspective of {@code agentPlayerID}.</li>
     *       <li>Graph adjacency encoded as a set of adjacency vectors from the game map.</li>
     *       <li>These are combined into a single flat vector by
     *           {@code interleaveCitySlotsAndAdjacency}.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * All components are concatenated in the following order:
     * {@code [validRegions, resourceMarket, turnOrder, roundOrder, citiesOwned,
     * currentFutureVector, mapVector]}.
     * The resulting vector is suitable as a global observation input to learning agents.
     *
     * @param state         the current {@link AbstractGameState}, expected to be a
     *                      {@link PowerGridGameState}
     * @param agentPlayerID the index (0-based) of the focal agent; used when encoding city slots
     *                      (mine vs. opponent vs. empty) and other agent-centric features
     * @return a flat {@code double[]} representing the global, normalized game state from the
     *         perspective of {@code agentPlayerID}; length should be fixed depending on Map, North America 
     *         has a different observation size than Europe. 
     */
	
	private double[] buildGlobalObservation(AbstractGameState state, int agentPlayerID) {
	    PowerGridGameState pggs = (PowerGridGameState) state;
	    PowerGridResourceMarket market = pggs.getResourceMarket();

	    double[] validRegions  = pggs.getOneHotRegions();         
	    //agent centric in order to simplify observation space treats all opponents as enemy rather than one hot encoding them for each space
	    List<double[]>  cityslotid  = encodeSlotsMineOppEmpty(pggs.getCitySlotsById(), agentPlayerID);  
	    List<double[]> graphAdjacency = pggs.getGameMap().getAdjacencyVector();
	    double[] mapVector = interleaveCitySlotsAndAdjacency(cityslotid,graphAdjacency);
	    double[] resourceMarket= (market != null) ? market.flattenAvailableNormalized() : new double[4];
	    double[] turnOrder = encodeOrderNormalized(pggs.getTurnOrder(),PowerGridParameters.MAXPLAYERS);
	    double [] roundOrder = encodeOrderNormalized(pggs.getRoundOrder(),PowerGridParameters.MAXPLAYERS);
	    double [] citiesOwned = normalizePoweredCities(pggs.getPoweredCities(),pggs.getMaxCitiesOwned(),PowerGridParameters.MAXPLAYERS);
	   

	    double[] currentFutureVector = new double[8];
	    Deck<PowerGridCard> cm = pggs.getCurrentMarket();
	    Deck<PowerGridCard> fm = pggs.getFutureMarket();
	    for (int i = 0; i < 4; i++) {
	        currentFutureVector[i]     = plantNo(cm, i); 
	        currentFutureVector[i + 4] = plantNo(fm, i); 
	    }


	    return concatDoubleArrays(
	        validRegions,
	        resourceMarket,       
	        turnOrder,
	        roundOrder,
	        citiesOwned,
	        currentFutureVector,
	        mapVector 
	    );
	}

	private static double clamp01(double x) {
	    return x < 0 ? 0 : (x > 1 ? 1 : x);
	}

	private static double n(double v, double denom) {
	    return denom <= 0 ? 0 : clamp01(v / denom);
	}

	
	//concats two double arrays 
	public double[] concatDoubleArrays(double[]... arrays) {
	    int totalLen = 0;
	    for (double[] arr : arrays)
	        if (arr != null)
	            totalLen += arr.length; 

	    double[] out = new double[totalLen];
	    int pos = 0;
	    for (double[] arr : arrays) {
	        if (arr == null) continue;
	        for (double v : arr)
	            out[pos++] = v;
	    }
	    return out;
	}
	
    /**
     * Interleaves city slot ownership encodings with city adjacency encodings.
     * <p>
     * Given two parallel lists—one dynamic encoding per-city slot state  (e.g., mine / opponent / empty)
     * and one static encoding per-city adjacency (graph connections)—this method combines them into
     * a single flat vector by alternating their entries in city order:
     * <pre>
     * [city0_slots, city0_adj, city1_slots, city1_adj, ...]
     * </pre>
     *
     * This observation will have all cities even ones not in play. Cities not in play will have the first part the per city slot state  be 0,0,1 for the duration 
     * of the episode. Having the whole board available allows every possible map to be ran with out changing the size of the observation. 
     *
     * @param cityslotidByCity   a list of per-city slot/status feature arrays
     * @param adjacencyByCity    a list of per-city adjacency feature arrays
     * @return a flattened double array interleaving slot vectors and adjacency vectors
     *         for each city in order
     */

	
	private double[] interleaveCitySlotsAndAdjacency(
	        List<double[]> cityslotidByCity,
	        List<double[]> adjacencyByCity) {

	    int n = Math.min(cityslotidByCity.size(), adjacencyByCity.size());

	    // compute total length first
	    int totalLen = 0;
	    for (int i = 0; i < n; i++) {
	        totalLen += cityslotidByCity.get(i).length + adjacencyByCity.get(i).length;
	    }

	    // build the output array
	    double[] out = new double[totalLen];
	    int pos = 0;

	    for (int i = 0; i < n; i++) {
	        double[] a = cityslotidByCity.get(i);
	        double[] b = adjacencyByCity.get(i);

	        System.arraycopy(a, 0, out, pos, a.length);
	        pos += a.length;

	        System.arraycopy(b, 0, out, pos, b.length);
	        pos += b.length;
	    }

	    return out;
	}



	/**
	 * Encodes valid player turn order as normalized rank values.
	 * <p>
	 * Assumes that {@code order} contains every player ID from 0 to {@code maxPlayers-1},
	 * exactly once. The first player in {@code order} is assigned 1.0, the last is
	 * assigned 0.0, and intermediate positions are linearly spaced between.
	 *
	 * @param order a permutation of all player IDs [0 .. maxPlayers-1]
	 * @param maxPlayers total number of players
	 * @return a double array of length {@code maxPlayers} where index equals the player ID
	 */
	private double[] encodeOrderNormalized(List<Integer> order, int maxPlayers) {
	    double[] out = new double[maxPlayers];
	    final int last = maxPlayers - 1;

	    for (int rank = 0; rank < maxPlayers; rank++) {
	        int pid = order.get(rank);
	        out[pid] = 1.0 - (double) rank / last;   // 1.0 .. 0.0
	    }
	    return out;
	}

	//helper method that shows how many of each players cities are actually being powered 
	private double[] normalizePoweredCities(int[] poweredCities, int maxCities, int maxPlayers) {
	    double[] out = new double[maxPlayers];
	    float denom = (maxCities > 0) ? (float) maxCities : 1f; // avoid div by zero

	    for (int i = 0; i < maxPlayers; i++) {
	        int cities = (i < poweredCities.length) ? poweredCities[i] : 0;
	        out[i] = Math.min(1f, cities / denom);
	    }
	    return out;
	}

	
	//helper method that either returns the plant number in teh players hand if it exists or 0
	private double plantNo(Deck<PowerGridCard> deck, int idx) {
	    if (deck == null || idx < 0 || idx >= deck.getSize()) return 0d;
	    // normalize plant number roughly to [0,1]
	    return Math.min(1f, deck.getComponents().get(idx).getNumber() / 50d);
	}
	
	
	// helper method that encodes the city slots as a list of vectors [mine, opponent, empty]
	private List<double[]> encodeSlotsMineOppEmpty(int[][] slots, int meId) {
	    List<double[]> encoded = new ArrayList<>();

	    for (int[] row : slots) {
	        for (int owner : row) {
	            double[] vec = new double[3]; // [mine, opponent, empty]
	            if (owner < 0) {
	                vec[2] = 1.0;  // empty
	            } else if (owner == meId) {
	                vec[0] = 1.0;  // mine
	            } else {
	                vec[1] = 1.0;  // opponent
	            }
	            encoded.add(vec);
	        }
	    }

	    return encoded;
	}


	@Override
	public String[] names() {
	    List<String> names = new ArrayList<>();

	    // --- Player blocks ---
	    String[] playerTemplate = {
	        "Money", "Resource:Coal", "Resource:Gas", "Resource:Oil", "Resource:Uranium",
	        "Generator", "Capacity", "Income", "Plant1", "Plant2", "Plant3"
	    };

	    final int players = playerAgnostic ? 6 : totalPlayers;

	    for (int p = 0; p < players; p++) {
	        String prefix = (p == 0) ? "Agent " : "Player" + p + " ";
	        for (String var : playerTemplate) {
	            names.add(prefix + var);
	        }
	    }

	    // --- Power Grid state variables ---
	    names.addAll(Arrays.asList(
	        // Regions
	        "Region1", "Region2", "Region3", "Region4", "Region5", "Region6", "Region7",
	        // Resource Market
	        "Coal_Amount", "Gas_Amount", "Oil_Amount", "Uranium_Amount",
	        // Turn Order
	        "TO_First", "TO_Second", "TO_Third", "TO_Fourth", "TO_Fifth", "TO_Sixth",
	        // Round Order
	        "RO_First", "RO_Second", "RO_Third", "RO_Fourth", "RO_Fifth", "RO_Sixth",
	        // Player city counts
	        "Player0_Cities_Owned", "Player1_Cities_Owned", "Player2_Cities_Owned",
	        "Player3_Cities_Owned", "Player4_Cities_Owned", "Player5_Cities_Owned",
	        // Current Market
	        "Current_Market1", "Current_Market2", "Current_Market3", "Current_Market4",
	        // Future Market
	        "Future_Market1", "Future_Market2", "Future_Market3", "Future_Market4"
	    ));

	    // For 6 players -> 671, for fewer players -> 671 - (6 - players)*11
	    final int targetSize = 671 - (6 - players) * 11;

	    while (names.size() < targetSize) {
	        names.add("F" + names.size());
	    }

	    return names.toArray(new String[0]);
	}

}
