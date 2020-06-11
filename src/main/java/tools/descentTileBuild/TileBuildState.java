package tools.descentTileBuild;

import core.AbstractGameParameters;
import core.AbstractGameState;
import core.components.Component;
import core.components.GridBoard;
import core.turnorders.AlternatingTurnOrder;

import java.util.ArrayList;
import java.util.List;

public class TileBuildState extends AbstractGameState {

    GridBoard<String> tile;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     */
    public TileBuildState(AbstractGameParameters gameParameters, int nPlayers) {
        super(gameParameters, new AlternatingTurnOrder(nPlayers));
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(tile);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        TileBuildState copy = new TileBuildState(gameParameters, getNPlayers());
        copy.tile = tile.copy();
        return copy;
    }

    @Override
    protected double _getScore(int playerId) {
        return 0;
    }

    @Override
    protected void _reset() {
        tile = null;
    }
}
