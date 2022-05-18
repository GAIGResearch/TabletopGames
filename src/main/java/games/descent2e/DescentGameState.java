package games.descent2e;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.interfaces.IGamePhase;
import core.interfaces.IPrintable;
import games.GameType;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.*;

public class DescentGameState extends AbstractGameState implements IPrintable {

    public enum DescentPhase implements IGamePhase {
        ForceMove  // Used when a figure started a (possibly valid move action) and is currently overlapping a friendly figure
    }
    DescentGameData data;

    // For reference only
    HashMap<Integer, GridBoard> tiles;  // Mapping from board node ID in board configuration to tile configuration
    int[][] tileReferences;  // int corresponds to component ID of tile at that location in master board
    HashMap<String, HashSet<Vector2D>> gridReferences;  // Mapping from tile name to list of coordinates in master board for each cell

    GridBoard masterBoard;

    ArrayList<Hero> heroes;
    Figure overlord;
    ArrayList<ArrayList<Monster>> monsters;

    int overlordPlayer;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players for this game.
     */
    public DescentGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new DescentTurnOrder(nPlayers), GameType.Descent2e);
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
        copy.overlord = overlord.copy();
        copy.heroes = new ArrayList<>();
        for (Hero f: heroes) {
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
    protected double _getHeuristicScore(int playerId) {
        // TODO
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        // TODO
        return 0;
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        // TODO
        return null;
    }

    @Override
    protected void _reset() {
        // TODO
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DescentGameState)) return false;
        if (!super.equals(o)) return false;
        DescentGameState that = (DescentGameState) o;
        return overlordPlayer == that.overlordPlayer &&
                Objects.equals(tiles, that.tiles) &&
                Arrays.equals(tileReferences, that.tileReferences) &&
                Objects.equals(gridReferences, that.gridReferences) &&
                Objects.equals(masterBoard, that.masterBoard) &&
                Objects.equals(heroes, that.heroes) &&
                Objects.equals(overlord, that.overlord) &&
                Objects.equals(monsters, that.monsters);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), tiles, gridReferences, masterBoard, heroes, overlord, monsters, overlordPlayer);
        result = 31 * result + Arrays.hashCode(tileReferences);
        return result;
    }

    DescentGameData getData() {
        return data;
    }

    public GridBoard getMasterBoard() {
        return masterBoard;
    }

    public ArrayList<Hero> getHeroes() {
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

    @Override
    public String toString() {
        return masterBoard.toString();
    }
}
