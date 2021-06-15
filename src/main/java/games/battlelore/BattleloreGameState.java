package games.battlelore;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.turnorders.AlternatingTurnOrder;
import games.GameType;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BattleloreGameState extends AbstractGameState
{
    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param turnOrder      - turn order for this game.
     * @param gameType
     */

    GridBoard<MapTile> gameBoard;

    public BattleloreGameState(AbstractParameters gameParameters, int nPlayers)
    {
        super(gameParameters, new AlternatingTurnOrder(nPlayers) ,GameType.Battlelore);
    }

    public void AddUnit(int locX, int locY, Unit unit)
    {
        MapTile tile = gameBoard.getElement(locX, locY);
        if (tile != null)
        {
            //if()//TODO ADD UNIT
            //gameBoard.getElement(locX, locY).SetUnits();
        }
    }

    public GridBoard<MapTile> getBoard() {
        return gameBoard;
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
        //TODO_ERTUGRUL: Add the heuristic
        return 0;
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
