package games.battlelore;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.grid.HexCoord;
import core.components.grid.SparseGrid;
import core.interfaces.IGamePhase;
import games.GameType;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;

import java.util.*;

public class BattleloreGameState extends AbstractGameState {


    public enum BattleloreGamePhase implements IGamePhase {
        CommandAndOrderStep, //Player Plays One Command Card
        MoveStep,
        AttackStep,
        VictoryPointStep,
        DrawStep,
        LoreStep
        //Last three steps can be implemented later
    }

    public enum UnitType {
        Decoy, BloodHarvester, ViperLegion, CitadelGuard, YeomanArcher
    }

    int[] playerScores;
    // the tiles, by their hexes
    SparseGrid<HexCoord, MapTile> gameBoard;
    List<Unit> unitTypes;

    public BattleloreGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        playerScores = new int[nPlayers];
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Battlelore;
    }

    public Unit GetUnitFromType(UnitType type) {
        int unitType = 0;
        switch (type) {
            case BloodHarvester:
                unitType = 1;
                break;
            case ViperLegion:
                unitType = 2;
                break;
            case CitadelGuard:
                unitType = 3;
                break;
            case YeomanArcher:
                unitType = 4;
                break;
        }
        return (Unit)unitTypes.get(unitType).copy();
    }

    public void AddUnit(int locX, int locY, Unit unit) {
        MapTile tile = getTile(locX, locY);
        if (tile != null) {
            List<Unit> units = tile.GetUnits() == null ? new ArrayList<>() : new ArrayList<>(tile.GetUnits());
            units.add(unit);
            setTile(tile.withUnits(units));
        }
    }

    public void AddScore(int playerId, int score) {
        playerScores[playerId] += score;
    }

    public void SetUnitsAsOrderable(int locX, int locY) {
        SetUnitOrders(getTile(locX, locY), true, true);
    }

    /**
     * Replaces the units on the tile with ones that may or may not still move and attack.
     */
    public void SetUnitOrders(MapTile tile, boolean canMove, boolean canAttack) {
        if (tile != null && tile.GetUnits() != null) {
            List<Unit> units = new ArrayList<>();
            for (Unit unit : tile.GetUnits()) {
                units.add(unit.withOrders(canMove, canAttack));
            }
            setTile(tile.withUnits(units));
        }
    }

    public void RemoveUnit(int locX, int locY) {
        MapTile tile = getTile(locX, locY);
        if (tile != null && tile.GetUnits() != null) {
            List<Unit> units = new ArrayList<>(tile.GetUnits());
            units.remove(units.size() - 1);
            setTile(tile.withUnits(units));
        }
    }

    /**
     * The tile at the offset position on the board, whose odd rows are shifted right; null if it is off the board.
     */
    public MapTile getTile(int locX, int locY) {
        return gameBoard.get(HexCoord.fromOffset(locX, locY, HexCoord.Offset.ODD_R));
    }

    /**
     * The tile with the componentID, as it now is.
     */
    public MapTile getTile(int componentID) {
        // not getComponentById, which may hold an earlier version of the tile
        for (MapTile tile : gameBoard.asMap().values()) {
            if (tile.getComponentID() == componentID) {
                return tile;
            }
        }
        return null;
    }

    /**
     * Puts the tile on the board, in place of the one at its position.
     */
    public void setTile(MapTile tile) {
        gameBoard.set(tile.getHex(), tile);
    }

    public int getWidth() {
        return ((BattleloreGameParameters) gameParameters).hexWidth;
    }

    public int getHeight() {
        return ((BattleloreGameParameters) gameParameters).hexHeight;
    }

    public ArrayList<MapTile> GetMoveableUnitsFromTile(Unit.Faction faction) {
        ArrayList<MapTile> tiles = new ArrayList<MapTile>();

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                MapTile tile = getTile(x, y);
                if (tile.GetUnits() != null && tile.GetFaction() == faction &&
                        tile.GetUnits().get(0).CanMove()) {
                    tiles.add(tile);
                }
            }
        }
        return tiles;
    }

    public ArrayList<MapTile> GetReadyForAttackUnitsFromTile(Unit.Faction faction) {
        ArrayList<MapTile> tiles = new ArrayList<MapTile>();
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                MapTile tile = getTile(x, y);
                if (tile.GetUnits() != null && tile.GetFaction() == faction &&
                        tile.GetUnits().get(0).CanAttack()) {
                    tiles.add(tile);
                }
            }
        }
        return tiles;
    }

    /**
     * The empty tiles the units on the tile can move to.
     */
    public List<MapTile> GetPossibleLocationsForUnits(MapTile tile) {
        // within their move range, or anywhere if no tile in range is empty
        List<MapTile> possibleLocations = GetPossibleLocations(tile, false);
        if (possibleLocations.isEmpty()) {
            possibleLocations = GetPossibleLocations(tile, true);
        }
        return possibleLocations;
    }

    private List<MapTile> GetPossibleLocations(MapTile tile, boolean isMovementFlexible) {
        int moveRange = tile.GetUnits().get(0).moveRange;
        List<MapTile> possibleLocations = new ArrayList<>();
        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                MapTile possibleTile = getTile(x, y);
                if (possibleTile.GetUnits() == null
                        && (possibleTile.getHex().distance(tile.getHex()) <= moveRange || isMovementFlexible)) {
                    possibleLocations.add(possibleTile);
                }
            }
        }
        return possibleLocations;
    }

    /**
     * The tiles with enemy units within range of the units on the tile.
     */
    public List<MapTile> GetPossibleTargetUnits(MapTile attackUnit) {
        boolean isMelee = attackUnit.GetUnits().get(0).isMelee;
        BattleloreGameParameters parameters = (BattleloreGameParameters) gameParameters;
        int range = parameters.getTroopRange(isMelee);
        List<MapTile> possibleLocations = new ArrayList<>();

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                MapTile possibleTile = getTile(x, y);
                if (possibleTile.GetUnits() != null && possibleTile.GetFaction() != attackUnit.GetFaction()
                        && possibleTile.getHex().distance(attackUnit.getHex()) <= range) {
                    possibleLocations.add(possibleTile);
                }
            }
        }
        return possibleLocations;
    }

    public SparseGrid<HexCoord, MapTile> getBoard() {
        return gameBoard;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{ add(gameBoard); }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        BattleloreGameState state = new BattleloreGameState(gameParameters.copy(), getNPlayers());

        state.gameBoard = gameBoard.copy();
        state.unitTypes = unitTypes; // immutable
        System.arraycopy(playerScores, 0, state.playerScores, 0, playerScores.length);

        return state;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        /*
         * This provides the current score in game turns. This will only be relevant for games that have the concept
         * of victory points, etc.
         * If a game does not support this directly, then just return 0.0
         *
         * @param playerId
         * @return - double, score of current state
         */
        return new BattleloreHeuristic().evaluateState(this, playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        return playerScores[playerId];
    }


    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BattleloreGameState that)) return false;
        return Arrays.equals(playerScores, that.playerScores) && Objects.equals(gameBoard, that.gameBoard) && Objects.equals(unitTypes, that.unitTypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), gameBoard, unitTypes);
        result = 31 * result + Arrays.hashCode(playerScores);
        return result;
    }

    @Override
    public String toString() {
        return gameParameters.hashCode() + "|" +
                gameStatus.hashCode() + "|" +
                gamePhase.hashCode() + "|" +
                Arrays.hashCode(playerResults) + "|*|" +
                unitTypes.hashCode() + "|" +
                gameBoard.hashCode() + "|" +
                Arrays.hashCode(playerScores) + "|";
    }
}
