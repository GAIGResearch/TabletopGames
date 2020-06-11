package minigames.descentTileBuild;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.GridBoard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static games.descent.DescentTypes.*;

public class TileBuildFM extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        TileBuildState tbs = (TileBuildState) firstState;
        int size = ((TileBuildParameters)firstState.getGameParameters()).defaultGridSize;
        tbs.tile = new GridBoard<>(size, size, String.class, "plain");
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        action.execute(currentState);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        TileBuildState tbs = (TileBuildState) gameState;

        HashSet<String> terrains = TerrainType.getWalkableStringTiles();
        terrains.add("open");
        terrains.add("block");
        terrains.add("null");

        // TODO: only place "open" spaces outside edges of tile (should have only 1 orthogonal neighbour that's inside terrain)
        // and also disable actions for other types of terrains if placed next to "open" and that would be invalid "open" placement as above
        for (String t: terrains) {
            for (int i = 0; i < tbs.tile.getHeight(); i++) {
                for (int j = 0; j < tbs.tile.getWidth(); j++) {
                    actions.add(new SetGridValueAction<>(tbs.tile.getComponentID(), j, i, t));
                }
            }
        }

        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new TileBuildFM();
    }
}
