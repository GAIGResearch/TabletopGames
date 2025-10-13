package games.powergrid;


import core.AbstractGameState;
import core.CoreConstants.GameResult;
import core.interfaces.IStateHeuristic;

public class PowerGridHeuristic implements IStateHeuristic {



    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        PowerGridGameState s = (PowerGridGameState) gs;
        PowerGridParameters p = (PowerGridParameters) s.getGameParameters();
        if (s.isGameOver()) {
            if (gs.getWinner() == playerId)
                return 10;
            else {
            	return 0;
            }
            
        }

        int endTrigger = p.citiesToTriggerEnd[s.getNPlayers() - 1];
        int cities     = s.getCityCountByPlayer(playerId);
        int powered    = s.getPoweredCities(playerId);
        int capacity   = s.getPlayerCapacity(playerId);

        // 1) Reward actually powering cities (stable denominator).
        double poweredTerm = endTrigger > 0 ? (double) powered / endTrigger : 0.0;

        // 2) Soft capacity↔cities match.
        double matchTerm = gaussianMatch(capacity, cities, 3.0);

        // 3) City pressure that ramps up near the end (smoothstep is flat early, steep late).
        double progress  = endTrigger > 0 ? (double) cities / endTrigger : 0.0;
        double cityTerm  = smoothstep(0.55, 1.0, progress);  // little early, big push after ~55%

        // Dynamic weights: less match-penalty late game; more city pressure late game.
        double wCity    = lerp(0.20, 0.50, progress);
        double wMatch   = lerp(0.30, 0.10, progress);
        double wPowered = 1.0 - wCity - wMatch;              // stays ~0.4–0.6

        double score = wPowered * poweredTerm + wMatch * matchTerm + wCity * cityTerm;
        double clampedScore = clamp01(score);
        System.out.println("Player: " + playerId + " Score: " +  clampedScore);
        return clampedScore;
    }

    private static double gaussianMatch(int a, int b, double sigma) {
        double d = a - b;
        return Math.exp(-(d * d) / (2.0 * sigma * sigma));
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        if (edge1 <= edge0) return x; // guard
        double t = clamp01((x - edge0) / (edge1 - edge0));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * clamp01(t);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }



public static double capacityCityMatchScore(int capacity, int cities) {
	    // If no cities, don't penalize (nothing to power).
	    if (cities <= 0) return 0.0;
	
	    // Ratio centered at 1.0
	    double ratio = capacity / (double) cities;
	    double dx = ratio - 1.0;
	
	    // Width of the bell curve (standard deviation in ratio-space).
	    // Larger tauOver => gentler penalty for surplus.
	    // Smaller tauUnder => stronger penalty for shortage.
	    double tauOver  = 0.90; // surplus capacity -> slight penalty
	    double tauUnder = 0.30; // shortage       -> stronger penalty
	
	    double tau = dx >= 0 ? tauOver : tauUnder;
	    double score = Math.exp(- (dx * dx) / (2.0 * tau * tau));
	
	    // Safety clamp (should already be in (0,1])
	    return Math.max(0.0, Math.min(1.0, score));
	}

}