package players.heuristics;

import core.interfaces.*;
import org.json.simple.JSONObject;


public class LogisticActionHeuristic extends LinearActionHeuristic {

    public LogisticActionHeuristic(IActionFeatureVector actionFeatureVector, IStateFeatureVector featureVector,
                                   String coefficientsFile) {
        super(actionFeatureVector, featureVector, coefficientsFile);
        setInverseLinkFunction(x -> 1.0 / (1.0 + Math.exp(-x)));
    }
    public LogisticActionHeuristic(IActionFeatureVector actionFeatureVector, IStateFeatureVector featureVector,
                                   double[] coefficients) {
        super(actionFeatureVector, featureVector, coefficients);
        setInverseLinkFunction(x -> 1.0 / (1.0 + Math.exp(-x)));
    }
    public LogisticActionHeuristic(JSONObject obj) {
        super(obj);
        setInverseLinkFunction(x -> 1.0 / (1.0 + Math.exp(-x)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        json.put("class", "players.heuristics.LogisticActionHeuristic");
        return json;
    }
}
