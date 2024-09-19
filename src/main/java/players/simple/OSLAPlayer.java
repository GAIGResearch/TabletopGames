package players.simple;

import core.interfaces.IStateHeuristic;
import players.heuristics.GameDefaultHeuristic;
import players.search.MaxNSearchParameters;
import players.search.MaxNSearchPlayer;


public class OSLAPlayer extends MaxNSearchPlayer {


    public OSLAPlayer() {
        this(new GameDefaultHeuristic());
    }

    public OSLAPlayer(IStateHeuristic heuristic) {
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
