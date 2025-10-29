package players.groupB;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.basicMCTS.BasicMCTSParams;
import players.basicMCTS.BasicMCTSPlayer;

/**
 * OppAwareMCTSPlayer
 * -------------------
 * Custom agent extending the BasicMCTSPlayer provided in TAG.
 * Adds an opponent modelling component (FrequencyOpponentModel)
 * and an evaluation heuristic (OpponentAwareHeuristic).
 *
 * This implementation is assignment-compliant:
 *  - It does not modify the TAG framework.
 *  - All new code lives inside players.groupB.
 *  - It can be run directly from a JSON config.
 *
 * Author: Ali Askari
 * MSc Artificial Intelligence (ECS7032P)
 */
public class OppAwareMCTSPlayer extends BasicMCTSPlayer {

    // Models opponent behaviour (frequency of actions)
    private final FrequencyOpponentModel opponentModel;
    // Evaluates draft states using self + opponent information
    private final OpponentAwareHeuristic heuristic;

    /** Default constructor used by the TAG factory and JSON configs */
    public OppAwareMCTSPlayer() {
        super(); // loads default BasicMCTS parameters
        this.opponentModel = new FrequencyOpponentModel();
        this.heuristic     = new OpponentAwareHeuristic(opponentModel);
    }

    /** Optional constructor for manual parameter tuning */
    public OppAwareMCTSPlayer(BasicMCTSParams params) {
        super(params);
        this.opponentModel = new FrequencyOpponentModel();
        this.heuristic     = new OpponentAwareHeuristic(opponentModel);
    }

    /** Standard deep copy used internally by TAG tournaments */
    @Override
    public OppAwareMCTSPlayer copy() {
        return new OppAwareMCTSPlayer((BasicMCTSParams) parameters.copy());
    }

    // ---------------------------------------------------------------------
    // Behavioural extensions
    // ---------------------------------------------------------------------

    /**
     * Returns a heuristic evaluation of the given state.
     * This can be called during rollouts or when choosing actions.
     */
    public double evaluateState(AbstractGameState gameState, int playerId) {
        return heuristic.evaluate(gameState, playerId);
    }

    /**
     * Call this when an opponent performs an observable action.
     * Updates the frequency-based opponent model.
     */
    public void onOpponentAction(int opponentId, AbstractAction action) {
        if (action != null) {
            opponentModel.updateModel(opponentId, action.toString());
        }
    }

    /**
     * Call this periodically (e.g., before your move) to update
     * the opponent model based on the current game state.
     * Currently this is a lightweight hook that can be expanded later.
     */
    public void onObservedState(AbstractGameState gameState) {
        opponentModel.updateFromGameState(gameState);
    }
    @Override
    public String toString() {
        return "OppAwareMCTS";
    }

}
