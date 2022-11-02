package players.simple;

import core.*;
import core.interfaces.*;
import evaluation.TunableParameters;
import org.json.simple.JSONObject;


/**
 * This is a wrapper to use any parameterisable heuristic with the OSLA player
 */
public class OSLAHeuristic extends TunableParameters {

    int plyDepth = 1;
    private IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;

    public OSLAHeuristic() {
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
        _reset();
    }

    @Override
    protected AbstractParameters _copy() {
        OSLAHeuristic retValue = new OSLAHeuristic();
        retValue.plyDepth = plyDepth;
        retValue.heuristic = heuristic;
        return retValue;
    }

    /**
     * Any nested tunable parameter space is highly likely to be an IStateHeuristic
     * If it is, then we set this as the heuristic after the parent code in TunableParameters
     * has done the work to merge the search spaces together.
     *
     * @param json The raw JSON
     * @return The instantiated object
     */
    @Override
    public Object registerChild(String nameSpace, JSONObject json) {
        Object child = super.registerChild(nameSpace, json);
        if (child instanceof IStateHeuristic) {
            heuristic = (IStateHeuristic) child;
        }
        return child;
    }

    @Override
    public void _reset() {
        if (heuristic instanceof TunableParameters) {
            TunableParameters tunableHeuristic = (TunableParameters) heuristic;
            for (String name : tunableHeuristic.getParameterNames()) {
                tunableHeuristic.setParameterValue(name, this.getParameterValue("heuristic." + name));
            }
        }
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof OSLAHeuristic) {
           OSLAHeuristic other = (OSLAHeuristic) o;
           return other.plyDepth == plyDepth && other.heuristic.equals(heuristic);
        }
        return false;
    }

    @Override
    public Object instantiate() {
        return new OSLAPlayer(heuristic);
    }

}
