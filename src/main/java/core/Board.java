package core;

import content.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Hash;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Board {

    // List of nodes in the board graph
    protected List<BoardNode> boardNodes;


    public Board()
    {
        boardNodes = new ArrayList<>();
    }

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
            JSONObject board = (JSONObject) jsonParser.parse(reader);
            String boardType = (String) board.get("type"); //This could come in handy one day.
            String verticesKey = (String) board.get("verticesKey");
            String neighboursKey = (String) board.get("neighboursKey");
            int maxNeighbours = (int) (long) board.get("maxNeighbours");


            JSONArray nodeList = (JSONArray) board.get("nodes");
            for(Object o : nodeList)
            {
                // Add nodes to board nodes
                JSONObject node = (JSONObject) o;
                BoardNode newBN = parseNode(node);
                newBN.setMaxNeighbours(maxNeighbours);
                boardNodes.add(newBN);
            }

            int _hash_neighbours_ = Hash.GetInstance().hash(neighboursKey);
            int _hash_vertices_ = Hash.GetInstance().hash(verticesKey);

            for (BoardNode bn : boardNodes) {
                Property p = bn.getProperty(_hash_neighbours_);
                if (p instanceof PropertyStringArray) {
                    PropertyStringArray psa = (PropertyStringArray) p;
                    for (String str : psa.getValues()) {
                        BoardNode neigh = this.getNodeByProperty(_hash_vertices_, new PropertyString(str));
                        if (neigh != null) {
                            bn.addNeighbour(neigh);
                            neigh.addNeighbour(bn);
                        }
                    }
                }
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
    protected BoardNode parseNode(JSONObject obj)
    {
        BoardNode bn = new BoardNode();

        for(Object o : obj.keySet())
        {
            String key = (String)o;
            JSONArray value = (JSONArray) obj.get(key);
            String type = (String) value.get(0);

            Property prop = null;
            if(type.contains("[]"))
            {
                JSONArray values = (JSONArray) value.get(1);

                if(type.contains("String"))
                {
                    prop = new PropertyStringArray(key, values);


                }
                //More types of arrays to come.
            }else
            {
                if(type.contains("String"))
                {
                    prop = new PropertyString(key, (String) value.get(1));
                }else if (type.contains("Color")){
                    prop = new PropertyColor(key, (String) value.get(1));
                }else if (type.contains("Vector2D")){
                    prop = new PropertyVector2D(key, (JSONArray) value.get(1));
                }
            }
            bn.addProperty(Hash.GetInstance().hash(prop.getHashString()), prop);
        }


        return bn;
    }

    /**
     * Returns the node in the list which matches the given name
     * @param prop_id - ID of the property to look for.
     * @param p - Property that has the value to look for.
     * @return - node matching name.
     */
    protected BoardNode getNodeByProperty(int prop_id, Property p) {
        for (BoardNode n : boardNodes) {
            Property prop = n.getProperty(prop_id);
            if(prop != null)
            {
                if(prop.equals(p))
                    return n;
            }

            //if (n.getName().equals(name)) return n;
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
    public Board copy()
    {
        Board b = new Board();
        b.setBoardNodes(new ArrayList<>(boardNodes));
        return b;
    }

    /**
     * Sets the list of board nodes to the given list.
     * @param boardNodes - new list of board nodes.
     */
    private void setBoardNodes(List<BoardNode> boardNodes) {
        this.boardNodes = boardNodes;
    }

    /**
     * Main method for testing.
     */
    public static void main(String[] args) throws InterruptedException {
        Board pb = new Board();
        String dataPath = "data/pandemicBoard.json";

        pb.loadBoard(dataPath);
        for (BoardNode b : pb.boardNodes) {
            System.out.println(b);
        }

    }
}
