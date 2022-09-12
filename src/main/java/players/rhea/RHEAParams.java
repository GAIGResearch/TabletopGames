package players.rhea;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;
import org.json.simple.JSONObject;
import players.PlayerParameters;

import java.util.Arrays;

public class RHEAParams extends PlayerParameters
{

    public int horizon = 10;
    public double discountFactor = 0.9;
    public int populationSize = 10;
    public int eliteCount = 2;
    public int childCount = 10;
    public int mutationCount = 1;
    RHEAEnums.SelectionType selectionType = RHEAEnums.SelectionType.TOURNAMENT;
    public int tournamentSize = 4;
    RHEAEnums.CrossoverType crossoverType = RHEAEnums.CrossoverType.UNIFORM;
    public boolean shiftLeft;
    protected IStateHeuristic heuristic = AbstractGameState::getGameScore;
    public boolean useMAST;


    public RHEAParams() {
        this(System.currentTimeMillis());
    }

    public RHEAParams(long seed) {
        super(seed);
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
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getGameScore);
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
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        if (heuristic instanceof TunableParameters) {
            TunableParameters tunableHeuristic = (TunableParameters) heuristic;
            for (String name : tunableHeuristic.getParameterNames()) {
                tunableHeuristic.setParameterValue(name, this.getParameterValue("heuristic." + name));
            }
        }
    }

    @Override
    protected RHEAParams _copy() {
         RHEAParams retValue = new RHEAParams(System.currentTimeMillis());
         retValue.horizon = horizon;
         retValue.discountFactor = discountFactor;
         retValue.populationSize = populationSize;
         retValue.shiftLeft = shiftLeft;
         retValue.eliteCount = eliteCount;
         retValue.childCount = childCount;
         retValue.selectionType = selectionType;
         retValue.tournamentSize = tournamentSize;
         retValue.crossoverType = crossoverType;
         retValue.mutationCount = mutationCount;
         retValue.heuristic = heuristic;
         retValue.useMAST = useMAST;
         return retValue;
    }


    @Override
    public RHEAPlayer instantiate() {
        return new RHEAPlayer(this);
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
        if ("heuristic".equals(nameSpace)) {
            heuristic = (IStateHeuristic) child;
            setParameterValue("heuristic", child);
        } else {
            throw new AssertionError("Unknown nameSpace : " + nameSpace);
        }
        return child;
    }

    public IStateHeuristic getHeuristic() {
        return heuristic;
    }

}
