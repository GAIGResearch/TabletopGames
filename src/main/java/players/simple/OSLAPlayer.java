package players.simple;

import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.AbstractPlayer;
import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import core.turnorders.SimultaneousTurnOrder;

import java.util.List;
import java.util.Random;

import static utilities.Utils.noise;

public class OSLAPlayer extends AbstractPlayer {

    private Random random; // random generator for noise
    public double epsilon = 1e-6;
    // Heuristics used for the agent
    IStateHeuristic heuristic;

    public OSLAPlayer(Random random) {
        this.random = random;
        setName("OSLA");
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
    public AbstractAction getAction(AbstractGameState gs, List<AbstractAction> actions) {

        double maxQ = Double.NEGATIVE_INFINITY;
        AbstractAction bestAction = null;

        for (AbstractAction action : actions) {
            AbstractGameState gsCopy = gs.copy();

            getForwardModel().next(gsCopy, action);

            if (gsCopy.getTurnOrder() instanceof SimultaneousTurnOrder) {
                advanceToEndOfRoundWithRandomActions(gsCopy);
            }

            double valState;
            if (heuristic != null) {
                valState = heuristic.evaluateState(gsCopy, this.getPlayerID());
            } else {
                valState = gsCopy.getHeuristicScore(this.getPlayerID());
            }

            double Q = noise(valState, this.epsilon, this.random.nextDouble());

            if (Q > maxQ) {
                maxQ = Q;
                bestAction = action;
            }
        }

        return bestAction;
    }

    private void advanceToEndOfRoundWithRandomActions(AbstractGameState gsCopy) {
        // we assume that every other player now has to make a decision
        RandomPlayer rnd = new RandomPlayer(random);
        AbstractForwardModel fm = getForwardModel();
        for (int p = 0; p < gsCopy.getNPlayers() - 1; p++) {
            if (gsCopy.getCurrentPlayer() == getPlayerID()) {
                throw new AssertionError("Not expecting to return to player " + getPlayerID());
            }
            AbstractAction action = rnd.getAction(gsCopy, fm.computeAvailableActions(gsCopy));
            fm.next(gsCopy, action);
        }
    }
}
