package games.powergrid;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import core.AbstractParameters;
import games.powergrid.components.PowerGridCard;

public class PowerGridParameters extends AbstractParameters{
	public static final String CARD_ASSET_PATH = "data/powergrid";
	public enum Resource { COAL,  GAS, OIL, URANIUM}
	public enum PlantType { COAL, GAS, OIL, URANIUM, HYBRID, GREEN }

    public enum Step { STEP1, STEP2, STEP3 }
    
    public int maxRounds = 100;
    public int startingMoney = 50;
    
    public int[] citySlotPrices = new int[]{10, 15, 20};
    public int[] startinResources = new int[]{23, 18, 14,2};
    
    // Step 2 Trigger
    public int [] step2Trigger = new int [] {0,0,7,7,7,6};
    
    
    // end-game trigger 
    public  int [] citiesToTriggerEnd = new int [] {0,0,17,17,15,14};

    
    //Resource Refresh Rates 
    //Coal,Gas,Oil,Uranium 
    public int[][] resourceRefreshNA_2P = new int[][] {{2,2,3,2},{5,2,1,1},{2,4,3,2}};
    public int[][] resourceRefreshNA_3P = new int[][] {{2,2,3,2},{5,2,1,1},{2,4,3,2}};
    public int[][] resourceRefreshNA_4P = new int[][] {{3,3,3,2},{6,3,2,1},{3,5,4,2}};
    public int[][] resourceRefreshNA_5P = new int[][] {{4,3,4,3},{7,3,2,2},{4,6,5,3}};
    public int[][] resourceRefreshNA_6P = new int[][] {{5,4,5,3},{8,4,3,2},{5,7,6,4}};
    
    public int[][] resourceRefreshEU_2P = new int[][] {{2,2,2,1},{6,3,2,1},{2,5,3,2}};
    public int[][] resourceRefreshEU_3P = new int[][] {{2,2,2,1},{6,3,2,1},{2,5,3,2}};
    public int[][] resourceRefreshEU_4P = new int[][] {{3,3,3,1},{7,4,3,2},{4,5,4,2}};
    public int[][] resourceRefreshEU_5P = new int[][] {{3,3,4,2},{8,5,3,3},{4,7,5,3}};
    public int[][] resourceRefreshEU_6P = new int[][] {{5,4,4,2},{10,6,5,3},{5,8,6,4}};
    
    public static final int[] coalPrice = new int[] {1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,5,5,5,6,6,6,7,7,8,8,9,9};

    public static final int[] gasPrice = new int[] {1,1,1,2,2,2,3,3,3,4,4,4,5,5,5,6,6,6,7,7,7,8,8,8};

    public static final int[] oilPrice = new int[] {1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,9,9};

    public static final int[] uraniumPrice = new int[] {1,2,3,4,5,6,7,7,8,8,9,9};

    public static final int[] INCOME_TRACK = {10,22,33,44,54,64,73,82,90,98,105,112,118,124,129,134,138,42,145,148,150};
    
    public List<PowerGridCard> plantsIncludedInGame = List.of(
    		//NOTE RENEWABLES RESOURCE COAL BUT COST NONE
    		//initial cards
    	    PowerGridCard.plant(3, 1, Map.of(Resource.COAL, 2)),
    	    PowerGridCard.plant(4, 1, Map.of(Resource.COAL, 2)),
    	    PowerGridCard.plant(5, 1, Map.of(Resource.GAS, 2)),
    	    PowerGridCard.plant(6, 1, Map.of(Resource.OIL, 1)),
    	    PowerGridCard.plant(7, 1, Map.of(Resource.COAL, 1)),
    	    PowerGridCard.plant(8, 2, Map.of(Resource.GAS, 3, Resource.OIL, 3)),
    	    PowerGridCard.plant(9, 2, Map.of(Resource.COAL, 3)),
    	    PowerGridCard.plant(10, 2, Map.of(Resource.OIL, 2)),
    	    PowerGridCard.plant(11, 1, Map.of(Resource.COAL, 0)), 
    	    PowerGridCard.plant(12, 2, Map.of(Resource.COAL, 2)),
    	    PowerGridCard.plant(13, 2, Map.of(Resource.URANIUM, 1)),
    	    PowerGridCard.plant(14, 2, Map.of(Resource.GAS, 1)),
    	    PowerGridCard.plant(15, 2, Map.of(Resource.COAL, 1)),
    	    //rest of deck
    	    PowerGridCard.plant(16, 3, Map.of(Resource.GAS, 2)),
    	    PowerGridCard.plant(17, 2, Map.of(Resource.COAL, 0)),
    	    PowerGridCard.plant(18, 3, Map.of(Resource.OIL, 2)),
    	    PowerGridCard.plant(19, 3, Map.of(Resource.GAS, 1)),
    	    PowerGridCard.plant(20, 4, Map.of(Resource.COAL, 3)),
    	    PowerGridCard.plant(21, 3, Map.of(Resource.URANIUM, 1)),
    	    PowerGridCard.plant(22, 5, Map.of(Resource.GAS, 3, Resource.OIL, 3)),
    	    PowerGridCard.plant(23, 4,  Map.of(Resource.OIL, 2)),
    	    PowerGridCard.plant(24, 3, Map.of(Resource.COAL, 0)),
    	    PowerGridCard.plant(25, 5, Map.of(Resource.COAL, 2)),
    	    PowerGridCard.plant(26, 4, Map.of(Resource.GAS, 1)),
    	    PowerGridCard.plant(27, 4, Map.of(Resource.COAL, 1)),
    	    PowerGridCard.plant(28, 3,  Map.of(Resource.COAL, 0)),
    	    PowerGridCard.plant(29, 5, Map.of(Resource.COAL, 2)),
    	    PowerGridCard.plant(30, 5, Map.of(Resource.OIL, 2)),
    	    PowerGridCard.plant(31, 4, Map.of(Resource.COAL, 0)),
    	    PowerGridCard.plant(32, 5, Map.of(Resource.URANIUM, 2)),
    	    PowerGridCard.plant(33, 6, Map.of(Resource.COAL, 3)),
    	    PowerGridCard.plant(34, 6, Map.of(Resource.GAS, 3)),
    	    PowerGridCard.plant(35, 5, Map.of(Resource.OIL, 2, Resource.GAS, 2)),
    	    PowerGridCard.plant(36, 5,  Map.of(Resource.COAL, 0)),
    	    PowerGridCard.plant(37, 6, Map.of(Resource.URANIUM, 2)),
    	    PowerGridCard.plant(38, 6, Map.of(Resource.OIL, 3)),
    	    PowerGridCard.plant(39, 6, Map.of(Resource.GAS, 2)),
    	    PowerGridCard.plant(40, 6, Map.of(Resource.COAL, 3)),
    	    PowerGridCard.plant(42, 6, Map.of(Resource.OIL, 2)),
    	    PowerGridCard.plant(44, 6,  Map.of(Resource.COAL, 0)),
    	    PowerGridCard.plant(46, 7, Map.of(Resource.GAS, 2)),
    	    PowerGridCard.plant(50, 7, Map.of(Resource.URANIUM, 2))
    	);


	@Override
	protected AbstractParameters _copy() {
	    PowerGridParameters p = new PowerGridParameters();

	    p.maxRounds = this.maxRounds;
	    p.startingMoney = this.startingMoney;

	    p.citySlotPrices    = this.citySlotPrices == null ? null : this.citySlotPrices.clone();
	    p.startinResources  = this.startinResources == null ? null : this.startinResources.clone();
	    p.step2Trigger      = this.step2Trigger == null ? null : this.step2Trigger.clone();
	    p.citiesToTriggerEnd= this.citiesToTriggerEnd == null ? null : this.citiesToTriggerEnd.clone();

	    // Deep-copy the refresh tables
	    p.resourceRefreshNA_2P = deepCopy(this.resourceRefreshNA_2P);
	    p.resourceRefreshNA_3P = deepCopy(this.resourceRefreshNA_3P);
	    p.resourceRefreshNA_4P = deepCopy(this.resourceRefreshNA_4P);
	    p.resourceRefreshNA_5P = deepCopy(this.resourceRefreshNA_5P);
	    p.resourceRefreshNA_6P = deepCopy(this.resourceRefreshNA_6P);

	    p.resourceRefreshEU_2P = deepCopy(this.resourceRefreshEU_2P);
	    p.resourceRefreshEU_3P = deepCopy(this.resourceRefreshEU_3P);
	    p.resourceRefreshEU_4P = deepCopy(this.resourceRefreshEU_4P);
	    p.resourceRefreshEU_5P = deepCopy(this.resourceRefreshEU_5P);
	    p.resourceRefreshEU_6P = deepCopy(this.resourceRefreshEU_6P);

	    // Cards are immutable
	    p.plantsIncludedInGame = (this.plantsIncludedInGame == null)
	            ? null
	            : List.copyOf(this.plantsIncludedInGame);

	    return p;
	}

	private static int[][] deepCopy(int[][] src) {
	    if (src == null) return null;
	    int[][] out = new int[src.length][];
	    for (int i = 0; i < src.length; i++) {
	        out[i] = (src[i] == null) ? null : src[i].clone();
	    }
	    return out;
	}

	@Override
	protected boolean _equals(Object o) {
	    if (this == o) return true;
	    if (!(o instanceof PowerGridParameters that)) return false;
	    if (!super.equals(o)) return false;

	    return maxRounds == that.maxRounds
	            && startingMoney == that.startingMoney
	            && Arrays.equals(citySlotPrices, that.citySlotPrices)
	            && Arrays.equals(startinResources, that.startinResources)
	            && Arrays.equals(step2Trigger, that.step2Trigger)
	            && Arrays.equals(citiesToTriggerEnd, that.citiesToTriggerEnd)
	            && Arrays.deepEquals(resourceRefreshNA_2P, that.resourceRefreshNA_2P)
	            && Arrays.deepEquals(resourceRefreshNA_3P, that.resourceRefreshNA_3P)
	            && Arrays.deepEquals(resourceRefreshNA_4P, that.resourceRefreshNA_4P)
	            && Arrays.deepEquals(resourceRefreshNA_5P, that.resourceRefreshNA_5P)
	            && Arrays.deepEquals(resourceRefreshNA_6P, that.resourceRefreshNA_6P)
	            && Arrays.deepEquals(resourceRefreshEU_2P, that.resourceRefreshEU_2P)
	            && Arrays.deepEquals(resourceRefreshEU_3P, that.resourceRefreshEU_3P)
	            && Arrays.deepEquals(resourceRefreshEU_4P, that.resourceRefreshEU_4P)
	            && Arrays.deepEquals(resourceRefreshEU_5P, that.resourceRefreshEU_5P)
	            && Arrays.deepEquals(resourceRefreshEU_6P, that.resourceRefreshEU_6P)
	            && Objects.equals(plantsIncludedInGame, that.plantsIncludedInGame);
	}



}
