package core.components;

import core.CoreConstants;
import core.interfaces.IComponentContainer;
import core.properties.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Hash;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static core.CoreConstants.imgHash;
import static core.CoreConstants.nameHash;

public class GraphBoardWithEdges extends Component implements IComponentContainer<BoardNodeWithEdges> {

    // List of nodes in the board graph, mapping component ID to object reference
    protected Map<Integer, BoardNodeWithEdges> boardNodes;

    public GraphBoardWithEdges(String name)
    {
        super(CoreConstants.ComponentType.BOARD, name);
        boardNodes = new HashMap<>();
    }

    public GraphBoardWithEdges()
    {
        super(CoreConstants.ComponentType.BOARD);
        boardNodes = new HashMap<>();
    }

    protected GraphBoardWithEdges(String name, int ID)
    {
        super(CoreConstants.ComponentType.BOARD, name, ID);
        boardNodes = new HashMap<>();
    }

    GraphBoardWithEdges(int ID)
    {
        super(CoreConstants.ComponentType.BOARD, ID);
        boardNodes = new HashMap<>();
    }

    /**
     * Copy method, to be implemented by all subclasses.
     * @return - a new instance of this Board, deep copy.
     */
    @Override
    public GraphBoardWithEdges copy()
    {
        GraphBoardWithEdges b = new GraphBoardWithEdges(componentName, componentID);
        HashMap<Integer, BoardNodeWithEdges> nodeCopies = new HashMap<>();
        HashMap<Integer, Edge> edgeCopies = new HashMap<>();
        // Copy board nodes
        for (BoardNodeWithEdges bn: boardNodes.values()) {
            BoardNodeWithEdges bnCopy = bn.copy();
            if (bnCopy == null) bnCopy = new BoardNodeWithEdges(bn.ownerId, bn.getComponentID());
            bn.copyComponentTo(bnCopy);
            nodeCopies.put(bn.getComponentID(), bnCopy);
            // Copy edges
            for (Edge e: bn.neighbourEdgeMapping.keySet()) {
                edgeCopies.put(e.componentID, e.copy());
            }
        }
        // Assign neighbours and edges
        for (BoardNodeWithEdges bn: boardNodes.values()) {
            BoardNodeWithEdges bnCopy = nodeCopies.get(bn.getComponentID());
            for (Map.Entry<Edge, BoardNodeWithEdges> e: bn.neighbourEdgeMapping.entrySet()) {
                bnCopy.addNeighbour(nodeCopies.get(e.getValue().getComponentID()), edgeCopies.get(e.getKey().componentID));
            }
        }

        // Assign new neighbours
        b.setBoardNodes(nodeCopies);

        // Copy properties
        copyComponentTo(b);
        return b;
    }

    /**
     * @return the list of board nodes
     */
    public Collection<BoardNodeWithEdges> getBoardNodes() {
        return boardNodes.values();
    }

    /**
     * Returns the node in the list which matches the given ID
     * @param id - ID of node to search for.
     * @return - node matching ID.
     */
    public BoardNodeWithEdges getNodeByID(int id) {
        return boardNodes.get(id);
    }

    /**
     * Sets the list of board nodes to the given list.
     * @param boardNodes - new list of board nodes.
     */
    public void setBoardNodes(List<BoardNodeWithEdges> boardNodes) {
        for (BoardNodeWithEdges bn: boardNodes) {
            this.boardNodes.put(bn.getComponentID(), bn);
        }
    }
    public void setBoardNodes(Map<Integer, BoardNodeWithEdges> boardNodes) {
        this.boardNodes = boardNodes;
    }

    public void addBoardNode(BoardNodeWithEdges bn) {
        this.boardNodes.put(bn.getComponentID(), bn);
    }

    public void removeBoardNode(BoardNodeWithEdges bn) {
        this.boardNodes.remove(bn.getComponentID());
    }

    public void breakConnection(BoardNodeWithEdges bn1, BoardNodeWithEdges bn2, Edge edge) {
        bn1.removeNeighbour(bn2, edge);
        bn2.removeNeighbour(bn1, edge);

        // Check if they have at least 1 more neighbour on this board. If not, remove node from this board
        boolean inBoard = false;
        for (BoardNodeWithEdges n: bn1.getNeighbours()) {
            if (boardNodes.containsKey(n.getComponentID())) {
                inBoard = true;
                break;
            }
        }
        if (!inBoard) boardNodes.remove(bn1.getComponentID());

        inBoard = false;
        for (BoardNodeWithEdges n: bn2.getNeighbours()) {
            if (boardNodes.containsKey(n.getComponentID())) {
                inBoard = true;
                break;
            }
        }
        if (!inBoard) boardNodes.remove(bn2.getComponentID());
    }

    public Edge addConnection(BoardNodeWithEdges bn1, BoardNodeWithEdges bn2) {
        Edge edge = new Edge();
        addConnection(bn1, bn2, edge);
        return edge;
    }

    public Edge addConnection(int bn1id, int bn2id) {
        BoardNodeWithEdges bn1 = boardNodes.get(bn1id);
        BoardNodeWithEdges bn2 = boardNodes.get(bn2id);
        Edge edge = new Edge();
        addConnection(bn1, bn2, edge);
        return edge;
    }

    public void addConnection(BoardNodeWithEdges bn1, BoardNodeWithEdges bn2, Edge edge) {
        bn1.addNeighbour(bn2, edge);
        bn2.addNeighbour(bn1, edge);
    }

    public static List<GraphBoardWithEdges> loadBoards(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<GraphBoardWithEdges> graphBoards = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {
                GraphBoardWithEdges newGraphBoard = new GraphBoardWithEdges();
                newGraphBoard.loadBoard((JSONObject) o);
                graphBoards.add(newGraphBoard);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return graphBoards;
    }

    /**
     * Loads board nodes from a JSON file.
     * @param board - board to load in JSON format
     */
    public void loadBoard(JSONObject board) {
        System.out.println("load board called");
        componentName = (String) board.get("id");
        String boardType = (String) board.get("type");
        String verticesKey = (String) board.get("verticesKey");
        String edgesKey = (String) board.get("edgesKey");
        String neighboursKey = (String) board.get("neighboursKey");
        int maxNeighbours = (int) (long) board.get("maxNeighbours");

        properties.put(Hash.GetInstance().hash("boardType"), new PropertyString("boardType", boardType));
        if (board.get("img") != null) {
            properties.put(imgHash, new PropertyString("img", (String) board.get("img")));
        }

        JSONArray nodeList = (JSONArray) board.get("nodes");
        for(Object o : nodeList) //nodes into boardNode
        {
            JSONObject node = (JSONObject) o;
            BoardNodeWithEdges newBN = new BoardNodeWithEdges();
            newBN.loadBoardNodeWithEdge(node);
            newBN.setComponentName(((PropertyString)newBN.getProperty(nameHash)).value);
            newBN.setMaxNeighbours(maxNeighbours);
            boardNodes.put(newBN.componentID, newBN);
        }


        int _hash_vertices_ = Hash.GetInstance().hash(verticesKey);



        JSONArray edgeList = (JSONArray) board.get("edges");

        System.out.println("Edge list: " + edgeList);
        System.out.println("Edge list size: " + edgeList.size());

        // Step 2: Load edges
        for (Object edgeObj : edgeList) {
            JSONObject currentEdge = (JSONObject) edgeObj;



            JSONArray edgeConnections = (JSONArray) currentEdge.get("nodes");
            JSONArray nodesArray = (JSONArray) edgeConnections.get(1);


            //Get the two nodes that form an edge
            String edge1Name = (String) nodesArray.get(0);
            String edge2Name = (String) nodesArray.get(1);

            BoardNodeWithEdges node1 = (BoardNodeWithEdges) this.getNodeByProperty(_hash_vertices_, new PropertyString("name", edge1Name));
            BoardNodeWithEdges node2 = (BoardNodeWithEdges) this.getNodeByProperty(_hash_vertices_, new PropertyString("name", edge2Name));

            Edge edge;
            edge = CreateEdgeWithProperties(currentEdge); //make edge

            if (node1 != null && node2 != null) {
                node1.addNeighbour(node2, edge); //add to neighbourEdgeMapping

            }

        }
    }

    public BoardNodeWithEdges getNodeByProperty(int prop_id, Property p) {
        for (BoardNodeWithEdges n : boardNodes.values()) {
            Property prop = n.getProperty(prop_id);
            if(prop != null)
            {
                if(prop.equals(p))
                    return n;
            }
        }
        return null;
    }

    public Edge CreateEdgeWithProperties(JSONObject edge) {

        //Create new edge and give it properties from jsonobject edge
        Edge newEdge = new Edge();

        for (Object currentKeyObj : edge.keySet()) {
            String key = (String) currentKeyObj;
            Object valueObj = edge.get(key);

            if (valueObj instanceof JSONArray) {
                JSONArray valueArray = (JSONArray) valueObj;
                String type = (String) valueArray.get(0);
                Object value = valueArray.get(1);

                Property property = null;

                if (type.equals("String[]")) {
                    property = new PropertyStringArray(key, (JSONArray) value);
                }
                else if (type.equals("String")) {
                    property = new PropertyString(key, (String) value);
                }
                else if (type.equals("Integer")) {
                    property = new PropertyInt(key, ((Long) value).intValue());
                }
                else if (type.equals("Boolean")) {
                    property = new PropertyBoolean(key, (Boolean) value);
                }
                if (property != null) {
                    newEdge.setProperty(property);
                }
            }
        }
        return newEdge;
    }
    public BoardNodeWithEdges getNodeByStringProperty(int prop_id, String value)
    {
        return getNodeByProperty(prop_id, new PropertyString(value));
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GraphBoardWithEdges) {
            GraphBoardWithEdges other = (GraphBoardWithEdges) o;
            return componentID == other.componentID && other.boardNodes.equals(boardNodes);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentID, boardNodes);
    }

    @Override
    public List<BoardNodeWithEdges> getComponents() {
        return new ArrayList<>(getBoardNodes());
    }

    public Map<Integer, BoardNodeWithEdges> getBoardNodeMap() {
        return boardNodes;
    }

    @Override
    public CoreConstants.VisibilityMode getVisibilityMode() {
        return CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
    }
}
