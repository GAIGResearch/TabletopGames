package players.simple;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import core.turnorders.StandardTurnOrder;

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
        int playerID = gs.getCurrentPlayer();

        double[] valState = new double[actions.size()];
        for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
            AbstractAction action = actions.get(actionIndex);
            AbstractGameState gsCopy = gs.copy();

            getForwardModel().next(gsCopy, action);

            if (gsCopy.getTurnOrder() instanceof StandardTurnOrder) {
                advanceToEndOfRoundWithRandomActions(gsCopy, playerID);
            }

            if (heuristic != null) {
                valState[actionIndex] = heuristic.evaluateState(gsCopy, playerID);
            } else {
                valState[actionIndex] = gsCopy.getHeuristicScore(playerID);
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

    @Override
    public OSLAPlayer copy() {
        return new OSLAPlayer(heuristic, new Random(random.nextInt()));
    }

    private void advanceToEndOfRoundWithRandomActions(AbstractGameState gsCopy, int startingPlayer) {
        // we assume that every other player now has to make a decision
        RandomPlayer rnd = new RandomPlayer(random);
        AbstractForwardModel fm = getForwardModel();
        if (gsCopy.getCurrentPlayer() == startingPlayer) {
            // first get to the end of our actions
            while (gsCopy.getCurrentPlayer() == startingPlayer && gsCopy.isNotTerminal()) {
                AbstractAction action = rnd.getAction(gsCopy, fm.computeAvailableActions(gsCopy));
                fm.next(gsCopy, action);
            }
        }
        // then each other player gets their round
        if (gsCopy.isNotTerminal()) {
            for (int p = 0; p < gsCopy.getNPlayers() - 1; p++) {
                int currentPlayer = gsCopy.getCurrentPlayer();
                if (currentPlayer == startingPlayer) {
                    throw new AssertionError("Not expecting to return to player " + getPlayerID());
                }
                while (gsCopy.getCurrentPlayer() == currentPlayer && gsCopy.isNotTerminal()) {
                    AbstractAction action = rnd.getAction(gsCopy, fm.computeAvailableActions(gsCopy));
                    fm.next(gsCopy, action);
                }
            }
        }
    }
}
