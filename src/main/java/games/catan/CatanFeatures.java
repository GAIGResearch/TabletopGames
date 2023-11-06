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
        JSONObject tiles_json = new JSONObject();
        JSONObject tile_json = new JSONObject();
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

                //TODO these are duplicates - turn into function?

                int[] edges = new int[tile.getEdgeIDs().length];

                // Register the edges
                for (int n = 0; n < tile.getEdgeIDs().length; n++) {

                    // Get the edge ID
                    int edgeID = tile.getEdgeIDs()[n];

                    // If Edge Exists
                    if (edgeID != -1) {

                        // If the edge has not been registered yet
                        // Register it
                        if (!orderedEdges.containsKey(edgeID)) {
                            orderedEdges.put(edgeID, edgeCounter);
                            edgeCounter++;
                        }

                        // Assign edge to the ordered value
                        edges[n] = orderedEdges.get(edgeID);
                    }
                }
                tile_json.put("Edges", edges);
                int[] nodes = new int[tile.getVerticesBoardNodeIDs().length];

                // Register the nodes
                for (int n = 0; n < tile.getVerticesBoardNodeIDs().length; n++) {

                    // Get the node ID
                    int nodeID = tile.getVerticesBoardNodeIDs()[n];

                    // If Node Exists
                    if (nodeID != -1) {

                        // If the node has not been registered yet
                        // Register it
                        if (!orderedNodes.containsKey(nodeID)) {
                            orderedNodes.put(nodeID, nodeCounter);
                            nodeCounter++;
                        }

                        // Assign node to the ordered value
                        nodes[n] = orderedNodes.get(nodeID);
                    }
                }

                tile_json.put("Nodes", nodes);
                tiles_json.put("Tile " + i + ", " + j, tile_json);
            }
        }

        board_json.put("Tiles", tiles_json);

        // Nodes
        JSONObject nodes_json = new JSONObject();

        // Roads (Edges) are stored in the buildings (I think) so need to extract them
        Map<Integer, Edge> roads = new HashMap<>();

        // TODO Must be a better way of doing this
        // A map of the ordered node ID to the json object,
        // so that we can order the nodes in the json
        Map<Integer, JSONObject> jsonNodeMap = new HashMap<>();

        // Get ID, Node Pair
        for (Map.Entry<Integer, BoardNodeWithEdges> pair : catanGameState.catanGraph.getBoardNodeMap().entrySet()) {

            JSONObject node_json = new JSONObject();

            int id = pair.getKey();
            Building node = (Building) pair.getValue();
            int orderedID = orderedNodes.get(id);

            node_json.put("Owner", node.getOwnerId());
            node_json.put("Type", node.getBuildingType().toString());
            if (node.getHarbour() != null) {
                node_json.put("Harbour", node.getHarbour().toString());
            }
            else {
                node_json.put("Harbour", "None");
            }


            // Get the edges and put them in a map (using their component ID as key)
            for (Edge e : node.getEdges()) {
                roads.put(e.getComponentID(), e);
            }

            // nodes_json.put("Node " + orderedID, node_json);
            jsonNodeMap.put(orderedID, node_json);
        }

        for (int i = 0; i < jsonNodeMap.size(); i++) {
            nodes_json.put("Node " + i, jsonNodeMap.get(i));
        }


//        JSONObject edges_jason = new JSONObject();

//        // For Edges
//        for (Map.Entry<Integer, Edge> pair : roads.entrySet()) {
//            JSONObject edge_json = new JSONObject();
//            int id = pair.getKey();
//            Edge edge = pair.getValue();
//            int orderedID = orderedEdges.get(id);
//            edge_json.put("Owner", String.valueOf(edge.getOwnerId()));
//            edges_jason.put("Edge " + orderedID, edge_json);
//        }
//
        board_json.put("Nodes", nodes_json);
        json.put("Board", board_json);
//        board_json.put("Edges", edges_jason);
        // json.put("Board", board_json);
        return json.toJSONString();
    }
}
