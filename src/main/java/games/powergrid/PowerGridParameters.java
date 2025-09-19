package games.powergrid;


import java.util.List;
import java.util.Map;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;
import games.powergrid.components.PowerGridCard;

public class PowerGridParameters extends TunableParameters {
	public static final String CARD_ASSET_PATH = "data/powergrid";
	public enum Resource { COAL,  GAS, OIL, URANIUM }
	public enum PlantType { COAL, GAS, OIL, URANIUM, HYBRID, GREEN }
	public enum Phase {PLAYER_ORDER, AUCTION,  RESOURCE_BUY,BUILD,BUREAUCRACY;
	    public Phase next() {
	        return switch (this) {
	            case PLAYER_ORDER -> AUCTION;
	            case AUCTION -> RESOURCE_BUY;
	            case RESOURCE_BUY -> BUILD;
	            case BUILD -> BUREAUCRACY;
	            case BUREAUCRACY -> PLAYER_ORDER; 
	        };
	    }
	}
    public enum Step { STEP1, STEP2, STEP3 }
    
    public int maxRounds = 100;
    public int startingMoney = 50;
    
    public int[] citySlotPrices = new int[]{10, 15, 20};
    
    // Step 2 Trigger
    public int step2Trigger = 7;
    public int step2Trigger_6P = 6;
    
    // end-game trigger 
    public static final int CITIES_TO_TRIGGER_END_2P = 17;
    public static final int CITIES_TO_TRIGGER_END_3P = 17;
    public static final int CITIES_TO_TRIGGER_END_4P = 17;
    public static final int CITIES_TO_TRIGGER_END_5P = 15;
    public static final int CITIES_TO_TRIGGER_END_6P = 14;
    
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
    
    public static final int[] coalPrice = new int[] {9,9,8,8,7,7,6,6,6,5,5,5,4,4,4,3,3,3,3,3,3,3,3,1,1,1,1};
    public static final int[] gasPrice = new int[] {8,8,8,7,7,7,6,6,6,5,5,5,4,4,4,3,3,3,2,2,2,1,1,1};
    public static final int[] oilPrice = new int[] {9,9,9,9,8,8,7,7,6,6,5,5,4,4,3,3,2,2,1,1};
    public static final int[] uraniumPrice = new int[] {9,9,8,8,7,7,6,5,4,3,2,1};
    public static final int[] INCOME_TRACK = {
    	     0,  10,  82, 129,
    	    22,   7,  90, 134,
    	    33,   9,  98, 138,
    	    44,  10, 105, 142,
    	    54,  11, 112, 145,
    	    64,  12, 118, 148,
    	    73,  13, 120, 150
    	};
    
    public List<PowerGridCard> plantsIncludedInGame = List.of(
    		//initial cards
    	    PowerGridCard.plant(3, 1, Map.of(Resource.COAL, 2)),
    	    PowerGridCard.plant(4, 1, Map.of(Resource.COAL, 2)),
    	    PowerGridCard.plant(5, 1, Map.of(Resource.GAS, 2)),
    	    PowerGridCard.plant(6, 1, Map.of(Resource.OIL, 1)),
    	    PowerGridCard.plant(7, 1, Map.of(Resource.COAL, 1)),
    	    PowerGridCard.plant(8, 2, Map.of(Resource.GAS, 3, Resource.OIL, 3)),
    	    PowerGridCard.plant(9, 2, Map.of(Resource.COAL, 3)),
    	    PowerGridCard.plant(10, 2, Map.of(Resource.OIL, 2)),
    	    PowerGridCard.plant(11, 1, Map.of()), // renewable, no fuel cost
    	    PowerGridCard.plant(12, 2, Map.of(Resource.COAL, 2)),
    	    PowerGridCard.plant(13, 2, Map.of(Resource.URANIUM, 1)),
    	    PowerGridCard.plant(14, 2, Map.of(Resource.GAS, 1)),
    	    PowerGridCard.plant(15, 2, Map.of(Resource.COAL, 1)),
    	    //rest of deck
    	    PowerGridCard.plant(16, 3, Map.of(Resource.GAS, 2)),
    	    PowerGridCard.plant(17, 2, Map.of()),
    	    PowerGridCard.plant(18, 3, Map.of(Resource.OIL, 2)),
    	    PowerGridCard.plant(19, 3, Map.of(Resource.GAS, 1)),
    	    PowerGridCard.plant(20, 4, Map.of(Resource.COAL, 3)),
    	    PowerGridCard.plant(21, 3, Map.of(Resource.URANIUM, 1)),
    	    PowerGridCard.plant(22, 5, Map.of(Resource.GAS, 3, Resource.OIL, 3)),
    	    PowerGridCard.plant(23, 4,  Map.of(Resource.OIL, 2)),
    	    PowerGridCard.plant(24, 3, Map.of()),
    	    PowerGridCard.plant(25, 5, Map.of(Resource.COAL, 2)),
    	    PowerGridCard.plant(26, 4, Map.of(Resource.GAS, 1)),
    	    PowerGridCard.plant(27, 4, Map.of(Resource.COAL, 1)),
    	    PowerGridCard.plant(28, 3,  Map.of()),
    	    PowerGridCard.plant(29, 5, Map.of(Resource.COAL, 2)),
    	    PowerGridCard.plant(30, 5, Map.of(Resource.OIL, 2)),
    	    PowerGridCard.plant(31, 4, Map.of()),
    	    PowerGridCard.plant(32, 5, Map.of(Resource.URANIUM, 2)),
    	    PowerGridCard.plant(33, 6, Map.of(Resource.COAL, 3)),
    	    PowerGridCard.plant(34, 6, Map.of(Resource.GAS, 3)),
    	    PowerGridCard.plant(35, 5, Map.of(Resource.OIL, 2, Resource.GAS, 2)),
    	    PowerGridCard.plant(36, 5,  Map.of()),
    	    PowerGridCard.plant(37, 6, Map.of(Resource.URANIUM, 2)),
    	    PowerGridCard.plant(38, 6, Map.of(Resource.OIL, 3)),
    	    PowerGridCard.plant(39, 6, Map.of(Resource.GAS, 2)),
    	    PowerGridCard.plant(40, 6, Map.of(Resource.COAL, 3)),
    	    PowerGridCard.plant(42, 6, Map.of(Resource.OIL, 2)),
    	    PowerGridCard.plant(44, 6,  Map.of()),
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
		// TODO Auto-generated method stub
		return null;
	}

}
