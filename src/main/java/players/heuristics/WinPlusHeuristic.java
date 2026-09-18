package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import core.interfaces.IToJSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class WinPlusHeuristic extends WinOnlyHeuristic implements IToJSON {

    double scale;
    public WinPlusHeuristic(double scale) {
        this.scale = scale;
    }
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (gs.isNotTerminalForPlayer(playerId))
            return Math.clamp(gs.getHeuristicScore(playerId) / scale, 0.05, 0.95);

        return super.evaluateState(gs, playerId);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WinPlusHeuristic;
    }
    @Override
    public int hashCode() {
        return 5 + (int) (scale * 100);
    }

    @Override
    public String toString() {
        return "WinPlusHeuristic: " + String.format("%.2g", scale);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject retValue = new JSONObject();
        JSONArray args = new JSONArray();
        args.add(scale);
        retValue.put("class", "players.heuristics.WinPlusHeuristic");
        retValue.put("args", args);
        return retValue;
    }
}