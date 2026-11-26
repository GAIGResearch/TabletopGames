package games.chinesecheckers;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.chinesecheckers.components.Peg;
import games.chinesecheckers.components.StarBoard;


public class CCHeuristic extends TunableParameters implements IStateHeuristic {

    @Override
    protected AbstractParameters _copy() {
        return new CCHeuristic();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof CCHeuristic;
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        CCGameState state = (CCGameState) gs;
        CCParameters params = (CCParameters) state.getGameParameters();

        // get player colour from parameters
        Peg.Colour playercolour = state.getPlayerColour(playerId);
        Peg.Colour oppositeColour = params.boardOpposites.get(playercolour);

        int[] targetNodes = params.colourIndices.get(playercolour);
        int referenceNode = targetNodes[0];

        // give one point for each peg in the target area of the opposite colour
        // and subtract one point for each peg in the player's own starting area


        double score = 0;
        StarBoard board = state.getStarBoard();
        for (int node = 0; node < board.nNodes(); node++) {
            Peg peg = board.getPeg(node);
            if (peg != null && peg.colour() == playercolour) {
                if (board.getBaseColour(node) == playercolour)
                    score += 1.0;
                else if (board.getBaseColour(node) == oppositeColour)
                    score -= 1.0;
                else {
                    int distanceToTarget = board.distanceBetween(node, referenceNode);
                    score += (Math.max(0, 10 - distanceToTarget)) / 10.0; // closer to target node gives more points
                }
            }
        }
        return score / 10.0; // normalize score to be between -1 and 1
    }

    @Override
    public Object instantiate() {
        return this._copy();
    }

    @Override
    public void _reset() {
    }
}
