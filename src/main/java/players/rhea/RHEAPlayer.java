package players.rhea;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import players.PlayerConstants;
import utilities.ElapsedCpuTimer;

import java.util.*;

public class RHEAPlayer extends AbstractPlayer {
    private final Random randomGenerator;
    RHEAParams params;
    IStateHeuristic heuristic;
    private List<RHEAIndividual> population = new ArrayList<>();
    // Budgets
    private double timePerIteration = 0, timeTaken = 0, acumTimeTaken = 0, initTime = 0;
    private int numIters = 0;
    private int fmCalls = 0;
    private int copyCalls = 0;
    private int repairCount, nonRepairCount;

    public RHEAPlayer() {
        this(System.currentTimeMillis());
    }

    public RHEAPlayer(RHEAParams params) {
        randomGenerator = new Random(params.getRandomSeed());
        this.params = params;
        setName("rhea");
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
        timer.setMaxTimeMillis(params.budget);
        acumTimeTaken = 0;
        numIters = 0;
        fmCalls = 0;
        copyCalls = 0;
        repairCount = 0;
        nonRepairCount = 0;

        // Initialise individuals
        if (params.shiftLeft && !population.isEmpty()) {
            for (RHEAIndividual genome : population) {
                System.arraycopy(genome.actions, 1, genome.actions, 0, genome.actions.length - 1);
                // we shift all actions along, and then rollout with repair
                genome.gameStates[0] = stateObs.copy();
                fmCalls += genome.rollout(getForwardModel(), 0, getPlayerID(), true);
            }
        } else {
            population = new ArrayList<>();
            for (int i = 0; i < params.populationSize; ++i) {
                population.add(new RHEAIndividual(params.horizon, params.discountFactor, getForwardModel(), stateObs, getPlayerID(), randomGenerator, heuristic));
                fmCalls += population.get(i).length;
            }
        }

        initTime = timer.elapsedMillis();
        // Run evolution
        boolean keepIterating = true;
        while (keepIterating) {
            // Check budget depending on budget type
            if (params.budgetType == PlayerConstants.BUDGET_TIME) {
                long remaining = timer.remainingTimeMillis();
                keepIterating = remaining > timeTaken && remaining > params.breakMS;
            } else if (params.budgetType == PlayerConstants.BUDGET_FM_CALLS) {
                keepIterating = fmCalls < params.budget;
            } else if (params.budgetType == PlayerConstants.BUDGET_COPY_CALLS) {
                keepIterating = copyCalls < params.budget && numIters < params.budget;
            } else if (params.budgetType == PlayerConstants.BUDGET_FMANDCOPY_CALLS) {
                keepIterating = (fmCalls + copyCalls) < params.budget;
            } else if (params.budgetType == PlayerConstants.BUDGET_ITERATIONS) {
                keepIterating = numIters < params.budget;
            }

            population.sort(Comparator.naturalOrder());
            if (keepIterating) // this is after the above check in case the initialisation of the population uses up our time!
                runIteration();
        }

        timeTaken = timer.elapsedMillis();
        timePerIteration = acumTimeTaken / numIters;  // exludes initialisation
        if (statsLogger != null)
            logStatistics(stateObs);
        // Return first action of best individual
        return population.get(0).actions[0];
    }

    @Override
    public RHEAPlayer copy() {
        RHEAParams newParams = (RHEAParams) params.copy();
        newParams.setRandomSeed(randomGenerator.nextInt());
        return new RHEAPlayer(newParams);
    }

    private RHEAIndividual crossover(RHEAIndividual p1, RHEAIndividual p2) {
        switch (params.crossoverType) {
            case NONE: // we just take the first parent
                return new RHEAIndividual(p1);
            case UNIFORM:
                return uniformCrossover(p1, p2);
            case ONE_POINT:
                return onePointCrossover(p1, p2);
            case TWO_POINT:
                return twoPointCrossover(p1, p2);
            default:
                throw new RuntimeException("Unexpected crossover type");
        }
    }

    private RHEAIndividual uniformCrossover(RHEAIndividual p1, RHEAIndividual p2) {
        RHEAIndividual child = new RHEAIndividual(p1);
        copyCalls += child.length;
        int min = Math.min(p1.length, p2.length);
        for (int i = 0; i < min; ++i) {
            if (randomGenerator.nextFloat() >= 0.5f) {
                child.actions[i] = p2.actions[i];
                child.gameStates[i] = p2.gameStates[i]; //.copy();
            }
        }
        return child;
    }

    private RHEAIndividual onePointCrossover(RHEAIndividual p1, RHEAIndividual p2) {
        RHEAIndividual child = new RHEAIndividual(p1);
        copyCalls += child.length;
        int tailLength = Math.min(p1.length, p2.length) / 2;

        for (int i = 0; i < tailLength; ++i) {
            child.actions[child.length - 1 - i] = p2.actions[p2.length - 1 - i];
            child.gameStates[child.length - 1 - i] = p2.gameStates[p2.length - 1 - i]; //.copy();
        }
        return child;
    }

    private RHEAIndividual twoPointCrossover(RHEAIndividual p1, RHEAIndividual p2) {
        RHEAIndividual child = new RHEAIndividual(p1);
        copyCalls += child.length;
        int tailLength = Math.min(p1.length, p2.length) / 3;
        for (int i = 0; i < tailLength; ++i) {
            child.actions[i] = p2.actions[i];
            child.gameStates[i] = p2.gameStates[i]; //.copy();
            child.actions[child.length - 1 - i] = p2.actions[p2.length - 1 - i];
            child.gameStates[child.length - 1 - i] = p2.gameStates[p2.length - 1 - i]; //.copy();
        }
        return child;
    }

    RHEAIndividual[] selectParents() {
        RHEAIndividual[] parents = new RHEAIndividual[2];

        switch (params.selectionType) {
            case TOURNAMENT:
                parents[0] = tournamentSelection();
                parents[1] = tournamentSelection();
                break;
            case RANK:
                parents[0] = rankSelection();
                parents[1] = rankSelection();
                break;
            default:
                throw new RuntimeException("Unexpected selection type");
        }

        return parents;
    }

    RHEAIndividual tournamentSelection() {
        RHEAIndividual best = null;
        for (int i = 0; i < params.tournamentSize; ++i) {
            int rand = randomGenerator.nextInt(population.size());

            RHEAIndividual current = population.get(rand);
            if (best == null || current.value > best.value)
                best = current;
        }
        return best;
    }

    RHEAIndividual rankSelection() {
        population.sort(Comparator.naturalOrder());
        int rankSum = 0;
        for (int i = 0; i < population.size(); ++i)
            rankSum += i + 1;
        int ran = randomGenerator.nextInt(rankSum);
        int p = 0;
        for (int i = 0; i < population.size(); ++i) {
            p += population.size() - (i);
            if (p >= ran)
                return population.get(i);
        }
        throw new RuntimeException("Random Generator generated an invalid goal, goal: " + ran + " p: " + p);
    }

    /**
     * Run evolutionary process for one generation
     */
    private void runIteration() {
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
        //copy elites
        List<RHEAIndividual> newPopulation = new ArrayList<>();
        int statesUpdated = 0;
        for (int i = 0, max = Math.min(params.eliteCount, population.size()); i < max; ++i) {
            newPopulation.add(new RHEAIndividual(population.get(i)));
        }
        //crossover
        for (int i = 0; i < params.childCount; ++i) {
            RHEAIndividual[] parents = selectParents();
            RHEAIndividual child = crossover(parents[0], parents[1]);
            //statesUpdated += child.mutate(getForwardModel(), getPlayerID());
            //fmCalls += child.rollout(child.gameStates[0], getForwardModel(), 0, child.actions.length, getPlayerID());
            population.add(child);
        }

        for (int i = 0; i < population.size(); ++i) {
            statesUpdated += population.get(i).mutate(getForwardModel(), getPlayerID(), params.mutationCount);
        }

        for (RHEAIndividual individual : population) {
            repairCount += individual.repairCount;
            nonRepairCount += individual.nonRepairCount;
        }
        //sort
        population.sort(Comparator.naturalOrder());

        //best ones get moved to the new population
        for (int i = 0; i < params.populationSize - params.eliteCount; ++i) {
            newPopulation.add(population.get(i));
        }

        population = newPopulation;

        population.sort(Comparator.naturalOrder());
        fmCalls += statesUpdated;
        copyCalls += statesUpdated; // as mutate() copyies once each time it applies the forward model
        // Update budgets
        numIters++;
        acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
    }

    protected void logStatistics(AbstractGameState state) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("round", state.getTurnOrder());
        stats.put("turn", state.getTurnOrder().getTurnCounter());
        stats.put("turnOwner", state.getTurnOrder().getTurnOwner());
        stats.put("iterations", numIters);
        stats.put("fmCalls", fmCalls);
        stats.put("copyCalls", copyCalls);
        stats.put("time", timeTaken);
        stats.put("timePerIteration", timePerIteration);
        stats.put("initTime", initTime);
        stats.put("hiReward", population.get(0).value);
        stats.put("loReward", population.get(population.size() - 1).value);
        stats.put("medianReward", population.get(population.size() / 2 - 1).value);
        stats.put("repairProportion", repairCount == 0 ? 0.0 : repairCount / (double) (repairCount + nonRepairCount));
        stats.put("repairsPerIteration", repairCount == 0 ? 0.0 : repairCount / (double) numIters);
        statsLogger.record(stats);
    }
}
