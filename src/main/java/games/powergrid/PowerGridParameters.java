package games.powergrid;


import java.util.List;
import java.util.Map;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;
import games.powergrid.components.PowerGridCard;

public class PowerGridParameters extends TunableParameters {
	public static final String CARD_ASSET_PATH = "data/powergrid";
	public enum Resource { COAL,  GAS, OIL, URANIUM} //TODO implement None Type resource
	public enum PlantType { COAL, GAS, OIL, URANIUM, HYBRID, GREEN }

    public enum Step { STEP1, STEP2, STEP3 }
    
    public int maxRounds = 100;
    public int startingMoney = 50;
    
    public int[] citySlotPrices = new int[]{10, 15, 20};
    public int[] startinResources = new int[]{23, 18, 14,2};
    
    // Step 2 Trigger
    public int step2Trigger = 7;
    public int step2Trigger_6P = 6;
    
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
	protected boolean _equals(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object instantiate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void _reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected AbstractParameters _copy() {
		return this; //The current version does not support changing any of the parameters
	}

}
