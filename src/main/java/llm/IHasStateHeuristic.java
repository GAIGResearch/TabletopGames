package llm;

import core.interfaces.IStateHeuristic;

public interface IHasStateHeuristic {

    void setStateHeuristic(IStateHeuristic heuristic);

    IStateHeuristic getStateHeuristic();

}
