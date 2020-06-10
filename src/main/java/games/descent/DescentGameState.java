package games.descent;

import core.AbstractGameParameters;
import core.AbstractGameState;
import core.components.Component;
import core.components.GridBoard;
import core.interfaces.IPrintable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DescentGameState extends AbstractGameState implements IPrintable {

    // For reference only
    HashMap<Integer, GridBoard> tiles;  // Mapping from board node ID in board configuration to tile configuration
    int[][] tileReferences;  // int corresponds to component ID of tile at that location in master board

    GridBoard<String> masterBoard;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players for this game.
     */
    public DescentGameState(AbstractGameParameters gameParameters, int nPlayers) {
        super(gameParameters, new DescentTurnOrder(nPlayers));
        tiles = new HashMap<>();
        data = new DescentGameData();
        data.load(((DescentParameters)gameParameters).getDataPath());
    }

    @Override
    protected List<Component> _getAllComponents() {
        ArrayList<Component> components = new ArrayList<>();
        components.add(masterBoard);
        // TODO
        return components;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        DescentGameState copy = new DescentGameState(gameParameters, getNPlayers());
        copy.tiles = new HashMap<>(tiles);  // TODO: deep copy
        copy.masterBoard = masterBoard.copy();
        // TODO
        return copy;
    }

    @Override
    protected double _getScore(int playerId) {
        // TODO
        return 0;
    }

    @Override
    protected void _reset() {
        // TODO
    }

    DescentGameData getData() {
        return (DescentGameData) data;
    }

    public GridBoard<String> getMasterBoard() {
        return masterBoard;
    }

    @Override
    public String toString() {
        return masterBoard.toString();
    }
}
