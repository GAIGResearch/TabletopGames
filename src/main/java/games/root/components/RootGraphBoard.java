package games.root.components;

import core.CoreConstants;
import core.components.Component;
import core.interfaces.IComponentContainer;
import games.root.RootParameters;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class RootGraphBoard extends Component implements IComponentContainer<RootBoardNodeWithRootEdges> {

    // List of nodes in the board graph, mapping component ID to object reference
    protected Map<Integer, RootBoardNodeWithRootEdges> boardNodes;

    public RootGraphBoard(String name) {
        super(CoreConstants.ComponentType.BOARD, name);
        boardNodes = new HashMap<>();
    }

    public RootGraphBoard() {
        super(CoreConstants.ComponentType.BOARD);
        boardNodes = new HashMap<>();
    }

    protected RootGraphBoard(String name, int ID) {
        super(CoreConstants.ComponentType.BOARD, name, ID);
        boardNodes = new HashMap<>();
    }

    RootGraphBoard(int ID) {
        super(CoreConstants.ComponentType.BOARD, ID);
        boardNodes = new HashMap<>();
    }

    /**
     * Copy method, to be implemented by all subclasses.
     *
     * @return - a new instance of this Board, deep copy.
     */
    @Override
    public RootGraphBoard copy() {
        RootGraphBoard b = new RootGraphBoard(componentName, componentID);
        HashMap<Integer, RootBoardNodeWithRootEdges> nodeCopies = new HashMap<>();
        HashMap<Integer, RootEdge> RootEdgeCopies = new HashMap<>();
        // Copy board nodes
        for (RootBoardNodeWithRootEdges bn : boardNodes.values()) {
            RootBoardNodeWithRootEdges bnCopy = bn.copy();
            if (bnCopy == null)
                bnCopy = new RootBoardNodeWithRootEdges(bn.getOwnerId(), bn.getComponentID(), bn.corner, bn.identifier);
            bn.copyComponentTo(bnCopy);
            bnCopy.setXY(bn.getX(), bn.getY());
            bnCopy.setClearingType(bn.getClearingType());
            bnCopy.maxBuildings = bn.maxBuildings;
            bnCopy.currentBuildings = bn.currentBuildings;
            bnCopy.rulerID = bn.rulerID;
            bnCopy.ruins = bn.ruins;

            //cat
            bnCopy.recruiter = bn.recruiter;
            bnCopy.workshops = bn.workshops;
            bnCopy.sawmill = bn.sawmill;
            bnCopy.wood = bn.wood;
            bnCopy.catWarriors = bn.catWarriors;
            bnCopy.keep = bn.keep;

            bnCopy.roost = bn.roost;
            bnCopy.eyrieWarriors = bn.eyrieWarriors;

            bnCopy.base = bn.base;
            bnCopy.sympathy = bn.sympathy;
            bnCopy.woodlandWarriors = bn.woodlandWarriors;

            bnCopy.vagabond = bn.vagabond;
            nodeCopies.put(bn.getComponentID(), bnCopy);

            // Copy RootEdges
            for (RootEdge e : bn.neighbourRootEdgeMapping.keySet()) {
                RootEdgeCopies.put(e.getComponentID(), e.copy());
            }
        }
        // Assign neighbours and RootEdges
        for (RootBoardNodeWithRootEdges bn : boardNodes.values()) {
            RootBoardNodeWithRootEdges bnCopy = nodeCopies.get(bn.getComponentID());
            for (Map.Entry<RootEdge, RootBoardNodeWithRootEdges> e : bn.neighbourRootEdgeMapping.entrySet()) {
                bnCopy.addNeighbour(nodeCopies.get(e.getValue().getComponentID()), RootEdgeCopies.get(e.getKey().getComponentID()));
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
    public Collection<RootBoardNodeWithRootEdges> getBoardNodes() {
        return boardNodes.values();
    }

    public Collection<RootBoardNodeWithRootEdges> getNonForrestBoardNodes() {
        return boardNodes.values().stream()
                .filter(node -> !node.getClearingType().equals(RootParameters.ClearingTypes.Forrest))
                .collect(Collectors.toList());
    }

    public Collection<RootBoardNodeWithRootEdges> getNonSympathyNodesAdjacentToSympathy(){
        return boardNodes.values().stream().filter(node -> (!node.sympathy && !node.getClearingType().equals(RootParameters.ClearingTypes.Forrest) && node.hasSympatheticNeighbour())).collect(Collectors.toList());
    }

    public Collection<RootBoardNodeWithRootEdges> getBaseNodes(){
        return boardNodes.values().stream().filter(node -> node.base).collect(Collectors.toList());
    }
    public Collection<RootBoardNodeWithRootEdges> getSympatheticClearings() {
        return boardNodes.values().stream().filter(RootBoardNodeWithRootEdges::getSympathy).collect(Collectors.toList());
    }

    public int getSympatheticClearingsOfTypeCount(RootParameters.ClearingTypes clearingType){
        return boardNodes.values().stream().filter(node -> node.getSympathy() && node.getClearingType() == clearingType).toList().size();
    }

    public RootBoardNodeWithRootEdges getVagabondClearing(){
        for (RootBoardNodeWithRootEdges node: boardNodes.values()){
            if (node.vagabond == 1){
                return node;
            }
        }
        return null;
    }
    public RootBoardNodeWithRootEdges getKeepNode() {
        for (RootBoardNodeWithRootEdges node : boardNodes.values()) {
            if (node.getKeep()) {
                return node;
            }
        }

        System.out.println("Keep is not built");
        return null;

    }

    /**
     * Returns the node in the list which matches the given ID
     *
     * @param id - ID of node to search for.
     * @return - node matching ID.
     */
    public RootBoardNodeWithRootEdges getNodeByID(int id) {
        return boardNodes.get(id);
    }

    /**
     * Sets the list of board nodes to the given list.
     *
     * @param boardNodes - new list of board nodes.
     */
    public void setBoardNodes(List<RootBoardNodeWithRootEdges> boardNodes) {
        for (RootBoardNodeWithRootEdges bn : boardNodes) {
            this.boardNodes.put(bn.getComponentID(), bn);
        }
    }

    public void setBoardNodes(Map<Integer, RootBoardNodeWithRootEdges> boardNodes) {
        this.boardNodes = boardNodes;
    }

    public void addBoardNode(RootBoardNodeWithRootEdges bn) {
        this.boardNodes.put(bn.getComponentID(), bn);
    }

    public void removeBoardNode(RootBoardNodeWithRootEdges bn) {
        this.boardNodes.remove(bn.getComponentID());
    }

    public void breakConnection(RootBoardNodeWithRootEdges bn1, RootBoardNodeWithRootEdges bn2, RootEdge RootEdge) {
        bn1.removeNeighbour(bn2, RootEdge);
        bn2.removeNeighbour(bn1, RootEdge);

        // Check if they have at least 1 more neighbour on this board. If not, remove node from this board
        boolean inBoard = false;
        for (RootBoardNodeWithRootEdges n : bn1.getNeighbours()) {
            if (boardNodes.containsKey(n.getComponentID())) {
                inBoard = true;
                break;
            }
        }
        if (!inBoard) boardNodes.remove(bn1.getComponentID());

        inBoard = false;
        for (RootBoardNodeWithRootEdges n : bn2.getNeighbours()) {
            if (boardNodes.containsKey(n.getComponentID())) {
                inBoard = true;
                break;
            }
        }
        if (!inBoard) boardNodes.remove(bn2.getComponentID());
    }

    public RootEdge addConnection(RootBoardNodeWithRootEdges bn1, RootBoardNodeWithRootEdges bn2) {
        RootEdge RootEdge = new RootEdge();
        addConnection(bn1, bn2, RootEdge);
        return RootEdge;
    }

    public void addConnection(int bn1id, int bn2id) {
        RootBoardNodeWithRootEdges bn1 = boardNodes.get(bn1id);
        RootBoardNodeWithRootEdges bn2 = boardNodes.get(bn2id);
        RootEdge RootEdge = new RootEdge();
        addConnection(bn1, bn2, RootEdge);
    }

    public void addConnection(RootBoardNodeWithRootEdges bn1, RootBoardNodeWithRootEdges bn2, RootEdge RootEdge) {
        bn1.addNeighbour(bn2, RootEdge);
        bn2.addNeighbour(bn1, RootEdge);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RootGraphBoard that = (RootGraphBoard) o;
        return Objects.equals(boardNodes, that.boardNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), boardNodes);
    }

    @Override
    public List<RootBoardNodeWithRootEdges> getComponents() {
        return new ArrayList<>(getBoardNodes());
    }

    @Override
    public CoreConstants.VisibilityMode getVisibilityMode() {
        return CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
    }

    public int getNumberRoosts() {
        int amount = 0;
        for (RootBoardNodeWithRootEdges node : boardNodes.values()) {
            amount += node.roost;
        }
        return amount;
    }

    public RootBoardNodeWithRootEdges getFewestWarriorsNode() {
        int amount = Integer.MAX_VALUE;
        RootBoardNodeWithRootEdges tmp = null;
        for (RootBoardNodeWithRootEdges node : boardNodes.values()) {
            int nodeAmount = node.catWarriors + node.eyrieWarriors + node.woodlandWarriors + node.vagabond;
            if (nodeAmount < amount && node.getClearingType() != RootParameters.ClearingTypes.Forrest && node.hasBuildingRoom() && !node.keep && node.hasBuildingRoom()) {
                tmp = node;
                amount = nodeAmount;
            }
        }
        return tmp;
    }

    public List<RootBoardNodeWithRootEdges> getNodesOfType(RootParameters.ClearingTypes type) {
        if(type == RootParameters.ClearingTypes.Bird){
            return boardNodes.values().stream().filter(node-> node.getClearingType()!= RootParameters.ClearingTypes.Forrest).collect(Collectors.toList());
        }
        return boardNodes.values().stream().filter(node -> node.getClearingType() == type).collect(Collectors.toList());
    }

    public List<RootBoardNodeWithRootEdges> getRecruiters() {
        return boardNodes.values().stream().filter(node -> node.recruiter > 0).collect(Collectors.toList());
    }

    public List<RootBoardNodeWithRootEdges> getSawmills(){
        return boardNodes.values().stream().filter(node -> node.sawmill > 0).collect(Collectors.toList());
    }

    public void updateRulers(){
        for (RootBoardNodeWithRootEdges node: boardNodes.values()){
            node.updateOwner();
        }
    }

}
