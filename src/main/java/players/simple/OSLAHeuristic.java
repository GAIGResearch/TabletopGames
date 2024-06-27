package players.simple;

import core.AbstractGameState;
import core.AbstractParameters;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;


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
        OSLAParameters params = new OSLAParameters();
        params.heuristicFunc = heuristic;
        return new OSLAPlayer(params);
    }

}
