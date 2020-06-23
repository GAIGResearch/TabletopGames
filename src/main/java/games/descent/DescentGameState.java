package games.descent;

import core.AbstractGameParameters;
import core.AbstractGameState;
import core.components.Component;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.interfaces.IPrintable;
import games.descent.components.Figure;
import games.descent.components.Monster;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class DescentGameState extends AbstractGameState implements IPrintable {

    // For reference only
    HashMap<Integer, GridBoard> tiles;  // Mapping from board node ID in board configuration to tile configuration
    int[][] tileReferences;  // int corresponds to component ID of tile at that location in master board
    HashMap<String, HashSet<Vector2D>> gridReferences;  // Mapping from tile name to list of coordinates in master board for each cell

    GridBoard<String> masterBoard;
    GridBoard<Integer> masterBoardOccupancy;
    GraphBoard masterGraph;

    ArrayList<Figure> heroes;
    Figure overlord;
    ArrayList<ArrayList<Monster>> monsters;

    int overlordPlayer;

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

        heroes = new ArrayList<>();
        monsters = new ArrayList<>();
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
        copy.masterBoardOccupancy = masterBoardOccupancy.copy();
        copy.masterGraph = masterGraph.copy();
        copy.heroes = new ArrayList<>();
        for (Figure f: heroes) {
            copy.heroes.add(f.copy());
        }
        copy.monsters = new ArrayList<>();
        for (ArrayList<Monster> ma: monsters) {
            ArrayList<Monster> maC = new ArrayList<>();
            for (Monster m: ma) {
                maC.add(m.copy());
            }
            copy.monsters.add(maC);
        }
        copy.tileReferences = tileReferences.clone();  // TODO deep
        copy.gridReferences = new HashMap<>(gridReferences); // TODO deep
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

    public GraphBoard getMasterGraph() {
        return masterGraph;
    }

    public ArrayList<Figure> getHeroes() {
        return heroes;
    }

    public ArrayList<ArrayList<Monster>> getMonsters() {
        return monsters;
    }

    public int[][] getTileReferences() {
        return tileReferences;
    }

    public HashMap<String, HashSet<Vector2D>> getGridReferences() {
        return gridReferences;
    }

    public GridBoard<Integer> getMasterBoardOccupancy() {
        return masterBoardOccupancy;
    }

    @Override
    public String toString() {
        return masterBoard.toString();
    }
}
