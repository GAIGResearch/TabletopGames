package players.rhea;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import players.PlayerConstants;
import utilities.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class RHEAPlayer extends AbstractPlayer
{
    RHEAParams params;
    private RHEAIndividual bestIndividual;

    private ArrayList<RHEAIndividual> population;
    private final Random randomGenerator;
    IStateHeuristic heuristic;

    // Budgets
    private double avgTimeTaken = 0, acumTimeTaken = 0;
    private int numIters = 0;
    private int fmCalls = 0;
    private int copyCalls = 0;

    public RHEAPlayer() {
        this(System.currentTimeMillis());
    }

    public RHEAPlayer(RHEAParams params) {
        randomGenerator = new Random(params.getRandomSeed());
        this.params = params;
        setName("RHEA");
    }

    public RHEAPlayer(long seed) {
        this(new RHEAParams(seed));
    }

    public RHEAPlayer(IStateHeuristic heuristic) {
        this(System.currentTimeMillis());
        this.heuristic = heuristic;
    }

    public RHEAPlayer(RHEAParams params, IStateHeuristic heuristic) {
        this(params);
        this.heuristic = heuristic;
    }

    public RHEAPlayer(long seed, IStateHeuristic heuristic) {
        this(new RHEAParams(seed));
        this.heuristic = heuristic;
    }

    @Override
    public AbstractAction getAction(AbstractGameState stateObs, List<AbstractAction> actions) {
        ElapsedCpuTimer timer = new ElapsedCpuTimer();  // New timer for this game tick
        avgTimeTaken = 0;
        acumTimeTaken = 0;
        numIters = 0;
        fmCalls = 0;
        copyCalls = 0;

        // Initialise individuals
        population = new ArrayList<RHEAIndividual>();
        for(int i = 0; i < params.populationSize; ++i)
        {
            population.add(new RHEAIndividual(params.horizon, params.discountFactor, getForwardModel(), stateObs, getPlayerID(), randomGenerator, heuristic));
            fmCalls += population.get(i).length;
        }

        // Run evolution
        boolean keepIterating = true;
        while (keepIterating) {
            runIteration(stateObs);

            // Check budget depending on budget type
            if (params.budgetType == PlayerConstants.BUDGET_TIME) {
                long remaining = timer.remainingTimeMillis();
                keepIterating = remaining > avgTimeTaken && remaining > params.breakMS;
            } else if (params.budgetType == PlayerConstants.BUDGET_FM_CALLS) {
                keepIterating = fmCalls < params.budget;
            } else if (params.budgetType == PlayerConstants.BUDGET_COPY_CALLS) {
                keepIterating = copyCalls < params.budget && numIters < params.budget;
            } else if (params.budgetType == PlayerConstants.BUDGET_FMANDCOPY_CALLS) {
                keepIterating = (fmCalls + copyCalls) < params.budget;
            } else if (params.budgetType == PlayerConstants.BUDGET_ITERATIONS) {
                keepIterating = numIters < params.budget;
            }
        }

        // Return first action of best individual
        return bestIndividual.actions[0];
    }

    @Override
    public RHEAPlayer copy() {
        RHEAParams newParams = (RHEAParams) params.copy();
        newParams.setRandomSeed(randomGenerator.nextInt());
        return new RHEAPlayer(newParams);
    }

    private RHEAIndividual crossover(RHEAIndividual p1, RHEAIndividual p2)
    {
        RHEAIndividual child = new RHEAIndividual(p1);
        copyCalls += child.length;

        for(int i = child.length / 2; i < child.length; ++i)
        {
            child.actions[i] = p2.actions[i];
            child.gameStates[i] = p2.gameStates[i].copy();
        }
        return child;
    }

    /**
     * Run evolutionary process for one generation
     * @param stateObs - current game state
     */
    private void runIteration(AbstractGameState stateObs) {
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

        population.sort(Comparator.naturalOrder());

        ArrayList<RHEAIndividual> newPopulation = new ArrayList<RHEAIndividual>();
        int statesUpdated = 0;
        for(int i = 0; i < params.eliteCount; ++i)
        {
            newPopulation.add(population.get(i));
        }

        for(int i = 0; i < params.childCount; ++i)
        {
            RHEAIndividual p1 = population.get(0);
            RHEAIndividual p2 = population.get(1);

            RHEAIndividual child = crossover(p1, p2);
            statesUpdated += child.mutate(getForwardModel(), getPlayerID());
            //fmCalls += child.rollout(child.gameStates[0], getForwardModel(), 0, child.actions.length, getPlayerID());
            population.add(child);
        }
        population.sort(Comparator.naturalOrder());
        for(int i = 0; i < params.populationSize - params.eliteCount; ++i)
        {
            newPopulation.add(population.get(i));
        }
        population = newPopulation;
        fmCalls += statesUpdated;
        copyCalls += statesUpdated; // as mutate() copyies once each time it applies the forward model
        // Update budgets
        numIters++;
        acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
        avgTimeTaken = acumTimeTaken / numIters;
    }
}
