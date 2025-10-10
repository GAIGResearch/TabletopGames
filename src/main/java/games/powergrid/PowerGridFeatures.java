package games.powergrid;

import java.util.List;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IStateFeatureJSON;
import core.interfaces.IStateFeatureVector;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.components.PowerGridCard;
import games.powergrid.components.PowerGridResourceMarket;

public class PowerGridFeatures implements IStateFeatureVector, IStateFeatureJSON {
	
	

	@Override
	public String getObservationJson(AbstractGameState gameState, int playerId) {
		// TODO Auto-generated method stub
		return null;
	}
	

	@Override
	public double[] doubleVector(AbstractGameState state, int playerId) {
	    // return the actual numbers without calling featureVector()
		System.out.println("GLOBAL VECTOR LENGTH"  + buildGlobalObservation(state,playerId).length);
	    return concatDoubleArrays(buildPlayersObservation(state, playerId), buildGlobalObservation(state,playerId)); // or your 11-feature self-only vector
	}
	public double[] buildPlayersObservation(AbstractGameState state, int agentPlayerID) {
	    PowerGridGameState pggs = (PowerGridGameState) state;
	    final int MAX_PLAYERS = 6, F = 11; // 8 stats + 3 plant numbers

	    double[] out = new double[F * MAX_PLAYERS]; // 66

	    int w = 0; // write pointer

	    // self
	    out[w+0] = pggs.getPlayersMoney(agentPlayerID);
	    out[w+1] = pggs.getFuel(agentPlayerID, Resource.COAL);
	    out[w+2] = pggs.getFuel(agentPlayerID, Resource.GAS);
	    out[w+3] = pggs.getFuel(agentPlayerID, Resource.OIL);
	    out[w+4] = pggs.getFuel(agentPlayerID, Resource.URANIUM);
	    out[w+5] = pggs.getCityCountByPlayer(agentPlayerID);
	    out[w+6] = pggs.getPlayerCapacity(agentPlayerID);
	    out[w+7] = pggs.getIncome(agentPlayerID);

	    Deck<PowerGridCard> hand = pggs.getOwnedPlantsByPlayer(agentPlayerID);
	    out[w+8]  = plantNo(hand, 0);
	    out[w+9]  = plantNo(hand, 1);
	    out[w+10] = plantNo(hand, 2);
	    w += F;

	    // the 5 others in seat order after the agent; zeros remain for empty seats
	    for (int offset = 1; offset < MAX_PLAYERS; offset++) {
	        int pid = (agentPlayerID + offset) % MAX_PLAYERS;
	        if (pid < pggs.getNPlayers()) {
	            out[w+0] = pggs.getPlayersMoney(pid);
	            out[w+1] = pggs.getFuel(pid, Resource.COAL);
	            out[w+2] = pggs.getFuel(pid, Resource.GAS);
	            out[w+3] = pggs.getFuel(pid, Resource.OIL);
	            out[w+4] = pggs.getFuel(pid, Resource.URANIUM);
	            out[w+5] = pggs.getCityCountByPlayer(pid);
	            out[w+6] = pggs.getPlayerCapacity(pid);
	            out[w+7] = pggs.getIncome(pid);

	            Deck<PowerGridCard> opp = pggs.getOwnedPlantsByPlayer(pid);
	            out[w+8]  = plantNo(opp, 0);
	            out[w+9]  = plantNo(opp, 1);
	            out[w+10] = plantNo(opp, 2);
	        }
	        w += F;
	    }

	    return out;
	}
	
	public double[] buildGlobalObservation(AbstractGameState state, int agentPlayerID) {
	    PowerGridGameState pggs = (PowerGridGameState) state;
	    PowerGridResourceMarket market = pggs.getResourceMarket();

	    double[] validRegions  = pggs.getOneHotRegions();                        
	    double[] cityslotid    = encodeSlotsMineOppEmpty(pggs.getCitySlotsById(), agentPlayerID); 
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


	    return concatDoubleArrays(
	        validRegions,
	        resourceMarket,       // safe even if market was null (we made [0,0,0,0])
	        turnOrder,
	        roundOrder,
	        citiesOwned,
	        currentFutureVector,
	        cityslotid
	    );
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


	
	public static double[] encodeOrderNormalized(List<Integer> order, int maxPlayers) {
	    double[] out = new double[maxPlayers];
	    int activeCount = 0;

	    for (int pid : order)
	        if (pid >= 0 && pid < maxPlayers)
	            activeCount++;

	    for (int i = 0; i < order.size(); i++) {
	        int pid = order.get(i);
	        if (pid >= 0 && pid < maxPlayers) {
	            // 1.0 = first, 0 = last among active players
	            out[pid] = activeCount <= 1 ? 1f : (float) (activeCount - i) / (activeCount - 1);
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
	
	
	//helper method that encodes the game map 
	public static double[] encodeSlotsMineOppEmpty(int[][] slots, int meId) {
	    int total = 0;
	    for (int[] row : slots) total += row.length;
	    int width = 3; // mine, opponent, empty
	    double[] out = new double[total * width];

	    int k = 0;
	    for (int[] row : slots) {
	        for (int owner : row) {
	            int base = k * width;
	            if (owner < 0) {
	                out[base + 2] = 0f;              // empty
	            } else if (owner == meId) {
	                out[base + 0] = 1f;              // mine
	            } else {
	                out[base + 1] = 1f;              // opponent
	            }
	            k++;
	        }
	    }
	    return out;
	}

	@Override
	public String[] names() {
		return new String[]{ "Player Money", "Resource:Coal","Resource:Gas","Resource:Oil","Resource:Uranium", "Generator", "Capacity", "Income","Plant1","Plant2","Plant3"};
	}  
    public int getObservationSpace() {
        return names().length;
    }
}
