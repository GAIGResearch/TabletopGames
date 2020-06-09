package games.descent;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.properties.PropertyInt;
import core.properties.PropertyStringArray;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static core.CoreConstants.neighbourHash;
import static core.CoreConstants.orientationHash;
import static games.descent.DescentConstants.connectionHash;

public class DescentForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        DescentGameState dgs = (DescentGameState) firstState;

        DescentGameData _data = dgs.getData();
        // 1. Read the graph board configuration for the master grid board
        GraphBoard config = _data.findGraphBoard("board1");

        // 2. Read all necessary tiles, which are all grid boards. Keep in a list.
        dgs.tiles = new HashMap<>();
        for (BoardNode bn: config.getBoardNodes()) {
            GridBoard tile = _data.findGridBoard(bn.getComponentName());
            dgs.tiles.put(bn.getComponentID(), tile);
        }

        // 3. Put together the master grid board
        // Find maximum board width and height, if all were put together side by side
        int width = 0;
        int height = 0;
        for (BoardNode bn: config.getBoardNodes()) {
            // Find width of this tile, according to orientation
            GridBoard tile = dgs.tiles.get(bn.getComponentID());
            int orientation = ((PropertyInt)bn.getProperty(orientationHash)).value;
            if (orientation % 2 == 0) {
                width += tile.getWidth();
                height += tile.getHeight();
            } else {
                width += tile.getHeight();
                height += tile.getWidth();
            }
        }

        // First tile will be in the center, board could expand in more directions
        width *= 2;
        height *= 2;

        // Create big board
        String[][] board = new String[height][width];
        dgs.tileReferences = new int[height][width];
        HashSet<BoardNode> drawn = new HashSet<>();
        addTilesToBoard(config.getBoardNodes().get(0), width/2, height/2, board, dgs.tiles, dgs.tileReferences, drawn);  // TODO: not all tiles might be connected
        dgs.masterBoard = new GridBoard<>(board, String.class);
        // TODO
    }

    private void addTilesToBoard(BoardNode bn, int x, int y, String[][] board, HashMap<Integer, GridBoard> tiles,
                                 int[][] tileReferences, HashSet<BoardNode> drawn) {
        if (!drawn.contains(bn)) {
            // Draw this tile in the big board at x, y location
            GridBoard tile = tiles.get(bn.getComponentID());
            String[][] tileGrid = (String[][]) tile.rotate(((PropertyInt)bn.getProperty(orientationHash)).value);
            int height = tileGrid.length;
            int width = tileGrid[0].length;

            HashMap<String, ArrayList<Vector2D>> openings = new HashMap<>();
            for (int i = y; i < y + height; i++) {
                for (int j = x; j < x + width; x++) {
                    board[i][j] = tileGrid[i-y][j-x];
                    tileReferences[i][j] = tile.getComponentID();

                    if (tileGrid[i-y][j-x].equalsIgnoreCase("open")) {
                        // Which side are we on?
                        // TODO: corners, non-grid tiles
                        String side;
                        if (i - y == 0) { // top
                            side = "N";
                        } else if (i - y == height) {  // bottom
                            side = "S";
                        } else if (j - x == 0) {
                            side = "W";
                        } else {
                            side = "E";
                        }
                        if (!openings.containsKey(side)) {
                            openings.put(side, new ArrayList<>());
                        }
                        openings.get(side).add(new Vector2D(j, i));
                    }
                }
            }
            drawn.add(bn);

            // Draw neighbours
            for (BoardNode neighbour: bn.getNeighbours()) {
                String[] neighs = ((PropertyStringArray) bn.getProperty(neighbourHash)).getValues();
                String[] connections = ((PropertyStringArray) bn.getProperty(connectionHash)).getValues();

                Vector2D topLeftCorner = null;

                // Find location to start drawing neighbour
                for (int i = 0; i < neighs.length; i++) {
                    if (neighs[i].equalsIgnoreCase(neighbour.getComponentName())) {
                        String conn = connections[i];

                        String side = conn.split("-")[0];
                        int countFromTop = Integer.parseInt(conn.split("-")[1]);
                        Vector2D open = openings.get(side).get(countFromTop);

                        // TODO: find orientation and opening connection from neighbour, check if they can match-up, generate top-left corner of neighbour from that
                        topLeftCorner = open;
                    }
                }

                if (topLeftCorner != null) {
                    addTilesToBoard(neighbour, topLeftCorner.getX(), topLeftCorner.getY(), board, tiles, tileReferences, drawn);
                }
            }
        }
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        action.execute(currentState);
        if (checkEndOfGame()) return;
        currentState.getTurnOrder().endPlayerTurn(currentState);
        // TODO
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        actions.add(new DoNothing());
        // TODO
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new DescentForwardModel();
    }

    private boolean checkEndOfGame() {
        // TODO
        return false;
    }
}
