package games.chinesecheckers;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.chinesecheckers.components.CCNode;
import games.chinesecheckers.components.Peg;

import java.util.List;

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
        Peg.Colour playercolour = params.playerColours.get(state.getNPlayers())[state.getCurrentPlayer()];
        Peg.Colour oppositeColour = params.boardOpposites.get(playercolour);

        int[] homeNodes = params.colourIndices.get(playercolour);
        int[] targetNodes = params.colourIndices.get(oppositeColour);

        // give one point for each peg in the target area of the opposite colour
        // and subtract one point for each peg in the player's own starting area

        int score = 0;
        List<CCNode> nodes = state.getStarBoard().getBoardNodes();
        for (int i : homeNodes) {
            if (nodes.get(i).isNodeOccupied() && nodes.get(i).getOccupiedPeg().getColour() == playercolour) {
                score--;
            }
        }
        for (int i : targetNodes) {
            if (nodes.get(i).isNodeOccupied() && nodes.get(i).getOccupiedPeg().getColour() == playercolour) {
                score++;
            }
        }
        return score;
    }

    @Override
    public Object instantiate() {
        return this._copy();
    }

    @Override
    public void _reset() {
    }
}
