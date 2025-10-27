package players.groupB;

import core.AbstractGameState;
import players.basicMCTS.BasicMCTSPlayer;
import players.basicMCTS.BasicMCTSParams;

/**
 * OppAwareMCTSPlayer
 * Author: Ali Askari
 * MSc AI Coursework – ECS7032P
 *
 * Blueprint version:
 * - Extends BasicMCTSPlayer
 * - Integrates OpponentAwareHeuristic and FrequencyOpponentModel (to be expanded later)
 */
public class OppAwareMCTSPlayer extends BasicMCTSPlayer {

    private FrequencyOpponentModel opponentModel;
    private OpponentAwareHeuristic heuristic;

    // Default constructor
    public OppAwareMCTSPlayer() {
        super();
        this.opponentModel = new FrequencyOpponentModel();
        this.heuristic = new OpponentAwareHeuristic(opponentModel);
    }

    // Copy constructor using parameters
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
        // Placeholder – later will integrate real opponent-aware logic
        return heuristic.evaluate(gameState, playerID);
    }
}
