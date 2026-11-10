package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;

import java.util.List;
import java.util.Random;

import static utilities.Utils.noise;

public class OSLAPlayer extends AbstractPlayer {

    // Heuristic used for the agent
    IStateHeuristic heuristic;

    public OSLAPlayer(Random random) {
        super(null, "SuperOSLA");
        this.rnd = random;
    }

    public OSLAPlayer() {
        this(new Random());
    }

    public OSLAPlayer(IStateHeuristic heuristic) {
        this(heuristic, new Random());
    }

    public OSLAPlayer(IStateHeuristic heuristic, Random random) {
        this(random);
        this.heuristic = heuristic;
        setName("OSLA");
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gs, List<AbstractAction> actions) {
        double maxQ = Double.NEGATIVE_INFINITY;
        AbstractAction bestAction = null;
        double[] valState = new double[actions.size()];
        int playerID = gs.getCurrentPlayer();

        for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
            AbstractAction action = actions.get(actionIndex);
            AbstractGameState gsCopy = gs.copy();
            rollNextGameState(gsCopy, action.copy());

            if (heuristic != null) {
                valState[actionIndex] = heuristic.evaluateState(gsCopy, playerID);
            } else {
                valState[actionIndex] = gsCopy.getHeuristicScore(playerID);
            }

            double Q = noise(valState[actionIndex], getParameters().noiseEpsilon, rnd.nextDouble());

            if (Q > maxQ || bestAction == null) {
                maxQ = Q;
                bestAction = action;
            }
        }

        return bestAction;
    }

    // Override this when need to roll toward endRound or endTurn (not just one step)
    protected void rollNextGameState(AbstractGameState gs, AbstractAction a) {
        getForwardModel().next(gs, a);
    }

    @Override
    public OSLAPlayer copy() {
        OSLAPlayer retValue = new OSLAPlayer(heuristic, new Random(rnd.nextInt()));
        retValue.setForwardModel(getForwardModel());
        retValue.setName(toString());
        return retValue;
    }

}
