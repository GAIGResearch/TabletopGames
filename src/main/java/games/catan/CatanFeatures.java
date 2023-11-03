package games.catan;

import core.AbstractGameState;
import core.interfaces.IStateFeatureJSON;
import games.catan.components.Building;
import games.catan.components.CatanTile;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CatanFeatures implements IStateFeatureJSON {

    // Convert the state to a JSON
    // Long function ahead
    @Override
    public String getObservationJson(AbstractGameState gameState, int playerId) {
        CatanGameState catanGameState = (CatanGameState) gameState;
        JSONObject json = new JSONObject();

        // Maps for ordering nodes and edges
        Map<Integer, Integer> orderedEdges = new HashMap<>();
        Map<Integer, Integer> orderedNodes = new HashMap<>();
        int edgeCounter = 0;
        int nodeCounter = 0;

        // Convert the board / tiles to json
        // Ignoring edges for now
        JSONObject board_json = new JSONObject();
        JSONObject tile_json = new JSONObject();
        JSONObject nodes_json = new JSONObject();
        for (int i = 0; i < catanGameState.board.length; i++) {
            for (int j = 0; j < catanGameState.board[i].length; j++) {

                // Encode all information contained in the tile
                tile_json.clear();
                CatanTile tile = catanGameState.board[i][j];

                // Top level information about the tile
                tile_json.put("X Position", tile.x);
                tile_json.put("Y Position", tile.y);
                tile_json.put("Type", tile.getTileType().toString());
                tile_json.put("Number", tile.getNumber());
                tile_json.put("Robber", tile.hasRobber());

                // Encode the nodes of the tile (cities, etc.)
                for (int id : tile.getVerticesBoardNodeIDs()) {

                    // Convert to building
                    nodes_json.clear();
                    Building building = (Building) catanGameState.catanGraph.getBoardNodeMap().get(id);
                    nodes_json.put("Type", building.getBuildingType().toString());
                    nodes_json.put("Owner", building.getOwnerId());
                    nodes_json.put("Harbour", building.getHarbour().toString());
                    tile_json.put("Node " + id, nodes_json);
                }

                board_json.put("Tile " + i + " " + j, tile_json);



            }
        }
        json.put("Board", board_json);
        return json.toJSONString();
    }
}
