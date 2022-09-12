package players.rmhc;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import players.PlayerConstants;
import utilities.ElapsedCpuTimer;

import java.util.*;


public class RMHCPlayer extends AbstractPlayer {

    RMHCParams params;
    private Individual bestIndividual;
    private final Random randomGenerator;
    IStateHeuristic heuristic;

    // Budgets
    private double avgTimeTaken = 0, acumTimeTaken = 0;
    private int numIters = 0;
    private int fmCalls = 0;
    private int copyCalls = 0;

    public RMHCPlayer() {
        this(System.currentTimeMillis());
    }

    public RMHCPlayer(RMHCParams params) {
        randomGenerator = new Random(params.getRandomSeed());
        this.params = params;
        heuristic = params.gameHeuristic;
        setName("RMHC");
    }

    public RMHCPlayer(long seed) {
        this(new RMHCParams(seed));
    }

    public RMHCPlayer(IStateHeuristic heuristic) {
        this(System.currentTimeMillis());
        this.heuristic = heuristic;
    }

    public RMHCPlayer(RMHCParams params, IStateHeuristic heuristic) {
        this(params);
        this.heuristic = heuristic;
    }

    public RMHCPlayer(long seed, IStateHeuristic heuristic) {
        this(new RMHCParams(seed));
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

        // Initialise individual
        bestIndividual = new Individual(params.horizon, params.discountFactor, getForwardModel(), stateObs, getPlayerID(), randomGenerator, heuristic);
        fmCalls += bestIndividual.length;

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
    public RMHCPlayer copy() {
        RMHCParams newParams = (RMHCParams) params.copy();
        newParams.setRandomSeed(randomGenerator.nextInt());
        return new RMHCPlayer(newParams);
    }

    /**
     * Run evolutionary process for one generation
     *
     * @param stateObs - current game state
     */
    private void runIteration(AbstractGameState stateObs) {
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

        // Create new individual through mutation
        Individual newIndividual = new Individual(bestIndividual);
        copyCalls += newIndividual.length;
        int statesUpdated = newIndividual.mutate(getForwardModel(), getPlayerID());
        fmCalls += statesUpdated;
        copyCalls += statesUpdated; // as mutate() copyies once each time it applies the forward model

        // Keep new individual if better than current
        if (newIndividual.value > bestIndividual.value)
            bestIndividual = newIndividual;

        // Update budgets
        numIters++;
        acumTimeTaken += (elapsedTimerIteration.elapsedMillis());
        avgTimeTaken = acumTimeTaken / numIters;
    }

//    public static void main(String[] args){
//        /* 1. Action controller for GUI interactions. If set to null, running without visuals. */
//        ActionController ac = new ActionController(); //null;
//
//        /* 2. Game seed */
//        long seed = System.currentTimeMillis(); //0;
//
//        /* 3. Set up players for the game */
//        ArrayList<AbstractPlayer> players = new ArrayList<>();
//        players.add(new RandomPlayer(new Random()));
//        players.add(new RMHC());
//
//        /* 4. Run! */
//        int wonGames = 0;
//        for (int i = 0; i < 1000; i++) {
//            Game game = runOne(TicTacToe, players, seed, ac, false);
//            if (game.getGameState().getPlayerResults()[1] == Utils.GameResult.WIN) {
//                wonGames += 1;
//            }
//        }
//        System.out.println(wonGames);
//    }

}
