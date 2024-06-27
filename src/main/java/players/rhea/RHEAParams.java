package players.rhea;

import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import players.PlayerParameters;
import players.heuristics.StateHeuristicType;

import java.util.Arrays;

public class RHEAParams extends PlayerParameters
{

    public int horizon = 10;
    public double discountFactor = 0.9;
    public int populationSize = 10;
    public int eliteCount = 2;
    public int childCount = 10;
    public int mutationCount = 1;
    public RHEAEnums.SelectionType selectionType = RHEAEnums.SelectionType.TOURNAMENT;
    public int tournamentSize = 4;
    public RHEAEnums.CrossoverType crossoverType = RHEAEnums.CrossoverType.UNIFORM;
    public boolean shiftLeft;
    public StateHeuristicType heuristic = StateHeuristicType.PureScoreHeuristic;
    public boolean useMAST;

    IStateHeuristic heuristicFunc;


    public RHEAParams() {
        addTunableParameter("horizon", 10, Arrays.asList(1, 3, 5, 10, 20, 30));
        addTunableParameter("discountFactor", 0.9, Arrays.asList(0.5, 0.8, 0.9, 0.95, 0.99, 0.999, 1.0));
        addTunableParameter("populationSize", 10, Arrays.asList(6, 8, 10, 12, 14, 16, 18, 20));
        addTunableParameter("eliteCount", 2, Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16, 18, 20));
        addTunableParameter("childCount", 10, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        addTunableParameter("selectionType", RHEAEnums.SelectionType.TOURNAMENT, Arrays.asList(RHEAEnums.SelectionType.values()));
        addTunableParameter("tournamentSize", 4, Arrays.asList(1, 2, 3, 4, 5, 6));
        addTunableParameter("crossoverType", RHEAEnums.CrossoverType.UNIFORM, Arrays.asList(RHEAEnums.CrossoverType.values()));
        addTunableParameter("shiftLeft", false, Arrays.asList(false, true));
        addTunableParameter("mutationCount", 1, Arrays.asList(1, 3, 10));
        addTunableParameter("heuristic", StateHeuristicType.PureScoreHeuristic, Arrays.asList(StateHeuristicType.values()));
        addTunableParameter("useMAST", false, Arrays.asList(false, true));
    }

    @Override
    public void _reset() {
        super._reset();
        horizon = (int) getParameterValue("horizon");
        discountFactor = (double) getParameterValue("discountFactor");
        populationSize = (int) getParameterValue("populationSize");
        eliteCount = (int) getParameterValue("eliteCount");
        childCount = (int) getParameterValue("childCount");
        selectionType = (RHEAEnums.SelectionType) getParameterValue("selectionType");
        tournamentSize = (int) getParameterValue("tournamentSize");
        crossoverType = (RHEAEnums.CrossoverType) getParameterValue("crossoverType");
        shiftLeft = (boolean) getParameterValue("shiftLeft");
        mutationCount = (int) getParameterValue("mutationCount");
        useMAST = (boolean) getParameterValue("useMAST");

        if (heuristic != getParameterValue("heuristic")) {
            heuristic = (StateHeuristicType) getParameterValue("heuristic");
            heuristicFunc = heuristic.getHeuristic();

            if (heuristicFunc instanceof TunableParameters) {
                TunableParameters tunableHeuristic = (TunableParameters) heuristicFunc;
                for (String name : tunableHeuristic.getParameterNames()) {
                    tunableHeuristic.setParameterValue(name, this.getParameterValue("heuristic." + name));
                }
            }
        }
    }

    @Override
    protected RHEAParams _copy() {
        RHEAParams p = new RHEAParams();
        p.heuristicFunc = heuristicFunc;
        return p;
    }


    @Override
    public RHEAPlayer instantiate() {
        return new RHEAPlayer(this);
    }

    public IStateHeuristic getHeuristic() {
        return heuristicFunc;
    }

    @Override
    public IStateHeuristic getStateHeuristic() {
        return getHeuristic();
    }

}
