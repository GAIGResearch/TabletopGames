package players;

import actions.Action;
import actions.DoNothing;
import core.AIPlayer;
import core.GameState;
import players.heuristics.PandemicHeuristic;
import players.heuristics.StateHeuristic;

import java.util.List;
import java.util.Random;

public class OSLA implements AIPlayer {

    /**
     * Random generator for this agent.
     */
    private Random random;
    private StateHeuristic stateHeuristic;
    private boolean rndOpponentModel;
    public double epsilon = 1e-6;

    public OSLA(){
        this.random = new Random();
    }

    public OSLA(Random random)
    {
        this.random = random;
    }

    @Override
    public Action getAction(GameState gameState) {
        List<Action> actions = gameState.possibleActions();

        stateHeuristic = new PandemicHeuristic(gameState);
        rndOpponentModel = true;

        double maxQ = Double.NEGATIVE_INFINITY;
        Action bestAction = null;

        for (Action action : actions) {
            GameState gsCopy = gameState.copy();
            rollRnd(gsCopy, action);
            double valState = stateHeuristic.evaluateState(gsCopy);

            double Q = noise(valState, this.epsilon, this.random.nextDouble());

            if (Q > maxQ) {
                maxQ = Q;
                bestAction = action;
            }

        }

        return bestAction;
    }

    private void rollRnd(GameState gs, Action action)
    {
        // Pommerman version
        //Simple, all random first, then my position.
//        int nPlayers = 4;
//        Action[] actionsAll = new Action[4];
//
//        for(int i = 0; i < nPlayers; ++i)
//        {
//            if(i == getPlayerID() - Types.TILETYPE.AGENT0.getKey())
//            {
//                actionsAll[i] = act;
//            }else{
//                if(rndOpponentModel){
//                    int actionIdx = random.nextInt(gs.nActions());
//                    actionsAll[i] = Types.ACTIONS.all().get(actionIdx);
//                }else
//                {
//                    actionsAll[i] = new DoNothing();
//                }
//            }
//        }
//
//        gs.next(actionsAll);
        gs.next(action);
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
