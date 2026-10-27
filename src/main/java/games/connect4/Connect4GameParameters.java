package games.connect4;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.*;

public class Connect4GameParameters extends TunableParameters<Game> {


    public int width = 7;
    public int height = 6;
    public int winCount = 4;

    public Connect4GameParameters() {
        addTunableParameter("width",    7, Arrays.asList(5, 6, 7, 8, 9, 10));
        addTunableParameter("height",   6, Arrays.asList(4, 5, 6, 7, 8));
        addTunableParameter("winCount", 4, Arrays.asList(3, 4, 5, 6));
        _reset();
    }

    @Override
    public void _reset() {
        width    = (int) getParameterValue("width");
        height   = (int) getParameterValue("height");
        winCount = (int) getParameterValue("winCount");
    }

    @Override
    protected AbstractParameters _copy() {
        return new Connect4GameParameters();
    }

    @Override
    public boolean _equals(Object o) {
        // width/height/winCount are tunable parameters, so they are already compared by
        // TunableParameters.equals (via currentValues). Calling super.equals here would recurse
        // infinitely, as TunableParameters.equals delegates back to _equals.
        return o instanceof Connect4GameParameters;
    }

    @Override
    public Game instantiate() {
        return GameType.Connect4.createGameInstance(2, this);
    }


}
