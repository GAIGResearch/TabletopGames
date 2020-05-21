package players;

import core.ForwardModel;
import core.actions.IAction;
import core.AbstractPlayer;
import core.AbstractGameState;
import core.observations.IObservation;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import players.heuristics.PandemicHeuristic;
import players.heuristics.StateHeuristic;
import utilities.ElapsedCpuTimer;
import utilities.Utils;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/* One Turn Look Ahead (4 actions)*/
public class OTLA extends AbstractPlayer {

    private Random random; // random generator for noise
    private StateHeuristic stateHeuristic;
    private ForwardModel fm;
    public double epsilon = 1e-6;
    public boolean rollN = true;
    public final static int time_to_act = 100; // in milliseconds

    public OTLA(ForwardModel fm){
        this.fm = fm;
        this.random = new Random();
    }

    public OTLA(ForwardModel fm, Random random)
    {
        this.fm = fm;
        this.random = random;
    }

    @Override
    public int getAction(IObservation observation, List<IAction> actions) {
        // todo execute n actions and return the first action only
        PandemicGameState gs = (PandemicGameState)observation;

        stateHeuristic = new PandemicHeuristic(gs);

        double maxQ = Double.NEGATIVE_INFINITY;
        IAction bestAction = null;

//        GameState gsCopy = gameState.copy(); // todo
        PandemicGameState gsCopy = gs;


        ElapsedCpuTimer ect = new ElapsedCpuTimer();
        ect.setMaxTimeMillis(time_to_act);

        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int numIters = 0;
        int remainingLimit = 5;
        boolean stop = false;

        while (!stop) {
            // todo this part requires generalization and avoid the usage of pandemic specific things
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();

            if (rollN) {
                // version 1, assume that the action space doesn't change over the turn
                // shuffle actions to avoid selecting the same actions twice
                Collections.shuffle(actions);
                int i = 0;
                while (gs.getTurnOrder().getCurrentPlayer(gs) == this.getPlayerID() || gsCopy.getGameStatus() !=  Utils.GameResult.GAME_ONGOING) {
//                    System.out.println("Active player = " + gameState.getActingPlayer() + " and acting player = "+ gameState.getActingPlayer());
                    // todo numbers are too large here
                    fm.next(gsCopy, actions.get(i%actions.size()));
                    System.out.println(i);
                    i++;
                }

                // stop condition
                numIters++;
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
                avgTimeTaken  = acumTimeTaken/numIters;
                remaining = ect.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
                double valState = stateHeuristic.evaluateState(gsCopy);
                double Q = noise(valState, this.epsilon, this.random.nextDouble());

                if (Q > maxQ) {
                    maxQ = Q;
                    bestAction = actions.get(0);
                }

            } else {
                // version 2, request the actions space again each time

            }
        }

        return actions.indexOf(bestAction);
    }

    public void rollRnd(AbstractGameState gs, IAction[] actions){
        for (IAction a: actions){
            fm.next(gs, a);
        }
    }


    public static double noise(double input, double epsilon, double random)
    {
        if(input != -epsilon) {
            return (input + epsilon) * (1.0 + epsilon * (random - 0.5));
        }else {
            return (input + epsilon) * (1.0 + epsilon * (random - 0.5));
        }
    }

    @Override
    public void initializePlayer(IObservation observation) {

    }

    @Override
    public void finalizePlayer(IObservation observation) {

    }


    @Override
    public void registerUpdatedObservation(IObservation observation) {

    }
}
