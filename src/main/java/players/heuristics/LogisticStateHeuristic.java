package players.heuristics;

import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import org.json.simple.JSONObject;


public class LogisticStateHeuristic extends LinearStateHeuristic {

    public LogisticStateHeuristic(IStateFeatureVector featureVector, String coefficientsFile, IStateHeuristic defaultHeuristic) {
        super(featureVector, coefficientsFile, defaultHeuristic);
        setInverseLinkFunction(x -> 1.0 / (1.0 + Math.exp(-x)));
    }
    public LogisticStateHeuristic(IStateFeatureVector featureVector, double[] coefficients, IStateHeuristic defaultHeuristic) {
        super(featureVector, coefficients, defaultHeuristic);
        setInverseLinkFunction(x -> 1.0 / (1.0 + Math.exp(-x)));
    }
    public LogisticStateHeuristic(JSONObject obj) {
        super(obj);
        setInverseLinkFunction(x -> 1.0 / (1.0 + Math.exp(-x)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("class", "players.heuristics.LogisticStateHeuristic");
        return json;
    }

}
