package games.powergrid;


import java.util.Map;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.components.PowerGridCard;
import games.powergrid.components.PowerGridCard.PlantInput;

public class PowerGridHeuristic implements IStateHeuristic {

	/**
	 * Evaluates the current {@link PowerGridGameState} from the perspective of the specified player
	 * using a leader-based heuristic that compares the player's performance against the current leader
	 * across multiple key categories.
	 * <p>
	 * This heuristic is designed for intermediate or non-terminal game states in Power Grid.
	 * It returns a normalized score in the range [0, 1], representing how well the player is doing
	 * relative to the top-performing players in the following categories:
	 * <ul>
	 *   <li>Number of cities controlled</li>
	 *   <li>Money (cash on hand)</li>
	 *   <li>Income (turn-based earnings)</li>
	 *   <li>Power plant capacity</li>
	 *   <li>Resource reserves vs. required fuel</li>
	 * </ul>
	 * Each component is scaled, weighted, and combined into a single heuristic value.
	 * If the game is over, this function returns {@code 10.0} for a win and {@code 0.0} otherwise.
	 * <p>
	 * The function relies on helper methods like {@code getMaxExcluding()} to determine the
	 * leader in each category (excluding the current player) and {@code shapedGap()} to apply
	 * a nonlinear scaling that rewards proximity to or exceeding the leader's value.
	 *
	 * @param gs        the current abstract game state, expected to be an instance of {@link PowerGridGameState}
	 * @param playerId  the ID of the player whose perspective the state is being evaluated from
	 * @return a normalized heuristic value in [0, 1] representing the player’s standing compared to the current leaders;
	 *         returns {@code 10.0} if the player is the winner, {@code 0.0} if the player has lost
	 *
	 * @see PowerGridGameState
	 * @see #getMaxExcluding(int[], int)
	 * @see #shapedGap(double, double, double, double, double, double)
	 */

	@Override
	public double evaluateState(AbstractGameState gs, int playerId) {
	    PowerGridGameState s = (PowerGridGameState) gs;
	    if (s.isGameOver()) {
	        return (gs.getWinner() == playerId) ? 10.0 : 0.0;
	    }
	    //calculates the total resources the player has regardless of type required by the player
    	Deck<PowerGridCard> hand = s.getPlayerPlantDeck(playerId);
    	int totalPlayerFuel = 0;
    	for (Resource r : Resource.values()) {
    	    totalPlayerFuel += s.getFuel(playerId, r);            
    	}
    	//calculates the total resources required regardless of type required by the player
    	int totalResourceReq = 0;    	
    	for (PowerGridCard card : hand) {
    	    PlantInput input = card.getInput();
    	    Map<Resource, Integer> req = input.asMap();
    	    if (req == null || req.isEmpty()) continue;

    	    for (int amount : req.values()) {
    	        totalResourceReq += amount;  
    	    }
    	}
  
	    int numberOfCities = s.getCityCountByPlayer(playerId);
	    int money          = s.getPlayersMoney(playerId);
	    int income         = s.getIncome(playerId);
	    int capacity       = s.getPlayerCapacity(playerId);

	    int[] cityCountArray = s.getCityCountByPlayer();
	    int[] moneyArray     = s.getPlayerMoney();
	    int[] incomeArray    = s.getIncome().stream().mapToInt(Integer::intValue).toArray();
	    int[] capacityArray  = s.getPlayerCapacity();

	    int mostCities   = getMaxExcluding(cityCountArray, playerId);
	    int mostMoney    = getMaxExcluding(moneyArray, playerId);
	    int mostIncome   = getMaxExcluding(incomeArray, playerId);
	    int mostCapacity = getMaxExcluding(capacityArray, playerId);


	    double posCap = 1.2, negCap = 2.0;

	    // Scale factors 
	    double cityPosScale = 1.0, cityNegScale = 3.0;
	    double moneyPosScale = 1.0, moneyNegScale = 1.0;
	    double incomePosScale = 2.0, incomeNegScale = 1.0; 
	    double capPosScale  = 1.0, capNegScale  = 1.0;
	    double resPosScale  = 1.0, resNegScale  = 1.0;

	    //leaders in each category 
	    double cityScore   = shapedGap(numberOfCities, mostCities, posCap, negCap, cityPosScale, cityNegScale);
	    double moneyScore  = shapedGap(money,          mostMoney,  posCap, negCap, moneyPosScale, moneyNegScale);
	    double incomeScore = shapedGap(income,         mostIncome, posCap, negCap, incomePosScale, incomeNegScale);
	    double capScore    = shapedGap(capacity,       mostCapacity, posCap, negCap, capPosScale, capNegScale);
	    double resScore    = shapedGap(totalPlayerFuel,       totalResourceReq, posCap, negCap, resPosScale, resNegScale);


	    // Weights
	    double wCities = 0.4;
	    double wMoney  = 0.1;
	    double wIncome = 0.3;
	    double wCap    = 0.15;
	    double wResource = 0.05;

	    // Weighted sum
	    double sumScore = wCities * cityScore + wMoney * moneyScore + wIncome * incomeScore + wCap * capScore + wResource * resScore;;

	    // Compute bounds
	    double Wpos = wCities * posCap + wMoney * posCap + wIncome * posCap + wCap * posCap + wResource * posCap;
	    double Wneg = wCities * negCap + wMoney * negCap + wIncome * negCap + wCap * negCap + wResource * negCap;

	    double norm01 = (sumScore + Wneg) / (Wneg + Wpos); 
	    double heuristic = clamp01(norm01);

	    return heuristic;
	}


	// Asymmetric, saturating gap score in [-negCap, +posCap]
	private static double shapedGap(int self, int leader,double posCap, double negCap,double posScale, double negScale) {
	    int diff = self - leader; // >0 = ahead, <0 = behind
	    if (diff >= 0) {
	        // reward that saturates toward +posCap
	        double g = 1.0 - Math.exp(-(diff) / Math.max(1e-9, posScale));
	        return posCap * g;
	    } else {
	        // penalty that saturates toward -negCap (usually larger magnitude)
	        double gap = -diff;
	        double g = 1.0 - Math.exp(-(gap) / Math.max(1e-9, negScale));
	        return -negCap * g;
	    }
	}
	
	
	/**
	 * Returns the largest value in the given array, excluding the element at the specified index.
	 * <p>
	 * This method is useful for comparing a specific player's value to the leader's value
	 * in a particular category, without considering the player's own score.
	 *
	 * @param array        the array of integer values to search through
	 * @param excludeIndex the index of the element to exclude from the comparison
	 * @return the largest integer value in the array excluding the specified index;
	 *         {@link Integer#MIN_VALUE} if all elements are excluded or the array is empty
	 */
	public static int getMaxExcluding(int[] array, int excludeIndex) {
	    int max = Integer.MIN_VALUE;
	    for (int i = 0; i < array.length; i++) {
	        if (i == excludeIndex) continue; 
	        if (array[i] > max) {
	            max = array[i];
	        }
	    }
	    return max;
	}
	//clamps value between 0 and 1 
    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

}