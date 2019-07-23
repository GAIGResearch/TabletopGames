package core;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class Board {

    // List of nodes in the board graph
    protected List<BoardNode> boardNodes;

    /**
     * @return the list of board nodes
     */
    public List<BoardNode> getBoardNodes() {
        return boardNodes;
    }

    /**
     * Loads board nodes from a JSON file.
     * @param path - path to JSON file to load.
     */
    public void loadBoard(String path) {
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(path)) {
            JSONArray nodeList = (JSONArray) jsonParser.parse(reader);

            // Add nodes to board nodes
            for (Object o: nodeList) {
                JSONObject node = (JSONObject)o;
                boardNodes.add(parseNode(node));
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses a BoardNode object from a JSON object.
     * @param obj - JSON object to parse.
     * @return new BoardNode object with properties as defined in JSON.
     */
    private BoardNode parseNode(JSONObject obj) {
        String name = (String) obj.get("name");
        String color = (String) obj.get("color");
        int maxNeighbours = -1;
        try {
            maxNeighbours = (int) obj.get("maxNeighbours");
        } catch (Exception ignored) {}

        return new BoardNode(maxNeighbours, name, Utils.stringToColor(color));
    }

    /**
     * Returns the node in the list which matches the given name
     * @param name - name of node to search for.
     * @return - node matching name.
     */
    protected BoardNode getNodeByName(String name) {
        for (BoardNode n : boardNodes) {
            if (n.getName().equals(name)) return n;
        }
        return null;
    }

    /**
     * Returns the node in the list which matches the given ID
     * @param id - ID of node to search for.
     * @return - node matching ID.
     */
    protected BoardNode getNodeByID(int id) {
        for (BoardNode n : boardNodes) {
            if (n.getId() == id) return n;
        }
        return null;
    }

    /**
     * Copy method, to be implemented by all subclasses.
     * @return - a new instance of this Board, deep copy.
     */
    public abstract Board copy();

    /**
     * Copies super class parameters.
     * @param b - board object to copy parameters for.
     */
    protected void copyTo(Board b) {
        b.setBoardNodes(new ArrayList<>(boardNodes));
    }

    /**
     * Sets the list of board nodes to the given list.
     * @param boardNodes - new list of board nodes.
     */
    private void setBoardNodes(List<BoardNode> boardNodes) {
        this.boardNodes = boardNodes;
    }
}
