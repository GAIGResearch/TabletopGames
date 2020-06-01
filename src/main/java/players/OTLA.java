package players;

import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.AbstractPlayer;
import core.AbstractGameState;
import core.interfaces.IObservation;
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
    public double epsilon = 1e-6;
    public boolean rollN = true;
    public final static int time_to_act = 100; // in milliseconds

    public OTLA(){
        this.random = new Random();
    }

    public OTLA(Random random)
    {
        this.random = random;
    }

    @Override
    public int getAction(AbstractGameState observation) {
        // todo execute n actions and return the first action only
        PandemicGameState gs = (PandemicGameState)observation;

        stateHeuristic = new PandemicHeuristic(gs);

        double maxQ = Double.NEGATIVE_INFINITY;
        AbstractAction bestAction = null;

        AbstractGameState gsCopy = gs.copy();
        List<AbstractAction> actions = gsCopy.getActions();


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
                while (gs.getTurnOrder().getCurrentPlayer(gs) == this.getPlayerID() ){ // || gsCopy.getGameStatus() !=  Utils.GameResult.GAME_ONGOING) {
//                    System.out.println("Active player = " + gameState.getActingPlayer() + " and acting player = "+ gameState.getActingPlayer());
                    // todo numbers are too large here
                    getForwardModel().next(gsCopy, actions.get(i%actions.size()));
                    System.out.println(i);
                    i++;
                }

                // stop condition
                numIters++;
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
                avgTimeTaken  = acumTimeTaken/numIters;
                remaining = ect.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
                double valState = stateHeuristic.evaluateState((PandemicGameState)gsCopy);
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

    public void rollRnd(AbstractGameState gs, AbstractAction[] actions){
//        for (AbstractAction a: actions){
//            gs.next(a);
//        }
    }


    public static double noise(double input, double epsilon, double random)
    {
        if(input != -epsilon) {
            return (input + epsilon) * (1.0 + epsilon * (random - 0.5));
        }else {
            return (input + epsilon) * (1.0 + epsilon * (random - 0.5));
        }
    }
}
