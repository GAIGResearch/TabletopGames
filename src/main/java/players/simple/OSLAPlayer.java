package players.simple;

import core.interfaces.IStateHeuristic;
import players.heuristics.GameDefaultHeuristic;
import players.search.SearchParameters;
import players.search.SearchPlayer;


public class OSLAPlayer extends SearchPlayer {


    public OSLAPlayer() {
        this(new GameDefaultHeuristic());
    }

    public OSLAPlayer(IStateHeuristic heuristic) {
        super(constructParameters(heuristic));
        setName("OSLA");
    }

    private static SearchParameters constructParameters(IStateHeuristic heuristic) {
        SearchParameters params = new SearchParameters();
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
