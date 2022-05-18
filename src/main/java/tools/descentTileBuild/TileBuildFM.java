package tools.descentTileBuild;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.BoardNode;
import core.components.GridBoard;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static games.descent2e.DescentTypes.*;
import static utilities.Utils.getNeighbourhood;

public class TileBuildFM extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        TileBuildState tbs = (TileBuildState) firstState;
        int size = ((TileBuildParameters)firstState.getGameParameters()).defaultGridSize;

        // By default filled with plain, and with a null border
        BoardNode bn = new BoardNode(-1, "plain");
        tbs.tile = new GridBoard(size, size, bn.copy());
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == 0 || j == 0 || i == size - 1 || j == size - 1) {
                    tbs.tile.setElement(j, i, null);
                }
            }
        }
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
        terrains.addAll(TerrainType.getMarginStringTiles());
        terrains.add("block");

        // Only place "open" spaces outside edges of tile (should have only 1 orthogonal neighbour that's inside terrain)
        // and also disable actions for other types of terrains if placed next to "open" and that would be invalid "open" placement as above
        for (String t: terrains) {
            BoardNode bn = new BoardNode(-1, t);
            for (int i = 0; i < tbs.tile.getHeight(); i++) {
                for (int j = 0; j < tbs.tile.getWidth(); j++) {
                    if (t.equals("open")) {
                        int nInsideNeighbours = countInsideNeighboursOpenTile(j, i, tbs.tile.getWidth(), tbs.tile.getHeight(), tbs.tile).size();
                        if (nInsideNeighbours <= 1) {
                            actions.add(new SetGridValueAction(tbs.tile.getComponentID(), j, i, bn.copy()));
                        }
                    } else {
                        if (!t.equals("null")) {
                            List<Vector2D> neighbours = getNeighbourhood(j, i, tbs.tile.getWidth(), tbs.tile.getHeight(), false);
                            boolean anyOpen = false;
                            for (Vector2D n : neighbours) {
                                BoardNode el = tbs.tile.getElement(n.getX(), n.getY());
                                if (el != null && el.getComponentName().equals("open")) {
                                    anyOpen = true;
                                    // Check if this would be valid placement
                                    List<Vector2D> insideNeighbours = countInsideNeighboursOpenTile(n.getX(), n.getY(), tbs.tile.getWidth(), tbs.tile.getHeight(), tbs.tile);
                                    boolean add = true;
                                    for (Vector2D v : insideNeighbours) {
                                        if (v.getX() == j && v.getY() == i) {
                                            add = false;
                                            break;
                                        }
                                    }
                                    int nInsideNeighbours = insideNeighbours.size();
                                    if (add) nInsideNeighbours += 1;
                                    if (nInsideNeighbours <= 1) {
                                        actions.add(new SetGridValueAction(tbs.tile.getComponentID(), j, i, bn.copy()));
                                    }
                                }
                            }
                            if (!anyOpen) {
                                actions.add(new SetGridValueAction(tbs.tile.getComponentID(), j, i, bn.copy()));
                            }
                        } else {
                            actions.add(new SetGridValueAction(tbs.tile.getComponentID(), j, i, bn.copy()));
                        }
                    }
                }
            }
        }

        return actions;
    }

    private List<Vector2D> countInsideNeighboursOpenTile(int x, int y, int width, int height, GridBoard tile) {
        List<Vector2D> neighbours = getNeighbourhood(x, y, width, height, false);
        List<Vector2D> insideTileNeighbours = new ArrayList<>();
        for (Vector2D n: neighbours) {
            BoardNode t = tile.getElement(n.getX(), n.getY());
            if (t != null && TerrainType.isInsideTile(t.getComponentName())) {
                insideTileNeighbours.add(n.copy());
            }
        }
        return insideTileNeighbours;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new TileBuildFM();
    }
}
