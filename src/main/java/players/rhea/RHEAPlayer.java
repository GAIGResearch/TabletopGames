package players.rhea;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.IAnyTimePlayer;
import players.PlayerConstants;
import players.mcts.MASTPlayer;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;
import utilities.Pair;
import utilities.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class RHEAPlayer extends AbstractPlayer implements IAnyTimePlayer {
    private static final AbstractPlayer randomPlayer = new RandomPlayer();
    List<Map<Object, Pair<Integer, Double>>> MASTStatistics; // a list of one Map per player. Action -> (visits, totValue)
    protected List<RHEAIndividual> population = new ArrayList<>();
    // Budgets
    protected double timePerIteration = 0, timeTaken = 0, initTime = 0;
    protected int numIters = 0;
    protected int fmCalls = 0;
    protected int copyCalls = 0;
    protected int repairCount, nonRepairCount;
    private MASTPlayer mastPlayer;

    public RHEAPlayer(RHEAParams params) {
        super(params, "RHEAPlayer");
    }

    public RHEAPlayer(RHEAParams params, String name) {
        super(params, name);
    }

    @Override
    public RHEAParams getParameters() {
        return (RHEAParams) parameters;
    }
    @Override
    public void initializePlayer(AbstractGameState state) {
        MASTStatistics = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++)
            MASTStatistics.add(new HashMap<>());
        population = new ArrayList<>();
    }

    @Override
    public AbstractAction _getAction(AbstractGameState stateObs, List<AbstractAction> possibleActions) {
        ElapsedCpuTimer timer = new ElapsedCpuTimer();  // New timer for this game tick
        timer.setMaxTimeMillis(parameters.budget);
        numIters = 0;
        fmCalls = 0;
        copyCalls = 0;
        repairCount = 0;
        nonRepairCount = 0;
        RHEAParams params = getParameters();

        if (params.useMAST) {
            if (MASTStatistics == null) {
                MASTStatistics = new ArrayList<>();
                for (int i = 0; i < stateObs.getNPlayers(); i++)
                    MASTStatistics.add(new HashMap<>());
            } else {
                MASTStatistics = MASTStatistics.stream()
                        .map(m -> Utils.decay(m, params.discountFactor))
                        .collect(Collectors.toList());
            }
            mastPlayer = new MASTPlayer(null, 1.0, 0.0, System.currentTimeMillis(), 0.0);
            mastPlayer.setMASTStats(MASTStatistics);
        }
        // Initialise individuals
        if (params.shiftLeft && !population.isEmpty()) {
            population.forEach(i -> i.value = Double.NEGATIVE_INFINITY);  // so that any we don't have time to shift are ignored when picking an action
            for (RHEAIndividual genome : population) {
                if (!budgetLeft(timer)) break;
                System.arraycopy(genome.actions, 1, genome.actions, 0, genome.actions.length - 1);
                // we shift all actions along, and then rollout with repair
                genome.gameStates[0] = stateObs.copy();
                Pair<Integer, Integer> calls = genome.rollout(getForwardModel(), 0, getPlayerID(), true);
                fmCalls += calls.a;
                copyCalls += calls.b;
            }
        } else {
            population = new ArrayList<>();
            for (int i = 0; i < params.populationSize; ++i) {
                if (!budgetLeft(timer)) break;
                population.add(new RHEAIndividual(params.horizon, params.discountFactor, getForwardModel(), stateObs,
                        getPlayerID(), rnd, params.heuristic, params.useMAST ? mastPlayer : randomPlayer));
                fmCalls += population.get(i).length;
                copyCalls += population.get(i).length;
            }
        }

        population.sort(Comparator.naturalOrder());
        initTime = timer.elapsedMillis();
        // Run evolution
        while (budgetLeft(timer)) {
            runIteration();
        }

        timeTaken = timer.elapsedMillis();
        timePerIteration = numIters == 0 ? 0.0 : (timeTaken - initTime) / numIters;
        // Return first action of best individual
        AbstractAction retValue = population.get(0).actions[0];
        List<AbstractAction> actions = getForwardModel().computeAvailableActions(stateObs, params.actionSpace);
        if (!actions.contains(retValue))
            throw new AssertionError("Action chosen is not legitimate " + numIters + ", " + params.shiftLeft);
        return retValue;
    }

    private boolean budgetLeft(ElapsedCpuTimer timer) {
        RHEAParams params = getParameters();
        if (params.budgetType == PlayerConstants.BUDGET_TIME) {
            long remaining = timer.remainingTimeMillis();
            return remaining > params.breakMS;
        } else if (params.budgetType == PlayerConstants.BUDGET_FM_CALLS) {
            return fmCalls < params.budget;
        } else if (params.budgetType == PlayerConstants.BUDGET_COPY_CALLS) {
            return copyCalls < params.budget && numIters < params.budget;
        } else if (params.budgetType == PlayerConstants.BUDGET_FMANDCOPY_CALLS) {
            return (fmCalls + copyCalls) < params.budget;
        } else if (params.budgetType == PlayerConstants.BUDGET_ITERATIONS) {
            return numIters < params.budget;
        }
        throw new AssertionError("This should be unreachable : " + params.budgetType);
    }

    @Override
    public RHEAPlayer copy() {
        RHEAParams newParams = (RHEAParams) parameters.copy();
        newParams.setRandomSeed(rnd.nextInt());
        RHEAPlayer retValue = new RHEAPlayer(newParams, toString());
        retValue.setForwardModel(getForwardModel());
        return retValue;
    }

    private RHEAIndividual crossover(RHEAIndividual p1, RHEAIndividual p2) {
        switch (getParameters().crossoverType) {
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
            if (rnd.nextFloat() >= 0.5f) {
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

        switch (getParameters().selectionType) {
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
        for (int i = 0; i < getParameters().tournamentSize; ++i) {
            int rand = rnd.nextInt(population.size());

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
        int ran = rnd.nextInt(rankSum);
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
        //copy elites
        RHEAParams params = getParameters();
        List<RHEAIndividual> newPopulation = new ArrayList<>();
        for (int i = 0, max = Math.min(params.eliteCount, population.size()); i < max; ++i) {
            newPopulation.add(new RHEAIndividual(population.get(i)));
        }
        //crossover
        for (int i = 0; i < params.childCount; ++i) {
            RHEAIndividual[] parents = selectParents();
            RHEAIndividual child = crossover(parents[0], parents[1]);
            population.add(child);
        }

        for (RHEAIndividual individual : population) {
            Pair<Integer, Integer> calls = individual.mutate(getForwardModel(), getPlayerID(), params.mutationCount);
            fmCalls += calls.a;
            copyCalls += calls.b;
            repairCount += individual.repairCount;
            nonRepairCount += individual.nonRepairCount;
            if (params.useMAST)
                MASTBackup(individual.actions, individual.value, getPlayerID());
        }

        //sort
        population.sort(Comparator.naturalOrder());

        //best ones get moved to the new population
        for (int i = 0; i < Math.min(population.size(), params.populationSize - params.eliteCount); ++i) {
            newPopulation.add(population.get(i));
        }

        population = newPopulation;

        population.sort(Comparator.naturalOrder());
        // Update budgets
        numIters++;
    }


    protected void MASTBackup(AbstractAction[] rolloutActions, double delta, int player) {
        for (int i = 0; i < rolloutActions.length; i++) {
            AbstractAction action = rolloutActions[i];
            if (action == null)
                break;
            Pair<Integer, Double> stats = MASTStatistics.get(player).getOrDefault(action, new Pair<>(0, 0.0));
            stats.a++;  // visits
            stats.b += delta;   // value
            MASTStatistics.get(player).put(action.copy(), stats);
        }
    }


    @Override
    public void setBudget(int budget) {
        parameters.budget = budget;
        parameters.setParameterValue("budget", budget);
    }

    @Override
    public int getBudget() {
        return parameters.budget;
    }
}
