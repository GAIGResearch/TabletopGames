package games.battlelore;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
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
    GridBoard gameBoard;
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
        MapTile tile = (MapTile) gameBoard.getElement(locX, locY);
        if (tile != null) {
            tile.AddUnit(unit);
        }
    }

    public void AddScore(int playerId, int score) {
        playerScores[playerId] += score;
    }

    public void SetUnitsAsOrderable(int locX, int locY) {
        MapTile tile = (MapTile) gameBoard.getElement(locX, locY);
        if (tile != null) {
            tile.SetAsOrderable();
        }
    }

    public void RemoveUnit(int locX, int locY) {
        MapTile tile = (MapTile) gameBoard.getElement(locX, locY);
        if (tile != null) {
            tile.RemoveUnit();
        }
    }

    public ArrayList<MapTile> GetMoveableUnitsFromTile(Unit.Faction faction) {
        ArrayList<MapTile> tiles = new ArrayList<MapTile>();

        for (int x = 0; x < gameBoard.getWidth(); x++) {
            for (int y = 0; y < gameBoard.getHeight(); y++) {
                MapTile tile = (MapTile) gameBoard.getElement(x, y);
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
        for (int x = 0; x < gameBoard.getWidth(); x++) {
            for (int y = 0; y < gameBoard.getHeight(); y++) {
                MapTile tile = (MapTile) gameBoard.getElement(x, y);
                if (tile.GetUnits() != null && tile.GetFaction() == faction &&
                        tile.GetUnits().get(0).CanAttack()) {
                    tiles.add(tile);
                }
            }
        }
        return tiles;
    }

    public int[][] GetPossibleLocationsForUnits(MapTile tile) {
        int[][] possibleLocations = new int[gameBoard.getWidth()][2];
        GetPossibleLocations(tile, possibleLocations, false);
        if (possibleLocations.length == 0) {
            GetPossibleLocations(tile, possibleLocations, true);
        }
        return possibleLocations;
    }

    private void GetPossibleLocations(MapTile tile, int[][] possibleLocations, boolean isMovementFlexible) {
        int moveRange = tile.GetUnits().get(0).moveRange;
        int counter = 0;
        for (int x = 0; x < gameBoard.getWidth(); x++) {
            for (int y = 0; y < gameBoard.getHeight(); y++) {
                possibleLocations[x][0] = -1;
                possibleLocations[x][1] = -1;

                MapTile possibleTile = (MapTile) gameBoard.getElement(x, y);
                if (possibleTile.GetUnits() == null) {
                    double distance = Math.sqrt(Math.pow(Math.abs(possibleTile.getLocationX() - tile.getLocationX()), 2) +
                            Math.pow(Math.abs(possibleTile.getLocationY() - tile.getLocationY()), 2));
                    if (distance <= moveRange || isMovementFlexible) {
                        possibleLocations[counter][0] = possibleTile.getLocationX();
                        possibleLocations[counter][1] = possibleTile.getLocationY();
                        counter++;
                    }
                }
            }
        }
    }

    public int[][] GetPossibleTargetUnits(MapTile attackUnit) {
        int[][] possibleLocations = new int[gameBoard.getWidth()][nPlayers];
        boolean isMelee = attackUnit.GetUnits().get(0).isMelee;
        BattleloreGameParameters parameters = (BattleloreGameParameters) gameParameters;
        int range = parameters.getTroopRange(isMelee);
        int counter = 0;

        for (int x = 0; x < gameBoard.getWidth(); x++) {
            for (int y = 0; y < gameBoard.getHeight(); y++) {
                possibleLocations[x][0] = -1;
                possibleLocations[x][1] = -1;
                MapTile possibleTile = (MapTile) gameBoard.getElement(x, y);
                if (possibleTile.GetUnits() != null && possibleTile.GetFaction() != attackUnit.GetFaction()) {
                    double distance = Math.sqrt(Math.pow(Math.abs(possibleTile.getLocationX() - attackUnit.getLocationX()), 2) +
                            Math.pow(Math.abs(possibleTile.getLocationY() - attackUnit.getLocationY()), 2));
                    if (distance <= range) {
                        possibleLocations[counter][0] = possibleTile.getLocationX();
                        possibleLocations[counter][1] = possibleTile.getLocationY();
                        counter++;
                    }
                }
            }
        }
        return possibleLocations;
    }

    public GridBoard getBoard() {
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

        for (int x = 0; x < gameBoard.getWidth(); x++) {
            for(int y = 0; y < gameBoard.getHeight(); y++) {
                state.gameBoard.setElement(x, y, gameBoard.getElement(x,y).copy());
            }
        }
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
        if (!super.equals(o)) return false;
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
