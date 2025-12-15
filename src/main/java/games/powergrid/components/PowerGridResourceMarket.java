package games.powergrid.components;

import core.CoreConstants;
import core.components.Component;
import games.powergrid.PowerGridParameters;
import java.util.EnumMap;
import java.util.Map;

/**
 * Mutable model of the Power Grid resource market.
 *
 * <p>The market tracks, for each {@link PowerGridParameters.Resource}:
 * <ul>
 *   <li><b>avail</b> – cubes currently for sale on the market board,</li>
 *   <li><b>discard</b> – cubes off-board in the supply (spent fuel and setup stock).</li>
 * </ul>
 *
 * <p><b>Core behaviors</b>
 * <ul>
 *   <li>{@link #setUpMarket(int[])} initializes the market (fills the supply and
 *       moves the starting quantities onto the board).</li>
 *   <li>{@link #buy(PowerGridParameters.Resource, int)} removes cubes from the board
 *       after validating availability; {@link #costToBuy(PowerGridParameters.Resource, int)}
 *       computes the marginal cost using the official price curves
 *       (see {@code PowerGridParameters.*Price}).</li>
 *   <li>{@link #returnToDiscardPile(PowerGridParameters.Resource, int)} returns spent fuel
 *       to the off-board supply.</li>
 *   <li>{@link #replenishMarket(int[])} and {@link #refill(PowerGridParameters, int, int, boolean)}
 *       move cubes from the supply to the board based on step, player count, and map.</li>
 *   <li>{@link #snapshot()} provides a defensive copy of on-board availability for UI or logging.</li>
 * </ul>
 *
 * <p><b>Pricing model</b>: Prices increase as the market empties. The total cost for
 * purchasing {@code amount} cubes is computed by summing the appropriate tail of the
 * resource-specific price array (coal/gas/oil/uranium).
 *
 * @see PowerGridParameters.Resource
 * @see PowerGridParameters#coalPrice
 * @see PowerGridParameters#gasPrice
 * @see PowerGridParameters#oilPrice
 * @see PowerGridParameters#uraniumPrice
 */

public class PowerGridResourceMarket extends Component {
	
	private final EnumMap<PowerGridParameters.Resource, Integer> avail = new EnumMap<>(PowerGridParameters.Resource.class);
	private final EnumMap<PowerGridParameters.Resource, Integer> discard = new EnumMap<>(PowerGridParameters.Resource.class);
	
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
	    for (var e : avail.entrySet()) {
	        c.avail.put(e.getKey(), e.getValue());
	    }
	    for (var e : discard.entrySet()) {
	        c.discard.put(e.getKey(), e.getValue());
	    }
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
	
    //maps the changing of prices with how many are available for purchase
	private int[] priceCurveFor(PowerGridParameters.Resource r) {
	    return switch (r) {
	        case COAL    -> PowerGridParameters.coalPrice;
	        case GAS     -> PowerGridParameters.gasPrice;
	        case OIL     -> PowerGridParameters.oilPrice;
	        case URANIUM -> PowerGridParameters.uraniumPrice;
	    };
	}
	

	
	private int[][] pickTable(PowerGridParameters p, int step, int nPlayers, boolean europeMap) {
	    if (nPlayers <= 2) return europeMap ? p.resourceRefreshEU_2P : p.resourceRefreshNA_2P;
	    if (nPlayers == 3) return europeMap ? p.resourceRefreshEU_3P : p.resourceRefreshNA_3P;
	    if (nPlayers == 4) return europeMap ? p.resourceRefreshEU_4P : p.resourceRefreshNA_4P;
	    if (nPlayers == 5) return europeMap ? p.resourceRefreshEU_5P : p.resourceRefreshNA_5P;
	    return europeMap ? p.resourceRefreshEU_6P : p.resourceRefreshNA_6P;
	}

	public void refill(PowerGridParameters params, int step, int nPlayers, boolean europeMap) {
	    int[][] table = pickTable(params, step, nPlayers, europeMap);
	    PowerGridParameters.Resource[] R = PowerGridParameters.Resource.values();

	    // guard if your table is 0-based and step is 1..3
	    if (step < 1 || step >= table.length + 1)
	        throw new IllegalArgumentException("Invalid step: " + step);

	    for (int i = 0; i < R.length; i++) {
	        int add = table[step - 1][i];     // cubes to add for resource R[i]
	        replenishResource(R[i], add); // pulls from discard up to capacity
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
    
    public double[] flattenAvailableNormalized() {
        double[] out = new double[4];

        double maxCoal     = 27d;
        double maxGas      = 24d;
        double maxOil      = 20d;
        double maxUranium  = 12d;

        out[0] = avail.get(PowerGridParameters.Resource.COAL)    / maxCoal;
        out[1] = avail.get(PowerGridParameters.Resource.GAS)     / maxGas;
        out[2] = avail.get(PowerGridParameters.Resource.OIL)     / maxOil;
        out[3] = avail.get(PowerGridParameters.Resource.URANIUM) / maxUranium;

        return out;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PowerGridResourceMarket m)) return false;
        return this.avail.equals(m.avail) && this.discard.equals(m.discard);
    }

    @Override
    public int hashCode() {
        int h = 1;
        for (PowerGridParameters.Resource r : PowerGridParameters.Resource.values()) {
            h = 31 * h + Integer.hashCode(avail.getOrDefault(r, 0));
            h = 31 * h + Integer.hashCode(discard.getOrDefault(r, 0));
        }
        return h;
    }

    

}
