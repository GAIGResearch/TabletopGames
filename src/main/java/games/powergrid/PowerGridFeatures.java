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

public class PowerGridFeatures implements IStateFeatureVector {
	
	


	@Override
	public double[] doubleVector(AbstractGameState state, int playerId) {
	    // return the actual numbers without calling featureVector()
		double [] array = concatDoubleArrays(buildPlayersObservation(state, playerId), buildGlobalObservation(state,playerId));
		if (hasValueGreaterThanOne(array)) {
		    System.out.println("Found a value > 1!");
		}
	    return concatDoubleArrays(buildPlayersObservation(state, playerId), buildGlobalObservation(state,playerId)); // or your 11-feature self-only vector
	}
	
	
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




	public double[] buildPlayersObservation(AbstractGameState state, int agentPlayerID) {
	    PowerGridGameState pggs = (PowerGridGameState) state;
	    PowerGridParameters params = (PowerGridParameters) state.getGameParameters();

	    final int MAX_PLAYERS = 6, F = 11;
	    final double MONEY_MAX = 500.0;                 
	    final double COAL_MAX = 9.0, GAS_MAX = 9.0, OIL_MAX = 9.0, UR_MAX = 6.0; //would be better if these corresponded to players actual capacity will experiment 
	    final double CAP_MAX  = PowerGridParameters.MAX_CAPACITY;
	    final double INC_MAX  = PowerGridParameters.MAX_INCOME;
	    final double CARD_MAX = PowerGridParameters.MAX_CARD;
	    final int nPlayers    = pggs.getNPlayers();
	    final double cityMax  = (params.citiesToTriggerEnd[nPlayers - 1]) + 2.0; // your prior logic

	    double[] out = new double[F * MAX_PLAYERS];
	    int w = 0;

	    // RL agent
	    out[w+0] = n(pggs.getPlayersMoney(agentPlayerID), MONEY_MAX);
	    out[w+1] = n(pggs.getFuel(agentPlayerID, PowerGridParameters.Resource.COAL), COAL_MAX);
	    out[w+2] = n(pggs.getFuel(agentPlayerID, PowerGridParameters.Resource.GAS),  GAS_MAX);
	    out[w+3] = n(pggs.getFuel(agentPlayerID, PowerGridParameters.Resource.OIL),  OIL_MAX);
	    out[w+4] = n(pggs.getFuel(agentPlayerID, PowerGridParameters.Resource.URANIUM), UR_MAX);
	    out[w+5] = n(pggs.getCityCountByPlayer(agentPlayerID), cityMax);
	    out[w+6] = n(pggs.getPlayerCapacity(agentPlayerID), CAP_MAX);
	    out[w+7] = n(pggs.getIncome(agentPlayerID),          INC_MAX);

	    Deck<PowerGridCard> hand = pggs.getOwnedPlantsByPlayer(agentPlayerID);
	    out[w+8]  = n(plantNo(hand, 0), CARD_MAX);
	    out[w+9]  = n(plantNo(hand, 1), CARD_MAX);
	    out[w+10] = n(plantNo(hand, 2), CARD_MAX);
	    w += F;

	    // Other Players normalized 
	    for (int offset = 1; offset < MAX_PLAYERS; offset++) {
	        int pid = (agentPlayerID + offset) % MAX_PLAYERS;
	        if (pid < nPlayers) {
	            out[w+0] = n(pggs.getPlayersMoney(pid), MONEY_MAX);
	            out[w+1] = n(pggs.getFuel(pid, PowerGridParameters.Resource.COAL), COAL_MAX);
	            out[w+2] = n(pggs.getFuel(pid, PowerGridParameters.Resource.GAS),  GAS_MAX);
	            out[w+3] = n(pggs.getFuel(pid, PowerGridParameters.Resource.OIL),  OIL_MAX);
	            out[w+4] = n(pggs.getFuel(pid, PowerGridParameters.Resource.URANIUM), UR_MAX);
	            out[w+5] = n(pggs.getCityCountByPlayer(pid), cityMax);
	            out[w+6] = n(pggs.getPlayerCapacity(pid), CAP_MAX);
	            out[w+7] = n(pggs.getIncome(pid),          INC_MAX);

	            Deck<PowerGridCard> opp = pggs.getOwnedPlantsByPlayer(pid);
	            out[w+8]  = n(plantNo(opp, 0), CARD_MAX);
	            out[w+9]  = n(plantNo(opp, 1), CARD_MAX);
	            out[w+10] = n(plantNo(opp, 2), CARD_MAX);
	        } else {
	            // leave zeros for empty seats
	        }
	        w += F;
	    }

	    return out;
	}

	
	public double[] buildGlobalObservation(AbstractGameState state, int agentPlayerID) {
	    PowerGridGameState pggs = (PowerGridGameState) state;
	    PowerGridResourceMarket market = pggs.getResourceMarket();

	    double[] validRegions  = pggs.getOneHotRegions();         
	    
	    List<double[]>  cityslotid    = encodeSlotsMineOppEmpty(pggs.getCitySlotsById(), agentPlayerID); 
	    List<double[]> graphAdjacency = pggs.getGameMap().getAdjacencyVector();
	    double[] mapVector = interleaveCitySlotsAndAdjacency(cityslotid,graphAdjacency);
	    double[] resourceMarket= (market != null) ? market.flattenAvailableNormalized() : new double[4];
	    double[] turnOrder     = encodeOrderNormalized(pggs.getTurnOrder(),      PowerGridParameters.MAXPLAYERS);
	    double [] roundOrder    = encodeOrderNormalized(pggs.getRoundOrder(),     PowerGridParameters.MAXPLAYERS);
	    double [] citiesOwned   = normalizePoweredCities(pggs.getPoweredCities(),
	                                                   pggs.getMaxCitiesOwned(),
	                                                   PowerGridParameters.MAXPLAYERS);
	   
	    // Current + Future markets (corrected: cm=four, fm=four; fixed indices)
	    double[] currentFutureVector = new double[8];
	    Deck<PowerGridCard> cm = pggs.getCurrentMarket();
	    Deck<PowerGridCard> fm = pggs.getFutureMarket();
	    for (int i = 0; i < 4; i++) {
	        currentFutureVector[i]     = plantNo(cm, i); 
	        currentFutureVector[i + 4] = plantNo(fm, i); 
	    }
	    if (hasValueGreaterThanOne(turnOrder)) {
		    System.out.println("Found VR value > 1!");
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

	
	public static double[] concatDoubleArrays(double[]... arrays) {
	    int totalLen = 0;
	    for (double[] arr : arrays)
	        if (arr != null)
	            totalLen += arr.length; // treat null as empty

	    double[] out = new double[totalLen];
	    int pos = 0;
	    for (double[] arr : arrays) {
	        if (arr == null) continue;
	        for (double v : arr)
	            out[pos++] = v;
	    }
	    return out;
	}
	
	public static double[] interleaveCitySlotsAndAdjacency(
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



	public static double[] encodeOrderNormalized(List<Integer> order, int maxPlayers) {
	    double[] out = new double[maxPlayers];

	    // Count only valid player IDs
	    int activeCount = 0;
	    for (int pid : order)
	        if (pid >= 0 && pid < maxPlayers) activeCount++;

	    if (activeCount == 0) return out;           // all zeros
	    if (activeCount == 1) {                      // single active gets 1.0
	        for (int pid : order)
	            if (pid >= 0 && pid < maxPlayers) { out[pid] = 1.0; break; }
	        return out;
	    }

	    // rank players among valid entries only
	    int rank = 0; // 0 for first, activeCount-1 for last
	    for (int pid : order) {
	        if (pid >= 0 && pid < maxPlayers) {
	            double v = 1.0 - ((double) rank / (activeCount - 1)); // 1.0 .. 0.0
	            // optional clamp to be extra safe
	            out[pid] = (v < 0 ? 0 : (v > 1 ? 1 : v));
	            rank++;
	        }
	    }
	    return out;
	}

	
	
	public static double[] normalizePoweredCities(int[] poweredCities, int maxCities, int maxPlayers) {
	    double[] out = new double[maxPlayers];
	    float denom = (maxCities > 0) ? (float) maxCities : 1f; // avoid div by zero

	    for (int i = 0; i < maxPlayers; i++) {
	        int cities = (i < poweredCities.length) ? poweredCities[i] : 0;
	        out[i] = Math.min(1f, cities / denom);
	    }
	    return out;
	}

	
	//helper method that either returns the plant number in teh players hand if it exists or 0
	private static double plantNo(Deck<PowerGridCard> deck, int idx) {
	    if (deck == null || idx < 0 || idx >= deck.getSize()) return 0d;
	    // normalize plant number roughly to [0,1]
	    return Math.min(1f, deck.getComponents().get(idx).getNumber() / 50d);
	}
	
	
	// Helper method that encodes the city slots as a list of vectors [mine, opponent, empty]
	public static List<double[]> encodeSlotsMineOppEmpty(int[][] slots, int meId) {
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
	    for (int p = 0; p < 6; p++) {
	        String prefix = (p == 0) ? "Agent " : "Player" + p + " ";
	        for (String var : playerTemplate) names.add(prefix + var);
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

	    // --- Fill remaining to reach total length 671 ---
	    while (names.size() < 671) {
	        names.add("F" + names.size());
	    }

	    return names.toArray(new String[0]);
	}
}
