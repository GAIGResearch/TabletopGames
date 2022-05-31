package tools.descentTileBuild;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.BoardNode;
import core.components.GridBoard;
import core.properties.PropertyVector2D;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static core.CoreConstants.coordinateHash;
import static games.descent2e.DescentTypes.*;
import static utilities.Utils.getNeighbourhood;

public class TileBuildFM extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        TileBuildState tbs = (TileBuildState) firstState;
        int size = ((TileBuildParameters)firstState.getGameParameters()).defaultGridSize;

        // By default filled with plain, and with a null border
        //BoardNode bn = new BoardNode(-1, "plain");
        tbs.tile = new GridBoard(size, size);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                //Borders:
                if (i == 0 || j == 0 || i == size - 1 || j == size - 1) {
                    tbs.tile.setElement(j, i, null);
                }else{
                    BoardNode bn = new BoardNode(-1, "plain");
                    tbs.tile.setElement(j, i, bn);
                    bn.setProperty(new PropertyVector2D("coordinates", new Vector2D(j,i)));
                }
            }
        }

        buildNeighbours(tbs);

    }

    private void buildNeighbours(TileBuildState tbs)
    {
        int w = tbs.tile.getWidth();
        int h = tbs.tile.getHeight();

        // Adding neighbours. For all tiles in the board
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) {

            BoardNode node = tbs.tile.getElement(i, j);
            // The tile must not be null (they have no neighbours)

            if (node != null) {
                node.clearNeighbours();

                // Go through all adjacent positions (x+-1, y+-1)
                for (int nx = i - 1; nx <= i + 1; nx++) for (int ny = j - 1; ny <= j + 1; ny++) {

                    // Adjacent candidates are not itself,
                    if (nx == i && ny == j) continue;

                    // not outside the board...
                    if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                        BoardNode bnN = tbs.tile.getElement(nx, ny);
                        if (bnN != null)  // ... and not null.
                        {
                            double cost = 1.0; //TODO: Cost needs be adjusted to tile type.
                            node.addNeighbour(bnN, cost);
                            System.out.println("Added neighbour: (" + i + "," + j + ") -> (" + nx + "," + ny + ")");
                        }
                    }
                }
            }
        }

    }


    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        action.execute(currentState);

        // TODO: Shotgun approach, this regenerates ALL neighbours. Can be made more efficient if needed.
        buildNeighbours((TileBuildState) currentState);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        TileBuildState tbs = (TileBuildState) gameState;

        HashSet<String> terrains = TerrainType.getWalkableStringTerrains();
        terrains.addAll(TerrainType.getMarginStringTerrains());
        terrains.add("block");

        // Only place "open" spaces outside edges of tile (should have only 1 orthogonal neighbour that's inside terrain)
        // and also disable actions for other types of terrains if placed next to "open" and that would be invalid "open" placement as above
        for (String t: terrains) {
            //BoardNode bn = new BoardNode(-1, t);
            for (int i = 0; i < tbs.tile.getHeight(); i++) {
                for (int j = 0; j < tbs.tile.getWidth(); j++) {
                    if (t.equals("open")) {
                        int nInsideNeighbours = countInsideNeighboursOpenTile(j, i, tbs.tile.getWidth(), tbs.tile.getHeight(), tbs.tile).size();
                        if (nInsideNeighbours <= 1) {
                            actions.add(new SetGridValueAction(tbs.tile.getComponentID(), j, i, new BoardNode(-1, t)));
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
                                        actions.add(new SetGridValueAction(tbs.tile.getComponentID(), j, i, new BoardNode(-1, t)));
                                    }
                                }
                            }
                            if (!anyOpen) {
                                actions.add(new SetGridValueAction(tbs.tile.getComponentID(), j, i, new BoardNode(-1, t)));
                            }
                        } else {
                            actions.add(new SetGridValueAction(tbs.tile.getComponentID(), j, i, new BoardNode(-1, t)));
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
            if (t != null && TerrainType.isInsideTerrain(t.getComponentName())) {
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
