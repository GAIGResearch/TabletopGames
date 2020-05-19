package core.components;

import core.content.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Hash;
import utilities.Utils.ComponentType;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Board extends Component implements IBoard {

    // List of nodes in the board graph
    protected List<BoardNode> boardNodes;
    protected String nameID;                //This is also in the properties hashmap, maybe remove from there?

    public Board()
    {
        boardNodes = new ArrayList<>();
        properties = new HashMap<>();
    }

    /**
     * @return the list of board nodes
     */
    @Override
    public List<BoardNode> getBoardNodes() {
        return boardNodes;
    }

    /**
     * Loads board nodes from a JSON file.
     * @param board - board to load in JSON format
     */
    public void loadBoard(JSONObject board) {

        String boardType = (String) board.get("type"); //This could come in handy one day.
        String verticesKey = (String) board.get("verticesKey");
        String neighboursKey = (String) board.get("neighboursKey");
        int maxNeighbours = (int) (long) board.get("maxNeighbours");

        nameID = (String) board.get("id");
        properties.put(Hash.GetInstance().hash("id"), new PropertyString(nameID));

        JSONArray nodeList = (JSONArray) board.get("nodes");
        for(Object o : nodeList)
        {
            // Add nodes to board nodes
            JSONObject node = (JSONObject) o;
            BoardNode newBN = (BoardNode) parseComponent(new BoardNode(), node);
            newBN.setMaxNeighbours(maxNeighbours);
            boardNodes.add(newBN);
        }

        int _hash_neighbours_ = Hash.GetInstance().hash(neighboursKey);
        int _hash_vertices_ = Hash.GetInstance().hash(verticesKey);

        for (IBoardNode bn : boardNodes) {
            Property p = ((BoardNode)bn).getProperty(_hash_neighbours_);
            if (p instanceof PropertyStringArray) {
                PropertyStringArray psa = (PropertyStringArray) p;
                for (String str : psa.getValues()) {
                    IBoardNode neigh = this.getNodeByProperty(_hash_vertices_, new PropertyString(str));
                    if (neigh != null) {
                        bn.addNeighbour(neigh);
                        neigh.addNeighbour(bn);
                    }
                }
            }
        }
    }


    /**
     * Returns the node in the list which matches the given name
     * @param prop_id - ID of the property to look for.
     * @param p - Property that has the value to look for.
     * @return - node matching name.
     */
    @Override
    public BoardNode getNodeByProperty(int prop_id, Property p) {
        for (BoardNode n : boardNodes) {
            Property prop = ((BoardNode)n).getProperty(prop_id);
            if(prop != null)
            {
                if(prop.equals(p))
                    return n;
            }

            //if (n.getName().equals(name)) return n;
        }
        return null;
    }


    @Override
    public BoardNode getNode(int hashID, String value)
    {
        return getNodeByProperty(hashID, new PropertyString(value));
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
    @Override
    public Board copy()
    {
        Board b = new Board();
        b.setBoardNodes(new ArrayList<>(boardNodes));
        b.nameID = nameID;
        return b;
    }

    /**
     * Sets the list of board nodes to the given list.
     * @param boardNodes - new list of board nodes.
     */
    private void setBoardNodes(List<BoardNode> boardNodes) {
        this.boardNodes = boardNodes;
    }

    @Override
    public String getNameID() {
        return nameID;
    }

    /**
     * Sets the correct type to the component
     */
    @Override
    public void setType(){
        super.type = ComponentType.BOARD;
    }

    public static List<Board> loadBoards(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<Board> boards = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {
                Board newBoard = new Board();
                newBoard.loadBoard((JSONObject) o);
                boards.add(newBoard);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return boards;
    }


}
