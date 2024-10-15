package players.simple;

import core.interfaces.IStateHeuristic;
import players.heuristics.GameDefaultHeuristic;
import players.search.MaxNSearchParameters;
import players.search.MaxNSearchPlayer;


public class SuperOSLAPlayer extends MaxNSearchPlayer {


    /**
     * This has the same functionality as OSLAPlayer, but uses the MaxN search algorithm to select the best action.
     * The old OSLAPlayer is kept for didactic purposes in the Game AI course (as the algorithm is much easier to follow).
     */
    public SuperOSLAPlayer() {
        this(new GameDefaultHeuristic());
    }

    public SuperOSLAPlayer(IStateHeuristic heuristic) {
        super(constructParameters(heuristic));
        setName("OSLA");
    }

    private static MaxNSearchParameters constructParameters(IStateHeuristic heuristic) {
        MaxNSearchParameters params = new MaxNSearchParameters();
        params.setParameterValue("BUDGET_TYPE", "BUDGET_TIME");
        params.setParameterValue("BUDGET", 100); // 100ms as timeout
        params.setParameterValue("searchDepth", 1);
        params.setParameterValue("searchUnit", "ACTION");
        if (heuristic != null)
            params.setParameterValue("heuristic", heuristic);
        else
            params.setParameterValue("heuristic", new GameDefaultHeuristic());
        return params;
    }

}
