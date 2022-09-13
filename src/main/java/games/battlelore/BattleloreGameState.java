package games.battlelore;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.interfaces.IGamePhase;
import core.turnorders.StandardTurnOrder;
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
        Decoy, BloodHarvester, ViperLegion, CitadelGuard, YeomanArcher;
    }

    int[] playerScores;
    GridBoard<MapTile> gameBoard;
    List<Unit> unitTypes;

    public BattleloreGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new StandardTurnOrder(nPlayers), GameType.Battlelore);
        playerScores = new int[nPlayers];
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
            default:
                unitType = 0;
                break;
        }
        Unit unit = (Unit)unitTypes.get(unitType).copy();
        return unit;
    }

    public void AddUnit(int locX, int locY, Unit unit) {
        MapTile tile = gameBoard.getElement(locX, locY);
        if (tile != null) {
            gameBoard.getElement(locX, locY).AddUnit(unit);
        }
    }

    public void AddScore(int playerId, int score) {
        playerScores[playerId] += score;
    }

    public void IncrementTurn(int playerId) {
        turnOrder.moveToNextPlayer(this, playerId);
    }

    public void SetUnitsAsOrderable(int locX, int locY) {
        MapTile tile = gameBoard.getElement(locX, locY);
        if (tile != null) {
            gameBoard.getElement(locX, locY).SetAsOrderable();
        }
    }

    public void RemoveUnit(int locX, int locY) {
        MapTile tile = gameBoard.getElement(locX, locY);
        if (tile != null) {
            gameBoard.getElement(locX, locY).RemoveUnit();
        }
    }

    public ArrayList<MapTile> GetMoveableUnitsFromTile(Unit.Faction faction) {
        ArrayList<MapTile> tiles = new ArrayList<MapTile>();

        for (int x = 0; x < gameBoard.getWidth(); x++) {
            for (int y = 0; y < gameBoard.getHeight(); y++) {
                MapTile tile = gameBoard.getElement(x, y);
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
                MapTile tile = gameBoard.getElement(x, y);
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
        possibleLocations = GetPossibleLocations(tile, possibleLocations, false);
        if (possibleLocations.length == 0) {
            possibleLocations = GetPossibleLocations(tile, possibleLocations, true);
        }
        return possibleLocations;
    }

    private int[][] GetPossibleLocations(MapTile tile, int[][] possibleLocations, boolean isMovementFlexible) {
        int moveRange = tile.GetUnits().get(0).moveRange;
        int counter = 0;
        for (int x = 0; x < gameBoard.getWidth(); x++) {
            for (int y = 0; y < gameBoard.getHeight(); y++) {
                possibleLocations[x][0] = -1;
                possibleLocations[x][1] = -1;

                MapTile possibleTile = gameBoard.getElement(x, y);
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
        return possibleLocations;
    }

    public int[][] GetPossibleTargetUnits(MapTile attackUnit) {
        int[][] possibleLocations = new int[gameBoard.getWidth()][turnOrder.nPlayers()];
        boolean isMelee = attackUnit.GetUnits().get(0).isMelee;
        BattleloreGameParameters parameters = (BattleloreGameParameters) gameParameters;
        int range = parameters.getTroopRange(isMelee);
        int counter = 0;

        for (int x = 0; x < gameBoard.getWidth(); x++) {
            for (int y = 0; y < gameBoard.getHeight(); y++) {
                possibleLocations[x][0] = -1;
                possibleLocations[x][1] = -1;
                MapTile possibleTile = gameBoard.getElement(x, y);
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

    public GridBoard<MapTile> getBoard() {
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
        /**
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
    protected void _reset() {
        gameBoard = null;
        playerScores = new int[getNPlayers()];
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof BattleloreGameState)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        BattleloreGameState other = (BattleloreGameState) o;
        return Objects.equals(gameBoard, other.gameBoard);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(gameParameters, turnOrder, gameStatus, gamePhase);
        result = 31 * result + Arrays.hashCode(playerResults);
        result = 31 * result + Objects.hash(unitTypes);
        result = 31 * result * Arrays.hashCode(playerScores);
        result = 31 * result * gameBoard.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(gameParameters.hashCode()).append("|");
        sb.append(turnOrder.hashCode()).append("|");
        sb.append(gameStatus.hashCode()).append("|");
        sb.append(gamePhase.hashCode()).append("|");
        sb.append(Arrays.hashCode(playerResults)).append("|*|");
        sb.append(unitTypes.hashCode()).append("|");
        sb.append(gameBoard.hashCode()).append("|");
        sb.append(Arrays.hashCode(playerScores)).append("|");
        return sb.toString();
    }
}
