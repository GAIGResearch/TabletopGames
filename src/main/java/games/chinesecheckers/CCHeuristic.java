package games.chinesecheckers;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.chinesecheckers.components.CCNode;
import games.chinesecheckers.components.Peg;

import java.util.List;

public class CCHeuristic extends TunableParameters implements IStateHeuristic  {

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
        CoreConstants.GameResult playerResult = gs.getPlayerResults()[playerId];
        CCGameState state = (CCGameState) gs;

        int score = 0;
        if(playerId == 0){
            List<CCNode> nodes = state.getStarBoard().getBoardNodes();
            for(int i = 111; i <= 120; i++){
                if(nodes.get(i).isNodeOccupied() && nodes.get(i).getOccupiedPeg().getColour() == Peg.Colour.purple){
                    score++;
                }
            }
        }
        if(playerId == 1){
            List<CCNode> nodes = state.getStarBoard().getBoardNodes();
            for(int i = 0; i <= 9; i++){
                if(nodes.get(i).isNodeOccupied() && nodes.get(i).getOccupiedPeg().getColour() == Peg.Colour.red){
                    score++;
                }
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
