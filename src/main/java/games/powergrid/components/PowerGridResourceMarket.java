package games.powergrid.components;

import core.CoreConstants;
import core.CoreConstants.ComponentType;
import core.components.Component;
import games.powergrid.PowerGridParameters;
import games.powergrid.PowerGridParameters.Step;

import java.util.EnumMap;
import java.util.Map;


public class PowerGridResourceMarket extends Component {
	
	private final EnumMap<PowerGridParameters.Resource, Integer> avail = new EnumMap<>(PowerGridParameters.Resource.class);
	private final EnumMap<PowerGridParameters.Resource, Integer> discard = new EnumMap<>(PowerGridParameters.Resource.class);
	public static final int[] discardStart = new int[]{27, 24, 20,12};
	
	public PowerGridResourceMarket() {
		super(CoreConstants.ComponentType.BOARD, "ResourceMarket");
		for (PowerGridParameters.Resource r : PowerGridParameters.Resource.values())//maps Resource type -> amount in teh discard start
	           discard.put(r, 0);
		for (PowerGridParameters.Resource r : PowerGridParameters.Resource.values())//maps Resource type -> zero
	           avail.put(r, 0);
	}
	
	public void setUpMarket(int [] startingResources){
		discard.put(PowerGridParameters.Resource.COAL, 27);
		discard.put(PowerGridParameters.Resource.GAS, 24);
		discard.put(PowerGridParameters.Resource.OIL, 20);
		discard.put(PowerGridParameters.Resource.URANIUM, 12);
		
		
		replenishMarket(startingResources);	
	}
	
	public void replenishMarket(int [] replenishAmount) {
		replenishResource(PowerGridParameters.Resource.COAL, replenishAmount[0]);
		replenishResource(PowerGridParameters.Resource.GAS, replenishAmount[1]);
		replenishResource(PowerGridParameters.Resource.OIL, replenishAmount[2]);
		replenishResource(PowerGridParameters.Resource.URANIUM, replenishAmount[3]);
	}
	
	
	private void replenishResource(PowerGridParameters.Resource resource, int amount) {
	    int inDiscard = discard.getOrDefault(resource, 0);
	    int toMove = Math.min(amount, inDiscard);
	    if (toMove <= 0) return; // nothing to move
	    discard.put(resource, inDiscard - toMove);
	    int inAvail = avail.getOrDefault(resource, 0);
	    avail.put(resource, inAvail + toMove);
	}
	
	public void returnToDiscardPile(PowerGridParameters.Resource resource, int amount) {
		int inDiscard = discard.getOrDefault(resource, 0);
		discard.put(resource, inDiscard + amount);
	}
	

    @Override
    public PowerGridResourceMarket copy() {
        PowerGridResourceMarket c = new PowerGridResourceMarket();
        for (var e : avail.entrySet()) c.avail.put(e.getKey(), e.getValue());
        return c;
    }
	
    public void buy(PowerGridParameters.Resource r, int amount) {
        int have = avail.get(r);
        if (amount < 0 || amount > have)
            throw new IllegalArgumentException("Invalid buy of " + amount + " " + r + " (have " + have + ")");
        avail.put(r, have - amount);
    }
	
    public int costToBuy(PowerGridParameters.Resource r, int amount) {
        int available = avail.get(r);
        if (amount < 0) throw new IllegalArgumentException("amount < 0");
        if (amount > available) return 100000;

        int[] prices = priceCurveFor(r);

        int len   = prices.length;
        int start = len - available;

        int cost = 0;
        for (int i = 0; i < amount; i++) cost += prices[start + i];
        return cost;
    }
	
	private int[] priceCurveFor(PowerGridParameters.Resource r) {
	    return switch (r) {
	        case COAL    -> PowerGridParameters.coalPrice;
	        case GAS     -> PowerGridParameters.gasPrice;
	        case OIL     -> PowerGridParameters.oilPrice;
	        case URANIUM -> PowerGridParameters.uraniumPrice;
	    };
	}
	
	private int stepIndex(Step s) {
	    return switch (s) {
	        case STEP1 -> 0;
	        case STEP2 -> 1;
	        case STEP3 -> 2;
	    };
	}
	
	private int[][] pickTable(PowerGridParameters p, Step step, int nPlayers, boolean europeMap) {
	    if (nPlayers <= 2) return europeMap ? p.resourceRefreshEU_2P : p.resourceRefreshNA_2P;
	    if (nPlayers == 3) return europeMap ? p.resourceRefreshEU_3P : p.resourceRefreshNA_3P;
	    if (nPlayers == 4) return europeMap ? p.resourceRefreshEU_4P : p.resourceRefreshNA_4P;
	    if (nPlayers == 5) return europeMap ? p.resourceRefreshEU_5P : p.resourceRefreshNA_5P;
	    /* nPlayers >= 6 */
	    return europeMap ? p.resourceRefreshEU_6P : p.resourceRefreshNA_6P;
	}
	private int capacityFor(PowerGridParameters.Resource r) {
	    return switch (r) {
	        case COAL -> 27;
	        case GAS -> 24;
	        case OIL -> 20;
	        case URANIUM -> 12;
	    };
	}
	public void refill(PowerGridParameters params, Step step, int nPlayers, boolean europeMap) {
	    int[][] table = pickTable(params, step, nPlayers, europeMap);
	    int row = stepIndex(step); // 0..2
	    PowerGridParameters.Resource[] R = PowerGridParameters.Resource.values(); // COAL,GAS,OIL,URANIUM 

	    for (int i = 0; i < R.length; i++) {
	        int add = table[row][i];                  // how many cubes to add of R[i]
	        int now = avail.get(R[i]);
	        int cap = capacityFor(R[i]);
	        int next = Math.min(cap, now + add);      // clamp to capacity 
	        avail.put(R[i], next);
	    }
	}
    public int getAvailable(PowerGridParameters.Resource r) { return avail.get(r); }
    public Map<PowerGridParameters.Resource, Integer> snapshot() { return new EnumMap<>(avail); }
    
    @Override
    public String toString() {
        return "RM{coal=" + avail.get(PowerGridParameters.Resource.COAL) +
               ", gas=" + avail.get(PowerGridParameters.Resource.GAS) +
               ", oil=" + avail.get(PowerGridParameters.Resource.OIL) +
               ", ur=" + avail.get(PowerGridParameters.Resource.URANIUM) + "}";
    }
    

}
