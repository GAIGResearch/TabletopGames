package games.dotsboxes;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.Arrays;
import java.util.Objects;

public class DBParameters extends TunableParameters {
    int gridWidth = 7;
    int gridHeight = 5;
    int disallowThreeBoxCreationUntilMove = 0;

    public DBParameters() {
        addTunableParameter("gridWidth", 7, Arrays.asList(5, 7, 11, 15, 19));
        addTunableParameter("gridHeight", 5, Arrays.asList(5, 7, 11, 15, 19));
        addTunableParameter("disallowThreeBoxCreationUntilMove", 0);
        _reset();
    }

    @Override
    public void _reset() {
        gridWidth = (int) getParameterValue("gridWidth");
        gridHeight = (int) getParameterValue("gridHeight");
        disallowThreeBoxCreationUntilMove = (int) getParameterValue("disallowThreeBoxCreationUntilMove");
    }

    @Override
    protected AbstractParameters _copy() {
        return new DBParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DBParameters)) return false;
        DBParameters that = (DBParameters) o;
        return gridWidth == that.gridWidth &&
                gridHeight == that.gridHeight &&
                disallowThreeBoxCreationUntilMove == that.disallowThreeBoxCreationUntilMove;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.DotsAndBoxes, new DBForwardModel(), new DBGameState(this, GameType.DotsAndBoxes.getMinPlayers()));
    }
}
