package games.dotsboxes;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.interfaces.IStateHeuristic;
import core.turnorders.AlternatingTurnOrder;

import java.util.*;

public class DBGameState extends AbstractGameState {

    IStateHeuristic heuristic = new DotsAndBoxesHeuristic();

    // Only component needed
    GridBoard<DBCell> grid;

    // Variables for speeding up computations
    int nCellsComplete;
    int[] nCellsPerPlayer;
    HashMap<DBEdge, HashSet<DBCell>> edgeToCellMap;  // Mapping from each edge to the cells it neighbours

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers      - number of players.
     */
    public DBGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new AlternatingTurnOrder(nPlayers));
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{ add(grid); }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        DBGameState dbgs = new DBGameState(gameParameters, getNPlayers());
        dbgs.grid = deepCopyGrid();
        dbgs.nCellsPerPlayer = nCellsPerPlayer.clone();
        dbgs.nCellsComplete = nCellsComplete;
        dbgs.edgeToCellMap = generateEdgeToCellMap(dbgs.grid);  // Re-generate this mapping from the copied grid
        return dbgs;
    }

    private GridBoard<DBCell> deepCopyGrid() {
        GridBoard<DBCell> gridCopy = grid.copy();
        for (int i = 0; i < grid.getHeight(); i++) {
            for (int j = 0; j < grid.getWidth(); j++) {
                gridCopy.setElement(j, i, grid.getElement(j, i).copy());
            }
        }
        return gridCopy;
    }

    private HashMap<DBEdge, HashSet<DBCell>> generateEdgeToCellMap(GridBoard<DBCell> g) {
        HashMap<DBEdge, HashSet<DBCell>> copy = new HashMap<>();
        for (int i = 0; i < g.getHeight(); i++) {
            for (int j = 0; j < g.getWidth(); j++) {
                DBCell c = g.getElement(j, i);
                for (DBEdge edge: c.edges) {
                    if (!copy.containsKey(edge)) {
                        copy.put(edge, new HashSet<>());
                    }
                    copy.get(edge).add(c);
                }
            }
        }
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
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
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<>();  // fully observable at all times
    }

    @Override
    protected void _reset() {
        grid = null;
        nCellsComplete = 0;
        nCellsPerPlayer = null;
        edgeToCellMap = null;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DBGameState)) return false;
        if (!super.equals(o)) return false;
        DBGameState that = (DBGameState) o;
        return nCellsComplete == that.nCellsComplete &&
                Objects.equals(grid, that.grid) &&
                Arrays.equals(nCellsPerPlayer, that.nCellsPerPlayer) &&
                Objects.equals(edgeToCellMap, that.edgeToCellMap);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), grid, nCellsComplete, edgeToCellMap);
        result = 31 * result + Arrays.hashCode(nCellsPerPlayer);
        return result;
    }
}
