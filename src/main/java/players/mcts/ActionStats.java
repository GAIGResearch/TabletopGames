package players.mcts;

import core.actions.AbstractAction;

import java.util.*;

public class ActionStats {
    // Total value of this node
    public double[] totValue;
    public double[] squaredTotValue;
    // Number of visits
    public int nVisits;
    public int validVisits;

    public ActionStats(int nPlayers) {
        totValue = new double[nPlayers];
        squaredTotValue = new double[nPlayers];
        nVisits = 0;
    }

    public void update(double[] results) {
        for (int i = 0; i < results.length; i++) {
            totValue[i] += results[i];
            squaredTotValue[i] += results[i] * results[i];
        }
        nVisits++;
    }

}
