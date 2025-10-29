package players.groupB;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;

/**
 * OppAwareMCTSPlayer
 * Extends BasicMCTSPlayer with opponent modelling and adaptive heuristic.
 */
public class OppAwareMCTSPlayer extends BasicMCTSPlayer {

    private final FrequencyOpponentModel opponentModel;
    private final OpponentAwareHeuristic heuristic;

    public OppAwareMCTSPlayer(BasicMCTSParams params) {
        super(params);
        this.opponentModel = new FrequencyOpponentModel();
        this.heuristic = new OpponentAwareHeuristic(opponentModel);
    }

    @Override
    public OppAwareMCTSPlayer copy() {
        return new OppAwareMCTSPlayer((BasicMCTSParams) parameters.copy());
    }


    public double getHeuristicValue(AbstractGameState gameState, int playerID) {
        return heuristic.evaluate(gameState, playerID);
    }

    /**
     * Example hook — if available, track opponent actions after rollouts.
     */
    public void onOpponentAction(int opponentId, AbstractAction action) {
        opponentModel.updateModel(opponentId, action.toString());
    }
}
