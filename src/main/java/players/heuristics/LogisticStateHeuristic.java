package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;


public class LogisticStateHeuristic extends LinearStateHeuristic {

    public LogisticStateHeuristic(IStateFeatureVector featureVector, String coefficientsFile, IStateHeuristic defaultHeuristic) {
        super(featureVector, coefficientsFile, defaultHeuristic);
        setInverseLinkFunction(x -> 1.0 / (1.0 + Math.exp(-x)));
    }
}
