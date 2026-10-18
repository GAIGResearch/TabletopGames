package games.connect4;

import core.components.BoardNode;
import core.components.GridBoard;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.Pair;

import java.util.LinkedList;

/**
 * Serialization of {@link Connect4GameState} to and from JSON, following the same pattern as
 * {@code games.backgammon.BGStateJSON}: the shared AbstractGameState fields (parameters, game phase,
 * turn counters, ...) are handled by {@link core.AbstractGameState#abstractGameStateToJSON()} /
 * {@link core.AbstractGameState#loadAbstractGameStateFromJSON}, and this class adds the Connect4
 * specific board state.
 * <p>
 * Component identity is preserved on reload: occupied cells reference the shared static
 * {@link Connect4Constants#playerMapping} markers (so their componentIDs and any subsequent
 * getComponentById lookups line up), and the board itself is rebuilt with its original componentID
 * so that the restored state is {@code equals()} to the original.
 */
public class Connect4StateJSON {

    @SuppressWarnings("unchecked")
    public static JSONObject toJSON(Connect4GameState state) {
        JSONObject json = new JSONObject();
        json.put("class", "games.connect4.Connect4GameState");

        // this includes parameters and game phase
        json.put("abstractGameState", state.abstractGameStateToJSON());

        GridBoard board = state.gridBoard;
        json.put("gridBoardID", board.getComponentID());
        json.put("width", board.getWidth());
        json.put("height", board.getHeight());

        // The board is stored row by row (y outer, x inner). Each cell records its name: which
        // marker, or the empty cell.
        JSONArray rows = new JSONArray();
        for (int y = 0; y < board.getHeight(); y++) {
            JSONArray row = new JSONArray();
            for (int x = 0; x < board.getWidth(); x++) {
                BoardNode cell = board.get(x, y);
                JSONObject cellJSON = new JSONObject();
                cellJSON.put("name", cell == null ? Connect4Constants.emptyCell : cell.getComponentName());
                row.add(cellJSON);
            }
            rows.add(row);
        }
        json.put("cells", rows);

        JSONArray winnerCells = new JSONArray();
        for (Pair<Integer, Integer> cell : state.winnerCells) {
            JSONArray pair = new JSONArray();
            pair.add(cell.a);
            pair.add(cell.b);
            winnerCells.add(pair);
        }
        json.put("winnerCells", winnerCells);

        return json;
    }

    public static void loadFromJSON(Connect4GameState state, JSONObject json) {
        JSONObject abstractGS = (JSONObject) json.get("abstractGameState");
        state.loadAbstractGameStateFromJSON(abstractGS);

        int gridBoardID = ((Number) json.get("gridBoardID")).intValue();
        JSONArray rows = (JSONArray) json.get("cells");
        int height = rows.size();
        int width = ((JSONArray) rows.get(0)).size();

        GridBoard grid = new GridBoard(width, height, state.gridBoard.getComponentName(), gridBoardID);
        for (int y = 0; y < height; y++) {
            JSONArray row = (JSONArray) rows.get(y);
            for (int x = 0; x < width; x++) {
                String name = (String) ((JSONObject) row.get(x)).get("name");
                grid.set(x, y, marker(name));
            }
        }
        state.gridBoard = grid;

        LinkedList<Pair<Integer, Integer>> winnerCells = new LinkedList<>();
        JSONArray winnerArray = (JSONArray) json.get("winnerCells");
        if (winnerArray != null) {
            for (Object o : winnerArray) {
                JSONArray pair = (JSONArray) o;
                int x = ((Number) pair.get(0)).intValue();
                int y = ((Number) pair.get(1)).intValue();
                winnerCells.add(new Pair<>(x, y));
            }
        }
        state.winnerCells = winnerCells;
    }

    /**
     * The shared static playerMapping marker with the name, so that componentIDs are identical to the
     * pre-serialization state; null for the empty cell.
     */
    private static BoardNode marker(String name) {
        for (BoardNode marker : Connect4Constants.playerMapping) {
            if (marker.getComponentName().equals(name))
                return marker;
        }
        return null;
    }
}
