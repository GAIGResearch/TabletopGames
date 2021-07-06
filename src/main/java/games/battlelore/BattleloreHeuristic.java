package games.battlelore;

import core.AbstractGameState;
import core.AbstractParameters;
import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;
import utilities.Utils;

public class BattleloreHeuristic extends TunableParameters implements IStateHeuristic
{
    @Override
    protected AbstractParameters _copy() {
        return null;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId)
    {
        BattleloreGameState gameState = (BattleloreGameState) gs;
        BattleloreGameParameters gameParams = (BattleloreGameParameters) gameState.getGameParameters();
        Utils.GameResult playerResult = gameState.getPlayerResults()[playerId];

        if (playerResult == Utils.GameResult.LOSE)
        {
            return -1;
        }
        if (playerResult == Utils.GameResult.WIN)
        {
            return 1;
        }

        if (gameState.getGamePhase() == BattleloreGameState.BattleloreGamePhase.CommandAndOrderStep)
        {

        }


        return 0;
    }

    @Override
    public Object instantiate() {
        return null;
    }

    @Override
    public void _reset()
    {

    }
}
