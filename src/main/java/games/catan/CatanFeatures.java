package games.catan;

import core.AbstractGameState;
import core.components.BoardNodeWithEdges;
import core.components.Edge;
import core.interfaces.IStateFeatureJSON;
import games.catan.components.Building;
import games.catan.components.CatanTile;
import org.json.simple.JSONObject;
import scala.util.parsing.json.JSON;
import utilities.Pair;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CatanFeatures implements IStateFeatureJSON {

    // Convert the state to a JSON
    // Long function ahead
    @Override
    public String getObservationJson(AbstractGameState gameState, int playerId) {
        CatanGameState catanGameState = (CatanGameState) gameState;
        JSONObject json = new JSONObject();

        // Maps for ordering nodes and edgesS
        Map<Integer, Integer> orderedEdges = new HashMap<>();
        Map<Integer, Integer> orderedNodes = new HashMap<>();
        int edgeCounter = 0;
        int nodeCounter = 0;

        // Convert the board / tiles to json
        JSONObject board_json = new JSONObject();
        for (int i = 0; i < catanGameState.board.length; i++) {
            for (int j = 0; j < catanGameState.board[i].length; j++) {

                // Encode all information contained in the tile
                JSONObject tile_json = new JSONObject();
                CatanTile tile = catanGameState.board[i][j];

                // Top level information about the tile

                //tile_json.put("X Position", tile.x);
                //tile_json.put("Y Position", tile.y);
                tile_json.put("Type", tile.getTileType().toString());
                tile_json.put("Number", tile.getNumber());
                tile_json.put("Robber", tile.hasRobber());


                // Edges (Roads)
                Edge[] edges = catanGameState.getRoads(tile);
                int[] roads = new int[edges.length];

                // If edge is null then there is no road
                // Otherwise ownerID is the owner of the road
                for (int n = 0; n < edges.length; n++) {
                    if (edges[n] != null) {
                        roads[n] = edges[n].getOwnerId();
                    } else {
                        roads[n] = -1;
                    }
                }

                // Node (Settlements Harbours and Cities)
                Building[] nodes = catanGameState.getBuildings(tile);
                int[] settlements = new int[nodes.length];
                int[] harbours = new int[nodes.length];
                int[] cities = new int[nodes.length];

                for (int n =0; n < nodes.length; n++) {
                    Building node = nodes[n];

                    // Check for settlements
                    if (node.getBuildingType() == Building.Type.Settlement) {
                        settlements[n] = node.getOwnerId();
                    }
                    else {
                        settlements[n] = -1;
                    }

                    // Check for cities
                    if (node.getBuildingType() == Building.Type.City) {
                        cities[n] = node.getOwnerId();
                    }
                    else {
                        cities[n] = -1;
                    }

                    // Check for harbours
                    if (node.getHarbour() != null) {
                        harbours[n] = node.getHarbour().ordinal();
                    }
                    else {
                        harbours[n] = -1;
                    }
                }

                tile_json.put("Roads", roads);
                tile_json.put("Settlements", settlements);
                tile_json.put("Cities", cities);
                tile_json.put("Harbours", harbours);
                board_json.put("Tile " + i + ", " + j, tile_json);
            }
        }
        json.put("Board", board_json);
        return json.toJSONString();
    }
}
