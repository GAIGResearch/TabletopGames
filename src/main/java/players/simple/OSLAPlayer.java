package players.simple;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
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

        double[] valState = new double[actions.size()];
        for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
            AbstractAction action = actions.get(actionIndex);
            AbstractGameState gsCopy = gs.copy();

            getForwardModel().next(gsCopy, action);

            if (gsCopy.getTurnOrder() instanceof SimultaneousTurnOrder) {
                advanceToEndOfRoundWithRandomActions(gsCopy);
            }

            if (heuristic != null) {
                valState[actionIndex] = heuristic.evaluateState(gsCopy, this.getPlayerID());
            } else {
                valState[actionIndex] = gsCopy.getHeuristicScore(this.getPlayerID());
            }

            double Q = noise(valState[actionIndex], this.epsilon, this.random.nextDouble());
       //     System.out.println(Arrays.stream(valState).mapToObj(v -> String.format("%1.3f", v)).collect(Collectors.joining("\t")));

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
