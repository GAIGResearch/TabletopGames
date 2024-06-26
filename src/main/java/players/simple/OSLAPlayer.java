package players.simple;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractGameStateWithTurnOrder;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.turnorders.StandardTurnOrder;

import java.util.List;
import java.util.Random;

import static utilities.Utils.noise;

public class OSLAPlayer extends AbstractPlayer {

    public OSLAPlayer() {
        this(new Random());
    }

    public OSLAPlayer(Random random) {
        super(null, "OSLA");
        this.rnd = random;
        this.parameters = new OSLAParameters();
    }

    public OSLAPlayer(OSLAParameters params) {
        super(null, "OSLA");
        this.rnd = new Random();
        this.parameters = params;
    }

    public OSLAPlayer(Random random, OSLAParameters params) {
        super(null, "OSLA");
        this.rnd = random;
        this.parameters = params;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gs, List<AbstractAction> actions) {
        double maxQ = Double.NEGATIVE_INFINITY;
        AbstractAction bestAction = null;
        double[] valState = new double[actions.size()];
        int playerID = gs.getCurrentPlayer();
        OSLAParameters params = (OSLAParameters) getParameters();

        for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
            AbstractAction action = actions.get(actionIndex);
            AbstractGameState gsCopy = gs.copy();
            getForwardModel().next(gsCopy, action);

            if (gs instanceof AbstractGameStateWithTurnOrder && ((AbstractGameStateWithTurnOrder)gsCopy).getTurnOrder() instanceof StandardTurnOrder) {
                advanceToEndOfRoundWithRandomActions(gsCopy, playerID);
            }

            if (params.getHeuristic() != null) {
                valState[actionIndex] = params.getHeuristic().evaluateState(gsCopy, playerID);
            } else {
                valState[actionIndex] = gsCopy.getHeuristicScore(playerID);
            }

            double Q = noise(valState[actionIndex], getParameters().noiseEpsilon, rnd.nextDouble());
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
        OSLAPlayer retValue = new OSLAPlayer(new Random(rnd.nextInt()), (OSLAParameters) parameters.copy());
        retValue.setForwardModel(getForwardModel().copy());
        return retValue;
    }

    private void advanceToEndOfRoundWithRandomActions(AbstractGameState gsCopy, int startingPlayer) {
        // we assume that every other player now has to make a decision
        RandomPlayer rnd = new RandomPlayer(this.rnd);
        AbstractForwardModel fm = getForwardModel();
        if (gsCopy.getCurrentPlayer() == startingPlayer) {
            // first get to the end of our actions
            while (gsCopy.getCurrentPlayer() == startingPlayer && gsCopy.isNotTerminal()) {
                AbstractAction action = rnd.getAction(gsCopy, fm.computeAvailableActions(gsCopy, rnd.parameters.actionSpace));
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
                    AbstractAction action = rnd.getAction(gsCopy, fm.computeAvailableActions(gsCopy, rnd.parameters.actionSpace));
                    fm.next(gsCopy, action);
                }
            }
        }
    }
}
