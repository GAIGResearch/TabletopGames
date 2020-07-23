package players;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import utilities.ElapsedCpuTimer;
import utilities.Utils;

import java.util.*;

import static core.Game.runOne;
import static games.GameType.TicTacToe;


public class RMHC extends AbstractPlayer {
    // Public Parameters
    public final static int time_to_act = 100; // in milliseconds

    // Parameters
    private int HORIZON = 10;
    private IStateHeuristic stateHeuristic = null;

    // Constants
    private final long BREAK_MS = 10;

    // Class vars
    private Individual bestIndividual;
    private final Random randomGenerator;

    // Budgets
    private ElapsedCpuTimer timer;
    private double avgTimeTaken = 0, acumTimeTaken = 0;
    private int numIters = 0;
    private boolean keepIterating = true;
    private long remaining;

    /**
     * Public constructor with state observation and time due.
     *
     */
    public RMHC() {
        randomGenerator = new Random();
        this.timer = new ElapsedCpuTimer();
    }

    public void setHORIZON(int HORIZON) {
        this.HORIZON = HORIZON;
    }

    public void setStateHeuristic(IStateHeuristic stateHeuristic) {
        this.stateHeuristic = stateHeuristic;
    }

    @Override
    public AbstractAction getAction(AbstractGameState stateObs){
        this.timer = new ElapsedCpuTimer();
        avgTimeTaken = 0;
        acumTimeTaken = 0;
        numIters = 0;
        remaining = timer.remainingTimeMillis();
        keepIterating = true;

        // INITIALISE POPULATION
        bestIndividual = new Individual(HORIZON, getForwardModel(), stateObs, getPlayerID(), randomGenerator);

        // RUN EVOLUTION
        remaining = timer.remainingTimeMillis();
        while (remaining > avgTimeTaken && remaining > BREAK_MS && keepIterating) {
            runIteration(stateObs);
            remaining = timer.remainingTimeMillis();
        }

        // RETURN ACTION
        return bestIndividual.actions[0];
    }

    /**
     * Run evolutionary process for one generation
     * @param stateObs - current game state
     */
    private void runIteration(AbstractGameState stateObs) {
        ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

        Individual newIndividual = new Individual(bestIndividual);
        newIndividual.mutate(getForwardModel(), getPlayerID());

        if (newIndividual.value > bestIndividual.value)
            bestIndividual = newIndividual;

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
