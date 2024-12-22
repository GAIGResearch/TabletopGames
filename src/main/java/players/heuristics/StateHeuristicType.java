package players.heuristics;

import core.interfaces.IStateHeuristic;

import javax.swing.plaf.nimbus.State;

public enum StateHeuristicType {
    StringHeuristic(),
    GameSpecificHeuristic(new GameDefaultHeuristic()),
    WinOnlyHeuristic(new WinOnlyHeuristic()),
    OrdinalPosition(new OrdinalPosition()),
    WinPlusHeuristic(new WinPlusHeuristic(1000.0)),
    LeaderHeuristic(new LeaderHeuristic()),
    PureScoreHeuristic(new PureScoreHeuristic()),
    ScoreHeuristic(new ScoreHeuristic()),
    NullHeuristic(new NullHeuristic());

    final IStateHeuristic heuristicFunc;

    StateHeuristicType() {
        this.heuristicFunc = null;
    }

    StateHeuristicType(IStateHeuristic heuristicFunc) {
        this.heuristicFunc = heuristicFunc;
    }

}
