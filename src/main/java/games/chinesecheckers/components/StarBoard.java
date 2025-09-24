package games.chinesecheckers.components;

import core.CoreConstants;
import core.components.Component;
import core.interfaces.IComponentContainer;
import games.chinesecheckers.CCParameters;

import java.util.*;

public class StarBoard extends Component implements IComponentContainer<CCNode> {

    // List of nodes in the board graph
    protected List<CCNode> boardNodes;
    protected Map<Integer, Map<Integer, Integer>> allPairDistances;
    protected Map<Integer, CCNode> nodesByID;

    public StarBoard() {
        super(CoreConstants.ComponentType.BOARD);
        boardNodes = new ArrayList<>();
        buildBoard();
        buildNodeIndex();
        calculateAllPairDistances();
        loadNodeBaseColours();
    }

    private StarBoard(String name, int ID) {
        super(CoreConstants.ComponentType.BOARD, name, ID);
    }

    /**
     * Copy method, to be implemented by all subclasses.
     *
     * @return - a new instance of this Board, deep copy.
     */
    @Override
    public StarBoard copy() {
        StarBoard retValue = new StarBoard(componentName, componentID);
        // Now we copy the board nodes (and pegs)

        retValue.boardNodes = new ArrayList<>();
        for (int i = 0; i < this.boardNodes.size(); i++) {
            CCNode currentNode = this.getBoardNodes().get(i);
            CCNode copiedNode = new CCNode(currentNode.getComponentID());
            copiedNode.copyComponentTo(currentNode);
            copiedNode.setColourNode(currentNode.getBaseColour());
            if (currentNode.isNodeOccupied()) {
                Peg pegCopy = (Peg) currentNode.getOccupiedPeg().copy();
                copiedNode.setOccupiedPeg(pegCopy);
            }
            copiedNode.setCoordinates(currentNode.getX(), currentNode.getY());
            retValue.boardNodes.add(copiedNode);
        }
        retValue.buildNodeIndex();

        // Assign neighbours
        for (int i = 0; i < this.boardNodes.size(); i++) {
            CCNode originalNode = this.boardNodes.get(i);
            CCNode copiedNode = retValue.boardNodes.get(i);
            for (CCNode originalNeighbour : originalNode.getNeighbours()) {
                CCNode copiedNeighbour = retValue.nodesByID.get(originalNeighbour.getID());
                int side = originalNode.getSideOfNeighbour(originalNeighbour.getID());
                copiedNode.addNeighbour(copiedNeighbour, side);
            }

        }
        retValue.allPairDistances = allPairDistances; // this is immutable, so we can just copy the reference
        // Copy properties
        copyComponentTo(retValue);

        return retValue;
    }

    /**
     * @return the list of board nodes
     */
    public List<CCNode> getBoardNodes() {
        return boardNodes;
    }


    public CCNode getNodeById(int id) {
        return nodesByID.get(id);
    }
    /**
     * Sets the list of board nodes to the given list.
     *
     * @param boardNodes - new list of board nodes.
     */
    public void setBoardNodes(List<CCNode> boardNodes) {
        this.boardNodes = boardNodes;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof StarBoard other) {
            return componentID == other.componentID && other.boardNodes.equals(boardNodes);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(componentID, boardNodes);
    }

    @Override
    public List<CCNode> getComponents() {
        return getBoardNodes();
    }

    @Override
    public CoreConstants.VisibilityMode getVisibilityMode() {
        return CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
    }

    private void loadNodeBaseColours() {
        // technically we would not have access to CCParameters here...but it prettifies the code
        // enough to be warranted
        CCParameters params = new CCParameters();
        // Load Purple Nodes
        for (Peg.Colour colour : params.colourIndices.keySet()) {
            for (int i : params.colourIndices.get(colour)) {
                this.getBoardNodes().get(i).setColourNode(colour);
            }
        }
    }

    public void buildNodeIndex() {
        nodesByID = new HashMap<>();
        for (CCNode node : boardNodes) {
            nodesByID.put(node.getComponentID(), node);
        }
    }

    public void buildBoard() {
        for (int i = 0; i < 121; i++) {
            this.getBoardNodes().add(new CCNode(i));
        }
        (this.getBoardNodes().get(0)).setCoordinates(6, 0);
        this.getBoardNodes().get(0).addNeighbour(this.getBoardNodes().get(1), 3);
        this.getBoardNodes().get(0).addNeighbour(this.getBoardNodes().get(2), 2);

        this.getBoardNodes().get(1).setCoordinates(5, 1);
        this.getBoardNodes().get(1).addNeighbour(this.getBoardNodes().get(3), 3);
        this.getBoardNodes().get(1).addNeighbour(this.getBoardNodes().get(4), 2);
        this.getBoardNodes().get(1).addNeighbour(this.getBoardNodes().get(2), 1);
        this.getBoardNodes().get(1).addNeighbour(this.getBoardNodes().get(0), 0);

        this.getBoardNodes().get(2).setCoordinates(6, 1);
        this.getBoardNodes().get(2).addNeighbour(this.getBoardNodes().get(4), 3);
        this.getBoardNodes().get(2).addNeighbour(this.getBoardNodes().get(5), 2);
        this.getBoardNodes().get(2).addNeighbour(this.getBoardNodes().get(0), 5);
        this.getBoardNodes().get(2).addNeighbour(this.getBoardNodes().get(1), 4);

        this.getBoardNodes().get(3).setCoordinates(5, 2);
        this.getBoardNodes().get(3).addNeighbour(this.getBoardNodes().get(6), 3);
        this.getBoardNodes().get(3).addNeighbour(this.getBoardNodes().get(7), 2);
        this.getBoardNodes().get(3).addNeighbour(this.getBoardNodes().get(4), 1);
        this.getBoardNodes().get(3).addNeighbour(this.getBoardNodes().get(1), 0);

        this.getBoardNodes().get(4).setCoordinates(6, 2);
        this.getBoardNodes().get(4).addNeighbour(this.getBoardNodes().get(7), 3);
        this.getBoardNodes().get(4).addNeighbour(this.getBoardNodes().get(8), 2);
        this.getBoardNodes().get(4).addNeighbour(this.getBoardNodes().get(5), 1);
        this.getBoardNodes().get(4).addNeighbour(this.getBoardNodes().get(2), 0);
        this.getBoardNodes().get(4).addNeighbour(this.getBoardNodes().get(1), 5);
        this.getBoardNodes().get(4).addNeighbour(this.getBoardNodes().get(3), 4);

        this.getBoardNodes().get(5).setCoordinates(7, 2);
        this.getBoardNodes().get(5).addNeighbour(this.getBoardNodes().get(8), 3);
        this.getBoardNodes().get(5).addNeighbour(this.getBoardNodes().get(9), 2);
        this.getBoardNodes().get(5).addNeighbour(this.getBoardNodes().get(2), 5);
        this.getBoardNodes().get(5).addNeighbour(this.getBoardNodes().get(4), 4);

        this.getBoardNodes().get(6).setCoordinates(4, 3);
        this.getBoardNodes().get(6).addNeighbour(this.getBoardNodes().get(14), 3);
        this.getBoardNodes().get(6).addNeighbour(this.getBoardNodes().get(15), 2);
        this.getBoardNodes().get(6).addNeighbour(this.getBoardNodes().get(7), 1);
        this.getBoardNodes().get(6).addNeighbour(this.getBoardNodes().get(3), 0);

        this.getBoardNodes().get(7).setCoordinates(5, 3);
        this.getBoardNodes().get(7).addNeighbour(this.getBoardNodes().get(15), 3);
        this.getBoardNodes().get(7).addNeighbour(this.getBoardNodes().get(16), 2);
        this.getBoardNodes().get(7).addNeighbour(this.getBoardNodes().get(8), 1);
        this.getBoardNodes().get(7).addNeighbour(this.getBoardNodes().get(4), 0);
        this.getBoardNodes().get(7).addNeighbour(this.getBoardNodes().get(3), 5);
        this.getBoardNodes().get(7).addNeighbour(this.getBoardNodes().get(6), 4);

        this.getBoardNodes().get(8).setCoordinates(6, 3);
        this.getBoardNodes().get(8).addNeighbour(this.getBoardNodes().get(16), 3);
        this.getBoardNodes().get(8).addNeighbour(this.getBoardNodes().get(17), 2);
        this.getBoardNodes().get(8).addNeighbour(this.getBoardNodes().get(9), 1);
        this.getBoardNodes().get(8).addNeighbour(this.getBoardNodes().get(5), 0);
        this.getBoardNodes().get(8).addNeighbour(this.getBoardNodes().get(4), 5);
        this.getBoardNodes().get(8).addNeighbour(this.getBoardNodes().get(7), 4);

        this.getBoardNodes().get(9).setCoordinates(7, 3);
        this.getBoardNodes().get(9).addNeighbour(this.getBoardNodes().get(17), 3);
        this.getBoardNodes().get(9).addNeighbour(this.getBoardNodes().get(18), 2);
        this.getBoardNodes().get(9).addNeighbour(this.getBoardNodes().get(5), 5);
        this.getBoardNodes().get(9).addNeighbour(this.getBoardNodes().get(8), 4);

        this.getBoardNodes().get(10).setCoordinates(0, 4);
        this.getBoardNodes().get(10).addNeighbour(this.getBoardNodes().get(11), 1);
        this.getBoardNodes().get(10).addNeighbour(this.getBoardNodes().get(23), 2);

        this.getBoardNodes().get(11).setCoordinates(1, 4);
        this.getBoardNodes().get(11).addNeighbour(this.getBoardNodes().get(12), 1);
        this.getBoardNodes().get(11).addNeighbour(this.getBoardNodes().get(24), 2);
        this.getBoardNodes().get(11).addNeighbour(this.getBoardNodes().get(23), 3);
        this.getBoardNodes().get(11).addNeighbour(this.getBoardNodes().get(10), 4);

        this.getBoardNodes().get(12).setCoordinates(2, 4);
        this.getBoardNodes().get(12).addNeighbour(this.getBoardNodes().get(13), 1);
        this.getBoardNodes().get(12).addNeighbour(this.getBoardNodes().get(25), 2);
        this.getBoardNodes().get(12).addNeighbour(this.getBoardNodes().get(24), 3);
        this.getBoardNodes().get(12).addNeighbour(this.getBoardNodes().get(11), 4);

        this.getBoardNodes().get(13).setCoordinates(3, 4);
        this.getBoardNodes().get(13).addNeighbour(this.getBoardNodes().get(14), 1);
        this.getBoardNodes().get(13).addNeighbour(this.getBoardNodes().get(26), 2);
        this.getBoardNodes().get(13).addNeighbour(this.getBoardNodes().get(25), 3);
        this.getBoardNodes().get(13).addNeighbour(this.getBoardNodes().get(12), 4);

        this.getBoardNodes().get(14).setCoordinates(4, 4);
        this.getBoardNodes().get(14).addNeighbour(this.getBoardNodes().get(6), 0);
        this.getBoardNodes().get(14).addNeighbour(this.getBoardNodes().get(15), 1);
        this.getBoardNodes().get(14).addNeighbour(this.getBoardNodes().get(27), 2);
        this.getBoardNodes().get(14).addNeighbour(this.getBoardNodes().get(26), 3);
        this.getBoardNodes().get(14).addNeighbour(this.getBoardNodes().get(13), 4);

        this.getBoardNodes().get(15).setCoordinates(5, 4);
        this.getBoardNodes().get(15).addNeighbour(this.getBoardNodes().get(7), 0);
        this.getBoardNodes().get(15).addNeighbour(this.getBoardNodes().get(16), 1);
        this.getBoardNodes().get(15).addNeighbour(this.getBoardNodes().get(28), 2);
        this.getBoardNodes().get(15).addNeighbour(this.getBoardNodes().get(27), 3);
        this.getBoardNodes().get(15).addNeighbour(this.getBoardNodes().get(14), 4);
        this.getBoardNodes().get(15).addNeighbour(this.getBoardNodes().get(6), 5);

        this.getBoardNodes().get(16).setCoordinates(6, 4);
        this.getBoardNodes().get(16).addNeighbour(this.getBoardNodes().get(8), 0);
        this.getBoardNodes().get(16).addNeighbour(this.getBoardNodes().get(17), 1);
        this.getBoardNodes().get(16).addNeighbour(this.getBoardNodes().get(29), 2);
        this.getBoardNodes().get(16).addNeighbour(this.getBoardNodes().get(28), 3);
        this.getBoardNodes().get(16).addNeighbour(this.getBoardNodes().get(15), 4);
        this.getBoardNodes().get(16).addNeighbour(this.getBoardNodes().get(7), 5);

        this.getBoardNodes().get(17).setCoordinates(7, 4);
        this.getBoardNodes().get(17).addNeighbour(this.getBoardNodes().get(9), 0);
        this.getBoardNodes().get(17).addNeighbour(this.getBoardNodes().get(18), 1);
        this.getBoardNodes().get(17).addNeighbour(this.getBoardNodes().get(30), 2);
        this.getBoardNodes().get(17).addNeighbour(this.getBoardNodes().get(29), 3);
        this.getBoardNodes().get(17).addNeighbour(this.getBoardNodes().get(16), 4);
        this.getBoardNodes().get(17).addNeighbour(this.getBoardNodes().get(8), 5);

        this.getBoardNodes().get(18).setCoordinates(8, 4);
        this.getBoardNodes().get(18).addNeighbour(this.getBoardNodes().get(19), 1);
        this.getBoardNodes().get(18).addNeighbour(this.getBoardNodes().get(31), 2);
        this.getBoardNodes().get(18).addNeighbour(this.getBoardNodes().get(30), 3);
        this.getBoardNodes().get(18).addNeighbour(this.getBoardNodes().get(17), 4);
        this.getBoardNodes().get(18).addNeighbour(this.getBoardNodes().get(9), 5);

        this.getBoardNodes().get(19).setCoordinates(9, 4);
        this.getBoardNodes().get(19).addNeighbour(this.getBoardNodes().get(20), 1);
        this.getBoardNodes().get(19).addNeighbour(this.getBoardNodes().get(32), 2);
        this.getBoardNodes().get(19).addNeighbour(this.getBoardNodes().get(31), 3);
        this.getBoardNodes().get(19).addNeighbour(this.getBoardNodes().get(18), 4);

        this.getBoardNodes().get(20).setCoordinates(10, 4);
        this.getBoardNodes().get(20).addNeighbour(this.getBoardNodes().get(21), 1);
        this.getBoardNodes().get(20).addNeighbour(this.getBoardNodes().get(33), 2);
        this.getBoardNodes().get(20).addNeighbour(this.getBoardNodes().get(32), 3);
        this.getBoardNodes().get(20).addNeighbour(this.getBoardNodes().get(19), 4);

        this.getBoardNodes().get(21).setCoordinates(11, 4);
        this.getBoardNodes().get(21).addNeighbour(this.getBoardNodes().get(22), 1);
        this.getBoardNodes().get(21).addNeighbour(this.getBoardNodes().get(34), 2);
        this.getBoardNodes().get(21).addNeighbour(this.getBoardNodes().get(33), 3);
        this.getBoardNodes().get(21).addNeighbour(this.getBoardNodes().get(20), 4);

        this.getBoardNodes().get(22).setCoordinates(12, 4);
        this.getBoardNodes().get(22).addNeighbour(this.getBoardNodes().get(34), 3);
        this.getBoardNodes().get(22).addNeighbour(this.getBoardNodes().get(21), 4);

        this.getBoardNodes().get(23).setCoordinates(0, 5);
        this.getBoardNodes().get(23).addNeighbour(this.getBoardNodes().get(11), 0);
        this.getBoardNodes().get(23).addNeighbour(this.getBoardNodes().get(24), 1);
        this.getBoardNodes().get(23).addNeighbour(this.getBoardNodes().get(35), 2);
        this.getBoardNodes().get(23).addNeighbour(this.getBoardNodes().get(10), 5);

        this.getBoardNodes().get(24).setCoordinates(1, 5);
        this.getBoardNodes().get(24).addNeighbour(this.getBoardNodes().get(12), 0);
        this.getBoardNodes().get(24).addNeighbour(this.getBoardNodes().get(25), 1);
        this.getBoardNodes().get(24).addNeighbour(this.getBoardNodes().get(36), 2);
        this.getBoardNodes().get(24).addNeighbour(this.getBoardNodes().get(35), 3);
        this.getBoardNodes().get(24).addNeighbour(this.getBoardNodes().get(23), 4);
        this.getBoardNodes().get(24).addNeighbour(this.getBoardNodes().get(11), 5);

        this.getBoardNodes().get(25).setCoordinates(2, 5);
        this.getBoardNodes().get(25).addNeighbour(this.getBoardNodes().get(13), 0);
        this.getBoardNodes().get(25).addNeighbour(this.getBoardNodes().get(26), 1);
        this.getBoardNodes().get(25).addNeighbour(this.getBoardNodes().get(37), 2);
        this.getBoardNodes().get(25).addNeighbour(this.getBoardNodes().get(36), 3);
        this.getBoardNodes().get(25).addNeighbour(this.getBoardNodes().get(24), 4);
        this.getBoardNodes().get(25).addNeighbour(this.getBoardNodes().get(12), 5);

        this.getBoardNodes().get(26).setCoordinates(3, 5);
        this.getBoardNodes().get(26).addNeighbour(this.getBoardNodes().get(14), 0);
        this.getBoardNodes().get(26).addNeighbour(this.getBoardNodes().get(27), 1);
        this.getBoardNodes().get(26).addNeighbour(this.getBoardNodes().get(38), 2);
        this.getBoardNodes().get(26).addNeighbour(this.getBoardNodes().get(37), 3);
        this.getBoardNodes().get(26).addNeighbour(this.getBoardNodes().get(25), 4);
        this.getBoardNodes().get(26).addNeighbour(this.getBoardNodes().get(13), 5);

        this.getBoardNodes().get(27).setCoordinates(4, 5);
        this.getBoardNodes().get(27).addNeighbour(this.getBoardNodes().get(15), 0);
        this.getBoardNodes().get(27).addNeighbour(this.getBoardNodes().get(28), 1);
        this.getBoardNodes().get(27).addNeighbour(this.getBoardNodes().get(39), 2);
        this.getBoardNodes().get(27).addNeighbour(this.getBoardNodes().get(38), 3);
        this.getBoardNodes().get(27).addNeighbour(this.getBoardNodes().get(26), 4);
        this.getBoardNodes().get(27).addNeighbour(this.getBoardNodes().get(14), 5);

        this.getBoardNodes().get(28).setCoordinates(5, 5);
        this.getBoardNodes().get(28).addNeighbour(this.getBoardNodes().get(16), 0);
        this.getBoardNodes().get(28).addNeighbour(this.getBoardNodes().get(29), 1);
        this.getBoardNodes().get(28).addNeighbour(this.getBoardNodes().get(40), 2);
        this.getBoardNodes().get(28).addNeighbour(this.getBoardNodes().get(39), 3);
        this.getBoardNodes().get(28).addNeighbour(this.getBoardNodes().get(27), 4);
        this.getBoardNodes().get(28).addNeighbour(this.getBoardNodes().get(15), 5);

        this.getBoardNodes().get(29).setCoordinates(6, 5);
        this.getBoardNodes().get(29).addNeighbour(this.getBoardNodes().get(17), 0);
        this.getBoardNodes().get(29).addNeighbour(this.getBoardNodes().get(30), 1);
        this.getBoardNodes().get(29).addNeighbour(this.getBoardNodes().get(41), 2);
        this.getBoardNodes().get(29).addNeighbour(this.getBoardNodes().get(40), 3);
        this.getBoardNodes().get(29).addNeighbour(this.getBoardNodes().get(28), 4);
        this.getBoardNodes().get(29).addNeighbour(this.getBoardNodes().get(16), 5);

        this.getBoardNodes().get(30).setCoordinates(7, 5);
        this.getBoardNodes().get(30).addNeighbour(this.getBoardNodes().get(18), 0);
        this.getBoardNodes().get(30).addNeighbour(this.getBoardNodes().get(31), 1);
        this.getBoardNodes().get(30).addNeighbour(this.getBoardNodes().get(42), 2);
        this.getBoardNodes().get(30).addNeighbour(this.getBoardNodes().get(41), 3);
        this.getBoardNodes().get(30).addNeighbour(this.getBoardNodes().get(29), 4);
        this.getBoardNodes().get(30).addNeighbour(this.getBoardNodes().get(17), 5);

        this.getBoardNodes().get(31).setCoordinates(8, 5);
        this.getBoardNodes().get(31).addNeighbour(this.getBoardNodes().get(19), 0);
        this.getBoardNodes().get(31).addNeighbour(this.getBoardNodes().get(32), 1);
        this.getBoardNodes().get(31).addNeighbour(this.getBoardNodes().get(43), 2);
        this.getBoardNodes().get(31).addNeighbour(this.getBoardNodes().get(42), 3);
        this.getBoardNodes().get(31).addNeighbour(this.getBoardNodes().get(30), 4);
        this.getBoardNodes().get(31).addNeighbour(this.getBoardNodes().get(18), 5);

        this.getBoardNodes().get(32).setCoordinates(9, 5);
        this.getBoardNodes().get(32).addNeighbour(this.getBoardNodes().get(20), 0);
        this.getBoardNodes().get(32).addNeighbour(this.getBoardNodes().get(33), 1);
        this.getBoardNodes().get(32).addNeighbour(this.getBoardNodes().get(44), 2);
        this.getBoardNodes().get(32).addNeighbour(this.getBoardNodes().get(43), 3);
        this.getBoardNodes().get(32).addNeighbour(this.getBoardNodes().get(31), 4);
        this.getBoardNodes().get(32).addNeighbour(this.getBoardNodes().get(19), 5);

        this.getBoardNodes().get(33).setCoordinates(10, 5);
        this.getBoardNodes().get(33).addNeighbour(this.getBoardNodes().get(21), 0);
        this.getBoardNodes().get(33).addNeighbour(this.getBoardNodes().get(34), 1);
        this.getBoardNodes().get(33).addNeighbour(this.getBoardNodes().get(45), 2);
        this.getBoardNodes().get(33).addNeighbour(this.getBoardNodes().get(44), 3);
        this.getBoardNodes().get(33).addNeighbour(this.getBoardNodes().get(32), 4);
        this.getBoardNodes().get(33).addNeighbour(this.getBoardNodes().get(20), 5);

        this.getBoardNodes().get(34).setCoordinates(11, 5);
        this.getBoardNodes().get(34).addNeighbour(this.getBoardNodes().get(22), 0);
        this.getBoardNodes().get(34).addNeighbour(this.getBoardNodes().get(45), 3);
        this.getBoardNodes().get(34).addNeighbour(this.getBoardNodes().get(33), 4);
        this.getBoardNodes().get(34).addNeighbour(this.getBoardNodes().get(21), 5);

        this.getBoardNodes().get(35).setCoordinates(1, 6);
        this.getBoardNodes().get(35).addNeighbour(this.getBoardNodes().get(24), 0);
        this.getBoardNodes().get(35).addNeighbour(this.getBoardNodes().get(36), 1);
        this.getBoardNodes().get(35).addNeighbour(this.getBoardNodes().get(46), 2);
        this.getBoardNodes().get(35).addNeighbour(this.getBoardNodes().get(23), 5);

        this.getBoardNodes().get(36).setCoordinates(2, 6);
        this.getBoardNodes().get(36).addNeighbour(this.getBoardNodes().get(25), 0);
        this.getBoardNodes().get(36).addNeighbour(this.getBoardNodes().get(37), 1);
        this.getBoardNodes().get(36).addNeighbour(this.getBoardNodes().get(47), 2);
        this.getBoardNodes().get(36).addNeighbour(this.getBoardNodes().get(46), 3);
        this.getBoardNodes().get(36).addNeighbour(this.getBoardNodes().get(35), 4);
        this.getBoardNodes().get(36).addNeighbour(this.getBoardNodes().get(24), 5);

        this.getBoardNodes().get(37).setCoordinates(3, 6);
        this.getBoardNodes().get(37).addNeighbour(this.getBoardNodes().get(26), 0);
        this.getBoardNodes().get(37).addNeighbour(this.getBoardNodes().get(38), 1);
        this.getBoardNodes().get(37).addNeighbour(this.getBoardNodes().get(48), 2);
        this.getBoardNodes().get(37).addNeighbour(this.getBoardNodes().get(47), 3);
        this.getBoardNodes().get(37).addNeighbour(this.getBoardNodes().get(36), 4);
        this.getBoardNodes().get(37).addNeighbour(this.getBoardNodes().get(25), 5);

        this.getBoardNodes().get(38).setCoordinates(4, 6);
        this.getBoardNodes().get(38).addNeighbour(this.getBoardNodes().get(27), 0);
        this.getBoardNodes().get(38).addNeighbour(this.getBoardNodes().get(39), 1);
        this.getBoardNodes().get(38).addNeighbour(this.getBoardNodes().get(49), 2);
        this.getBoardNodes().get(38).addNeighbour(this.getBoardNodes().get(48), 3);
        this.getBoardNodes().get(38).addNeighbour(this.getBoardNodes().get(37), 4);
        this.getBoardNodes().get(38).addNeighbour(this.getBoardNodes().get(26), 5);

        this.getBoardNodes().get(39).setCoordinates(5, 6);
        this.getBoardNodes().get(39).addNeighbour(this.getBoardNodes().get(28), 0);
        this.getBoardNodes().get(39).addNeighbour(this.getBoardNodes().get(40), 1);
        this.getBoardNodes().get(39).addNeighbour(this.getBoardNodes().get(50), 2);
        this.getBoardNodes().get(39).addNeighbour(this.getBoardNodes().get(49), 3);
        this.getBoardNodes().get(39).addNeighbour(this.getBoardNodes().get(38), 4);
        this.getBoardNodes().get(39).addNeighbour(this.getBoardNodes().get(27), 5);

        this.getBoardNodes().get(40).setCoordinates(6, 6);
        this.getBoardNodes().get(40).addNeighbour(this.getBoardNodes().get(29), 0);
        this.getBoardNodes().get(40).addNeighbour(this.getBoardNodes().get(41), 1);
        this.getBoardNodes().get(40).addNeighbour(this.getBoardNodes().get(51), 2);
        this.getBoardNodes().get(40).addNeighbour(this.getBoardNodes().get(50), 3);
        this.getBoardNodes().get(40).addNeighbour(this.getBoardNodes().get(39), 4);
        this.getBoardNodes().get(40).addNeighbour(this.getBoardNodes().get(28), 5);

        this.getBoardNodes().get(41).setCoordinates(7, 6);
        this.getBoardNodes().get(41).addNeighbour(this.getBoardNodes().get(30), 0);
        this.getBoardNodes().get(41).addNeighbour(this.getBoardNodes().get(42), 1);
        this.getBoardNodes().get(41).addNeighbour(this.getBoardNodes().get(52), 2);
        this.getBoardNodes().get(41).addNeighbour(this.getBoardNodes().get(51), 3);
        this.getBoardNodes().get(41).addNeighbour(this.getBoardNodes().get(40), 4);
        this.getBoardNodes().get(41).addNeighbour(this.getBoardNodes().get(29), 5);

        this.getBoardNodes().get(42).setCoordinates(8, 6);
        this.getBoardNodes().get(42).addNeighbour(this.getBoardNodes().get(31), 0);
        this.getBoardNodes().get(42).addNeighbour(this.getBoardNodes().get(43), 1);
        this.getBoardNodes().get(42).addNeighbour(this.getBoardNodes().get(53), 2);
        this.getBoardNodes().get(42).addNeighbour(this.getBoardNodes().get(52), 3);
        this.getBoardNodes().get(42).addNeighbour(this.getBoardNodes().get(41), 4);
        this.getBoardNodes().get(42).addNeighbour(this.getBoardNodes().get(30), 5);

        this.getBoardNodes().get(43).setCoordinates(9, 6);
        this.getBoardNodes().get(43).addNeighbour(this.getBoardNodes().get(32), 0);
        this.getBoardNodes().get(43).addNeighbour(this.getBoardNodes().get(44), 1);
        this.getBoardNodes().get(43).addNeighbour(this.getBoardNodes().get(54), 2);
        this.getBoardNodes().get(43).addNeighbour(this.getBoardNodes().get(53), 3);
        this.getBoardNodes().get(43).addNeighbour(this.getBoardNodes().get(42), 4);
        this.getBoardNodes().get(43).addNeighbour(this.getBoardNodes().get(31), 5);

        this.getBoardNodes().get(44).setCoordinates(10, 6);
        this.getBoardNodes().get(44).addNeighbour(this.getBoardNodes().get(33), 0);
        this.getBoardNodes().get(44).addNeighbour(this.getBoardNodes().get(45), 1);
        this.getBoardNodes().get(44).addNeighbour(this.getBoardNodes().get(55), 2);
        this.getBoardNodes().get(44).addNeighbour(this.getBoardNodes().get(54), 3);
        this.getBoardNodes().get(44).addNeighbour(this.getBoardNodes().get(43), 4);
        this.getBoardNodes().get(44).addNeighbour(this.getBoardNodes().get(32), 5);

        this.getBoardNodes().get(45).setCoordinates(11, 6);
        this.getBoardNodes().get(45).addNeighbour(this.getBoardNodes().get(34), 0);
        this.getBoardNodes().get(45).addNeighbour(this.getBoardNodes().get(55), 3);
        this.getBoardNodes().get(45).addNeighbour(this.getBoardNodes().get(44), 4);
        this.getBoardNodes().get(45).addNeighbour(this.getBoardNodes().get(33), 5);

        this.getBoardNodes().get(46).setCoordinates(1, 7);
        this.getBoardNodes().get(46).addNeighbour(this.getBoardNodes().get(36), 0);
        this.getBoardNodes().get(46).addNeighbour(this.getBoardNodes().get(47), 1);
        this.getBoardNodes().get(46).addNeighbour(this.getBoardNodes().get(56), 2);
        this.getBoardNodes().get(46).addNeighbour(this.getBoardNodes().get(35), 5);

        this.getBoardNodes().get(47).setCoordinates(2, 7);
        this.getBoardNodes().get(47).addNeighbour(this.getBoardNodes().get(37), 0);
        this.getBoardNodes().get(47).addNeighbour(this.getBoardNodes().get(48), 1);
        this.getBoardNodes().get(47).addNeighbour(this.getBoardNodes().get(57), 2);
        this.getBoardNodes().get(47).addNeighbour(this.getBoardNodes().get(56), 3);
        this.getBoardNodes().get(47).addNeighbour(this.getBoardNodes().get(46), 4);
        this.getBoardNodes().get(47).addNeighbour(this.getBoardNodes().get(36), 5);

        this.getBoardNodes().get(48).setCoordinates(3, 7);
        this.getBoardNodes().get(48).addNeighbour(this.getBoardNodes().get(38), 0);
        this.getBoardNodes().get(48).addNeighbour(this.getBoardNodes().get(49), 1);
        this.getBoardNodes().get(48).addNeighbour(this.getBoardNodes().get(58), 2);
        this.getBoardNodes().get(48).addNeighbour(this.getBoardNodes().get(57), 3);
        this.getBoardNodes().get(48).addNeighbour(this.getBoardNodes().get(47), 4);
        this.getBoardNodes().get(48).addNeighbour(this.getBoardNodes().get(37), 5);

        this.getBoardNodes().get(49).setCoordinates(4, 7);
        this.getBoardNodes().get(49).addNeighbour(this.getBoardNodes().get(39), 0);
        this.getBoardNodes().get(49).addNeighbour(this.getBoardNodes().get(50), 1);
        this.getBoardNodes().get(49).addNeighbour(this.getBoardNodes().get(59), 2);
        this.getBoardNodes().get(49).addNeighbour(this.getBoardNodes().get(58), 3);
        this.getBoardNodes().get(49).addNeighbour(this.getBoardNodes().get(48), 4);
        this.getBoardNodes().get(49).addNeighbour(this.getBoardNodes().get(38), 5);

        this.getBoardNodes().get(50).setCoordinates(5, 7);
        this.getBoardNodes().get(50).addNeighbour(this.getBoardNodes().get(40), 0);
        this.getBoardNodes().get(50).addNeighbour(this.getBoardNodes().get(51), 1);
        this.getBoardNodes().get(50).addNeighbour(this.getBoardNodes().get(60), 2);
        this.getBoardNodes().get(50).addNeighbour(this.getBoardNodes().get(59), 3);
        this.getBoardNodes().get(50).addNeighbour(this.getBoardNodes().get(49), 4);
        this.getBoardNodes().get(50).addNeighbour(this.getBoardNodes().get(39), 5);

        this.getBoardNodes().get(51).setCoordinates(6, 7);
        this.getBoardNodes().get(51).addNeighbour(this.getBoardNodes().get(41), 0);
        this.getBoardNodes().get(51).addNeighbour(this.getBoardNodes().get(52), 1);
        this.getBoardNodes().get(51).addNeighbour(this.getBoardNodes().get(61), 2);
        this.getBoardNodes().get(51).addNeighbour(this.getBoardNodes().get(60), 3);
        this.getBoardNodes().get(51).addNeighbour(this.getBoardNodes().get(50), 4);
        this.getBoardNodes().get(51).addNeighbour(this.getBoardNodes().get(40), 5);

        this.getBoardNodes().get(52).setCoordinates(7, 7);
        this.getBoardNodes().get(52).addNeighbour(this.getBoardNodes().get(42), 0);
        this.getBoardNodes().get(52).addNeighbour(this.getBoardNodes().get(53), 1);
        this.getBoardNodes().get(52).addNeighbour(this.getBoardNodes().get(62), 2);
        this.getBoardNodes().get(52).addNeighbour(this.getBoardNodes().get(61), 3);
        this.getBoardNodes().get(52).addNeighbour(this.getBoardNodes().get(51), 4);
        this.getBoardNodes().get(52).addNeighbour(this.getBoardNodes().get(41), 5);

        this.getBoardNodes().get(53).setCoordinates(8, 7);
        this.getBoardNodes().get(53).addNeighbour(this.getBoardNodes().get(43), 0);
        this.getBoardNodes().get(53).addNeighbour(this.getBoardNodes().get(54), 1);
        this.getBoardNodes().get(53).addNeighbour(this.getBoardNodes().get(63), 2);
        this.getBoardNodes().get(53).addNeighbour(this.getBoardNodes().get(62), 3);
        this.getBoardNodes().get(53).addNeighbour(this.getBoardNodes().get(52), 4);
        this.getBoardNodes().get(53).addNeighbour(this.getBoardNodes().get(42), 5);

        this.getBoardNodes().get(54).setCoordinates(9, 7);
        this.getBoardNodes().get(54).addNeighbour(this.getBoardNodes().get(44), 0);
        this.getBoardNodes().get(54).addNeighbour(this.getBoardNodes().get(55), 1);
        this.getBoardNodes().get(54).addNeighbour(this.getBoardNodes().get(64), 2);
        this.getBoardNodes().get(54).addNeighbour(this.getBoardNodes().get(63), 3);
        this.getBoardNodes().get(54).addNeighbour(this.getBoardNodes().get(53), 4);
        this.getBoardNodes().get(54).addNeighbour(this.getBoardNodes().get(43), 5);

        this.getBoardNodes().get(55).setCoordinates(10, 7);
        this.getBoardNodes().get(55).addNeighbour(this.getBoardNodes().get(45), 0);
        this.getBoardNodes().get(55).addNeighbour(this.getBoardNodes().get(64), 3);
        this.getBoardNodes().get(55).addNeighbour(this.getBoardNodes().get(54), 4);
        this.getBoardNodes().get(55).addNeighbour(this.getBoardNodes().get(44), 5);

        this.getBoardNodes().get(56).setCoordinates(2, 8);
        this.getBoardNodes().get(56).addNeighbour(this.getBoardNodes().get(47), 0);
        this.getBoardNodes().get(56).addNeighbour(this.getBoardNodes().get(57), 1);
        this.getBoardNodes().get(56).addNeighbour(this.getBoardNodes().get(66), 2);
        this.getBoardNodes().get(56).addNeighbour(this.getBoardNodes().get(65), 3);
        this.getBoardNodes().get(56).addNeighbour(this.getBoardNodes().get(46), 5);

        this.getBoardNodes().get(57).setCoordinates(3, 8);
        this.getBoardNodes().get(57).addNeighbour(this.getBoardNodes().get(48), 0);
        this.getBoardNodes().get(57).addNeighbour(this.getBoardNodes().get(58), 1);
        this.getBoardNodes().get(57).addNeighbour(this.getBoardNodes().get(67), 2);
        this.getBoardNodes().get(57).addNeighbour(this.getBoardNodes().get(66), 3);
        this.getBoardNodes().get(57).addNeighbour(this.getBoardNodes().get(56), 4);
        this.getBoardNodes().get(57).addNeighbour(this.getBoardNodes().get(47), 5);

        this.getBoardNodes().get(58).setCoordinates(4, 8);
        this.getBoardNodes().get(58).addNeighbour(this.getBoardNodes().get(49), 0);
        this.getBoardNodes().get(58).addNeighbour(this.getBoardNodes().get(59), 1);
        this.getBoardNodes().get(58).addNeighbour(this.getBoardNodes().get(68), 2);
        this.getBoardNodes().get(58).addNeighbour(this.getBoardNodes().get(67), 3);
        this.getBoardNodes().get(58).addNeighbour(this.getBoardNodes().get(57), 4);
        this.getBoardNodes().get(58).addNeighbour(this.getBoardNodes().get(48), 5);

        this.getBoardNodes().get(59).setCoordinates(5, 8);
        this.getBoardNodes().get(59).addNeighbour(this.getBoardNodes().get(50), 0);
        this.getBoardNodes().get(59).addNeighbour(this.getBoardNodes().get(60), 1);
        this.getBoardNodes().get(59).addNeighbour(this.getBoardNodes().get(69), 2);
        this.getBoardNodes().get(59).addNeighbour(this.getBoardNodes().get(68), 3);
        this.getBoardNodes().get(59).addNeighbour(this.getBoardNodes().get(58), 4);
        this.getBoardNodes().get(59).addNeighbour(this.getBoardNodes().get(49), 5);

        this.getBoardNodes().get(60).setCoordinates(6, 8);
        this.getBoardNodes().get(60).addNeighbour(this.getBoardNodes().get(51), 0);
        this.getBoardNodes().get(60).addNeighbour(this.getBoardNodes().get(61), 1);
        this.getBoardNodes().get(60).addNeighbour(this.getBoardNodes().get(70), 2);
        this.getBoardNodes().get(60).addNeighbour(this.getBoardNodes().get(69), 3);
        this.getBoardNodes().get(60).addNeighbour(this.getBoardNodes().get(59), 4);
        this.getBoardNodes().get(60).addNeighbour(this.getBoardNodes().get(50), 5);

        this.getBoardNodes().get(61).setCoordinates(7, 8);
        this.getBoardNodes().get(61).addNeighbour(this.getBoardNodes().get(52), 0);
        this.getBoardNodes().get(61).addNeighbour(this.getBoardNodes().get(62), 1);
        this.getBoardNodes().get(61).addNeighbour(this.getBoardNodes().get(71), 2);
        this.getBoardNodes().get(61).addNeighbour(this.getBoardNodes().get(70), 3);
        this.getBoardNodes().get(61).addNeighbour(this.getBoardNodes().get(60), 4);
        this.getBoardNodes().get(61).addNeighbour(this.getBoardNodes().get(51), 5);

        this.getBoardNodes().get(62).setCoordinates(8, 8);
        this.getBoardNodes().get(62).addNeighbour(this.getBoardNodes().get(53), 0);
        this.getBoardNodes().get(62).addNeighbour(this.getBoardNodes().get(63), 1);
        this.getBoardNodes().get(62).addNeighbour(this.getBoardNodes().get(72), 2);
        this.getBoardNodes().get(62).addNeighbour(this.getBoardNodes().get(71), 3);
        this.getBoardNodes().get(62).addNeighbour(this.getBoardNodes().get(61), 4);
        this.getBoardNodes().get(62).addNeighbour(this.getBoardNodes().get(52), 5);

        this.getBoardNodes().get(63).setCoordinates(9, 8);
        this.getBoardNodes().get(63).addNeighbour(this.getBoardNodes().get(54), 0);
        this.getBoardNodes().get(63).addNeighbour(this.getBoardNodes().get(64), 1);
        this.getBoardNodes().get(63).addNeighbour(this.getBoardNodes().get(73), 2);
        this.getBoardNodes().get(63).addNeighbour(this.getBoardNodes().get(72), 3);
        this.getBoardNodes().get(63).addNeighbour(this.getBoardNodes().get(62), 4);
        this.getBoardNodes().get(63).addNeighbour(this.getBoardNodes().get(53), 5);

        this.getBoardNodes().get(64).setCoordinates(10, 8);
        this.getBoardNodes().get(64).addNeighbour(this.getBoardNodes().get(55), 0);
        this.getBoardNodes().get(64).addNeighbour(this.getBoardNodes().get(74), 2);
        this.getBoardNodes().get(64).addNeighbour(this.getBoardNodes().get(73), 3);
        this.getBoardNodes().get(64).addNeighbour(this.getBoardNodes().get(63), 4);
        this.getBoardNodes().get(64).addNeighbour(this.getBoardNodes().get(54), 5);

        this.getBoardNodes().get(65).setCoordinates(1, 9);
        this.getBoardNodes().get(65).addNeighbour(this.getBoardNodes().get(56), 0);
        this.getBoardNodes().get(65).addNeighbour(this.getBoardNodes().get(66), 1);
        this.getBoardNodes().get(65).addNeighbour(this.getBoardNodes().get(76), 2);
        this.getBoardNodes().get(65).addNeighbour(this.getBoardNodes().get(75), 3);

        this.getBoardNodes().get(66).setCoordinates(2, 9);
        this.getBoardNodes().get(66).addNeighbour(this.getBoardNodes().get(57), 0);
        this.getBoardNodes().get(66).addNeighbour(this.getBoardNodes().get(67), 1);
        this.getBoardNodes().get(66).addNeighbour(this.getBoardNodes().get(77), 2);
        this.getBoardNodes().get(66).addNeighbour(this.getBoardNodes().get(76), 3);
        this.getBoardNodes().get(66).addNeighbour(this.getBoardNodes().get(65), 4);
        this.getBoardNodes().get(66).addNeighbour(this.getBoardNodes().get(56), 5);

        this.getBoardNodes().get(67).setCoordinates(3, 9);
        this.getBoardNodes().get(67).addNeighbour(this.getBoardNodes().get(58), 0);
        this.getBoardNodes().get(67).addNeighbour(this.getBoardNodes().get(68), 1);
        this.getBoardNodes().get(67).addNeighbour(this.getBoardNodes().get(78), 2);
        this.getBoardNodes().get(67).addNeighbour(this.getBoardNodes().get(77), 3);
        this.getBoardNodes().get(67).addNeighbour(this.getBoardNodes().get(66), 4);
        this.getBoardNodes().get(67).addNeighbour(this.getBoardNodes().get(57), 5);

        this.getBoardNodes().get(68).setCoordinates(4, 9);
        this.getBoardNodes().get(68).addNeighbour(this.getBoardNodes().get(59), 0);
        this.getBoardNodes().get(68).addNeighbour(this.getBoardNodes().get(69), 1);
        this.getBoardNodes().get(68).addNeighbour(this.getBoardNodes().get(79), 2);
        this.getBoardNodes().get(68).addNeighbour(this.getBoardNodes().get(78), 3);
        this.getBoardNodes().get(68).addNeighbour(this.getBoardNodes().get(67), 4);
        this.getBoardNodes().get(68).addNeighbour(this.getBoardNodes().get(58), 5);

        this.getBoardNodes().get(69).setCoordinates(5, 9);
        this.getBoardNodes().get(69).addNeighbour(this.getBoardNodes().get(60), 0);
        this.getBoardNodes().get(69).addNeighbour(this.getBoardNodes().get(70), 1);
        this.getBoardNodes().get(69).addNeighbour(this.getBoardNodes().get(80), 2);
        this.getBoardNodes().get(69).addNeighbour(this.getBoardNodes().get(79), 3);
        this.getBoardNodes().get(69).addNeighbour(this.getBoardNodes().get(68), 4);
        this.getBoardNodes().get(69).addNeighbour(this.getBoardNodes().get(59), 5);

        this.getBoardNodes().get(70).setCoordinates(6, 9);
        this.getBoardNodes().get(70).addNeighbour(this.getBoardNodes().get(61), 0);
        this.getBoardNodes().get(70).addNeighbour(this.getBoardNodes().get(71), 1);
        this.getBoardNodes().get(70).addNeighbour(this.getBoardNodes().get(81), 2);
        this.getBoardNodes().get(70).addNeighbour(this.getBoardNodes().get(80), 3);
        this.getBoardNodes().get(70).addNeighbour(this.getBoardNodes().get(69), 4);
        this.getBoardNodes().get(70).addNeighbour(this.getBoardNodes().get(60), 5);

        this.getBoardNodes().get(71).setCoordinates(7, 9);
        this.getBoardNodes().get(71).addNeighbour(this.getBoardNodes().get(62), 0);
        this.getBoardNodes().get(71).addNeighbour(this.getBoardNodes().get(72), 1);
        this.getBoardNodes().get(71).addNeighbour(this.getBoardNodes().get(82), 2);
        this.getBoardNodes().get(71).addNeighbour(this.getBoardNodes().get(81), 3);
        this.getBoardNodes().get(71).addNeighbour(this.getBoardNodes().get(70), 4);
        this.getBoardNodes().get(71).addNeighbour(this.getBoardNodes().get(61), 5);

        this.getBoardNodes().get(72).setCoordinates(8, 9);
        this.getBoardNodes().get(72).addNeighbour(this.getBoardNodes().get(63), 0);
        this.getBoardNodes().get(72).addNeighbour(this.getBoardNodes().get(73), 1);
        this.getBoardNodes().get(72).addNeighbour(this.getBoardNodes().get(83), 2);
        this.getBoardNodes().get(72).addNeighbour(this.getBoardNodes().get(82), 3);
        this.getBoardNodes().get(72).addNeighbour(this.getBoardNodes().get(71), 4);
        this.getBoardNodes().get(72).addNeighbour(this.getBoardNodes().get(62), 5);

        this.getBoardNodes().get(73).setCoordinates(9, 9);
        this.getBoardNodes().get(73).addNeighbour(this.getBoardNodes().get(64), 0);
        this.getBoardNodes().get(73).addNeighbour(this.getBoardNodes().get(74), 1);
        this.getBoardNodes().get(73).addNeighbour(this.getBoardNodes().get(84), 2);
        this.getBoardNodes().get(73).addNeighbour(this.getBoardNodes().get(83), 3);
        this.getBoardNodes().get(73).addNeighbour(this.getBoardNodes().get(72), 4);
        this.getBoardNodes().get(73).addNeighbour(this.getBoardNodes().get(63), 5);

        this.getBoardNodes().get(74).setCoordinates(10, 9);
        this.getBoardNodes().get(74).addNeighbour(this.getBoardNodes().get(85), 2);
        this.getBoardNodes().get(74).addNeighbour(this.getBoardNodes().get(84), 3);
        this.getBoardNodes().get(74).addNeighbour(this.getBoardNodes().get(73), 4);
        this.getBoardNodes().get(74).addNeighbour(this.getBoardNodes().get(64), 5);

        this.getBoardNodes().get(75).setCoordinates(1, 10);
        this.getBoardNodes().get(75).addNeighbour(this.getBoardNodes().get(65), 0);
        this.getBoardNodes().get(75).addNeighbour(this.getBoardNodes().get(76), 1);
        this.getBoardNodes().get(75).addNeighbour(this.getBoardNodes().get(87), 2);
        this.getBoardNodes().get(75).addNeighbour(this.getBoardNodes().get(86), 3);

        this.getBoardNodes().get(76).setCoordinates(2, 10);
        this.getBoardNodes().get(76).addNeighbour(this.getBoardNodes().get(66), 0);
        this.getBoardNodes().get(76).addNeighbour(this.getBoardNodes().get(77), 1);
        this.getBoardNodes().get(76).addNeighbour(this.getBoardNodes().get(88), 2);
        this.getBoardNodes().get(76).addNeighbour(this.getBoardNodes().get(87), 3);
        this.getBoardNodes().get(76).addNeighbour(this.getBoardNodes().get(75), 4);
        this.getBoardNodes().get(76).addNeighbour(this.getBoardNodes().get(65), 5);

        this.getBoardNodes().get(77).setCoordinates(3, 10);
        this.getBoardNodes().get(77).addNeighbour(this.getBoardNodes().get(67), 0);
        this.getBoardNodes().get(77).addNeighbour(this.getBoardNodes().get(78), 1);
        this.getBoardNodes().get(77).addNeighbour(this.getBoardNodes().get(89), 2);
        this.getBoardNodes().get(77).addNeighbour(this.getBoardNodes().get(88), 3);
        this.getBoardNodes().get(77).addNeighbour(this.getBoardNodes().get(76), 4);
        this.getBoardNodes().get(77).addNeighbour(this.getBoardNodes().get(66), 5);

        this.getBoardNodes().get(78).setCoordinates(4, 10);
        this.getBoardNodes().get(78).addNeighbour(this.getBoardNodes().get(68), 0);
        this.getBoardNodes().get(78).addNeighbour(this.getBoardNodes().get(79), 1);
        this.getBoardNodes().get(78).addNeighbour(this.getBoardNodes().get(90), 2);
        this.getBoardNodes().get(78).addNeighbour(this.getBoardNodes().get(89), 3);
        this.getBoardNodes().get(78).addNeighbour(this.getBoardNodes().get(77), 4);
        this.getBoardNodes().get(78).addNeighbour(this.getBoardNodes().get(67), 5);

        this.getBoardNodes().get(79).setCoordinates(5, 10);
        this.getBoardNodes().get(79).addNeighbour(this.getBoardNodes().get(69), 0);
        this.getBoardNodes().get(79).addNeighbour(this.getBoardNodes().get(80), 1);
        this.getBoardNodes().get(79).addNeighbour(this.getBoardNodes().get(91), 2);
        this.getBoardNodes().get(79).addNeighbour(this.getBoardNodes().get(90), 3);
        this.getBoardNodes().get(79).addNeighbour(this.getBoardNodes().get(78), 4);
        this.getBoardNodes().get(79).addNeighbour(this.getBoardNodes().get(68), 5);

        this.getBoardNodes().get(80).setCoordinates(6, 10);
        this.getBoardNodes().get(80).addNeighbour(this.getBoardNodes().get(70), 0);
        this.getBoardNodes().get(80).addNeighbour(this.getBoardNodes().get(81), 1);
        this.getBoardNodes().get(80).addNeighbour(this.getBoardNodes().get(92), 2);
        this.getBoardNodes().get(80).addNeighbour(this.getBoardNodes().get(91), 3);
        this.getBoardNodes().get(80).addNeighbour(this.getBoardNodes().get(79), 4);
        this.getBoardNodes().get(80).addNeighbour(this.getBoardNodes().get(69), 5);

        this.getBoardNodes().get(81).setCoordinates(7, 10);
        this.getBoardNodes().get(81).addNeighbour(this.getBoardNodes().get(71), 0);
        this.getBoardNodes().get(81).addNeighbour(this.getBoardNodes().get(82), 1);
        this.getBoardNodes().get(81).addNeighbour(this.getBoardNodes().get(93), 2);
        this.getBoardNodes().get(81).addNeighbour(this.getBoardNodes().get(92), 3);
        this.getBoardNodes().get(81).addNeighbour(this.getBoardNodes().get(80), 4);
        this.getBoardNodes().get(81).addNeighbour(this.getBoardNodes().get(70), 5);

        this.getBoardNodes().get(82).setCoordinates(8, 10);
        this.getBoardNodes().get(82).addNeighbour(this.getBoardNodes().get(72), 0);
        this.getBoardNodes().get(82).addNeighbour(this.getBoardNodes().get(83), 1);
        this.getBoardNodes().get(82).addNeighbour(this.getBoardNodes().get(94), 2);
        this.getBoardNodes().get(82).addNeighbour(this.getBoardNodes().get(93), 3);
        this.getBoardNodes().get(82).addNeighbour(this.getBoardNodes().get(81), 4);
        this.getBoardNodes().get(82).addNeighbour(this.getBoardNodes().get(71), 5);

        this.getBoardNodes().get(83).setCoordinates(9, 10);
        this.getBoardNodes().get(83).addNeighbour(this.getBoardNodes().get(73), 0);
        this.getBoardNodes().get(83).addNeighbour(this.getBoardNodes().get(84), 1);
        this.getBoardNodes().get(83).addNeighbour(this.getBoardNodes().get(95), 2);
        this.getBoardNodes().get(83).addNeighbour(this.getBoardNodes().get(94), 3);
        this.getBoardNodes().get(83).addNeighbour(this.getBoardNodes().get(82), 4);
        this.getBoardNodes().get(83).addNeighbour(this.getBoardNodes().get(72), 5);

        this.getBoardNodes().get(84).setCoordinates(10, 10);
        this.getBoardNodes().get(84).addNeighbour(this.getBoardNodes().get(85), 1);
        this.getBoardNodes().get(84).addNeighbour(this.getBoardNodes().get(96), 2);
        this.getBoardNodes().get(84).addNeighbour(this.getBoardNodes().get(95), 3);
        this.getBoardNodes().get(84).addNeighbour(this.getBoardNodes().get(83), 4);
        this.getBoardNodes().get(84).addNeighbour(this.getBoardNodes().get(74), 5);

        this.getBoardNodes().get(85).setCoordinates(11, 10);
        this.getBoardNodes().get(85).addNeighbour(this.getBoardNodes().get(97), 2);
        this.getBoardNodes().get(85).addNeighbour(this.getBoardNodes().get(96), 3);
        this.getBoardNodes().get(85).addNeighbour(this.getBoardNodes().get(84), 4);
        this.getBoardNodes().get(85).addNeighbour(this.getBoardNodes().get(74), 5);

        this.getBoardNodes().get(86).setCoordinates(0, 11);
        this.getBoardNodes().get(86).addNeighbour(this.getBoardNodes().get(75), 0);
        this.getBoardNodes().get(86).addNeighbour(this.getBoardNodes().get(87), 1);
        this.getBoardNodes().get(86).addNeighbour(this.getBoardNodes().get(99), 2);
        this.getBoardNodes().get(86).addNeighbour(this.getBoardNodes().get(98), 3);

        this.getBoardNodes().get(87).setCoordinates(1, 11);
        this.getBoardNodes().get(87).addNeighbour(this.getBoardNodes().get(76), 0);
        this.getBoardNodes().get(87).addNeighbour(this.getBoardNodes().get(88), 1);
        this.getBoardNodes().get(87).addNeighbour(this.getBoardNodes().get(100), 2);
        this.getBoardNodes().get(87).addNeighbour(this.getBoardNodes().get(99), 3);
        this.getBoardNodes().get(87).addNeighbour(this.getBoardNodes().get(86), 4);
        this.getBoardNodes().get(87).addNeighbour(this.getBoardNodes().get(75), 5);

        this.getBoardNodes().get(88).setCoordinates(2, 11);
        this.getBoardNodes().get(88).addNeighbour(this.getBoardNodes().get(77), 0);
        this.getBoardNodes().get(88).addNeighbour(this.getBoardNodes().get(89), 1);
        this.getBoardNodes().get(88).addNeighbour(this.getBoardNodes().get(101), 2);
        this.getBoardNodes().get(88).addNeighbour(this.getBoardNodes().get(100), 3);
        this.getBoardNodes().get(88).addNeighbour(this.getBoardNodes().get(87), 4);
        this.getBoardNodes().get(88).addNeighbour(this.getBoardNodes().get(76), 5);

        this.getBoardNodes().get(89).setCoordinates(3, 11);
        this.getBoardNodes().get(89).addNeighbour(this.getBoardNodes().get(78), 0);
        this.getBoardNodes().get(89).addNeighbour(this.getBoardNodes().get(90), 1);
        this.getBoardNodes().get(89).addNeighbour(this.getBoardNodes().get(102), 2);
        this.getBoardNodes().get(89).addNeighbour(this.getBoardNodes().get(101), 3);
        this.getBoardNodes().get(89).addNeighbour(this.getBoardNodes().get(88), 4);
        this.getBoardNodes().get(89).addNeighbour(this.getBoardNodes().get(77), 5);

        this.getBoardNodes().get(90).setCoordinates(4, 11);
        this.getBoardNodes().get(90).addNeighbour(this.getBoardNodes().get(79), 0);
        this.getBoardNodes().get(90).addNeighbour(this.getBoardNodes().get(91), 1);
        this.getBoardNodes().get(90).addNeighbour(this.getBoardNodes().get(103), 2);
        this.getBoardNodes().get(90).addNeighbour(this.getBoardNodes().get(102), 3);
        this.getBoardNodes().get(90).addNeighbour(this.getBoardNodes().get(89), 4);
        this.getBoardNodes().get(90).addNeighbour(this.getBoardNodes().get(78), 5);

        this.getBoardNodes().get(91).setCoordinates(5, 11);
        this.getBoardNodes().get(91).addNeighbour(this.getBoardNodes().get(80), 0);
        this.getBoardNodes().get(91).addNeighbour(this.getBoardNodes().get(92), 1);
        this.getBoardNodes().get(91).addNeighbour(this.getBoardNodes().get(104), 2);
        this.getBoardNodes().get(91).addNeighbour(this.getBoardNodes().get(103), 3);
        this.getBoardNodes().get(91).addNeighbour(this.getBoardNodes().get(90), 4);
        this.getBoardNodes().get(91).addNeighbour(this.getBoardNodes().get(79), 5);

        this.getBoardNodes().get(92).setCoordinates(6, 11);
        this.getBoardNodes().get(92).addNeighbour(this.getBoardNodes().get(81), 0);
        this.getBoardNodes().get(92).addNeighbour(this.getBoardNodes().get(93), 1);
        this.getBoardNodes().get(92).addNeighbour(this.getBoardNodes().get(105), 2);
        this.getBoardNodes().get(92).addNeighbour(this.getBoardNodes().get(104), 3);
        this.getBoardNodes().get(92).addNeighbour(this.getBoardNodes().get(91), 4);
        this.getBoardNodes().get(92).addNeighbour(this.getBoardNodes().get(80), 5);

        this.getBoardNodes().get(93).setCoordinates(7, 11);
        this.getBoardNodes().get(93).addNeighbour(this.getBoardNodes().get(82), 0);
        this.getBoardNodes().get(93).addNeighbour(this.getBoardNodes().get(94), 1);
        this.getBoardNodes().get(93).addNeighbour(this.getBoardNodes().get(106), 2);
        this.getBoardNodes().get(93).addNeighbour(this.getBoardNodes().get(105), 3);
        this.getBoardNodes().get(93).addNeighbour(this.getBoardNodes().get(92), 4);
        this.getBoardNodes().get(93).addNeighbour(this.getBoardNodes().get(81), 5);

        this.getBoardNodes().get(94).setCoordinates(8, 11);
        this.getBoardNodes().get(94).addNeighbour(this.getBoardNodes().get(83), 0);
        this.getBoardNodes().get(94).addNeighbour(this.getBoardNodes().get(95), 1);
        this.getBoardNodes().get(94).addNeighbour(this.getBoardNodes().get(107), 2);
        this.getBoardNodes().get(94).addNeighbour(this.getBoardNodes().get(106), 3);
        this.getBoardNodes().get(94).addNeighbour(this.getBoardNodes().get(93), 4);
        this.getBoardNodes().get(94).addNeighbour(this.getBoardNodes().get(82), 5);

        this.getBoardNodes().get(95).setCoordinates(9, 11);
        this.getBoardNodes().get(95).addNeighbour(this.getBoardNodes().get(84), 0);
        this.getBoardNodes().get(95).addNeighbour(this.getBoardNodes().get(96), 1);
        this.getBoardNodes().get(95).addNeighbour(this.getBoardNodes().get(108), 2);
        this.getBoardNodes().get(95).addNeighbour(this.getBoardNodes().get(107), 3);
        this.getBoardNodes().get(95).addNeighbour(this.getBoardNodes().get(94), 4);
        this.getBoardNodes().get(95).addNeighbour(this.getBoardNodes().get(83), 5);

        this.getBoardNodes().get(96).setCoordinates(10, 11);
        this.getBoardNodes().get(96).addNeighbour(this.getBoardNodes().get(85), 0);
        this.getBoardNodes().get(96).addNeighbour(this.getBoardNodes().get(97), 1);
        this.getBoardNodes().get(96).addNeighbour(this.getBoardNodes().get(109), 2);
        this.getBoardNodes().get(96).addNeighbour(this.getBoardNodes().get(108), 3);
        this.getBoardNodes().get(96).addNeighbour(this.getBoardNodes().get(95), 4);
        this.getBoardNodes().get(96).addNeighbour(this.getBoardNodes().get(84), 5);

        this.getBoardNodes().get(97).setCoordinates(11, 11);
        this.getBoardNodes().get(97).addNeighbour(this.getBoardNodes().get(110), 2);
        this.getBoardNodes().get(97).addNeighbour(this.getBoardNodes().get(109), 3);
        this.getBoardNodes().get(97).addNeighbour(this.getBoardNodes().get(96), 4);
        this.getBoardNodes().get(97).addNeighbour(this.getBoardNodes().get(85), 5);

        this.getBoardNodes().get(98).setCoordinates(0, 12);
        this.getBoardNodes().get(98).addNeighbour(this.getBoardNodes().get(86), 0);
        this.getBoardNodes().get(98).addNeighbour(this.getBoardNodes().get(99), 1);

        this.getBoardNodes().get(99).setCoordinates(1, 12);
        this.getBoardNodes().get(99).addNeighbour(this.getBoardNodes().get(87), 0);
        this.getBoardNodes().get(99).addNeighbour(this.getBoardNodes().get(100), 1);
        this.getBoardNodes().get(99).addNeighbour(this.getBoardNodes().get(98), 4);
        this.getBoardNodes().get(99).addNeighbour(this.getBoardNodes().get(86), 5);

        this.getBoardNodes().get(100).setCoordinates(2, 12);
        this.getBoardNodes().get(100).addNeighbour(this.getBoardNodes().get(88), 0);
        this.getBoardNodes().get(100).addNeighbour(this.getBoardNodes().get(101), 1);
        this.getBoardNodes().get(100).addNeighbour(this.getBoardNodes().get(99), 4);
        this.getBoardNodes().get(100).addNeighbour(this.getBoardNodes().get(87), 5);

        this.getBoardNodes().get(101).setCoordinates(3, 12);
        this.getBoardNodes().get(101).addNeighbour(this.getBoardNodes().get(89), 0);
        this.getBoardNodes().get(101).addNeighbour(this.getBoardNodes().get(102), 1);
        this.getBoardNodes().get(101).addNeighbour(this.getBoardNodes().get(100), 4);
        this.getBoardNodes().get(101).addNeighbour(this.getBoardNodes().get(88), 5);

        this.getBoardNodes().get(102).setCoordinates(4, 12);
        this.getBoardNodes().get(102).addNeighbour(this.getBoardNodes().get(90), 0);
        this.getBoardNodes().get(102).addNeighbour(this.getBoardNodes().get(103), 1);
        this.getBoardNodes().get(102).addNeighbour(this.getBoardNodes().get(111), 2);
        this.getBoardNodes().get(102).addNeighbour(this.getBoardNodes().get(101), 4);
        this.getBoardNodes().get(102).addNeighbour(this.getBoardNodes().get(89), 5);

        this.getBoardNodes().get(103).setCoordinates(5, 12);
        this.getBoardNodes().get(103).addNeighbour(this.getBoardNodes().get(91), 0);
        this.getBoardNodes().get(103).addNeighbour(this.getBoardNodes().get(104), 1);
        this.getBoardNodes().get(103).addNeighbour(this.getBoardNodes().get(112), 2);
        this.getBoardNodes().get(103).addNeighbour(this.getBoardNodes().get(111), 3);
        this.getBoardNodes().get(103).addNeighbour(this.getBoardNodes().get(102), 4);
        this.getBoardNodes().get(103).addNeighbour(this.getBoardNodes().get(90), 5);

        this.getBoardNodes().get(104).setCoordinates(6, 12);
        this.getBoardNodes().get(104).addNeighbour(this.getBoardNodes().get(92), 0);
        this.getBoardNodes().get(104).addNeighbour(this.getBoardNodes().get(105), 1);
        this.getBoardNodes().get(104).addNeighbour(this.getBoardNodes().get(113), 2);
        this.getBoardNodes().get(104).addNeighbour(this.getBoardNodes().get(112), 3);
        this.getBoardNodes().get(104).addNeighbour(this.getBoardNodes().get(103), 4);
        this.getBoardNodes().get(104).addNeighbour(this.getBoardNodes().get(91), 5);

        this.getBoardNodes().get(105).setCoordinates(7, 12);
        this.getBoardNodes().get(105).addNeighbour(this.getBoardNodes().get(93), 0);
        this.getBoardNodes().get(105).addNeighbour(this.getBoardNodes().get(106), 1);
        this.getBoardNodes().get(105).addNeighbour(this.getBoardNodes().get(114), 2);
        this.getBoardNodes().get(105).addNeighbour(this.getBoardNodes().get(113), 3);
        this.getBoardNodes().get(105).addNeighbour(this.getBoardNodes().get(104), 4);
        this.getBoardNodes().get(105).addNeighbour(this.getBoardNodes().get(92), 5);

        this.getBoardNodes().get(106).setCoordinates(8, 12);
        this.getBoardNodes().get(106).addNeighbour(this.getBoardNodes().get(94), 0);
        this.getBoardNodes().get(106).addNeighbour(this.getBoardNodes().get(107), 1);
        this.getBoardNodes().get(106).addNeighbour(this.getBoardNodes().get(114), 3);
        this.getBoardNodes().get(106).addNeighbour(this.getBoardNodes().get(105), 4);
        this.getBoardNodes().get(106).addNeighbour(this.getBoardNodes().get(93), 5);

        this.getBoardNodes().get(107).setCoordinates(9, 12);
        this.getBoardNodes().get(107).addNeighbour(this.getBoardNodes().get(95), 0);
        this.getBoardNodes().get(107).addNeighbour(this.getBoardNodes().get(108), 1);
        this.getBoardNodes().get(107).addNeighbour(this.getBoardNodes().get(106), 4);
        this.getBoardNodes().get(107).addNeighbour(this.getBoardNodes().get(94), 5);

        this.getBoardNodes().get(108).setCoordinates(10, 12);
        this.getBoardNodes().get(108).addNeighbour(this.getBoardNodes().get(96), 0);
        this.getBoardNodes().get(108).addNeighbour(this.getBoardNodes().get(109), 1);
        this.getBoardNodes().get(108).addNeighbour(this.getBoardNodes().get(107), 4);
        this.getBoardNodes().get(108).addNeighbour(this.getBoardNodes().get(95), 5);

        this.getBoardNodes().get(109).setCoordinates(11, 12);
        this.getBoardNodes().get(109).addNeighbour(this.getBoardNodes().get(97), 0);
        this.getBoardNodes().get(109).addNeighbour(this.getBoardNodes().get(110), 1);
        this.getBoardNodes().get(109).addNeighbour(this.getBoardNodes().get(108), 4);
        this.getBoardNodes().get(109).addNeighbour(this.getBoardNodes().get(96), 5);

        this.getBoardNodes().get(110).setCoordinates(12, 12);
        this.getBoardNodes().get(110).addNeighbour(this.getBoardNodes().get(109), 4);
        this.getBoardNodes().get(110).addNeighbour(this.getBoardNodes().get(97), 5);

        this.getBoardNodes().get(111).setCoordinates(4, 13);
        this.getBoardNodes().get(111).addNeighbour(this.getBoardNodes().get(103), 0);
        this.getBoardNodes().get(111).addNeighbour(this.getBoardNodes().get(112), 1);
        this.getBoardNodes().get(111).addNeighbour(this.getBoardNodes().get(115), 2);
        this.getBoardNodes().get(111).addNeighbour(this.getBoardNodes().get(102), 5);

        this.getBoardNodes().get(112).setCoordinates(5, 13);
        this.getBoardNodes().get(112).addNeighbour(this.getBoardNodes().get(104), 0);
        this.getBoardNodes().get(112).addNeighbour(this.getBoardNodes().get(113), 1);
        this.getBoardNodes().get(112).addNeighbour(this.getBoardNodes().get(116), 2);
        this.getBoardNodes().get(112).addNeighbour(this.getBoardNodes().get(115), 3);
        this.getBoardNodes().get(112).addNeighbour(this.getBoardNodes().get(111), 4);
        this.getBoardNodes().get(112).addNeighbour(this.getBoardNodes().get(103), 5);

        this.getBoardNodes().get(113).setCoordinates(6, 13);
        this.getBoardNodes().get(113).addNeighbour(this.getBoardNodes().get(105), 0);
        this.getBoardNodes().get(113).addNeighbour(this.getBoardNodes().get(114), 1);
        this.getBoardNodes().get(113).addNeighbour(this.getBoardNodes().get(117), 2);
        this.getBoardNodes().get(113).addNeighbour(this.getBoardNodes().get(116), 3);
        this.getBoardNodes().get(113).addNeighbour(this.getBoardNodes().get(112), 4);
        this.getBoardNodes().get(113).addNeighbour(this.getBoardNodes().get(104), 5);

        this.getBoardNodes().get(114).setCoordinates(7, 13);
        this.getBoardNodes().get(114).addNeighbour(this.getBoardNodes().get(106), 0);
        this.getBoardNodes().get(114).addNeighbour(this.getBoardNodes().get(117), 3);
        this.getBoardNodes().get(114).addNeighbour(this.getBoardNodes().get(113), 4);
        this.getBoardNodes().get(114).addNeighbour(this.getBoardNodes().get(105), 5);

        this.getBoardNodes().get(115).setCoordinates(5, 14);
        this.getBoardNodes().get(115).addNeighbour(this.getBoardNodes().get(112), 0);
        this.getBoardNodes().get(115).addNeighbour(this.getBoardNodes().get(116), 1);
        this.getBoardNodes().get(115).addNeighbour(this.getBoardNodes().get(118), 2);
        this.getBoardNodes().get(115).addNeighbour(this.getBoardNodes().get(111), 5);

        this.getBoardNodes().get(116).setCoordinates(6, 14);
        this.getBoardNodes().get(116).addNeighbour(this.getBoardNodes().get(113), 0);
        this.getBoardNodes().get(116).addNeighbour(this.getBoardNodes().get(117), 1);
        this.getBoardNodes().get(116).addNeighbour(this.getBoardNodes().get(119), 2);
        this.getBoardNodes().get(116).addNeighbour(this.getBoardNodes().get(118), 3);
        this.getBoardNodes().get(116).addNeighbour(this.getBoardNodes().get(115), 4);
        this.getBoardNodes().get(116).addNeighbour(this.getBoardNodes().get(112), 5);

        this.getBoardNodes().get(117).setCoordinates(7, 14);
        this.getBoardNodes().get(117).addNeighbour(this.getBoardNodes().get(114), 0);
        this.getBoardNodes().get(117).addNeighbour(this.getBoardNodes().get(119), 3);
        this.getBoardNodes().get(117).addNeighbour(this.getBoardNodes().get(116), 4);
        this.getBoardNodes().get(117).addNeighbour(this.getBoardNodes().get(113), 5);

        this.getBoardNodes().get(118).setCoordinates(5, 15);
        this.getBoardNodes().get(118).addNeighbour(this.getBoardNodes().get(116), 0);
        this.getBoardNodes().get(118).addNeighbour(this.getBoardNodes().get(119), 1);
        this.getBoardNodes().get(118).addNeighbour(this.getBoardNodes().get(120), 2);
        this.getBoardNodes().get(118).addNeighbour(this.getBoardNodes().get(115), 5);

        this.getBoardNodes().get(119).setCoordinates(6, 15);
        this.getBoardNodes().get(119).addNeighbour(this.getBoardNodes().get(117), 0);
        this.getBoardNodes().get(119).addNeighbour(this.getBoardNodes().get(120), 3);
        this.getBoardNodes().get(119).addNeighbour(this.getBoardNodes().get(118), 4);
        this.getBoardNodes().get(119).addNeighbour(this.getBoardNodes().get(116), 5);

        this.getBoardNodes().get(120).setCoordinates(6, 16);
        this.getBoardNodes().get(120).addNeighbour(this.getBoardNodes().get(119), 0);
        this.getBoardNodes().get(120).addNeighbour(this.getBoardNodes().get(118), 5);
    }

    /**
     * Calculates the shortest path distances between all pairs of nodes using Dijkstra's algorithm.
     * Stores the result in allPairDistances.
     */
    public void calculateAllPairDistances() {
        allPairDistances = new HashMap<>();
        for (CCNode startNode : boardNodes) {
            Map<Integer, Integer> distances = new HashMap<>();
            Set<Integer> visited = new HashSet<>();
            // Initialize distances
            for (CCNode node : boardNodes) {
                distances.put(node.getComponentID(), node == startNode ? 0 : Integer.MAX_VALUE);
            }
            // Priority queue for Dijkstra
            PriorityQueue<CCNode> queue = new PriorityQueue<>(Comparator.comparingInt(n -> distances.get(n.getComponentID())));
            queue.add(startNode);

            while (!queue.isEmpty()) {
                CCNode current = queue.poll();
                int currentId = current.getComponentID();
                if (!visited.add(currentId)) continue;
                int currentDist = distances.get(currentId);

                for (CCNode neighbor : current.getNeighbours()) {
                    int neighborId = neighbor.getComponentID();
                    int newDist = currentDist + 1;
                    if (newDist < distances.get(neighborId)) {
                        distances.put(neighborId, newDist);
                        queue.add(neighbor);
                    }
                }
            }
            allPairDistances.put(startNode.getComponentID(), distances);
        }
    }

    public int distanceBetween(int from, int to) {
        if (allPairDistances == null) {
            throw new UnsupportedOperationException("Distance table has not been calculated yet.");
        }
        Map<Integer, Integer> distances = allPairDistances.get(from);
        if (distances == null || !distances.containsKey(to)) {
            throw new IllegalArgumentException("Invalid node IDs: " + from + " or " + to);
        }
        return distances.getOrDefault(to, Integer.MAX_VALUE);
    }
}
