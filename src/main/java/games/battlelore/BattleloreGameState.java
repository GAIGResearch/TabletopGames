package games.battlelore;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.GridBoard;
import core.components.PartialObservableDeck;
import core.interfaces.IGamePhase;
import core.interfaces.IStateHeuristic;
import games.GameType;
import games.battlelore.cards.CommandCard;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;

import java.util.*;

public class BattleloreGameState extends AbstractGameState
{

    public enum BattleloreGamePhase implements IGamePhase
    {
        CommandAndOrderStep,//Player Plays One Command Card
        MoveStep,
        AttackStep,
        VictoryPointStep,
        DrawStep,
        LoreStep
    }

    public enum UnitType
    {
        Decoy, BloodHarvester, ViperLegion, CitadelGuard, YeomanArcher;
    }

    int playerCount;
    Random random;
    BattleloreGameParameters parameters;
    PartialObservableDeck<CommandCard>[] playerHands;
    Deck<CommandCard>[] playerDiscards;
    //Deck<CommandCard>[] playerActi;

    GridBoard<MapTile> gameBoard;
    List<Unit> unitTypes;


    public BattleloreGameState(AbstractParameters gameParameters, int nPlayers)
    {
        super(gameParameters, new BattleloreTurnOrder(nPlayers), GameType.Battlelore);
        data = new BattleloreData();
        playerCount = nPlayers;
        parameters = (BattleloreGameParameters) gameParameters;
        data.load(parameters.getDataPath());
    }

    public Unit GetUnitFromType(UnitType type)
    {
        int unitType = 0;
        switch (type)
        {
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

    public void AddUnit(int locX, int locY, Unit unit)
    {
        MapTile tile = gameBoard.getElement(locX, locY);
        if (tile != null)
        {
            gameBoard.getElement(locX, locY).AddUnit(unit);
        }
    }

    public void SetUnitsAsOrderable(int locX, int locY)
    {
        MapTile tile = gameBoard.getElement(locX, locY);
        if (tile != null)
        {
            gameBoard.getElement(locX, locY).SetAsOrderable();
        }
    }

    public void RemoveUnit(int locX, int locY)
    {
        MapTile tile = gameBoard.getElement(locX, locY);
        if (tile != null)
        {
            gameBoard.getElement(locX, locY).RemoveUnit();
        }
    }

    public ArrayList<MapTile> GetMoveableUnitsFromTile(Unit.Faction faction)
    {
        ArrayList<MapTile> tiles = new ArrayList<MapTile>();
        for (int x = 0; x < gameBoard.getWidth(); x++)
        {
            for (int y = 0; y < gameBoard.getHeight(); y++)
            {
                MapTile tile = gameBoard.getElement(x, y);
                if (tile.GetUnits() != null && tile.GetFaction() == faction &&
                        tile.GetUnits().get(0).CanMove())
                {
                    tiles.add(tile);
                }
            }
        }
        return tiles;
    }

    public int[][] GetPossibleLocationsForUnits(MapTile tile)
    {
        int[][] possibleLocations = new int[gameBoard.getWidth()][2];
        int moveRange = tile.GetUnits().get(0).moveRange;
        int counter = 0;
        for (int x = 0; x < gameBoard.getWidth(); x++)
        {
            for (int y = 0; y < gameBoard.getHeight(); y++)
            {
                possibleLocations[x][0] = -1;
                possibleLocations[x][1] = -1;

                MapTile possibleTile = gameBoard.getElement(x, y);
                if (possibleTile.GetUnits() == null)
                {

                    double distance = Math.sqrt(Math.pow(Math.abs(possibleTile.getLocationX() - tile.getLocationX()), 2) +
                            Math.pow(Math.abs(possibleTile.getLocationY() - tile.getLocationY()), 2));
                    if (distance <= moveRange)
                    {
                       //Maybe add if blocked check
                        possibleLocations[counter][0] = possibleTile.getLocationX();
                        possibleLocations[counter][1] = possibleTile.getLocationY();
                        counter++;
                    }

                }
            }
        }
        return possibleLocations;
    }

    public GridBoard<MapTile> getBoard()
    {
        return gameBoard;
    }

    BattleloreData getData()
    {
        // Only FM should have access to this for initialisation
        return (BattleloreData)data;
    }

    @Override
    protected List<Component> _getAllComponents()
    {
        return new ArrayList<Component>()
        {{
            add(gameBoard);
        }};
    }


    @Override
    protected AbstractGameState _copy(int playerId)
    {
        BattleloreGameState state = new BattleloreGameState(gameParameters.copy(), 2);
        state.gameBoard = gameBoard.copy();
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
        return 0;
    }

    @Override
    protected void _reset() {
        gameBoard = null;
    }

    @Override
    protected boolean _equals(Object o)
    {
        if (this== o)
        {
            return true;
        }

        if (!(o instanceof BattleloreGameState))
        {
            return false;
        }

        if (!super.equals(o))
        {
            return false;
        }

        BattleloreGameState other = (BattleloreGameState) o;
        return Objects.equals(gameBoard, other.gameBoard);
    }

}
