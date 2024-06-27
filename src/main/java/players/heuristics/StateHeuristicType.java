package players.heuristics;

import core.interfaces.IStateHeuristic;

public enum StateHeuristicType {
    StringHeuristic(new StringHeuristic()),
    WinOnlyHeuristic(new WinOnlyHeuristic()),
    OrdinalPosition(new OrdinalPosition()),
    WinPlusHeuristic(new WinPlusHeuristic()),
    GameSpecificHeuristic(new GameSpecificHeuristic()),
    LeaderHeuristic(new LeaderHeuristic()),
    PureScoreHeuristic(new PureScoreHeuristic());

    final IStateHeuristic heuristicFunc;

    StateHeuristicType(IStateHeuristic heuristicFunc) {
        this.heuristicFunc = heuristicFunc;
    }

    public IStateHeuristic getHeuristic() {
        return heuristicFunc;
    }

    public static StringHeuristic createStringHeuristic(String fileName, String className) {
        return new StringHeuristic(fileName, className);
    }
}
