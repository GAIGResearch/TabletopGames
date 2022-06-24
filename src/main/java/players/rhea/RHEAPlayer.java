package players.rhea;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import evaluation.ParameterSearch;
import evaluation.RoundRobinTournament;
import org.jetbrains.annotations.NotNull;
import players.PlayerConstants;
import players.human.ActionController;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static core.Game.runOne;
import static games.GameType.LoveLetter;
import static games.GameType.Pandemic;

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
        long t = timer.remainingTimeMillis();
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
        int iterations = 0;
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
        return population.get(0).actions[0];
    }

    @Override
    public RHEAPlayer copy() {
        RHEAParams newParams = (RHEAParams) params.copy();
        newParams.setRandomSeed(randomGenerator.nextInt());
        return new RHEAPlayer(newParams);
    }

    private RHEAIndividual crossover(RHEAIndividual p1, RHEAIndividual p2)
    {
        switch (params.crossoverType)
        {
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

    private RHEAIndividual uniformCrossover(RHEAIndividual p1, RHEAIndividual p2)
    {
        RHEAIndividual child = new RHEAIndividual(p1);
        copyCalls += child.length;
        int min = (p1.length > p2.length ? p2.length : p1.length);
        for(int i = 0; i < min; ++i)
        {
            if(randomGenerator.nextFloat() >= 0.5f)
            {
                child.actions[i] = p2.actions[i];
                child.gameStates[i] = p2.gameStates[i].copy();
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
            child.gameStates[child.length - 1 - i] = p2.gameStates[p2.length - 1 - i].copy();
        }
        return child;
    }

    private RHEAIndividual twoPointCrossover(RHEAIndividual p1, RHEAIndividual p2)
    {
        RHEAIndividual child = new RHEAIndividual(p1);
        copyCalls += child.length;
        int tailLength = Math.min(p1.length, p2.length) / 3;
        for(int i = 0; i < tailLength; ++i)
        {
            child.actions[i] = p2.actions[i];
            child.gameStates[i] = p2.gameStates[i].copy();
            child.actions[child.length - 1 - i] = p2.actions[p2.length - 1 - i];
            child.gameStates[child.length - 1 - i] = p2.gameStates[p2.length - 1 - i].copy();
        }
        return child;
    }

    RHEAIndividual[] selectParents()
    {
        RHEAIndividual[] parents = new RHEAIndividual[2];

        switch (params.selectionType)
        {
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

    RHEAIndividual tournamentSelection()
    {
        int rand = randomGenerator.nextInt(population.size() - params.tournamentSize);
        RHEAIndividual best = population.get(rand);
        for(int i = rand + 1; i < rand + params.tournamentSize; ++i)
        {
            RHEAIndividual current = population.get(i);
            if(current.value > best.value)
                best = current;
        }
        return best;
    }

    RHEAIndividual rankSelection()
    {
        population.sort(Comparator.naturalOrder());
        int rankSum = 0;
        for(int i = 0; i < population.size(); ++i)
            rankSum += i + 1;
        int ran = randomGenerator.nextInt(rankSum);
        int p = 0;
        for(int i = 0; i < population.size(); ++i)
        {
            p += population.size() - (i);
            if(p >= ran)
                return population.get(i);
        }
        throw new RuntimeException("Random Generator generated an invalid goal, goal: " + ran + " p: " + p);
    }
    /**
     * Run evolutionary process for one generation
     * @param stateObs - current game state
     */
    private void runIteration(AbstractGameState stateObs) {
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
        //selection
        population.sort(Comparator.naturalOrder());
        //copy elites
        ArrayList<RHEAIndividual> newPopulation = new ArrayList<RHEAIndividual>();
        int statesUpdated = 0;
        for(int i = 0; i < params.eliteCount; ++i)
        {
            newPopulation.add(new RHEAIndividual(population.get(i))); // todo: possibly cheating, needs to update copy calls?
        }
        //crossover
        for(int i = 0; i < params.childCount; ++i)
        {
            RHEAIndividual[] parents = selectParents();
            RHEAIndividual child = crossover(parents[0], parents[1]);
            //statesUpdated += child.mutate(getForwardModel(), getPlayerID());
            //fmCalls += child.rollout(child.gameStates[0], getForwardModel(), 0, child.actions.length, getPlayerID());
            population.add(child);
        }

        ElapsedCpuTimer test = new ElapsedCpuTimer();
        // mutation
        for(int i = 0; i < population.size(); ++i)
        {
            statesUpdated += population.get(i).mutate(getForwardModel(), getPlayerID());
        }

        //sort
        population.sort(Comparator.naturalOrder());

        //best ones get moved to the new population
        for(int i = 0; i < params.populationSize - params.eliteCount; ++i)
        {
            newPopulation.add(population.get(i));
        }

        population = newPopulation;

        population.sort(Comparator.naturalOrder());
        fmCalls += statesUpdated;
        copyCalls += statesUpdated; // as mutate() copyies once each time it applies the forward model
        // Update budgets
        numIters++;
        acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
        avgTimeTaken = acumTimeTaken / numIters;
    }

    public static void main(String[] args)
    {
        /* 1. Action controller for GUI interactions. If set to null, running without visuals. */
        ActionController ac = null; //null;
        /* 2. Game seed */
        //
        //
        Optimize();
        //RunFast();
        //RoundRobin();
        //Visual(args);
    }

    private static void RoundRobin() {
        String[] args;
        
        args = new String[6];
        args[0] = "game=LoveLetter";
        args[1] = "nPlayers=4";
        args[2] = "players=C:\\Users\\Me\\Documents\\GitHub\\TabletopGames2\\json";
        args[3] = "gamesPerMatchup=100";
        args[4] = "selfPlay=false";
        args[5] = "mode=exhaustive";
        RoundRobinTournament.main(args);
    }

    private static void Optimize() {
        String[] args;
        long seed = System.currentTimeMillis(); //0
        args = new String[6];
        args[0] = "C:\\Users\\Me\\Documents\\GitHub\\TabletopGames2\\optimization\\rheaoptimization.json";
        args[1] = "100";
        args[2] = "LoveLetter";
        args[3] = "nPlayers=4";
        args[4] = "opponent=C:\\Users\\Me\\Documents\\GitHub\\TabletopGames2\\json\\osla.json";
        //args[4] = "opponent=coop";

        args[5] = "repeat=10";
        ParameterSearch.main(args);
    }

    private static void RunFast() {
        ArrayList<AbstractPlayer> players = new ArrayList<>();
        players.add(new MCTSPlayer());
        players.add(new MCTSPlayer());
        players.add(new MCTSPlayer());
        players.add(new MCTSPlayer());
        /* 4. Run! */
        int rheaWonGames = 0;
        int mctsWonGames = 0;
        int rmhcWonGames = 0;
        int oslaWonGames = 0;
        for (int i = 0; i < 1000; i++) {
            System.out.println(i);
            Game game = runOne(Pandemic, null, players, 0, false, null, null, 0);
        }
        System.out.println("RHEA won: " + rheaWonGames);
        System.out.println("MCTS won: " + mctsWonGames);
        System.out.println("RMHC won: " + rmhcWonGames);
        System.out.println("OSLA won: " + oslaWonGames);

    }

    private static void Visual(String[] args)
    {
        gui.Frontend.main(args);
    }
}
