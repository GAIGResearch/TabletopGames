package games.dotsboxes;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.interfaces.IStateHeuristic;
import games.GameType;

import java.util.*;

public class DBGameState extends AbstractGameState {

    IStateHeuristic heuristic;

    // List of all edges possible
    HashSet<DBEdge> edges;
    // List of all cells possible
    HashSet<DBCell> cells;
    // Mapping from each edge to the cells it neighbours
    HashMap<DBEdge, HashSet<DBCell>> edgeToCellMap;
    // Mapping from each cell to its edges
    HashMap<DBCell, HashSet<DBEdge>> cellToEdgesMap;

    // Mutable state:
    int[] nCellsPerPlayer;
    HashMap<DBCell, Integer> cellToOwnerMap;  // Mapping from each cell to its owner, if complete
    HashMap<DBEdge, Integer> edgeToOwnerMap;  // Mapping from each edge to its owner, if placed
    boolean lastActionDidNotScore;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers      - number of players.
     */
    public DBGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.DotsAndBoxes;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{ addAll(edges); addAll(cells); }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        DBGameState dbgs = new DBGameState(gameParameters, getNPlayers());
        dbgs.edges = edges;
        dbgs.cells = cells;
        dbgs.edgeToCellMap = edgeToCellMap;
        dbgs.cellToEdgesMap = cellToEdgesMap;
        dbgs.lastActionDidNotScore = lastActionDidNotScore;

        dbgs.nCellsPerPlayer = nCellsPerPlayer.clone();
        dbgs.cellToOwnerMap = (HashMap<DBCell, Integer>) cellToOwnerMap.clone();
        dbgs.edgeToOwnerMap = (HashMap<DBEdge, Integer>) edgeToOwnerMap.clone();
        dbgs.heuristic = heuristic;
        return dbgs;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        if (heuristic == null) { // lazy initialization
            heuristic = new DotsAndBoxesHeuristic();
        }
        return heuristic.evaluateState(this, playerId);
    }

    /**
     * This provides the current score in game turns. This will only be relevant for games that have the concept
     * of victory points, etc.
     * If a game does not support this directly, then just return 0.0
     *
     * @param playerId
     * @return - double, score of current state
     */
    @Override
    public double getGameScore(int playerId) {
        return nCellsPerPlayer[playerId];
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DBGameState)) return false;
        if (!super.equals(o)) return false;
        DBGameState that = (DBGameState) o;
        return lastActionDidNotScore == that.lastActionDidNotScore && Objects.equals(heuristic, that.heuristic)
                && Objects.equals(edges, that.edges) && Objects.equals(cells, that.cells) &&
                Objects.equals(edgeToCellMap, that.edgeToCellMap) &&
                Objects.equals(cellToEdgesMap, that.cellToEdgesMap) &&
                Arrays.equals(nCellsPerPlayer, that.nCellsPerPlayer) &&
                Objects.equals(cellToOwnerMap, that.cellToOwnerMap) &&
                Objects.equals(edgeToOwnerMap, that.edgeToOwnerMap);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), edges, cells, edgeToCellMap, cellToEdgesMap,
                cellToOwnerMap, edgeToOwnerMap, lastActionDidNotScore);
        result = 31 * result + Arrays.hashCode(nCellsPerPlayer);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        int i = 0;

        for (DBEdge e: edges) {
            if (i++ != 0) {
                sb.append(",");
            }
            int owner = -1;
            if (edgeToOwnerMap.get(e) != null) {
                owner = edgeToOwnerMap.get(e);
            }
            sb.append("\"").append("Edge_Owner_").append(e.from.getX()).append("_").append(e.from.getY()).
                    append(e.to.getX()).append(e.to.getY()).append("\":").append(owner);
        }

        for (DBCell c: cells) {
            sb.append(",");
            int owner = -1;
            if (cellToOwnerMap.get(c) != null) {
                owner = cellToOwnerMap.get(c);
            }
            sb.append("\"").append("Cell_Owner_").append(c.position.getX()).append("_").append(c.position.getY()).append("\":").append(owner);
            sb.append(",");

            int edgeCount = 0;

            for (DBEdge e : cellToEdgesMap.get(c)) {
                if (edgeToOwnerMap.get(e) != null) {
                    edgeCount++;
                }
            }
            sb.append("\"").append("Cell_Edge_Count_").append(c.position.getX()).append("_").append(c.position.getY()).append("\":").append(edgeCount);
        }

        sb.append("}");
        return sb.toString();
    }


    public int countCompleteEdges(DBCell c) {
        int retValue = 0;
        for (DBEdge e: cellToEdgesMap.get(c)) {
            if (edgeToOwnerMap.containsKey(e)) {
                retValue++;
            }
        }
        return retValue;
    }
    public boolean getLastActionDidNotScore(){return lastActionDidNotScore;}
    public void setLastActionDidNotScore(boolean value){
        lastActionDidNotScore = value;}
}
