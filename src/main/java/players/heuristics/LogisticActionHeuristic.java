package players.heuristics;

import core.interfaces.*;


public class LogisticActionHeuristic extends LinearActionHeuristic {

    public LogisticActionHeuristic(IActionFeatureVector actionFeatureVector, IStateFeatureVector featureVector,
                                   String coefficientsFile) {
        super(actionFeatureVector, featureVector, coefficientsFile);
        setInverseLinkFunction(x -> 1.0 / (1.0 + Math.exp(-x)));
    }
}
