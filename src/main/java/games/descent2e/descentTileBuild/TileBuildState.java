package games.descent2e.descentTileBuild;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import games.GameType;
import utilities.Pathfinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TileBuildState extends AbstractGameState {

    GridBoard tile;

    Pathfinder pathfinder;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     */
    public TileBuildState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return null;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>() {{
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
    protected double _getHeuristicScore(int playerId) {
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        return 0;
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<>();
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TileBuildState that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(tile, that.tile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tile);
    }
}
