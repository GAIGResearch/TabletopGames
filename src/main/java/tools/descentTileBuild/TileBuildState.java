package tools.descentTileBuild;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.turnorders.AlternatingTurnOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TileBuildState extends AbstractGameState {

    GridBoard<String> tile;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     */
    public TileBuildState(AbstractParameters gameParameters, int nPlayers) {
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
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<>();
    }

    @Override
    protected void _reset() {
        tile = null;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TileBuildState)) return false;
        if (!super.equals(o)) return false;
        TileBuildState that = (TileBuildState) o;
        return Objects.equals(tile, that.tile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tile);
    }
}
