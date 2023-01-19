package games.dotsboxes;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.interfaces.IStateHeuristic;
import core.turnorders.AlternatingTurnOrder;
import core.turnorders.StandardTurnOrder;
import core.turnorders.TurnOrder;
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
    boolean lastActionScored;

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
    protected TurnOrder _createTurnOrder(int nPlayers) {
        return new AlternatingTurnOrder(nPlayers);
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
        dbgs.lastActionScored = lastActionScored;

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
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DBGameState)) return false;
        if (!super.equals(o)) return false;
        DBGameState that = (DBGameState) o;
        return Arrays.equals(nCellsPerPlayer, that.nCellsPerPlayer) &&
                lastActionScored == that.lastActionScored &&
                Objects.equals(edgeToOwnerMap, that.edgeToOwnerMap) &&
                Objects.equals(cellToOwnerMap, that.cellToOwnerMap);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(gameParameters, turnOrder, gamePhase, gameStatus, cellToOwnerMap, edgeToOwnerMap, lastActionScored);
        result = 31 * result + Arrays.hashCode(nCellsPerPlayer);
        return result;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int result = Objects.hash(gameParameters);
        sb.append(result).append("|");
        result = Objects.hash(turnOrder);
        sb.append(result).append("|");
        result = Objects.hash(getAllComponents());
        sb.append(result).append("|");
        result = Objects.hash(gameStatus);
        sb.append(result).append("|");
        result = Objects.hash(gamePhase);
        sb.append(result).append("|");
        result = Arrays.hashCode(playerResults);
        sb.append(result).append("|*|");
        result = Arrays.hashCode(nCellsPerPlayer);
        sb.append(result).append("|");
        result = Objects.hashCode(cellToOwnerMap);
        sb.append(result).append("|");
        result = Objects.hashCode(edgeToOwnerMap);
        sb.append(result).append("|");
        result = Objects.hashCode(lastActionScored);
        sb.append(result).append("|");

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
    public boolean getLastActionScored(){return lastActionScored;}
    public void setLastActionScored(boolean value){lastActionScored = value;}
}
